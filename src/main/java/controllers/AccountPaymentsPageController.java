package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import components.ActionableFilterComboBox;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.skins.MFXPopupSkin;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import models.AccountPayment;
import models.AccountPaymentContactDataPoint;
import models.User;
import org.controlsfx.control.PopOver;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;

import static com.dlsc.gemsfx.DialogPane.Type.*;

public class AccountPaymentsPageController extends DateSelectController{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
    private User selectedUser;
	private MFXDatePicker datePkr;
	private PopOver currentDatePopover;

	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane monthSelector;
	@FXML
	private MFXTextField monthSelectorField;
    @FXML
    private MFXTableView<AccountPayment> accountPaymentTable;
	@FXML
	private MFXTableView<AccountPaymentContactDataPoint> accountTotalsTable;
    @FXML
	private VBox addPaymentPopover;
    @FXML
	private Region contentDarken;
	@FXML
	private DialogPane dialogPane;
	@FXML
	private VBox dataEntryRowPane;
	@FXML
	private MFXFilterComboBox contactNameField;

	private MFXTableColumn<AccountPayment> contactCol;
	private MFXTableColumn<AccountPayment> invNumberCol;
	private MFXTableColumn<AccountPayment> invDateCol;
	private MFXTableColumn<AccountPayment> dueDateCol;
	private MFXTableColumn<AccountPayment> descriptionCol;
	private MFXTableColumn<AccountPayment> unitAmountCol;
	private MFXTableColumn<AccountPayment> accountAdjustedCol;
	private MFXTableColumn<AccountPaymentContactDataPoint> contactNameCol;
	private MFXTableColumn<AccountPaymentContactDataPoint> totalCol;
	
	 @FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(MainMenuController p){this.parent = p;}

	@Override
	public void fill() {

		ObservableList<String> contacts = FXCollections.observableArrayList();
		contacts.add("Test item 1");
		contacts.add("Test item 2");
		contacts.add("Test item 3");
		contacts.add("Test item 1");
		contacts.add("Test item 2");
		contacts.add("Test item 3");
		contacts.add("Test item 1");
		contacts.add("Test item 2");
		contacts.add("Test item 3");
		contacts.add("Test item 1");
		contacts.add("Test item 2");
		contacts.add("Test item 3");
		MFXButton addContactButton = new MFXButton("Add new Contact");
		addContactButton.setOnAction(actionEvent -> {
			dialogPane.showNode(BLANK, "", createGenericNode());
		});
		ActionableFilterComboBox afx = new ActionableFilterComboBox(addContactButton);

		afx.setFloatMode(FloatMode.ABOVE);
		afx.setFloatingText("Contact name");
		afx.setFloatingTextGap(5);
		afx.setBorderGap(0);
		afx.setItems(contacts);
		afx.setStyle("-mfx-gap: 5");
		afx.setMaxWidth(Double.MAX_VALUE);
		afx.setMinHeight(38.4);
		addPaymentPopover.getChildren().add(1,afx);

		//Init Payments Table
		contactCol = new MFXTableColumn<>("CONTACT",false, Comparator.comparing(AccountPayment::getContactName));
		invNumberCol = new MFXTableColumn<>("INVOICE NUMBER",false, Comparator.comparing(AccountPayment::getInvoiceNumber));
		invDateCol = new MFXTableColumn<>("INVOICE DATE",false, Comparator.comparing(AccountPayment::getInvDate));
		dueDateCol = new MFXTableColumn<>("DUE DATE",false, Comparator.comparing(AccountPayment::getDueDate));
		descriptionCol = new MFXTableColumn<>("DESCRIPTION",false, Comparator.comparing(AccountPayment::getDescription));
		unitAmountCol = new MFXTableColumn<>("UNIT AMOUNT",false, Comparator.comparing(AccountPayment::getUnitAmount));
		accountAdjustedCol = new MFXTableColumn<>("ACCOUNT ADJUSTED?",false, Comparator.comparing(AccountPayment::isAccountAdjusted));
		contactCol.setRowCellFactory(accountPayment -> new MFXTableRowCell<>(AccountPayment::getContactName));
		invNumberCol.setRowCellFactory(accountPayment -> new MFXTableRowCell<>(AccountPayment::getInvoiceNumber));
		invDateCol.setRowCellFactory(accountPayment -> new MFXTableRowCell<>(AccountPayment::getInvDate));
		dueDateCol.setRowCellFactory(accountPayment -> new MFXTableRowCell<>(AccountPayment::getDueDate));
		descriptionCol.setRowCellFactory(accountPayment -> new MFXTableRowCell<>(AccountPayment::getDescription));
		unitAmountCol.setRowCellFactory(accountPayment -> new MFXTableRowCell<>(AccountPayment::getUnitAmount));
		accountAdjustedCol.setRowCellFactory(accountPayment -> new MFXTableRowCell<>(AccountPayment::isAccountAdjusted));
		accountPaymentTable.getTableColumns().addAll(
				contactCol,
				invNumberCol,
				invDateCol,
				dueDateCol,
				descriptionCol,
				unitAmountCol,
				accountAdjustedCol
		);
		accountPaymentTable.autosizeColumnsOnInitialization();

		//Init Totals Table
		contactNameCol = new MFXTableColumn<>("CONTACT",false, Comparator.comparing(AccountPaymentContactDataPoint::getContactName));
		totalCol = new MFXTableColumn<>("TOTAL",false, Comparator.comparing(AccountPaymentContactDataPoint::getTotalValue));
		contactNameCol.setRowCellFactory(accountPaymentContactDataPoint -> new MFXTableRowCell<>(AccountPaymentContactDataPoint::getContactName));
		totalCol.setRowCellFactory(accountPaymentContactDataPoint -> new MFXTableRowCell<>(AccountPaymentContactDataPoint::getTotalValue));
		accountTotalsTable.getTableColumns().addAll(
				contactNameCol,
				totalCol
		);
		setDate(LocalDate.now());
		accountTotalsTable.autosizeColumnsOnInitialization();
	}

	private Node createGenericNode() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/AddNewContactDialog.fxml"));
		StackPane newContactDialog = null;
		try {
			newContactDialog = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return newContactDialog;
	}

	public void importFiles(){
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data entry File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		fileChooser.showOpenDialog(main.getStg());
	}

	public void addNewPayment(){
		contentDarken.setVisible(true);
		changeSize(addPaymentPopover,0);
	}

	public void closePopover(){
		changeSize(addPaymentPopover,375);
		contentDarken.setVisible(false);
	}

	public void changeSize(final VBox pane, double width) {
		Duration cycleDuration = Duration.millis(200);
		Timeline timeline = new Timeline(
				new KeyFrame(cycleDuration,
						new KeyValue(pane.translateXProperty(),width, Interpolator.EASE_BOTH))
		);
		timeline.play();
	}

	public void monthForward() {
		setDate(main.getCurrentDate().plusMonths(1));
	}

	public void monthBackward() {
		setDate(main.getCurrentDate().minusMonths(1));
	}

	public void openMonthSelector(){
		if(currentDatePopover!=null&&currentDatePopover.isShowing()){
			currentDatePopover.hide();
		}else {
			PopOver monthSelectorMenu = new PopOver();
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/MonthYearSelectorContent.fxml"));
			VBox monthSelectorMenuContent = null;
			try {
				monthSelectorMenuContent = loader.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
			MonthYearSelectorContentController rdc = loader.getController();
			rdc.setMain(main);
			rdc.setConnection(con);
			rdc.setParent(this);
			rdc.fill();

			monthSelectorMenu.setOpacity(1);
			monthSelectorMenu.setContentNode(monthSelectorMenuContent);
			monthSelectorMenu.setArrowSize(0);
			monthSelectorMenu.setAnimated(true);
			monthSelectorMenu.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
			monthSelectorMenu.setAutoHide(true);
			monthSelectorMenu.setDetachable(false);
			monthSelectorMenu.setHideOnEscape(true);
			monthSelectorMenu.setCornerRadius(10);
			monthSelectorMenu.setArrowIndent(0);
			monthSelectorMenu.show(monthSelector);
			currentDatePopover=monthSelectorMenu;
			monthSelectorField.requestFocus();
		}
	}


	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		fieldText += ", ";
		fieldText += main.getCurrentDate().getYear();
		monthSelectorField.setText(fieldText);
//		fillTable();
	}
}
