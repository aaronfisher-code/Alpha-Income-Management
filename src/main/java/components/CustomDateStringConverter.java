package components;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.util.StringConverter;

public class CustomDateStringConverter extends StringConverter<LocalDate> {
    private DateTimeFormatter dateFormatter;

    public CustomDateStringConverter(String pattern) {
        dateFormatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public String toString(LocalDate date) {
        if (date == null) {
            return "";
        }
        return dateFormatter.format(date);
    }

    @Override
    public LocalDate fromString(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString,  DateTimeFormatter.ofPattern("d/M/yyyy"));
        } catch (DateTimeParseException e) {
            try {
                // Append the current year if the user provided only "dd/MM"
                String stringWithYear = dateString + "/" + LocalDate.now().getYear();
                DateTimeFormatter formatterWithYear = DateTimeFormatter.ofPattern("d/M/yyyy");
                return LocalDate.parse(stringWithYear, formatterWithYear);
            } catch (DateTimeParseException ex2) {
                // Handle exception or return null if the input is not valid
                return null;
            }
        }
    }
}
