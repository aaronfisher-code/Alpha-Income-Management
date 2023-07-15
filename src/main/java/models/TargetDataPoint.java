package models;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TargetDataPoint {
    private LocalDate date;
    private String actual;
    private String target1;
    private String target2;

    public TargetDataPoint(LocalDate date, String actual, String target1, String target2) {
        this.date = date;
        this.actual = actual;
        this.target1 = target1;
        this.target2 = target2;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDateString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return formatter.format(date);
    }

    public String getActual() {
        if(actual.equals(""))
            return "";
        return NumberFormat.getCurrencyInstance().format(Double.parseDouble(actual));
    }

    public String getTarget1() {
        return NumberFormat.getCurrencyInstance().format(Double.parseDouble(target1));
    }

    public String getTarget2() {
        return NumberFormat.getCurrencyInstance().format(Double.parseDouble(target2));
    }
}
