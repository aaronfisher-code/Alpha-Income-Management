package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import models.AccountPayment;
import models.AccountPayment;
import models.AccountPaymentContactDataPoint;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;

public class AccountPaymentsPageController extends Controller{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
    private User selectedUser;

	private MFXDatePicker datePkr;

	@FXML
	private FlowPane datePickerPane;
    @FXML
    private MFXTableView<AccountPayment> accountPaymentTable;
	@FXML
	private MFXTableView<AccountPaymentContactDataPoint> accountTotalsTable;
    @FXML
	private VBox addPaymentPopover;


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
	private VBox dataEntryRowPane;
	
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

		datePkr = new MFXDatePicker();
//		datePkr.setOnAction(e -> updatePage());
		datePickerPane.getChildren().add(1,datePkr);
		datePkr.setValue(LocalDate.now());
		datePkr.setText(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
		datePkr.getStylesheets().add("/views/CSS/RosterPage.css");

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
		accountTotalsTable.autosizeColumnsOnInitialization();



	}

	public void importFiles(){
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data entry File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		fileChooser.showOpenDialog(main.getStg());
	}

	public void weekForward() {
		setDatePkr(datePkr.getValue().plusMonths(1));
	}

	public void weekBackward() {
		setDatePkr(datePkr.getValue().minusMonths(1));
	}

	public void setDatePkr(LocalDate date) {
		datePkr.setValue(date);
	}

	public void addNewPayment(){
		addPaymentPopover.setEffect(new DropShadow());
		changeSize(addPaymentPopover,0);
	}

	public void closePopover(){
		changeSize(addPaymentPopover,375);
		addPaymentPopover.setEffect(null);
	}

	public void changeSize(final VBox pane, double width) {
		Duration cycleDuration = Duration.millis(200);
		Timeline timeline = new Timeline(
				new KeyFrame(cycleDuration,
						new KeyValue(pane.translateXProperty(),width, Interpolator.EASE_BOTH))
		);
		timeline.play();
	}
	
}
