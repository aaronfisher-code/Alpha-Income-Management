package controllers;

import java.sql.Connection;

import application.Main;

public abstract class Controller {

	public abstract void setMain(Main main);

	public abstract void fill();
}
