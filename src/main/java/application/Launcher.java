package application;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Starts the application without loading JavaFX until the Linux display backend
 * and JavaFX native-access permissions have been selected. JavaFX 26 uses X11
 * on Linux, so a Wayland session must run it through XWayland. Both settings
 * have to be applied before the JavaFX toolkit starts.
 */
public final class Launcher {
    private static final String RELAUNCH_MARKER = "ALPHA_LAUNCHER_REEXEC";
    private static final String JAVAFX_GRAPHICS_MODULE = "javafx.graphics";

    private Launcher() {
    }

    public static void main(String[] args) {
        boolean needsXWayland = requiresXWaylandRelaunch(
                System.getenv(), System.getProperty("os.name", ""));
        boolean needsJavaFxNativeAccess = requiresJavaFxNativeAccessRelaunch();

        if (needsXWayland || needsJavaFxNativeAccess) {
            if ("1".equals(System.getenv(RELAUNCH_MARKER))) {
                System.err.println("Alpha Income launcher settings could not be applied after restarting.");
                System.exit(1);
            }

            if (needsXWayland && (System.getenv("DISPLAY") == null || System.getenv("DISPLAY").isBlank())) {
                System.err.println("Alpha Income requires XWayland in a Wayland session, but DISPLAY is not set.");
                System.err.println("Install or enable XWayland, then start Alpha Income again.");
                System.exit(1);
            }

            try {
                System.exit(relaunch(args, needsXWayland, needsJavaFxNativeAccess));
            } catch (IOException exception) {
                System.err.println("Unable to restart Alpha Income with its runtime settings: "
                        + exception.getMessage());
                exception.printStackTrace();
                System.exit(1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while starting Alpha Income using XWayland.");
                System.exit(1);
            }
        }

        Main.main(args);
    }

    static boolean requiresXWaylandRelaunch(Map<String, String> environment, String osName) {
        if (!osName.toLowerCase(Locale.ROOT).contains("linux")) {
            return false;
        }

        boolean waylandSession = "wayland".equalsIgnoreCase(environment.get("XDG_SESSION_TYPE"))
                || isSet(environment.get("WAYLAND_DISPLAY"));
        return waylandSession && !preferredBackendIs(environment.get("GDK_BACKEND"), "x11");
    }

    private static boolean requiresJavaFxNativeAccessRelaunch() {
        return ModuleLayer.boot()
                .findModule(JAVAFX_GRAPHICS_MODULE)
                .map(module -> !module.isNativeAccessEnabled())
                .orElse(false);
    }

    private static int relaunch(
            String[] applicationArgs,
            boolean useXWayland,
            boolean enableJavaFxNativeAccess
    ) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        if (enableJavaFxNativeAccess) {
            command.add("--enable-native-access=" + JAVAFX_GRAPHICS_MODULE);
        }
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Launcher.class.getName());
        command.addAll(Arrays.asList(applicationArgs));

        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
        if (useXWayland) {
            processBuilder.environment().put("GDK_BACKEND", "x11");
        }
        processBuilder.environment().put(RELAUNCH_MARKER, "1");
        return processBuilder.start().waitFor();
    }

    private static Path javaExecutable() throws IOException {
        String executableName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe" : "java";
        Path executable = Path.of(System.getProperty("java.home"), "bin", executableName);
        if (!Files.isRegularFile(executable)) {
            throw new IOException("Java executable not found at " + executable);
        }
        return executable;
    }

    private static boolean preferredBackendIs(String configuredBackends, String backend) {
        if (!isSet(configuredBackends)) {
            return false;
        }
        return backend.equalsIgnoreCase(configuredBackends.split("[,;:]", 2)[0].trim());
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
