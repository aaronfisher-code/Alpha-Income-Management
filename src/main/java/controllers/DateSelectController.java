package controllers;

import application.Main;

import java.sql.Connection;
import java.time.LocalDate;

public abstract class DateSelectController extends Controller {

	public abstract void setDate(LocalDate date);
}
