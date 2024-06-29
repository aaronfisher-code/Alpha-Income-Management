package utils;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampPrintStream extends PrintStream {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TimestampPrintStream(OutputStream out) {
        super(out);
    }

    @Override
    public void println(String x) {
        super.println(getTimestamp() + " " + x);
    }

    @Override
    public void println(Object x) {
        super.println(getTimestamp() + " " + x.toString());
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(formatter);
    }
}

