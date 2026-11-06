package utils;

import javafx.application.Platform;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Share one headless toolkit per test JVM; JavaFX cannot be restarted after exit. */
public final class FxTestSupport {
    private static boolean started;
    private FxTestSupport() {}

    public static synchronized void start() throws Exception {
        if (started) return;
        System.setProperty("java.awt.headless", "true");
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
        var ready = new FutureTask<Void>(() -> { Platform.setImplicitExit(false); return null; });
        Platform.startup(ready);
        ready.get(10, TimeUnit.SECONDS);
        started = true;
    }
}
