package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.DialogPane.Dialog;
import components.ActionableFilterComboBox;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import models.AccountPayment;
import models.AccountPaymentContactDataPoint;
import models.EODDataPoint;
import models.User;
import org.controlsfx.control.PopOver;

import java.io.IOException;
import java.sql.*;
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
	@FXML
	private MFXDatePicker invoiceDateField,dueDateField;
	@FXML
	private MFXTextField invoiceNoField,descriptionField,amountField;
	@FXML
	private MFXCheckbox accountAdjustedBox;

	private MFXTableColumn<AccountPayment> contactCol;
	private MFXTableColumn<AccountPayment> invNumberCol;
	private MFXTableColumn<AccountPayment> invDateCol;
	private MFXTableColumn<AccountPayment> dueDateCol;
	private MFXTableColumn<AccountPayment> descriptionCol;
	private MFXTableColumn<AccountPayment> unitAmountCol;
	private MFXTableColumn<AccountPayment> accountAdjustedCol;
	private MFXTableColumn<AccountPaymentContactDataPoint> contactNameCol;
	private MFXTableColumn<AccountPaymentContactDataPoint> totalCol;
	private ActionableFilterComboBox afx;
	private Dialog<Object> dialog;
	
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
		accountTotalsTable.autosizeColumnsOnInitialization();


		MFXButton addContactButton = new MFXButton("Add new Contact");
		addContactButton.setOnAction(actionEvent -> {
			dialog = new Dialog(dialogPane, BLANK);
			dialog.setPadding(false);
			dialog.setContent(createAddNewContactDialog());
			dialogPane.showDialog(dialog);
		});
		afx = new ActionableFilterComboBox(addContactButton);

		afx.setFloatMode(FloatMode.ABOVE);
		afx.setFloatingText("Contact name");
		afx.setFloatingTextGap(5);
		afx.setBorderGap(0);
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
	}

	private Node createAddNewContactDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/AddNewContactDialog.fxml"));
		StackPane newContactDialog = null;
		try {
			newContactDialog = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		AddNewContactDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setConnection(this.con);
		dialogController.setMain(this.main);
		return newContactDialog;
	}

	public void fillContactList(){
		ObservableList<AccountPaymentContactDataPoint> contacts = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM accountPaymentContacts where storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				contacts.add(new AccountPaymentContactDataPoint(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		afx.setItems(contacts);
	}

	public void fillTable(){
		ObservableList<AccountPayment> payments = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM accountPayments JOIN accountPaymentContacts a on a.idaccountPaymentContacts = accountPayments.contactID WHERE accountPayments.storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				payments.add(new AccountPayment(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		accountPaymentTable.setItems(payments);

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
	public Dialog<Object> getDialog() {
		return dialog;
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		fieldText += ", ";
		fieldText += main.getCurrentDate().getYear();
		monthSelectorField.setText(fieldText);
		fillTable();
		fillContactList();
	}

	public void addPayment(){
		AccountPaymentContactDataPoint contact = (AccountPaymentContactDataPoint) afx.getSelectedItem();
		String contactName = contact.getContactName();
		String invoiceNo = invoiceNoField.getText();
		LocalDate invoiceDate = invoiceDateField.getValue();
		LocalDate dueDate = dueDateField.getValue();
		String description = descriptionField.getText();
		Double unitAmount = Double.valueOf(amountField.getText());

		if(contactName.isEmpty() || contactName.isBlank()){
			//TODO: proper verification here
		}else{
			String sql = "INSERT INTO accountPayments(contactID,storeID,invoiceNo,invoiceDate,dueDate,description,unitAmount,accountAdjusted) VALUES(?,?,?,?,?,?,?,?)";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, contact.getContactID());
				preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
				preparedStatement.setString(3, invoiceNo);
				preparedStatement.setDate(4, Date.valueOf(invoiceDate));
				preparedStatement.setDate(5, Date.valueOf(dueDate));
				preparedStatement.setString(6, description);
				preparedStatement.setDouble(7, unitAmount);
				preparedStatement.setBoolean(8, accountAdjustedBox.isSelected());

				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			closePopover();
			fillTable();
			dialogPane.showInformation("Success","Payment was succesfully added");
		}

	}
}
