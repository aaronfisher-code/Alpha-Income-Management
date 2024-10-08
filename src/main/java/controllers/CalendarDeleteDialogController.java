package controllers;

import io.github.palexdev.materialfx.controls.MFXRadioButton;
import javafx.fxml.FXML;
import models.Shift;
import java.time.LocalDate;

public class CalendarDeleteDialogController {

	@FXML private MFXRadioButton followingButton;
	@FXML private MFXRadioButton allButton;
	private RosterPageController parent;
	private Shift shift;
	private LocalDate shiftCardDate;

	public void setParent(RosterPageController d) {this.parent = d;}

	public void fill(Shift s,LocalDate shiftCardDate) {
		if(s.getShiftStartDate().isEqual(shiftCardDate)||(s.getShiftEndDate()!=null&&s.getShiftEndDate().isEqual(shiftCardDate)))
			followingButton.setVisible(false);
		this.shift = s;
		this.shiftCardDate = shiftCardDate;
	}

	public void deleteShift(){
		if(allButton.isSelected()){
			parent.deleteShift(this.shift);
		}else if(followingButton.isSelected()){
			parent.deleteFutureShifts(this.shift,this.shiftCardDate);
		}else{
			parent.deleteCurrentShift(this.shift,this.shiftCardDate);
		}
		closeDialog();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}
}
