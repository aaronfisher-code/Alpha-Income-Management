package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import models.Shift;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class CalendarEditDialogController{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
	private RosterPageController parent;
	private Shift shift;
	private LocalDate shiftCardDate;

	@FXML
	private MFXRadioButton currentButton,followingButton,allButton;

	@FXML
	private void initialize() throws IOException {}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(RosterPageController d) {this.parent = d;}

	public void fill(Shift s,LocalDate shiftCardDate) {
		if(s.getShiftStartDate().isEqual(shiftCardDate)||(s.getShiftEndDate()!=null&&s.getShiftEndDate().isEqual(shiftCardDate)))
			followingButton.setVisible(false);
		this.shift = s;
		this.shiftCardDate = shiftCardDate;
	}

	public void editShift(){
		if(allButton.isSelected()){
			parent.editShift(this.shift);
		}else if(followingButton.isSelected()){
			parent.editFutureShifts(this.shift,this.shiftCardDate);
		}else{
			parent.editCurrentShift(this.shift,this.shiftCardDate);
		}
		closeDialog();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}


}
