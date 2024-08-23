package controllers;

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import models.Shift;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ShiftCardController extends PageController {

	@FXML private StackPane backgroundPane;
	@FXML private Label employeeIcon,employeeName,employeeRole,startTime,endTime,startAMPM,endAMPM,leaveLabel;
	@FXML private Region blurBG;
	private Shift shift;
	private LocalDate shiftCardDate;

	public ShiftCardController() {}

	public void setShift(Shift newShift) {
		this.shift = newShift;
	}

	public void setDate(LocalDate d){
		this.shiftCardDate = d;
	}

	public LocalDate getDate(){return this.shiftCardDate;}

	@Override
	public void fill() {
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit own shifts") && shift.getUsername().equals(main.getCurrentUser().getUsername()))||
				main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit all shifts"))) {
			backgroundPane.setCursor(Cursor.HAND);
		}else{
			backgroundPane.setCursor(Cursor.DEFAULT);
		}
		employeeName.setText(shift.getFirst_name() + ". " + shift.getLast_name().charAt(0));
		employeeRole.setText(shift.getRole());
		employeeIcon.setText(String.valueOf(shift.getFirst_name().charAt(0)));
		employeeIcon.setStyle("-fx-background-color: " + shift.getProfileBG() + ";-fx-background-radius: 26px;");
		employeeIcon.setTextFill(Paint.valueOf(shift.getProfileText()));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm");
		startTime.setText(shift.getShiftStartTime().format(formatter));
		endTime.setText(shift.getShiftEndTime().format(formatter));
		formatter = DateTimeFormatter.ofPattern("a");
		startAMPM.setText(shift.getShiftStartTime().format(formatter));
		endAMPM.setText(shift.getShiftEndTime().format(formatter));
	}

	public void setModification(String modificationText){
		if(modificationText!=null){
			blurBG.setVisible(true);
			leaveLabel.setText(modificationText);
			leaveLabel.setVisible(true);
		}
	}

	public void showDifference(Shift originalShift, Shift modifiedShift){
		if(!originalShift.getUsername().equals(modifiedShift.getUsername())){
			employeeName.setStyle("-fx-text-fill: RED");
			employeeRole.setStyle("-fx-text-fill: RED");
		}
		if(originalShift.getShiftStartTime()!=modifiedShift.getShiftStartTime()){
			startTime.setStyle("-fx-text-fill: RED");
			startAMPM.setStyle("-fx-text-fill: RED");
		}
		if(originalShift.getShiftEndTime()!=modifiedShift.getShiftEndTime()){
			endTime.setStyle("-fx-text-fill: RED");
			endAMPM.setStyle("-fx-text-fill: RED");
		}

	}

	public void hoverOn(){
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit own shifts") && shift.getUsername().equals(main.getCurrentUser().getUsername()))||
				main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit all shifts"))) {
			DropShadow d = new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#000000", 0.8), 5.56, 0.0, 0.0, 2.0);
			d.setHeight(24);
			d.setWidth(24);
			backgroundPane.setEffect(d);
			slide(100L, -2, backgroundPane);
		}
	}

	public void hoverOff(){
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit own shifts") && shift.getUsername().equals(main.getCurrentUser().getUsername()))||
				main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit all shifts"))) {
			DropShadow d = new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#000000", 0.1), 10, 0.0, 0.0, 4.0);
			d.setHeight(24);
			d.setWidth(24);
			backgroundPane.setEffect(d);
			slide(100L, 0, backgroundPane);
		}
	}

	public void slide(double duration, double targetMargin, StackPane targetButton){
		Animation animation = new Transition() {
			{
				setCycleDuration(Duration.millis(duration));
			}
			final double previousMargin = targetButton.getTranslateY();

			@Override
			protected void interpolate(double progress) {
				double total = targetMargin - previousMargin;
				double current = previousMargin+(progress * total);

				targetButton.setTranslateY(current);
			}
		};
		animation.playFromStart();
	}
}
