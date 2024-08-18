package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
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
import models.Permission;
import models.Store;
import models.User;
import models.Employment;
import services.*;
import utils.AnimationUtils;
import utils.ValidatorUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EditAccountController extends Controller{

	@FXML
	private BorderPane usersButton,storesButton;

	@FXML
	private VBox controlBox,editStorePopover,editUserPopover,editPermissionsPopover;

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
	private Label editStorePopoverTitle,editUserPopoverTitle,employeeIcon,employeeName,employeeRole,storeNameValidationLabel,usernameValidationLabel,firstNameValidationLabel,lastNameValidationLabel;

	@FXML
	private MFXRectangleToggleNode inactiveUserToggle;

	@FXML
	private MFXTextField usernameField,firstNameField,lastNameField,roleField;

	@FXML
	private ColorPicker profileTextPicker,profileBackgroundPicker;

	@FXML
	private MFXCheckListView<Store> storeSelector;

	@FXML
	private MFXCheckListView<Permission> permissionsSelector;

	@FXML
	private MFXButton saveStoreButton,passwordResetButton,saveUserButton;

	@FXML
	private DialogPane dialogPane;

	private MFXTableView<User> accountsTable = new MFXTableView<>();
	private MFXTableColumn<User> usernameCol;
	private MFXTableColumn<User> firstNameCol;
	private MFXTableColumn<User> lastNameCol;
	private MFXTableColumn<User> roleCol;
	private FilterView<User> userFilterView = new FilterView<>();

	private MFXTableView<Store> storesTable = new MFXTableView<>();
	private MFXTableColumn<Store> storeNameCol;
	private FilterView<Store> storeFilterView = new FilterView<>();

    private Main main;

	private UserService userService;
	private StoreService storeService;
	private PermissionService permissionService;
	private EmploymentService employmentService;
	private UserPermissionService userPermissionService;
	
	 @FXML
	private void initialize() {
		 userService = new UserService();
		 storeService = new StoreService();
		 permissionService = new PermissionService();
		 employmentService = new EmploymentService();
		 userPermissionService = new UserPermissionService();
	 }

	@Override
	public void setMain(Main main) {
		this.main = main;
	}

	@Override
	public void fill() {usersView();}

	public void usersView(){
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Users - Edit"))) {
			addList.setVisible(true);
		}else{
			addList.setVisible(false);
		}
		 //Change inactive user text depending on if it's selected or not
		inactiveUserToggle.selectedProperty().addListener(_ -> {
			if (inactiveUserToggle.isSelected()) {
				inactiveUserToggle.setText("Inactive");
			} else {
				inactiveUserToggle.setText("Active");
			}
		});
		formatTabSelect(usersButton);
		formatTabDeselect(storesButton);
		addButton.setOnAction(_ -> openUserPopover());

		userFilterView = new FilterView<>();
		userFilterView.setTitle("Current Users");
		userFilterView.setSubtitle("Double click a user to edit");
		userFilterView.setTextFilterProvider(text -> user -> user.getFirst_name().toLowerCase().contains(text) || user.getLast_name().toLowerCase().contains(text) || user.getRole().toLowerCase().contains(text));
		ObservableList<User> allUsers = userFilterView.getFilteredItems();
		usernameCol = new MFXTableColumn<>("STAFF ID",false, Comparator.comparing(User::getUsername));
		firstNameCol = new MFXTableColumn<>("FIRST NAME",false, Comparator.comparing(User::getFirst_name));
		lastNameCol = new MFXTableColumn<>("LAST NAME",false, Comparator.comparing(User::getLast_name));
		roleCol = new MFXTableColumn<>("ROLE",false, Comparator.comparing(User::getRole));
		usernameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getUsername));
		firstNameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getFirst_name));
		lastNameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getLast_name));
		roleCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getRole));
		accountsTable = new MFXTableView<>();
		accountsTable.getTableColumns().addAll(usernameCol,firstNameCol,lastNameCol,roleCol);
		accountsTable.getFilters().addAll(
				new StringFilter<>("Username",User::getUsername),
				new StringFilter<>("First Name",User::getFirst_name),
				new StringFilter<>("Last Name",User::getLast_name),
				new StringFilter<>("Role",User::getRole)
		);

		try {
			List<User> userList = userService.getAllUsers();
			allUsers = FXCollections.observableArrayList(userList);
			accountsTable.setItems(allUsers);
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while fetching users", ex.getMessage());
			ex.printStackTrace();
		}
		accountsTable.setFooterVisible(true);
		accountsTable.autosizeColumnsOnInitialization();
		accountsTable.setMaxWidth(Double.MAX_VALUE);
		accountsTable.setMaxHeight(Double.MAX_VALUE);
		userFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left

		controlBox.getChildren().removeAll(controlBox.getChildren());
		controlBox.getChildren().addAll(userFilterView,accountsTable);

		VBox.setVgrow(accountsTable, Priority.ALWAYS);
		accountsTable.virtualFlowInitializedProperty().addListener((_, _, _) -> {addUserDoubleClickfunction();});
		userFilterView.filteredItemsProperty().addListener((_, _, _) -> {addUserDoubleClickfunction();});
		accountsTable.setItems(allUsers);

		ValidatorUtils.setupRegexValidation(usernameField,usernameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveUserButton);
		ValidatorUtils.setupRegexValidation(firstNameField,firstNameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveUserButton);
		ValidatorUtils.setupRegexValidation(lastNameField,lastNameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveUserButton);
	}

	private void addUserDoubleClickfunction(){
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Users - Edit"))) {
			for (Map.Entry<Integer, MFXTableRow<User>> entry : accountsTable.getCells().entrySet()) {
				entry.getValue().setOnMouseClicked(event -> {
					if (event.getClickCount() == 2) openUserPopover(entry.getValue().getData());
				});
				if (entry.getValue().getData().getInactiveDate() != null) {
					entry.getValue().getStyleClass().add("disabledMFXTableRow");
				}
				for (MFXTableRowCell<User, ?> cell : entry.getValue().getCells()) {
					cell.setOnMouseClicked(event -> {
						if (event.getClickCount() == 2) {
							MFXTableRow<User> parentRow = (MFXTableRow<User>) cell.getParent();
							openUserPopover(parentRow.getData());
						}
					});
				}
			}
		}
	}

	public void storesView(){
        addList.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Stores - Edit")));
		formatTabSelect(storesButton);
		formatTabDeselect(usersButton);
		addButton.setOnAction(_ -> openStorePopover());

		storeFilterView = new FilterView<>();
		storeFilterView.setTitle("Current Stores");
		storeFilterView.setSubtitle("Double click a store to edit");
		storeFilterView.setTextFilterProvider(text -> store -> store.getStoreName().toLowerCase().contains(text));
		ObservableList<Store> allStores = storeFilterView.getFilteredItems();
		storeNameCol = new MFXTableColumn<>("STORE NAME",false, Comparator.comparing(Store::getStoreName));
		storeNameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(Store::getStoreName));
		storesTable = new MFXTableView<>();
		storesTable.getTableColumns().addAll(storeNameCol);
		storesTable.getFilters().addAll(
				new StringFilter<>("Store Name",Store::getStoreName)
		);

		try {
			List<Store> storeList = storeService.getAllStores();
			allStores = FXCollections.observableArrayList(storeList);
			storesTable.setItems(allStores);
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while fetching stores", ex.getMessage());
			ex.printStackTrace();
		}
		storesTable.setFooterVisible(true);
		storesTable.autosizeColumnsOnInitialization();
		storesTable.setMaxWidth(Double.MAX_VALUE);
		storesTable.setMaxHeight(Double.MAX_VALUE);
		storeFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left

		controlBox.getChildren().removeAll(controlBox.getChildren());
		controlBox.getChildren().addAll(storeFilterView,storesTable);

		VBox.setVgrow(storesTable, Priority.ALWAYS);
		storesTable.virtualFlowInitializedProperty().addListener((_, _, _) -> {addStoreDoubleClickFunction();});
		storeFilterView.filteredItemsProperty().addListener((_, _, _) -> {addStoreDoubleClickFunction();});
		storesTable.setItems(allStores);

		ValidatorUtils.setupRegexValidation(storeNameField,storeNameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveStoreButton);

	}

	private void addStoreDoubleClickFunction(){
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Stores - Edit"))) {
			for (Map.Entry<Integer, MFXTableRow<Store>> entry : storesTable.getCells().entrySet()) {
				entry.getValue().setOnMouseClicked(event -> {
					if (event.getClickCount() == 2) openStorePopover(entry.getValue().getData());
				});
				for (MFXTableRowCell<Store, ?> cell : entry.getValue().getCells()) {
					cell.setOnMouseClicked(event -> {
						if (event.getClickCount() == 2) {
							MFXTableRow<Store> parentRow = (MFXTableRow<Store>) cell.getParent();
							openStorePopover(parentRow.getData());
						}
					});
				}
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
		try {
			List<Store> allStores = storeService.getAllStores();
			storeSelector.setItems(FXCollections.observableArrayList(allStores));
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while fetching stores", ex.getMessage());
			ex.printStackTrace();
		}

		permissionsSelector.getSelectionModel().clearSelection();
		//Refresh permissions list for permissions selector
		try {
			List<Permission> allPermissions = permissionService.getAllPermissions();
			permissionsSelector.setItems(FXCollections.observableArrayList(allPermissions));
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while fetching permissions", ex.getMessage());
			ex.printStackTrace();
		}

		//Add live updates to person card preview
		firstNameField.textProperty().addListener((_, _, newValue) -> {
					employeeName.setText(newValue + "." + (lastNameField.getText().isEmpty()?"":lastNameField.getText(0,1)));
					employeeIcon.setText(newValue.isEmpty()?"":newValue.substring(0,1));
				}
		);
		lastNameField.textProperty().addListener((_, _, newValue) ->
				employeeName.setText(firstNameField.getText() + "." + (newValue.isEmpty()?"":newValue.substring(0,1))));
		roleField.textProperty().addListener((_, _, newValue) ->
				employeeRole.setText(newValue));
		profileBackgroundPicker.valueProperty().addListener((_, _, newValue) -> {
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
		profileTextPicker.valueProperty().addListener((_, _, newValue) -> {
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
		contentDarken.setOnMouseClicked(_ -> closeUserPopover());
		saveUserButton.setOnAction(_ -> addUser());
		AnimationUtils.slideIn(editUserPopover,0);
	}

	public void openUserPopover(User user){
		editUserPopoverTitle.setText("Edit user");
		inactiveUserToggle.setVisible(true);
		deleteUserButton.setVisible(true);
		inactiveUserToggle.setSelected(user.getInactiveDate()!=null);
		usernameField.setText(user.getUsername());
		firstNameField.setText(user.getFirst_name());
		lastNameField.setText(user.getLast_name());
		roleField.setText(user.getRole());
		usernameField.setDisable(true);
		profileTextPicker.setValue(Color.valueOf(user.getTextColour()));
		profileBackgroundPicker.setValue(Color.valueOf(user.getBgColour()));
		passwordResetButton.setVisible(true);

		try {
			if (!userService.isPasswordResetRequested(user.getUsername())) {
				passwordResetButton.setDisable(true);
				passwordResetButton.setText("Password reset requested");
			} else {
				passwordResetButton.setDisable(false);
				passwordResetButton.setText("Request Password Reset");
				passwordResetButton.setOnAction(_ -> resetPassword(user));
			}
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while fetching user information", ex.getMessage());
			ex.printStackTrace();
		}

		employeeName.setText(user.getFirst_name() + "." + user.getLast_name().charAt(0));
		employeeRole.setText(user.getRole());
		employeeIcon.setText(user.getFirst_name().substring(0,1));
		employeeIcon.setStyle("-fx-background-color: " + user.getBgColour()+ "; -fx-text-fill: " + user.getTextColour() + ";");

		storeSelector.getSelectionModel().clearSelection();
		//Refresh Store list for store selector
		try {
			List<Store> allStores = storeService.getAllStores();
			storeSelector.setItems(FXCollections.observableArrayList(allStores));

			List<Employment> staffStores = userService.getEmploymentsForUser(user.getUsername());
			for(Store s:allStores){
				for(Employment e:staffStores){
					if(s.getStoreID()==e.getStoreID()){
						storeSelector.getSelectionModel().selectItem(s);
					}
				}
			}
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while fetching employment information", ex.getMessage());
			ex.printStackTrace();
		}

		permissionsSelector.getSelectionModel().clearSelection();
		//Refresh permissions list for permissions selector
		try {
			List<Permission> allPermissions = permissionService.getAllPermissions();
			permissionsSelector.setItems(FXCollections.observableArrayList(allPermissions));

			List<Permission> userPermissions = userService.getUserPermissions(user.getUsername());
			for(Permission p:userPermissions){
				for(Permission p2:allPermissions){
					if(p.getPermissionID()==p2.getPermissionID()){
						permissionsSelector.getSelectionModel().selectItem(p2);
					}
				}
			}
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while fetching permission information", ex.getMessage());
			ex.printStackTrace();
		}

		//Add live updates to person card preview
		firstNameField.textProperty().addListener((_, _, newValue) -> {
					employeeName.setText(newValue + "." + (lastNameField.getText().isEmpty()?"":lastNameField.getText(0,1)));
					employeeIcon.setText(newValue.isEmpty()?"":newValue.substring(0,1));
				}
		);
		lastNameField.textProperty().addListener((_, _, newValue) ->
				employeeName.setText(firstNameField.getText() + "." + (newValue.isEmpty()?"":newValue.substring(0,1))));
		roleField.textProperty().addListener((_, _, newValue) ->
				employeeRole.setText(newValue));
		profileBackgroundPicker.valueProperty().addListener((_, _, newValue) -> {
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
		profileTextPicker.valueProperty().addListener((_, _, newValue) -> {
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
		contentDarken.setOnMouseClicked(_ -> closeUserPopover());
		saveUserButton.setOnAction(_ -> editUser(user));
		deleteUserButton.setOnAction(_ -> deleteUser(user));
		deleteUserButton.setVisible(true);
		AnimationUtils.slideIn(editUserPopover,0);
	}

	public void openStorePopover(){
		storeNameField.clear();
		editStorePopoverTitle.setText("Add new store");
		contentDarken.setVisible(true);
		contentDarken.setOnMouseClicked(_ -> closeStorePopover());
		saveStoreButton.setOnAction(_ -> addStore());
		deleteStoreButton.setVisible(false);
		AnimationUtils.slideIn(editStorePopover,0);
	}

	public void openPermissionsPopover(){
		contentDarken.setVisible(true);
		contentDarken.setOnMouseClicked(_ -> closePermissionsPopover());
		AnimationUtils.slideIn(editPermissionsPopover,0);
	}

	public void closePermissionsPopover(){
		AnimationUtils.slideIn(editPermissionsPopover,425);
		contentDarken.setOnMouseClicked(_ -> closeUserPopover());
		storeNameValidationLabel.setVisible(false);
	}

	public void openStorePopover(Store store){
		editStorePopoverTitle.setText("Edit store");
	 	storeNameField.setText(store.getStoreName());
		contentDarken.setVisible(true);
		contentDarken.setOnMouseClicked(_ -> closeStorePopover());
		saveStoreButton.setOnAction(_ -> editStore(store));
		deleteStoreButton.setOnAction(_ -> deleteStore(store));
		deleteStoreButton.setVisible(true);
		AnimationUtils.slideIn(editStorePopover,0);
	}

	public void closeStorePopover(){
		AnimationUtils.slideIn(editStorePopover,425);
		contentDarken.setVisible(false);
		storeNameValidationLabel.setVisible(false);
	}

	public void closeUserPopover(){
		AnimationUtils.slideIn(editUserPopover,425);
		contentDarken.setVisible(false);
		usernameValidationLabel.setVisible(false);
		firstNameValidationLabel.setVisible(false);
		lastNameValidationLabel.setVisible(false);
	}

	public void addStore(){
		String name = storeNameField.getText();
		if(!storeNameField.isValid()) {
			storeNameField.requestFocus();
		}else{
			try {
				Store store = new Store(name);
				storeService.addStore(store);
			} catch (SQLException ex) {
				dialogPane.showError("Error","An error occurred while adding a store", ex.getMessage());
				ex.printStackTrace();
			}
			dialogPane.showInformation("Success","Store was successfully added to the database");
			storesView();
		}
	}

	public void addUser() {
		String username = usernameField.getText();
		String fname = firstNameField.getText();
		String lname = lastNameField.getText();
		String role = roleField.getText();
		int r = (int) Math.round(profileTextPicker.getValue().getRed() * 255.0);
		int g = (int) Math.round(profileTextPicker.getValue().getGreen() * 255.0);
		int b = (int) Math.round(profileTextPicker.getValue().getBlue() * 255.0);
		String profileText = String.format("#%02x%02x%02x", r, g, b).toUpperCase();
		r = (int) Math.round(profileBackgroundPicker.getValue().getRed() * 255.0);
		g = (int) Math.round(profileBackgroundPicker.getValue().getGreen() * 255.0);
		b = (int) Math.round(profileBackgroundPicker.getValue().getBlue() * 255.0);
		String profileBG = String.format("#%02x%02x%02x", r, g, b).toUpperCase();
		try {
			if (!firstNameField.isValid()) {
				firstNameField.requestFocus();
			} else if (!lastNameField.isValid()) {
				lastNameField.requestFocus();
			} else if (userService.getUserByUsername(username)!=null) {
				dialogPane.showError("Error", "Username already exists, Please choose a different username");
			} else if (storeSelector.getSelectionModel().getSelection().isEmpty()) {
				dialogPane.showError("Error", "No store selected, Please select at least one store for the user to work at");
			}else{
				User newUser = new User();
				newUser.setUsername(username);
				newUser.setFirst_name(fname);
				newUser.setLast_name(lname);
				newUser.setRole(role);
				newUser.setBgColour(profileBG);
				newUser.setTextColour(profileText);
				userService.addUser(newUser);
				for (Store s : storeSelector.getSelectionModel().getSelection().values()) {
					employmentService.addEmployment(newUser, s);
				}
				for (Permission p : permissionsSelector.getSelectionModel().getSelection().values()) {
					userPermissionService.addUserPermission(newUser, p);
				}
				dialogPane.showInformation("Success", "User was successfully added to the database");
				usersView();
			}
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while adding a user", ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void editStore(Store store){
		String name = storeNameField.getText();
		if(!storeNameField.isValid()) {
			storeNameField.requestFocus();
		}else{
			try {
				store.setStoreName(name);
				storeService.updateStore(store);
			} catch (SQLException ex) {
				dialogPane.showError("Error","An error occurred while updating the store", ex.getMessage());
				ex.printStackTrace();
			}
			dialogPane.showInformation("Success","Store was succesfully updated");
			closeStorePopover();
			storesView();
		}

	}

	public void editUser(User user){
	 	LocalDate inactiveDate = null;
	 	if(inactiveUserToggle.isSelected() && user.getInactiveDate()!=null){
	 		inactiveDate = user.getInactiveDate();
		}else if(inactiveUserToggle.isSelected() && user.getInactiveDate()==null){
	 		inactiveDate = LocalDate.now();
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
		if(!firstNameField.isValid()) {
			firstNameField.requestFocus();
		}else if(!lastNameField.isValid()) {
			lastNameField.requestFocus();
		} else if (storeSelector.getSelectionModel().getSelection().isEmpty()) {
			dialogPane.showError("Error", "No store selected, Please select at least one store for the user to work at");
		}else{
			try {
				user.setFirst_name(fname);
				user.setLast_name(lname);
				user.setRole(role);
				user.setBgColour(profileBG);
				user.setTextColour(profileText);
				user.setInactiveDate(inactiveDate);
				userService.updateUser(user);
				employmentService.deleteEmploymentsForUser(user);
				for(Store s:storeSelector.getSelectionModel().getSelection().values()){
					employmentService.addEmployment(user,s);
				}
				userPermissionService.deletePermissionsForUser(user);
				for(Permission p:permissionsSelector.getSelectionModel().getSelection().values()){
					userPermissionService.addUserPermission(user,p);
				}
			} catch (SQLException ex) {
				dialogPane.showError("Error","An error occurred while updating the user", ex.getMessage());
				ex.printStackTrace();
			}
			dialogPane.showInformation("Success","User was succesfully updated");
			closeUserPopover();
			usersView();
		}
	}

	public void resetPassword(User user){
		try{
			userService.resetUserPassword(user);
		}catch(SQLException ex){
			dialogPane.showError("Error","An error occurred while updating user password", ex.getMessage());
			ex.printStackTrace();
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
				try {
					storeService.deleteStore(store);
				} catch (SQLException ex) {
					dialogPane.showError("Error","An error occurred while deleting store", ex.getMessage());
					ex.printStackTrace();
				}
				dialogPane.showInformation("Success","Store was succesfully deleted");
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
				try {
					userService.deleteUser(user);
				} catch (SQLException ex) {
					dialogPane.showError("Error","An error occurred while deleting user", ex.getMessage());
					ex.printStackTrace();
				}
				dialogPane.showInformation("Success","User was succesfully deleted");
				closeUserPopover();
				usersView();
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
}
