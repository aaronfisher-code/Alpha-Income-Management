package utils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class LogRedirector {

    public static void redirectOutputToFile(String logFilePath) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(logFilePath, true);
            PrintStream printStream = new TimestampPrintStream(fileOutputStream);
            System.setOut(printStream);
            System.setErr(printStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


