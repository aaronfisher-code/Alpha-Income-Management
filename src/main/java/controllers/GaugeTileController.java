package controllers;

import eu.hansolo.medusa.Gauge;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import strategies.GaugeTargetStrategy;
import strategies.LineGraphTargetStrategy;

public class GaugeTileController extends PageController {

    @FXML private StackPane gaugePane;
	private GaugeTargetStrategy strategy;

	@Override
	public void fill() {
	 	gaugePane.getChildren().add(fillGauge());
	}

	public void setStrategy(GaugeTargetStrategy strategy){
		this.strategy = strategy;
	}

	public StackPane fillGauge(){
		StackPane finalPane = new StackPane();
		Gauge g = new Gauge(Gauge.SkinType.SLIM);
		g.setMinValue(strategy.getMinValue());
		g.setMaxValue(strategy.getMaxValue());
		g.setValue(strategy.getActualValue());
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
