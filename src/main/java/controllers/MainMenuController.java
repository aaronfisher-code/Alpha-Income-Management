package controllers;


import application.Main;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.stage.PopupWindow;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class MainMenuController extends Controller {

    @FXML
    private Label userNameLabel,  userLabel;
    @FXML
    private VBox sidebar;
    @FXML
    private Button rosterButton,accountsButton,targetGraphButton;
    @FXML
    private BorderPane contentPane,topPane;
    @FXML
    private HBox controlBox,userNameBox;
    @FXML
    private Button maximize,minimize,close;

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private PopOver currentUserPopover;

    public void setMain(Main main) {
        this.main = main;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }

    public void fill() {
        userNameLabel.setText(main.currentUser.getFirst_name() + " " + main.currentUser.getLast_name());
        userLabel.setText(String.valueOf(main.currentUser.getFirst_name().charAt(0)));
        userLabel.setStyle("-fx-background-color: " + main.currentUser.getBgColour() + ";");
        userLabel.setTextFill(Paint.valueOf(main.currentUser.getTextColour()));
        for(Node b:sidebar.getChildren()){
            if(b.getAccessibleRole() == AccessibleRole.BUTTON){
                Button a = (Button) b;
                a.addEventHandler(MouseEvent.MOUSE_ENTERED,
                        e -> slide(150L, 20, a));

                a.addEventHandler(MouseEvent.MOUSE_EXITED,
                        e -> slide(150L, 15, a));
            }

        };

        this.main.getBs().setMoveControl(topPane);

        close.setOnAction(a -> this.main.getStg().close());

        minimize.setOnAction(a -> this.main.getStg().setIconified(true));

        maximize.setOnAction(a -> this.main.getBs().maximizeStage());


//        aeroSnap.selectedProperty().bindBidirectional(Main.borderlessScene.snapProperty());

        loadTargetGraphs();
    }



    public void extendMenu(){
        changeSize(sidebar,260);
        contentPane.setEffect(new GaussianBlur(5));
        for(Node b:sidebar.getChildren()){
            if(b.getAccessibleRole() == AccessibleRole.TEXT){
                Label a = (Label) b;
                a.setContentDisplay(ContentDisplay.LEFT);
            }else{
                Button a = (Button) b;
                a.setContentDisplay(ContentDisplay.LEFT);
                if(a.getStyle().equals("-fx-background-color: #161D31;")) {
                    a.setStyle("-fx-background-color: #0F60FF;");
                    DropShadow d = new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#0F60FF", 1.0), 10.0, 0.0, 0.0, 0.0);
                    d.setHeight(21);
                    d.setWidth(21);
                    b.setEffect(d);
                }
            }

        };
    }

    public void retractMenu(){
        changeSize(sidebar,80);
        contentPane.setEffect(null);
        for(Node b:sidebar.getChildren()){
            if(b.getAccessibleRole() == AccessibleRole.TEXT){
                Label a = (Label) b;
                a.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }else{
                Button a = (Button) b;
                a.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                if(a.getStyle().equals("-fx-background-color: #0F60FF;")){
                    a.setStyle("-fx-background-color: #161D31;");
                    b.setEffect(null);
                }

            }

        };
    }

    public void changeSize(final VBox pane, double width) {
        Duration cycleDuration = Duration.millis(200);
        Timeline timeline = new Timeline(
                new KeyFrame(cycleDuration,
                        new KeyValue(pane.prefWidthProperty(),width,Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    public void slide(double duration, double targetPadding, Button targetButton){
        Animation animation = new Transition() {
            {
                setCycleDuration(Duration.millis(duration));
            }
            double previousPadding = targetButton.getPadding().getLeft();

            @Override
            protected void interpolate(double progress) {
                double total = targetPadding - previousPadding;
                double current = previousPadding+(progress * total);

                targetButton.setPadding(new Insets(9, 0, 9, current));
            }
        };

        animation.playFromStart();
    }

    public void formatSelected(Button b){
        for(Node c:sidebar.getChildren()){
            if(c.getAccessibleRole() == AccessibleRole.BUTTON){
                Button a = (Button) c;
                a.setEffect(null);
                a.setStyle("-fx-background-color: #283046;");
                a.setTextFill(Color.web("#b4b7bd"));
                SVGPath icon = (SVGPath) a.getGraphic();
                icon.setFill(Color.web("#b4b7bd"));
            }
        };
        DropShadow d = new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#0F60FF",1.0),10.0,0.0,0.0,0.0);
        d.setHeight(21);
        d.setWidth(21);

        if(b.getContentDisplay() == ContentDisplay.LEFT){
            b.setStyle("-fx-background-color: #0F60FF;");
            b.setEffect(d);
        }else{
            b.setStyle("-fx-background-color: #161D31;");
            b.setEffect(null);
        }
        b.setTextFill(Color.WHITE);
        SVGPath icon = (SVGPath) b.getGraphic();
        icon.setFill(Color.WHITE);
    }

    public void showUserMenu(){
        if(currentUserPopover!=null&&currentUserPopover.isShowing()){
            currentUserPopover.hide();
        }else{
            PopOver userMenu = new PopOver();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/UserMenuContent.fxml"));
            VBox userMenuContent = null;
            try {
                userMenuContent = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            UserContentMenuController rdc = loader.getController();
            rdc.setMain(main);
            rdc.setConnection(con);
            rdc.setParent(this);
            rdc.fill();

            userMenu.setOpacity(1);
            userMenu.setContentNode(userMenuContent);
            userMenu.setArrowSize(0);
            userMenu.setAnimated(true);
            userMenu.setArrowLocation(PopOver.ArrowLocation.TOP_RIGHT);
            userMenu.setAutoHide(true);
            userMenu.setDetachable(false);
            userMenu.setHideOnEscape(true);
            userMenu.setCornerRadius(10);
            userMenu.setArrowIndent(-10);
//            userMenu.setY(100);
//            if(currentUserPopover!=null)
//                currentUserPopover.hide();
            currentUserPopover = userMenu;
            userMenu.show(controlBox,
                    main.getStg().getX()+controlBox.getLayoutX()+userNameBox.getWidth(),
                    main.getStg().getY()+controlBox.getLayoutY()+userNameBox.getHeight()+10);
        }


    }

    public void loadRosterPage(){
        formatSelected(rosterButton);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/RosterPage.fxml"));
        StackPane rosterPage = null;
        try {
            rosterPage = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        RosterPageController rpc = loader.getController();
        rpc.setMain(main);
        rpc.setConnection(con);
        rpc.setParent(this);
        rpc.fill();
        contentPane.setCenter(rosterPage);
    }

    public void loadAccountsPage(){
        formatSelected(accountsButton);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/AccountEdit.fxml"));
        StackPane accountPage = null;
        try {
            accountPage = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        EditAccountController ac = loader.getController();
        ac.setMain(main);
        ac.setConnection(con);
        ac.setParent(this);
        ac.fill();
        contentPane.setCenter(accountPage);
    }

    public void loadTargetGraphs(){
        formatSelected(targetGraphButton);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/TargetGraphsPage.fxml"));
        StackPane targetGraphsPage = null;
        try {
            targetGraphsPage = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        TargetGraphsPageController ac = loader.getController();
        ac.setMain(main);
        ac.setConnection(con);
        ac.setParent(this);
        ac.fill();
        contentPane.setCenter(targetGraphsPage);
//        contentPane.setFocusTraversable(false);
    }
}

