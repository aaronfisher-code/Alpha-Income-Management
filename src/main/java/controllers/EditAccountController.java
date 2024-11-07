package controllers;

import com.dlsc.gemsfx.FilterView;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXNodesList;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.filter.StringFilter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import models.Permission;
import models.Store;
import models.User;
import models.Employment;
import services.*;
import utils.AnimationUtils;
import utils.GUIUtils;
import utils.ValidatorUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

public class EditAccountController extends PageController {

	@FXML private BorderPane usersButton,storesButton;
	@FXML private VBox controlBox,editStorePopover,editUserPopover,editPermissionsPopover;
	@FXML private JFXNodesList addList;
	@FXML private JFXButton addButton;
	@FXML private Region contentDarken;
	@FXML private Button deleteStoreButton,deleteUserButton;
	@FXML private MFXTextField storeNameField,storeHoursField;
	@FXML private Label editStorePopoverTitle,editUserPopoverTitle,employeeIcon,employeeName,employeeRole,storeNameValidationLabel,usernameValidationLabel,firstNameValidationLabel,lastNameValidationLabel,storeHoursValidationLabel;
	@FXML private MFXRectangleToggleNode inactiveUserToggle;
	@FXML private MFXTextField usernameField,firstNameField,lastNameField,roleField;
	@FXML private ColorPicker profileTextPicker,profileBackgroundPicker;
	@FXML private MFXCheckListView<Store> storeSelector;
	@FXML private MFXCheckListView<Permission> permissionsSelector;
	@FXML private MFXButton saveStoreButton,passwordResetButton,saveUserButton;
	@FXML private MFXProgressSpinner userSpinner, storeSpinner;
	private MFXTableView<User> accountsTable = new MFXTableView<>();
	private MFXTableColumn<User> userIDCol;
	private MFXTableColumn<User> usernameCol;
	private MFXTableColumn<User> firstNameCol;
	private MFXTableColumn<User> lastNameCol;
	private MFXTableColumn<User> roleCol;
	private FilterView<User> userFilterView = new FilterView<>();
	private MFXTableView<Store> storesTable = new MFXTableView<>();
	private MFXTableColumn<Store> storeNameCol;
	private MFXTableColumn<Store> storeHoursCol;
	private FilterView<Store> storeFilterView = new FilterView<>();
	private UserService userService;
	private StoreService storeService;
	private PermissionService permissionService;
	private EmploymentService employmentService;
	private UserPermissionService userPermissionService;
	
	 @FXML
	private void initialize() {
		 try {
			 userService = new UserService();
			 storeService = new StoreService();
			 permissionService = new PermissionService();
			 employmentService = new EmploymentService();
			 userPermissionService = new UserPermissionService();
			 executor = Executors.newCachedThreadPool();
		 } catch (IOException ex) {
			 dialogPane.showError("Error","An error occurred while initializing the services", ex);
		 }
	 }

	@Override
	public void fill() {usersView();}

	public void usersView(){
        addList.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Users - Edit")));
		 //Change inactive user text depending on if it's selected or not
		inactiveUserToggle.selectedProperty().addListener(_ -> {
			if (inactiveUserToggle.isSelected()) {
				inactiveUserToggle.setText("Inactive");
			} else {
				inactiveUserToggle.setText("Active");
			}
		});
		GUIUtils.formatTabSelect(usersButton);
		GUIUtils.formatTabDeselect(storesButton);
		addButton.setOnAction(_ -> openUserPopover());
		userFilterView = new FilterView<>();
		userFilterView.setTitle("Current Users");
		userFilterView.setSubtitle("Double click a user to edit");
		userFilterView.setTextFilterProvider(text -> user -> user.getFirst_name().toLowerCase().contains(text) || user.getLast_name().toLowerCase().contains(text) || user.getRole().toLowerCase().contains(text));
		userIDCol = new MFXTableColumn<>("STAFF ID",false, Comparator.comparing(User::getUserID));
		usernameCol = new MFXTableColumn<>("USERNAME",false, Comparator.comparing(User::getUsername));
		firstNameCol = new MFXTableColumn<>("FIRST NAME",false, Comparator.comparing(User::getFirst_name));
		lastNameCol = new MFXTableColumn<>("LAST NAME",false, Comparator.comparing(User::getLast_name));
		roleCol = new MFXTableColumn<>("ROLE",false, Comparator.comparing(User::getRole));
		userIDCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getUserID));
		usernameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getUsername));
		firstNameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getFirst_name));
		lastNameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getLast_name));
		roleCol.setRowCellFactory(_ -> new MFXTableRowCell<>(User::getRole));
		accountsTable = new MFXTableView<>();
		accountsTable.getTableColumns().addAll(userIDCol,usernameCol,firstNameCol,lastNameCol,roleCol);
		accountsTable.getFilters().addAll(
				new StringFilter<>("Username",User::getUsername),
				new StringFilter<>("First Name",User::getFirst_name),
				new StringFilter<>("Last Name",User::getLast_name),
				new StringFilter<>("Role",User::getRole)
		);
		userSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
		Task<List<User>> fetchUsersTask = new Task<>() {
			@Override
			protected List<User> call() {
				return userService.getAllUsers();
			}
		};
		fetchUsersTask.setOnSucceeded(_ -> {
			ObservableList<User> allUsers = FXCollections.observableArrayList(fetchUsersTask.getValue());
			accountsTable.setItems(allUsers);
			userSpinner.setMaxWidth(0);
		});
		fetchUsersTask.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while fetching users", fetchUsersTask.getException());
			userSpinner.setMaxWidth(0);
		});
		executor.submit(fetchUsersTask);
		accountsTable.setFooterVisible(true);
		accountsTable.autosizeColumnsOnInitialization();
		accountsTable.setMaxWidth(Double.MAX_VALUE);
		accountsTable.setMaxHeight(Double.MAX_VALUE);
		userFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,lef
		controlBox.getChildren().removeAll(controlBox.getChildren());
		controlBox.getChildren().addAll(userFilterView,accountsTable);
		VBox.setVgrow(accountsTable, Priority.ALWAYS);
		accountsTable.virtualFlowInitializedProperty().addListener((_, _, _) -> {
			addUserDoubleClickFunction();});
		userFilterView.filteredItemsProperty().addListener((_, _, _) -> {
			addUserDoubleClickFunction();});
		ValidatorUtils.setupRegexValidation(usernameField,usernameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveUserButton);
		ValidatorUtils.setupRegexValidation(firstNameField,firstNameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveUserButton);
		ValidatorUtils.setupRegexValidation(lastNameField,lastNameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveUserButton);
	}

	private void addUserDoubleClickFunction(){
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
		GUIUtils.formatTabSelect(storesButton);
		GUIUtils.formatTabDeselect(usersButton);
		addButton.setOnAction(_ -> openStorePopover());
		storeFilterView = new FilterView<>();
		storeFilterView.setTitle("Current Stores");
		storeFilterView.setSubtitle("Double click a store to edit");
		storeFilterView.setTextFilterProvider(text -> store -> store.getStoreName().toLowerCase().contains(text));
		storeNameCol = new MFXTableColumn<>("STORE NAME",false, Comparator.comparing(Store::getStoreName));
		storeNameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(Store::getStoreName));
		storeHoursCol = new MFXTableColumn<>("HOURS PER DAY",false, Comparator.comparing(Store::getStoreHours));
		storeHoursCol.setRowCellFactory(_ -> new MFXTableRowCell<>(Store::getStoreHours));
		storesTable = new MFXTableView<>();
		storesTable.getTableColumns().addAll(storeNameCol,storeHoursCol);
		storesTable.getFilters().addAll(
				new StringFilter<>("Store Name",Store::getStoreName)
		);
		storeSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
		Task<List<Store>> fetchStoresTask = new Task<>() {
			@Override
			protected List<Store> call() {
				return storeService.getAllStores();
			}
		};
		fetchStoresTask.setOnSucceeded(_ -> {
			ObservableList<Store> allStores = FXCollections.observableArrayList(fetchStoresTask.getValue());
			storesTable.setItems(allStores);
			storeSpinner.setMaxWidth(0);
		});
		fetchStoresTask.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while fetching stores", fetchStoresTask.getException());
			storeSpinner.setMaxWidth(0);
		});
		executor.submit(fetchStoresTask);
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
		ValidatorUtils.setupRegexValidation(storeNameField,storeNameValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveStoreButton);
		ValidatorUtils.setupRegexValidation(storeHoursField,storeHoursValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.INT_ERROR,null,saveStoreButton);
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
		permissionsSelector.getSelectionModel().clearSelection();
		userSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
		CompletableFuture<List<Store>> allStoresFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return storeService.getAllStores();
			} catch (Exception ex) {
				throw new CompletionException(ex);
			}
		}, executor);
		CompletableFuture<List<Permission>> allPermissionsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return permissionService.getAllPermissions();
			} catch (Exception ex) {
				throw new CompletionException(ex);
			}
		}, executor);
		CompletableFuture.allOf(
				allStoresFuture,
				allPermissionsFuture
		).thenRunAsync(() -> {
			try {
				List<Store> allStores = allStoresFuture.get();
				List<Permission> allPermissions = allPermissionsFuture.get();
				Platform.runLater(() -> {
					storeSelector.setItems(FXCollections.observableArrayList(allStores));
					permissionsSelector.setItems(FXCollections.observableArrayList(allPermissions));
					userSpinner.setMaxWidth(0);
				});
			} catch (Exception ex) {
				Platform.runLater(() -> {
					dialogPane.showError("Error", "An error occurred while fetching user information", ex);
					userSpinner.setMaxWidth(0);
				});
			}
		}, executor);
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
		profileTextPicker.setValue(Color.valueOf(user.getTextColour()));
		profileBackgroundPicker.setValue(Color.valueOf(user.getBgColour()));
		passwordResetButton.setVisible(true);
		employeeName.setText(user.getFirst_name() + "." + user.getLast_name().charAt(0));
		employeeRole.setText(user.getRole());
		employeeIcon.setText(user.getFirst_name().substring(0,1));
		employeeIcon.setStyle("-fx-background-color: " + user.getBgColour()+ "; -fx-text-fill: " + user.getTextColour() + ";");
		storeSelector.getSelectionModel().clearSelection();
		permissionsSelector.getSelectionModel().clearSelection();
		userSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
		CompletableFuture<Boolean> passwordResetFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return userService.isPasswordResetRequested(user.getUserID());
			} catch (Exception ex) {
				throw new CompletionException(ex);
			}
		}, executor);
		CompletableFuture<List<Store>> allStoresFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return storeService.getAllStores();
			} catch (Exception ex) {
				throw new CompletionException(ex);
			}
		}, executor);
		CompletableFuture<List<Employment>> staffStoresFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return userService.getEmploymentsForUser(user.getUserID());
			} catch (Exception ex) {
				throw new CompletionException(ex);
			}
		}, executor);
		CompletableFuture<List<Permission>> allPermissionsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return permissionService.getAllPermissions();
			} catch (Exception ex) {
				throw new CompletionException(ex);
			}
		}, executor);
		CompletableFuture<List<Permission>> userPermissionsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return userService.getUserPermissions(user.getUserID());
			} catch (Exception ex) {
				throw new CompletionException(ex);
			}
		}, executor);
		CompletableFuture.allOf(
				passwordResetFuture,
				allStoresFuture,
				staffStoresFuture,
				allPermissionsFuture,
				userPermissionsFuture
		).thenRunAsync(() -> {
			try {
				boolean isPasswordResetRequested = passwordResetFuture.get();
				List<Store> allStores = allStoresFuture.get();
				List<Employment> staffStores = staffStoresFuture.get();
				List<Permission> allPermissions = allPermissionsFuture.get();
				List<Permission> userPermissions = userPermissionsFuture.get();
				Platform.runLater(() -> {
					if (isPasswordResetRequested) {
						passwordResetButton.setDisable(true);
						passwordResetButton.setText("Password reset requested");
					} else {
						passwordResetButton.setDisable(false);
						passwordResetButton.setText("Request Password Reset");
						passwordResetButton.setOnAction(_ -> resetPassword(user));
					}
					storeSelector.setItems(FXCollections.observableArrayList(allStores));
					for (Store s : allStores) {
						for (Employment e : staffStores) {
							if (s.getStoreID() == e.getStoreID()) {
								storeSelector.getSelectionModel().selectItem(s);
							}
						}
					}
					permissionsSelector.setItems(FXCollections.observableArrayList(allPermissions));
					for (Permission p : userPermissions) {
						for (Permission p2 : allPermissions) {
							if (p.getPermissionID() == p2.getPermissionID()) {
								permissionsSelector.getSelectionModel().selectItem(p2);
							}
						}
					}
					userSpinner.setMaxWidth(0);
				});
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
			} catch (Exception ex) {
				Platform.runLater(() -> {
					dialogPane.showError("Error", "An error occurred while fetching user information", ex);
					userSpinner.setMaxWidth(0);
				});
			}
		}, executor);
	}

	public void openStorePopover(){
		storeNameField.clear();
		storeHoursField.clear();
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
		storeHoursValidationLabel.setVisible(false);
	}

	public void openStorePopover(Store store){
		editStorePopoverTitle.setText("Edit store");
	 	storeNameField.setText(store.getStoreName());
		storeHoursField.setText(String.valueOf(store.getStoreHours()));
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
		storeHoursValidationLabel.setVisible(false);
	}

	public void closeUserPopover(){
		AnimationUtils.slideIn(editUserPopover,425);
		contentDarken.setVisible(false);
		usernameValidationLabel.setVisible(false);
		firstNameValidationLabel.setVisible(false);
		lastNameValidationLabel.setVisible(false);
	}

	public void addStore() {
		String name = storeNameField.getText();
		double hours = Double.parseDouble(storeHoursField.getText());
		if (!storeNameField.isValid()) {
			storeNameField.requestFocus();
		}else if (!storeHoursField.isValid()) {
			storeHoursField.requestFocus();
		} else {
			storeSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
			Task<Void> addStoreTask = new Task<>() {
				@Override
				protected Void call() {
					Store store = new Store(name, hours);
					storeService.addStore(store);
					return null;
				}
			};
			addStoreTask.setOnSucceeded(_ -> {
				dialogPane.showInformation("Success", "Store was successfully added to the database");
				storesView();
				storeSpinner.setMaxWidth(0);
			});
			addStoreTask.setOnFailed(_ -> {
				dialogPane.showError("Error", "An error occurred while adding a store", addStoreTask.getException());
				storeSpinner.setMaxWidth(0);
			});
			executor.submit(addStoreTask);
		}
	}

	public void addUser() {
		String username = usernameField.getText();
		String fname = firstNameField.getText();
		String lname = lastNameField.getText();
		String role = roleField.getText();
		String profileText = getColorString(profileTextPicker.getValue());
		String profileBG = getColorString(profileBackgroundPicker.getValue());
		if (!usernameField.isValid()) {
			usernameField.requestFocus();
		} else if (!firstNameField.isValid()) {
			firstNameField.requestFocus();
		} else if (!lastNameField.isValid()) {
			lastNameField.requestFocus();
		} else if (storeSelector.getSelectionModel().getSelection().isEmpty()) {
			dialogPane.showError("Error", "No store selected, Please select at least one store for the user to work at");
		} else {
			userSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
			Task<Void> addUserTask = new Task<>() {
				@Override
				protected Void call() throws Exception {
					// Check if username already exists
					if (userService.getUserByUsername(username) != null) {
						throw new Exception("Username already exists");
					}
					User newUser = new User();
					newUser.setUsername(username);
					newUser.setFirst_name(fname);
					newUser.setLast_name(lname);
					newUser.setRole(role);
					newUser.setBgColour(profileBG);
					newUser.setTextColour(profileText);
					userService.addUser(newUser);
					List<CompletableFuture<Void>> futures = new ArrayList<>();
					// Add employments in parallel
					for (Store s : storeSelector.getSelectionModel().getSelection().values()) {
						CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
							try {
								employmentService.addEmployment(newUser, s);
							} catch (Exception e) {
								throw new CompletionException(e);
							}
						}, executor);
						futures.add(future);
					}
					// Add user permissions in parallel
					for (Permission p : permissionsSelector.getSelectionModel().getSelection().values()) {
						CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
							try {
								userPermissionService.addUserPermission(newUser, p);
							} catch (Exception e) {
								throw new CompletionException(e);
							}
						}, executor);
						futures.add(future);
					}
					// Wait for all futures to complete
					CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
					return null;
				}
			};
			addUserTask.setOnSucceeded(_ -> {
				dialogPane.showInformation("Success", "User was successfully added to the database");
				closeUserPopover();
				usersView();
				userSpinner.setMaxWidth(0);
			});
			addUserTask.setOnFailed(_ -> {
				Throwable exception = addUserTask.getException();
				if (exception.getMessage().equals("Username already exists")) {
					dialogPane.showError("Error", "Username already exists, Please choose a different username");
				} else {
					dialogPane.showError("Error", "An error occurred while adding a user", exception);
				}
				userSpinner.setMaxWidth(0);
			});
			executor.submit(addUserTask);
		}
	}

	public void editStore(Store store) {
		String name = storeNameField.getText();
		double hours = Double.parseDouble(storeHoursField.getText());
		if (!storeNameField.isValid()) {
			storeNameField.requestFocus();
		} else if (!storeHoursField.isValid()) {
			storeHoursField.requestFocus();
		} else {
			storeSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
			Task<Void> editStoreTask = new Task<>() {
				@Override
				protected Void call() {
					store.setStoreName(name);
					store.setStoreHours(hours);
					storeService.updateStore(store);
					return null;
				}
			};
			editStoreTask.setOnSucceeded(_ -> {
				dialogPane.showInformation("Success", "Store was successfully updated");
				closeStorePopover();
				storesView();
				storeSpinner.setMaxWidth(0);
			});
			editStoreTask.setOnFailed(_ -> {
				dialogPane.showError("Error", "An error occurred while updating the store", editStoreTask.getException());
				storeSpinner.setMaxWidth(0);
			});
			executor.submit(editStoreTask);
		}
	}

	public void editUser(User user) {
		LocalDate inactiveDate = null;
		if (inactiveUserToggle.isSelected() && user.getInactiveDate() != null) {
			inactiveDate = user.getInactiveDate();
		} else if (inactiveUserToggle.isSelected() && user.getInactiveDate() == null) {
			inactiveDate = LocalDate.now();
		}
		String username = usernameField.getText();
		String fname = firstNameField.getText();
		String lname = lastNameField.getText();
		String role = roleField.getText();
		String profileText = getColorString(profileTextPicker.getValue());
		String profileBG = getColorString(profileBackgroundPicker.getValue());
		if (!firstNameField.isValid()) {
			firstNameField.requestFocus();
		} else if (!lastNameField.isValid()) {
			lastNameField.requestFocus();
		} else if (storeSelector.getSelectionModel().getSelection().isEmpty()) {
			dialogPane.showError("Error", "No store selected, Please select at least one store for the user to work at");
		} else {
			userSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
			LocalDate finalInactiveDate = inactiveDate;
			Task<Void> editUserTask = new Task<>() {
				@Override
				protected Void call() {
					// Update user details
					user.setUsername(username);
					user.setFirst_name(fname);
					user.setLast_name(lname);
					user.setRole(role);
					user.setBgColour(profileBG);
					user.setTextColour(profileText);
					user.setInactiveDate(finalInactiveDate);
					userService.updateUser(user);
					List<CompletableFuture<Void>> futures = new ArrayList<>();
					// Delete existing employments and permissions
					CompletableFuture<Void> deleteEmploymentsFuture = CompletableFuture.runAsync(() -> {
						try {
							employmentService.deleteEmploymentsForUser(user);
						} catch (Exception e) {
							throw new CompletionException(e);
						}
					}, executor);
					CompletableFuture<Void> deletePermissionsFuture = CompletableFuture.runAsync(() -> {
						try {
							userPermissionService.deletePermissionsForUser(user);
						} catch (Exception e) {
							throw new CompletionException(e);
						}
					}, executor);
					// Wait for deletions to complete before adding new ones
					CompletableFuture.allOf(deleteEmploymentsFuture, deletePermissionsFuture).join();
					// Add new employments in parallel
					for (Store s : storeSelector.getSelectionModel().getSelection().values()) {
						CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
							try {
								employmentService.addEmployment(user, s);
							} catch (Exception e) {
								throw new CompletionException(e);
							}
						}, executor);
						futures.add(future);
					}
					// Add new user permissions in parallel
					for (Permission p : permissionsSelector.getSelectionModel().getSelection().values()) {
						CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
							try {
								userPermissionService.addUserPermission(user, p);
							} catch (Exception e) {
								throw new CompletionException(e);
							}
						}, executor);
						futures.add(future);
					}
					// Wait for all futures to complete
					CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
					return null;
				}
			};
			editUserTask.setOnSucceeded(_ -> {
				dialogPane.showInformation("Success", "User was successfully updated");
				closeUserPopover();
				usersView();
				userSpinner.setMaxWidth(0);
			});
			editUserTask.setOnFailed(_ -> {
				dialogPane.showError("Error", "An error occurred while updating the user", editUserTask.getException());
				userSpinner.setMaxWidth(0);
			});
			executor.submit(editUserTask);
		}
	}

	public void resetPassword(User user) {
		userSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
		Task<Void> resetPasswordTask = new Task<>() {
			@Override
			protected Void call() {
				userService.resetUserPassword(user.getUserID());
				return null;
			}
		};
		resetPasswordTask.setOnSucceeded(_ -> {
			Platform.runLater(() -> {
				passwordResetButton.setText("Password reset requested");
				passwordResetButton.setDisable(true);
				dialogPane.showInformation("Success", "Password reset has been requested for the user");
			});
			userSpinner.setMaxWidth(0);
		});
		resetPasswordTask.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while updating user password", resetPasswordTask.getException());
			userSpinner.setMaxWidth(0);
		});
		executor.submit(resetPasswordTask);
	}

	public void deleteStore(Store store) {
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete the store from all systems,\n" +
						"Are you sure you still want to delete this store?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				storeSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
				Task<Void> deleteStoreTask = new Task<>() {
					@Override
					protected Void call() {
						storeService.deleteStore(store);
						return null;
					}
				};
				deleteStoreTask.setOnSucceeded(_ -> {
					dialogPane.showInformation("Success", "Store was successfully deleted");
					closeStorePopover();
					storesView();
					storeSpinner.setMaxWidth(0);
				});
				deleteStoreTask.setOnFailed(_ -> {
					dialogPane.showError("Error", "An error occurred while deleting store", deleteStoreTask.getException());
					storeSpinner.setMaxWidth(0);
				});
				executor.submit(deleteStoreTask);
			}
		});
	}

	public void deleteUser(User user) {
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete the user from all systems,\n" +
						"if the archived information for this user must be saved, it's instead \n" +
						"preferred that you mark them as a now inactive user\n\n" +
						"Are you sure you still want to delete this user?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				storeSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
				Task<Void> deleteUserTask = new Task<>() {
					@Override
					protected Void call() {
						userService.deleteUser(user.getUserID());
						return null;
					}
				};
				deleteUserTask.setOnSucceeded(_ -> {
					dialogPane.showInformation("Success", "User was successfully deleted");
					closeUserPopover();
					usersView();
					storeSpinner.setMaxWidth(0);
				});
				deleteUserTask.setOnFailed(_ -> {
					dialogPane.showError("Error", "An error occurred while deleting user", deleteUserTask.getException());
					storeSpinner.setMaxWidth(0);
				});
				executor.submit(deleteUserTask);
			}
		});
	}

	private String getColorString(Color color) {
		int r = (int) Math.round(color.getRed() * 255.0);
		int g = (int) Math.round(color.getGreen() * 255.0);
		int b = (int) Math.round(color.getBlue() * 255.0);
		return String.format("#%02x%02x%02x", r, g, b).toUpperCase();
	}
}
