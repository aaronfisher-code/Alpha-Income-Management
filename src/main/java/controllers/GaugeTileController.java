package controllers;

import eu.hansolo.medusa.Gauge;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class GaugeTileController extends PageController {

    @FXML private StackPane gaugePane;

	@Override
	public void fill() {
	 	gaugePane.getChildren().add(fillGauge());
	}

	public StackPane fillGauge(){
		StackPane finalPane = new StackPane();
		Gauge g = new Gauge(Gauge.SkinType.SLIM);
		g.setMinValue(0);
		g.setMaxValue(5);
		g.setValue(4.38);
		g.setAnimated(true);
		g.setBarColor(Color.web("#0F60FF"));
		g.setBarBackgroundColor(Color.web("#F3F2F7"));
		g.setValueColor(Color.web("#6e6b7b"));
		g.setAngleRange(90);
		g.setStartAngle(40);
		finalPane.getChildren().add(g);
		finalPane.setFocusTraversable(false);
		return finalPane;
	}
}
