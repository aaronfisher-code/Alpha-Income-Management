package controllers;

import application.Main;
import com.dlsc.gemsfx.FilterView;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXNodesList;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.filter.StringFilter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import models.Store;
import models.User;
import models.Employment;
import utils.AnimationUtils;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;

public class EditAccountController extends Controller{

	@FXML
	private StackPane backgroundPane;

	@FXML
	private BorderPane usersButton,storesButton;

	@FXML
	private VBox controlBox,editStorePopover,editUserPopover,userEntryContainer;

	@FXML
	private JFXNodesList addList;

	@FXML
	private JFXButton addButton;

	@FXML
	private Region contentDarken;

	@FXML
	private Button deleteStoreButton,deleteUserButton;

	@FXML
	private MFXTextField storeNameField;

	@FXML
	private Label editStorePopoverTitle,editUserPopoverTitle,employeeIcon,employeeName,employeeRole;

	@FXML
	private MFXRectangleToggleNode inactiveUserToggle;

	@FXML
	private MFXTextField usernameField,firstNameField,lastNameField,roleField;

	@FXML
	private ColorPicker profileTextPicker,profileBackgroundPicker;

	@FXML
	private MFXCheckListView<Store> storeSelector;

	@FXML
	private MFXButton saveStoreButton,passwordResetButton,staffPermissionsButton,saveUserButton;

	private MFXTableView<User> accountsTable = new MFXTableView<User>();
	private MFXTableColumn<User> usernameCol;
	private MFXTableColumn<User> firstNameCol;
	private MFXTableColumn<User> lastNameCol;
	private MFXTableColumn<User> roleCol;
	private FilterView<User> userFilterView = new FilterView<>();

	private MFXTableView<Store> storesTable = new MFXTableView<Store>();
	private MFXTableColumn<Store> storeNameCol;
	private FilterView<Store> storeFilterView = new FilterView<>();
	
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private User selectedUser;

	private ObservableList<User> allUsers = FXCollections.observableArrayList();
	private ObservableList<Store> allStores = FXCollections.observableArrayList();
	
	 @FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	@Override
	public void fill() {usersView();}

	public void usersView(){
		formatTabSelect(usersButton);
		formatTabDeselect(storesButton);
		addButton.setOnAction(e -> openUserPopover());


		userFilterView = new FilterView<>();
		userFilterView.setTitle("Current Users");
		userFilterView.setSubtitle("Double click a user to edit");
		userFilterView.setTextFilterProvider(text -> user -> user.getFirst_name().toLowerCase().contains(text) || user.getLast_name().toLowerCase().contains(text) || user.getRole().toLowerCase().contains(text));
		allUsers = userFilterView.getFilteredItems();
		usernameCol = new MFXTableColumn<>("STAFF ID",false, Comparator.comparing(User::getUsername));
		firstNameCol = new MFXTableColumn<>("FIRST NAME",false, Comparator.comparing(User::getFirst_name));
		lastNameCol = new MFXTableColumn<>("LAST NAME",false, Comparator.comparing(User::getLast_name));
		roleCol = new MFXTableColumn<>("ROLE",false, Comparator.comparing(User::getRole));
		usernameCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getUsername));
		firstNameCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getFirst_name));
		lastNameCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getLast_name));
		roleCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getRole));
		accountsTable = new MFXTableView<User>();
		accountsTable.getTableColumns().addAll(usernameCol,firstNameCol,lastNameCol,roleCol);
		accountsTable.getFilters().addAll(
				new StringFilter<>("Username",User::getUsername),
				new StringFilter<>("First Name",User::getFirst_name),
				new StringFilter<>("Last Name",User::getLast_name),
				new StringFilter<>("Role",User::getRole)
		);

		String sql = null;
		sql = "SELECT * FROM accounts";
		try {
			preparedStatement = con.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				userFilterView.getItems().add(new User(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		accountsTable.setFooterVisible(true);
		accountsTable.autosizeColumnsOnInitialization();
		accountsTable.setMaxWidth(Double.MAX_VALUE);
		accountsTable.setMaxHeight(Double.MAX_VALUE);
		userFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left

		controlBox.getChildren().removeAll(controlBox.getChildren());
		controlBox.getChildren().addAll(userFilterView,accountsTable);

		VBox.setVgrow(accountsTable, Priority.ALWAYS);
		accountsTable.virtualFlowInitializedProperty().addListener((observable, oldValue, newValue) -> {addUserDoubleClickfunction();});
		userFilterView.filteredItemsProperty().addListener((observable, oldValue, newValue) -> {addUserDoubleClickfunction();});
		accountsTable.setItems(allUsers);
	}

	private void addUserDoubleClickfunction(){
		for (Map.Entry<Integer, MFXTableRow<User>> entry:accountsTable.getCells().entrySet()) {
			entry.getValue().setOnMouseClicked(event -> {
				if(event.getClickCount()==2)openUserPopover(entry.getValue().getData());
			});
			for (MFXTableRowCell<User, ?> cell:entry.getValue().getCells()) {
				cell.setOnMouseClicked(event -> {
					if(event.getClickCount()==2){
						MFXTableRow<User> parentRow = (MFXTableRow<User>) cell.getParent();
						openUserPopover(parentRow.getData());
					}
				});
			}
		}
	}

	public void storesView(){
		formatTabSelect(storesButton);
		formatTabDeselect(usersButton);
		addButton.setOnAction(e -> openStorePopover());

		storeFilterView = new FilterView<>();
		storeFilterView.setTitle("Current Stores");
		storeFilterView.setSubtitle("Double click a store to edit");
		storeFilterView.setTextFilterProvider(text -> store -> store.getStoreName().toLowerCase().contains(text));
		allStores = storeFilterView.getFilteredItems();
		storeNameCol = new MFXTableColumn<>("STORE NAME",false, Comparator.comparing(Store::getStoreName));
		storeNameCol.setRowCellFactory(store -> new MFXTableRowCell<>(Store::getStoreName));
		storesTable = new MFXTableView<Store>();
		storesTable.getTableColumns().addAll(storeNameCol);
		storesTable.getFilters().addAll(
				new StringFilter<>("Store Name",Store::getStoreName)
		);

		String sql = null;
		sql = "SELECT * FROM stores";
		try {
			preparedStatement = con.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				storeFilterView.getItems().add(new Store(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		storesTable.setFooterVisible(true);
		storesTable.autosizeColumnsOnInitialization();
		storesTable.setMaxWidth(Double.MAX_VALUE);
		storesTable.setMaxHeight(Double.MAX_VALUE);
		storeFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left

		controlBox.getChildren().removeAll(controlBox.getChildren());
		controlBox.getChildren().addAll(storeFilterView,storesTable);

		VBox.setVgrow(storesTable, Priority.ALWAYS);
		storesTable.virtualFlowInitializedProperty().addListener((observable, oldValue, newValue) -> {addStoreDoubleClickFunction();});
		storeFilterView.filteredItemsProperty().addListener((observable, oldValue, newValue) -> {addStoreDoubleClickFunction();});
		storesTable.setItems(allStores);

	}

	private void addStoreDoubleClickFunction(){
		for (Map.Entry<Integer, MFXTableRow<Store>> entry:storesTable.getCells().entrySet()) {
			entry.getValue().setOnMouseClicked(event -> {
				if(event.getClickCount()==2)openStorePopover(entry.getValue().getData());
			});
			for (MFXTableRowCell<Store, ?> cell:entry.getValue().getCells()) {
				cell.setOnMouseClicked(event -> {
					if(event.getClickCount()==2){
						MFXTableRow<Store> parentRow = (MFXTableRow<Store>) cell.getParent();
						openStorePopover(parentRow.getData());
					}
				});
			}
		}
	}

	public void openUserPopover(){
		editUserPopoverTitle.setText("Add new user");
		inactiveUserToggle.setVisible(false);
		deleteUserButton.setVisible(false);
		inactiveUserToggle.setSelected(false);
		usernameField.setDisable(false);
		usernameField.clear();
		firstNameField.clear();
		lastNameField.clear();
		roleField.clear();
		profileTextPicker.setValue(Color.WHITE);
		profileBackgroundPicker.setValue(Color.WHITE);
		passwordResetButton.setVisible(false);
		employeeName.setText("");
		employeeRole.setText("");
		employeeIcon.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #FFFFFF;");

		storeSelector.getSelectionModel().clearSelection();
		//Refresh Store list for store selector
		allStores = FXCollections.observableArrayList();
		String sql = null;
		sql = "SELECT * FROM stores";
		try {
			preparedStatement = con.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				allStores.add(new Store(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		storeSelector.setItems(allStores);

		//Add live updates to person card preview
		firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
					employeeName.setText(newValue + "." + (lastNameField.getText().isEmpty()?"":lastNameField.getText(0,1)));
					employeeIcon.setText(newValue.isEmpty()?"":newValue.substring(0,1));
				}
		);
		lastNameField.textProperty().addListener((observable, oldValue, newValue) ->
				employeeName.setText(firstNameField.getText() + "." + (newValue.isEmpty()?"":newValue.substring(0,1))));
		roleField.textProperty().addListener((observable, oldValue, newValue) ->
				employeeRole.setText(newValue));
		profileBackgroundPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
					int r = (int)Math.round(newValue.getRed() * 255.0);
					int g = (int)Math.round(newValue.getGreen() * 255.0);
					int b = (int)Math.round(newValue.getBlue() * 255.0);
					String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					r = (int)Math.round(profileTextPicker.getValue().getRed() * 255.0);
					g = (int)Math.round(profileTextPicker.getValue().getGreen() * 255.0);
					b = (int)Math.round(profileTextPicker.getValue().getBlue() * 255.0);
					String profileText = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					employeeIcon.setStyle("-fx-background-color: " + profileBG + ";" + "-fx-text-fill: " + profileText + ";");
				}
		);
		profileTextPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
					int r = (int)Math.round(newValue.getRed() * 255.0);
					int g = (int)Math.round(newValue.getGreen() * 255.0);
					int b = (int)Math.round(newValue.getBlue() * 255.0);
					String profileText = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					r = (int)Math.round(profileBackgroundPicker.getValue().getRed() * 255.0);
					g = (int)Math.round(profileBackgroundPicker.getValue().getGreen() * 255.0);
					b = (int)Math.round(profileBackgroundPicker.getValue().getBlue() * 255.0);
					String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					employeeIcon.setStyle("-fx-background-color: " + profileBG + ";" + "-fx-text-fill: " + profileText + ";");
				}
		);


		contentDarken.setVisible(true);
		contentDarken.setOnMouseClicked(event -> closeUserPopover());
		saveUserButton.setOnAction(event -> addUser());
		AnimationUtils.slideIn(editUserPopover,0);
	}

	public void openUserPopover(User user){
		editUserPopoverTitle.setText("Edit user");
		inactiveUserToggle.setVisible(true);
		deleteUserButton.setVisible(true);
		//TODO change inactive text to reflect current active status
		inactiveUserToggle.setSelected(user.getInactiveDate()!=null);
		usernameField.setText(user.getUsername());
		firstNameField.setText(user.getFirst_name());
		lastNameField.setText(user.getLast_name());
		roleField.setText(user.getRole());
		usernameField.setDisable(true);
		profileTextPicker.setValue(Color.valueOf(user.getTextColour()));
		profileBackgroundPicker.setValue(Color.valueOf(user.getBgColour()));
		passwordResetButton.setVisible(true);
		passwordResetButton.setDisable(false);
		passwordResetButton.setOnAction(actionEvent -> resetPassword(user));
		employeeName.setText(user.getFirst_name() + "." + user.getLast_name().substring(0,1));
		employeeRole.setText(user.getRole());
		employeeIcon.setText(user.getFirst_name().substring(0,1));
		employeeIcon.setStyle("-fx-background-color: " + user.getBgColour()+ "; -fx-text-fill: " + user.getTextColour() + ";");

		storeSelector.getSelectionModel().clearSelection();
		//Refresh Store list for store selector
		allStores = FXCollections.observableArrayList();
		ObservableList<Employment> staffStores = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM stores";
			preparedStatement = con.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				allStores.add(new Store(resultSet));
			}
			storeSelector.setItems(allStores);

			sql = "SELECT * FROM employments WHERE username = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, user.getUsername());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				staffStores.add(new Employment(resultSet));
			}

			for(Store s:allStores){
				for(Employment e:staffStores){
					if(s.getStoreID()==e.getStoreID()){
						storeSelector.getSelectionModel().selectItem(s);
					}
				}
			}

		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}

		//Add live updates to person card preview
		firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
					employeeName.setText(newValue + "." + (lastNameField.getText().isEmpty()?"":lastNameField.getText(0,1)));
					employeeIcon.setText(newValue.isEmpty()?"":newValue.substring(0,1));
				}
		);
		lastNameField.textProperty().addListener((observable, oldValue, newValue) ->
				employeeName.setText(firstNameField.getText() + "." + (newValue.isEmpty()?"":newValue.substring(0,1))));
		roleField.textProperty().addListener((observable, oldValue, newValue) ->
				employeeRole.setText(newValue));
		profileBackgroundPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
					int r = (int)Math.round(newValue.getRed() * 255.0);
					int g = (int)Math.round(newValue.getGreen() * 255.0);
					int b = (int)Math.round(newValue.getBlue() * 255.0);
					String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					r = (int)Math.round(profileTextPicker.getValue().getRed() * 255.0);
					g = (int)Math.round(profileTextPicker.getValue().getGreen() * 255.0);
					b = (int)Math.round(profileTextPicker.getValue().getBlue() * 255.0);
					String profileText = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					employeeIcon.setStyle("-fx-background-color: " + profileBG + ";" + "-fx-text-fill: " + profileText + ";");
				}
		);
		profileTextPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
					int r = (int)Math.round(newValue.getRed() * 255.0);
					int g = (int)Math.round(newValue.getGreen() * 255.0);
					int b = (int)Math.round(newValue.getBlue() * 255.0);
					String profileText = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					r = (int)Math.round(profileBackgroundPicker.getValue().getRed() * 255.0);
					g = (int)Math.round(profileBackgroundPicker.getValue().getGreen() * 255.0);
					b = (int)Math.round(profileBackgroundPicker.getValue().getBlue() * 255.0);
					String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
					employeeIcon.setStyle("-fx-background-color: " + profileBG + ";" + "-fx-text-fill: " + profileText + ";");
				}
		);


		contentDarken.setVisible(true);
		contentDarken.setOnMouseClicked(event -> closeUserPopover());
		saveUserButton.setOnAction(event -> editUser(user));
		deleteUserButton.setOnAction(event -> deleteUser(user));
		deleteUserButton.setVisible(true);
		AnimationUtils.slideIn(editUserPopover,0);
	}

	public void openStorePopover(){
		storeNameField.clear();
		editStorePopoverTitle.setText("Add new store");
		contentDarken.setVisible(true);
		contentDarken.setOnMouseClicked(event -> closeStorePopover());
		saveStoreButton.setOnAction(event -> addStore());
		deleteStoreButton.setVisible(false);
		AnimationUtils.slideIn(editStorePopover,0);
	}

	public void openStorePopover(Store store){
		editStorePopoverTitle.setText("Edit store");
	 	storeNameField.setText(store.getStoreName());
		contentDarken.setVisible(true);
		contentDarken.setOnMouseClicked(event -> closeStorePopover());
		saveStoreButton.setOnAction(event -> editStore(store));
		deleteStoreButton.setOnAction(event -> deleteStore(store));
		deleteStoreButton.setVisible(true);
		AnimationUtils.slideIn(editStorePopover,0);
	}

	public void closeStorePopover(){
		AnimationUtils.slideIn(editStorePopover,425);
		contentDarken.setVisible(false);
	}

	public void closeUserPopover(){
		AnimationUtils.slideIn(editUserPopover,425);
		contentDarken.setVisible(false);
	}

	public void addStore(){
		String name = storeNameField.getText();
		if(name.isEmpty() || name.isBlank()){
			//TODO name field verification
		}else{
			String sql = "INSERT INTO stores(storeName) VALUES(?)";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, name);
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			Dialog<String> dialog = new Dialog<String>();
			dialog.setTitle("Success");
			ButtonType type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
			dialog.setContentText("Store was succesfully added to database");
			dialog.getDialogPane().getButtonTypes().add(type);
			dialog.showAndWait();
			storesView();
		}

	}

	public void addUser(){
		String username = usernameField.getText();
		String fname = firstNameField.getText();
		String lname = lastNameField.getText();
		String role = roleField.getText();
		int r = (int)Math.round(profileTextPicker.getValue().getRed() * 255.0);
		int g = (int)Math.round(profileTextPicker.getValue().getGreen() * 255.0);
		int b = (int)Math.round(profileTextPicker.getValue().getBlue() * 255.0);
		String profileText = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
		r = (int)Math.round(profileBackgroundPicker.getValue().getRed() * 255.0);
		g = (int)Math.round(profileBackgroundPicker.getValue().getGreen() * 255.0);
		b = (int)Math.round(profileBackgroundPicker.getValue().getBlue() * 255.0);
		String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();

		if(fname.isEmpty() || fname.isBlank()){
			//TODO properly implement field verification for user addition
		}else{
			String sql = "INSERT INTO accounts(username,first_name,last_name,role,profileBG,profileText) VALUES(?,?,?,?,?,?)";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, username);
				preparedStatement.setString(2, fname);
				preparedStatement.setString(3, lname);
				preparedStatement.setString(4, role);
				preparedStatement.setString(5, profileBG);
				preparedStatement.setString(6, profileText);
				preparedStatement.executeUpdate();
				for(Store s:storeSelector.getSelectionModel().getSelection().values()){
					sql = "INSERT INTO employments(username,storeID) VALUES(?,?)";
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setString(1, username);
					preparedStatement.setInt(2, s.getStoreID());
					preparedStatement.executeUpdate();
				}
				Dialog<String> dialog = new Dialog<String>();
				dialog.setTitle("Success");
				ButtonType type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
				dialog.setContentText("User was succesfully added to database");
				dialog.getDialogPane().getButtonTypes().add(type);
				dialog.showAndWait();
				usersView();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}

		}


	}

	public void editStore(Store store){
		String name = storeNameField.getText();
		if(name.isEmpty() || name.isBlank()){

		}else{
			String sql = "UPDATE stores SET storeName = ? WHERE storeID = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, name);
				preparedStatement.setInt(2, store.getStoreID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			Dialog<String> dialog = new Dialog<>();
			dialog.setTitle("Success");
			ButtonType type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
			dialog.setContentText("Store was succesfully updated");
			dialog.getDialogPane().getButtonTypes().add(type);
			dialog.showAndWait();
			closeStorePopover();
			storesView();
		}

	}

	public void editUser(User user){
	 	Date inactiveDate = null;
	 	if(inactiveUserToggle.isSelected() && user.getInactiveDate()!=null){
	 		inactiveDate = Date.valueOf(user.getInactiveDate());
		}else if(inactiveUserToggle.isSelected() && user.getInactiveDate()==null){
	 		inactiveDate = Date.valueOf(LocalDate.now());
		}

		String fname = firstNameField.getText();
		String lname = lastNameField.getText();
		String role = roleField.getText();
		int r = (int)Math.round(profileTextPicker.getValue().getRed() * 255.0);
		int g = (int)Math.round(profileTextPicker.getValue().getGreen() * 255.0);
		int b = (int)Math.round(profileTextPicker.getValue().getBlue() * 255.0);
		String profileText = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
		r = (int)Math.round(profileBackgroundPicker.getValue().getRed() * 255.0);
		g = (int)Math.round(profileBackgroundPicker.getValue().getGreen() * 255.0);
		b = (int)Math.round(profileBackgroundPicker.getValue().getBlue() * 255.0);
		String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
		if(fname.isEmpty() || fname.isBlank()){

		}else{
			String sql = "UPDATE accounts SET first_name = ?,last_name = ?,role = ?, profileBG = ?, profileText = ?,inactiveDate = ? WHERE username = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, fname);
				preparedStatement.setString(2, lname);
				preparedStatement.setString(3, role);
				preparedStatement.setString(4, profileBG);
				preparedStatement.setString(5, profileText);
				preparedStatement.setDate(6, inactiveDate);
				preparedStatement.setString(7, user.getUsername());
				preparedStatement.executeUpdate();

				sql = "DELETE from employments WHERE username = ?";
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, user.getUsername());
				preparedStatement.executeUpdate();
				for(Store s:storeSelector.getSelectionModel().getSelection().values()){
					sql = "INSERT INTO employments(username,storeID) VALUES(?,?)";
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setString(1, user.getUsername());
					preparedStatement.setInt(2, s.getStoreID());
					preparedStatement.executeUpdate();
				}
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			Dialog<String> dialog = new Dialog<>();
			dialog.setTitle("Success");
			ButtonType type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
			dialog.setContentText("User was succesfully updated");
			dialog.getDialogPane().getButtonTypes().add(type);
			dialog.showAndWait();
			closeUserPopover();
			usersView();
		}
	}

	public void resetPassword(User user){
		String sql = "UPDATE accounts SET password = ? WHERE username = ?";
		try {
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, null);
			preparedStatement.setString(2, user.getUsername());
			preparedStatement.executeUpdate();
		} catch (SQLException ex) {
			System.err.println(ex.getMessage());
		}
		passwordResetButton.setText("Password reset requested");
		passwordResetButton.setDisable(true);
	}

	public void deleteStore(Store store){
	 	Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Confirm Deletion");
		alert.setContentText("This action will permanently delete the store from all systems, are you sure?");
		ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
		ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
		alert.getButtonTypes().setAll(okButton, noButton);
		alert.showAndWait().ifPresent(type -> {
			if (type == okButton) {
				String sql = "DELETE from stores WHERE storeID = ?";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setInt(1, store.getStoreID());
					preparedStatement.executeUpdate();
				} catch (SQLException ex) {
					System.err.println(ex.getMessage());
				}
				Dialog<String> dialog = new Dialog<>();
				dialog.setTitle("Success");
				type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
				dialog.setContentText("Store was succesfully deleted");
				dialog.getDialogPane().getButtonTypes().add(type);
				dialog.showAndWait();
				closeStorePopover();
				storesView();
			} else if (type == noButton) {
			}
		});
	}

	public void deleteUser(User user){
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Confirm Deletion");
		alert.setContentText("This action will permanently delete the user from all systems,\n" +
							 "if the archived information for this user must be saved, its instead " +
							 "preferred that you mark them as a now inactive user\n\n" +
							 "Are you sure you still want to delete this user?");
		ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
		ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
		alert.getButtonTypes().setAll(okButton, noButton);
		alert.showAndWait().ifPresent(type -> {
			if (type == okButton) {
				String sql = "DELETE from accounts WHERE username = ?";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setString(1, user.getUsername());
					preparedStatement.executeUpdate();
				} catch (SQLException ex) {
					System.err.println(ex.getMessage());
				}
				Dialog<String> dialog = new Dialog<>();
				dialog.setTitle("Success");
				type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
				dialog.setContentText("User was succesfully deleted");
				dialog.getDialogPane().getButtonTypes().add(type);
				dialog.showAndWait();
				closeUserPopover();
				usersView();
			} else if (type == noButton) {
			}
		});
	}

	public void formatTabSelect(BorderPane b){
		for (Node n:b.getChildren()) {
			if(n.getAccessibleRole() == AccessibleRole.TEXT){
				Label a = (Label) n;
				a.setStyle("-fx-text-fill: #0F60FF");
			}
			if(n.getAccessibleRole() == AccessibleRole.PARENT){
				Region a = (Region) n;
				a.setStyle("-fx-background-color: #0F60FF");
			}
		}
	}

	public void formatTabDeselect(BorderPane b){
		for (Node n:b.getChildren()) {
			if(n.getAccessibleRole() == AccessibleRole.TEXT){
				Label a = (Label) n;
				a.setStyle("-fx-text-fill: #6e6b7b");
			}
			if(n.getAccessibleRole() == AccessibleRole.PARENT){
				Region a = (Region) n;
				a.setStyle("");
			}
		}
	}

	public boolean searchAccount(String userquery) {
		String usrname = userquery;
		if(usrname.isEmpty()) {
			return false;
		} else {
			//query
			String sql = "SELECT * FROM accounts Where username = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, usrname);
				resultSet = preparedStatement.executeQuery();
				if (resultSet == null || !resultSet.next()) {
					return false;
				} else {
					return true;
				}
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
				return false;
			}
		}
	}

	
}
