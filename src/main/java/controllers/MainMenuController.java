package controllers;


import application.Main;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import models.Store;
import org.controlsfx.control.PopOver;
import services.UserService;
import utils.AnimationUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class MainMenuController extends PageController {

    @FXML private Label userNameLabel,  userLabel, logoLabel;
    @FXML private VBox sidebar,buttonPane;
    @FXML private Button targetGraphButton,eodDataEntryButton,accountPaymentsButton,rosterButton,accountsButton,
            invoiceTrackingButton,basCheckerButton,budgetExpensesButton,monthlySummaryButton,settingsButton;
    @FXML private BorderPane contentPane,topPane;
    @FXML private HBox controlBox,userNameBox,windowControls;
    @FXML private Button maximize,minimize,close;
    @FXML private MFXFilterComboBox storeSearchCombo;
    @FXML private MFXScrollPane sidebarScroll;
    @FXML private Region contentDarken;
    private PopOver currentUserPopover;
    private PageController currentPageController;
    private UserService userService;

    @FXML
    private void initialize() {
        try{
            userService = new UserService();
        }catch (IOException ex){
            dialogPane.showError("Failed to initialize user service", ex);
        }
    }

    public void fill() {
        userNameLabel.setText(main.getCurrentUser().getFirst_name() + " " + main.getCurrentUser().getLast_name());
        userLabel.setText(String.valueOf(main.getCurrentUser().getFirst_name().charAt(0)));
        userLabel.setStyle("-fx-background-color: " + main.getCurrentUser().getBgColour() + ";");
        userLabel.setTextFill(Paint.valueOf(main.getCurrentUser().getTextColour()));
        try {
            for (Store store : userService.getStoresForUser(main.getCurrentUser().getUsername())) {
                storeSearchCombo.getItems().add(store);
            }
        } catch (Exception ex) {
            dialogPane.showError("Error loading stores", ex);
        }
        storeSearchCombo.setOnAction(_ -> main.setCurrentStore((Store) storeSearchCombo.getSelectedItem()));
        storeSearchCombo.selectFirst();
        Map<Button, String> buttonPermissions = new HashMap<>();
        buttonPermissions.put(eodDataEntryButton, "EOD - View");
        buttonPermissions.put(accountPaymentsButton, "Account Payments - View");
        buttonPermissions.put(rosterButton, "Roster - View");
        buttonPermissions.put(accountsButton, "Manage Employees - View");
        buttonPermissions.put(invoiceTrackingButton, "Invoicing - View");
        buttonPermissions.put(basCheckerButton, "BAS - View");
        buttonPermissions.put(budgetExpensesButton, "Budget - View");
        buttonPermissions.put(monthlySummaryButton, "Monthly Summary - View");
        buttonPermissions.forEach((button, _) -> button.setDisable(true));
        main.getCurrentUser().getPermissions().forEach(p -> {
            buttonPermissions.forEach((button, permission) -> {
                if (p.getPermissionName().equals(permission)) {
                    button.setDisable(false);
                }
            });
        });
        for(Node b:buttonPane.getChildren()){
            if(b.getAccessibleRole() == AccessibleRole.BUTTON){
                Button a = (Button) b;
                a.addEventHandler(MouseEvent.MOUSE_ENTERED,
                        _ -> slide(150L, 20, a));

                a.addEventHandler(MouseEvent.MOUSE_EXITED,
                        _ -> slide(150L, 15, a));
            }

        }
        this.main.getBs().setMoveControl(topPane);
        close.setOnAction(_ -> this.main.getStg().close());
        close.setOnMouseEntered(_ -> colourWindowButton(close,"#c42b1c","#FFFFFF"));
        close.setOnMouseExited(_ -> colourWindowButton(close,"#FFFFFF","#000000"));
        minimize.setOnAction(_ -> this.main.getStg().setIconified(true));
        minimize.setOnMouseEntered(_ -> colourWindowButton(minimize,"#f5f5f5","#000000"));
        minimize.setOnMouseExited(_ -> colourWindowButton(minimize,"#FFFFFF","#000000"));
        maximize.setOnAction(_ -> this.main.getBs().maximizeStage());
        maximize.setOnMouseEntered(_ -> colourWindowButton(maximize,"#f5f5f5","#000000"));
        maximize.setOnMouseExited(_ -> colourWindowButton(maximize,"#FFFFFF","#000000"));
        this.main.getBs().maximizedProperty().addListener(_ ->
        {
            if(this.main.getBs().isMaximized()){
                SVGPath newIcon = (SVGPath) maximize.getGraphic();
                newIcon.setContent("M13 0H6a2 2 0 0 0-2 2 2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2 2 2 0 0 0 2-2V2a2 2 0 0 0-2-2zm0 13V4a2 2 0 0 0-2-2H5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1zM3 4a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4z\n");
                newIcon.setScaleX(0.95);
                newIcon.setScaleY(0.75);
                windowControls.setPrefHeight(25);
            }else{
                SVGPath newIcon = (SVGPath) maximize.getGraphic();
                newIcon.setContent("M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h12zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2H2z\n");
                newIcon.setScaleX(0.575);
                newIcon.setScaleY(0.575);
                windowControls.setPrefHeight(30);
            }
        });
        loadTargetGraphs();
    }

    public void extendMenu(){
        AnimationUtils.changeSize(sidebar,260);
        contentDarken.setVisible(true);
        logoLabel.setContentDisplay(ContentDisplay.LEFT);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        for(Node n:buttonPane.getChildren()) {
            Button a = (Button) n;
            a.setContentDisplay(ContentDisplay.LEFT);
            if (a.getStyle().equals("-fx-background-color: #161D31;")) {
                a.setStyle("-fx-background-color: #0F60FF;");
                DropShadow d = new DropShadow(BlurType.THREE_PASS_BOX, Color.web("#0F60FF", 1.0), 10.0, 0.0, 0.0, 0.0);
                d.setHeight(21);
                d.setWidth(21);
                a.setEffect(d);
            }
        };
    }

    public void retractMenu(){
        AnimationUtils.changeSize(sidebar,80);
        contentDarken.setVisible(false);
        logoLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        for(Node n:buttonPane.getChildren()) {
            Button a = (Button) n;
            a.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            if(a.getStyle().equals("-fx-background-color: #0F60FF;")){
                a.setStyle("-fx-background-color: #161D31;");
                a.setEffect(null);
            }
        };
    }

    public void slide(double duration, double targetPadding, Button targetButton){
        Animation animation = new Transition() {
            {
                setCycleDuration(Duration.millis(duration));
            }
            final double previousPadding = targetButton.getPadding().getLeft();

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
        for(Node c:buttonPane.getChildren()){
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
                dialogPane.showError("Failed to open user menu", e);
            }
            UserContentMenuController rdc = loader.getController();
            rdc.setMain(main);
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
            currentUserPopover = userMenu;
            userMenu.show(controlBox,
                    main.getStg().getX()+controlBox.getLayoutX()+userNameBox.getWidth()+storeSearchCombo.getWidth()+10,
                    main.getStg().getY()+controlBox.getLayoutY()+userNameBox.getHeight()+10);
        }
    }

    private void colourWindowButton(Button b, String backgroundHex, String strokeHex){
        b.setStyle("-fx-background-color: "+ backgroundHex+ ";-fx-background-radius:0");
        SVGPath icon = (SVGPath) b.getGraphic();
        icon.setFill(Paint.valueOf(strokeHex));
        icon.setStroke(Paint.valueOf(strokeHex));
    }

    //Page Routing
    public void loadTargetGraphs(){changePage(targetGraphButton,"/views/FXML/TargetGraphsPage.fxml");}
    public void loadEODDataEntry(){changePage(eodDataEntryButton,"/views/FXML/EODDataEntryPage.fxml");}
    public void loadAccountPayments(){changePage(accountPaymentsButton,"/views/FXML/AccountPaymentsPage.fxml");}
    public void loadRosterPage(){changePage(rosterButton,"/views/FXML/RosterPage.fxml");}
    public void loadAccountsPage(){changePage(accountsButton,"/views/FXML/AccountEdit.fxml");}
    public void loadInvoiceTracking(){changePage(invoiceTrackingButton,"/views/FXML/InvoiceEntry.fxml");}
    public void loadBASChecker(){changePage(basCheckerButton,"/views/FXML/BASCheckerPage.fxml");}
    public void loadBudgetExpenses(){changePage(budgetExpensesButton,"/views/FXML/BudgetAndExpensesPage.fxml");}
    public void loadMonthlySummary(){changePage(monthlySummaryButton,"/views/FXML/MonthlySummary.fxml");}
    public void loadSettings(){changePage(settingsButton,"/views/FXML/SettingsPage.fxml");}

    public void changePage(Button b, String fxml){
        storeSearchCombo.setOnAction(_ -> {
            main.setCurrentStore((Store) storeSearchCombo.getSelectedItem());
            changePage(b,fxml);
        });
        formatSelected(b);
        updatePageFXML(fxml);
    }

    public void changePage(String fxml){
        storeSearchCombo.setOnAction(_ -> {
            main.setCurrentStore((Store) storeSearchCombo.getSelectedItem());
            changePage(fxml);
        });
        updatePageFXML(fxml);
    }

    private void updatePageFXML(String fxml) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        StackPane pageContent = null;
        try {
            pageContent = loader.load();
        } catch (IOException e) {
            dialogPane.showError("Failed to load page", e);
        }
        currentPageController = loader.getController();
        currentPageController.setMain(main);
        main.setDialogPane(currentPageController.getDialogPane());
        Platform.runLater(() -> {
            Thread.currentThread().setUncaughtExceptionHandler(main::handleGlobalException);
        });
        contentPane.setCenter(pageContent);
        currentPageController.fill();
    }
}

