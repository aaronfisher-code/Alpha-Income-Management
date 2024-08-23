package controllers;

import application.Main;

public abstract class Controller{
	protected Main main;
	public void setMain(Main main) {
		this.main = main;
	}
}
