package controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.scene.control.ToggleButton;
import javafx.fxml.FXML;
import java.time.LocalTime;

public class TimePickerContentController extends PageController {

    @FXML private ToggleButton amSelect,pmSelect;
    @FXML private MFXTextField hourField,minuteField;
    private LocalTime currentTime;

    @Override
    public void fill() {
        hourField.textProperty().addListener((_, oldValue, newValue) -> {
            if (!newValue.matches("\\d+") || Integer.parseInt(newValue) > 12) {
                hourField.setText(oldValue);
            }
        });
        minuteField.textProperty().addListener((_, oldValue, newValue) -> {
            if (!newValue.matches("\\d+") || Integer.parseInt(newValue) > 59) {
                minuteField.setText(oldValue);
            }
        });
        amSelect.setOnAction(_ -> {if(!amSelect.isSelected()){amSelect.setSelected(true);}});
        pmSelect.setOnAction(_ -> {if(!pmSelect.isSelected()){pmSelect.setSelected(true);}});
    }

    public LocalTime getCurrentTime() {return currentTime;}

    public void setCurrentTime(LocalTime currentTime) {
        this.currentTime = currentTime;
        int hour = currentTime.getHour()%12;
        hour = hour==0?12:hour;
        hourField.setText(String.valueOf(hour));
        minuteField.setText(String.format("%02d", currentTime.getMinute()));
        if(currentTime.getHour() >= 12){
            pmSelect.setSelected(true);
            amSelect.setSelected(false);
        }else{
            pmSelect.setSelected(false);
            amSelect.setSelected(true);
        }
    }

    public String getTimeString(){
        String output = "";
        int hour = getCurrentTime().getHour()%12;
        int minute = getCurrentTime().getMinute();
        String amPm = (getCurrentTime().getHour()>=12)?"PM":"AM";
        hour = hour==0?12:hour;
        output = hour+":"+String.format("%02d", minute)+" " + amPm;
        return output;
    }

    public void updateTime(){
        int hour = Integer.parseInt(hourField.getText());
        int min = Integer.parseInt(minuteField.getText());
        if(pmSelect.isSelected()){
            if(hour!=12) {hour+=12;}
        } else{
            if(hour==12) {hour = 0;}
        }

        setCurrentTime(LocalTime.of(hour,min,0));
    }
}
