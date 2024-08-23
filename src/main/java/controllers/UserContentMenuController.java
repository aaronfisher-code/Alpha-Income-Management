package controllers;

import javafx.fxml.FXML;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class UserContentMenuController extends Controller {

    @FXML private VBox buttonContainer;

    public void fill() {
        for(Node b:buttonContainer.getChildren()){
            if(b.getAccessibleRole() == AccessibleRole.BUTTON){
                Button a = (Button) b;
                a.addEventHandler(MouseEvent.MOUSE_ENTERED,
                        _ -> hoverSelect(a));
                a.addEventHandler(MouseEvent.MOUSE_EXITED,
                        _ -> hoverDeSelect(a));
            }
        };
    }

    // On main page, go back to login page when user logout
    public void logOut() throws IOException {
        main.changeScene("/views/FXML/LogIn.fxml");
    }

    public void hoverSelect(Button i){
        i.setStyle("-fx-background-color: #E5EEFF;-fx-text-fill: #0F60FF");
        i.getGraphic().setStyle("-fx-fill: #0F60FF");
    }

    public void hoverDeSelect(Button i){
        i.setStyle("-fx-background-color: #FFFFFF;-fx-text-fill: #6f6c7b");
        i.getGraphic().setStyle("-fx-fill: #6f6c7b");
    }
}
