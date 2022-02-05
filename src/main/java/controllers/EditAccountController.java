package controllers;

import application.Main;
import com.dlsc.gemsfx.FilterView;
import com.dlsc.gemsfx.FilterView.FilterGroup;
import io.github.palexdev.materialfx.controls.MFXPaginatedTableView;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.filter.StringFilter;
import io.github.palexdev.materialfx.utils.others.observables.When;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

public class EditAccountController extends Controller{

	@FXML
	private StackPane backgroundPane;
	private MFXPaginatedTableView<User> accountsTable = new MFXPaginatedTableView<User>();
	private MFXTableColumn<User> usernameCol;
	private MFXTableColumn<User> firstNameCol;
	private MFXTableColumn<User> lastNameCol;
	private MFXTableColumn<User> roleCol;
	
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private User selectedUser;

	private ObservableList<User> allUsers = FXCollections.observableArrayList();
	
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
	public void fill() {

		FilterView<User> filterView = new FilterView<>();
		filterView.setTitle("Current Users");
		filterView.setSubtitle("Double click on a user for more actions");
		filterView.setTextFilterProvider(text -> user -> user.getFirst_name().toLowerCase().contains(text) || user.getLast_name().toLowerCase().contains(text) || user.getRole().toLowerCase().contains(text));
		allUsers = filterView.getFilteredItems();

		usernameCol = new MFXTableColumn<>("Username",false, Comparator.comparing(User::getUsername));
		firstNameCol = new MFXTableColumn<>("First Name",false, Comparator.comparing(User::getFirst_name));
		lastNameCol = new MFXTableColumn<>("Last Name",false, Comparator.comparing(User::getLast_name));
		roleCol = new MFXTableColumn<>("Role",false, Comparator.comparing(User::getRole));
		usernameCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getUsername));
		firstNameCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getFirst_name));
		lastNameCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getLast_name));
		roleCol.setRowCellFactory(user -> new MFXTableRowCell<>(User::getRole));
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
				filterView.getItems().add(new User(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}


		accountsTable.setFooterVisible(true);
		accountsTable.autosizeColumnsOnInitialization();

		When.onChanged(accountsTable.currentPageProperty())
				.then((oldValue,newValue) -> accountsTable.autosizeColumns())
				.listen();

		accountsTable.setMaxWidth(Double.MAX_VALUE);
		accountsTable.setRowsPerPage(15);
		accountsTable.setItems(allUsers);

		VBox box = new VBox(filterView,accountsTable);
		box.setPadding(new Insets(20));
		box.setSpacing(10);

		VBox.setVgrow(accountsTable, Priority.ALWAYS);
		backgroundPane.getChildren().add(box);
		accountsTable.setItems(allUsers);


	}

//	// On register page
//	public void createAccount () throws IOException {
//
//		String usrname = username.getText();
//		String pswrd = password.getText();
//		String rpswrd = re_password.getText();
//		String fname = firstName.getText();
//		String lname = lastName.getText();
//		String role = roleSelect.getText();
//		Color c = bgColourPicker.getValue();
//		int r = (int)Math.round(c.getRed() * 255.0);
//		int g = (int)Math.round(c.getGreen() * 255.0);
//		int b = (int)Math.round(c.getBlue() * 255.0);
//		String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
//		String profileText = textColourPicker.getValue().toString();
//
//
//		if (fname.isEmpty() || lname.isEmpty() || usrname.isEmpty() || pswrd.isEmpty() || rpswrd.isEmpty() || role.isEmpty()) {
//			signUpError.setText("Please enter all fields!");
//		} else if (!rpswrd.toString().equals(pswrd.toString())) {
//			signUpError.setText("Password not matching!");
//		} else if (searchAccount(usrname)) {
//			//signUpError.setText("Username already exists");
//			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//			alert.setTitle("Username already exists");
//			alert.setContentText("This user already exists, would you like to update their details?");
//			ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
//			ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
//			alert.getButtonTypes().setAll(okButton, noButton);
//			alert.showAndWait().ifPresent(type -> {
//				if (type == okButton) {
//					updateAccount();
//				} else if (type == noButton) {
//				}
//			});
//		} else {
//			//query
//			String sql = "INSERT INTO accounts(username,password,first_name,last_name,role,profileBG,profileText) VALUES(?,?,?,?,?,?,?)";
//			try {
//				preparedStatement = con.prepareStatement(sql);
//				preparedStatement.setString(1, usrname);
//				preparedStatement.setString(2, pswrd);
//				preparedStatement.setString(3, fname);
//				preparedStatement.setString(4, lname);
//				preparedStatement.setString(5, role);
//				preparedStatement.setString(6, profileBG);
//				preparedStatement.setString(7, profileText);
//				preparedStatement.executeUpdate();
//				JOptionPane.showMessageDialog(null,"Account successfully created");
//			} catch (SQLException ex) {
//				System.err.println(ex.getMessage());
//			}
//		}
//		fill();
//
//	}

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

//	public void updateAccount(){
//		String usrname = username.getText();
//		String pswrd = password.getText();
//		String fname = firstName.getText();
//		String lname = lastName.getText();
//		String role = roleSelect.getText();
//		Color c = bgColourPicker.getValue();
//		int r = (int)Math.round(c.getRed() * 255.0);
//		int g = (int)Math.round(c.getGreen() * 255.0);
//		int b = (int)Math.round(c.getBlue() * 255.0);
//		String profileBG = String.format("#%02x%02x%02x" , r, g, b).toUpperCase();
//		String profileText = textColourPicker.getValue().toString();
//
//		String sql = "UPDATE accounts SET password = ?, first_name = ?, last_name = ?, role = ?, profileBG = ?, profileText = ? WHERE username = ?";
//		try {
//			preparedStatement = con.prepareStatement(sql);
//			preparedStatement.setString(1, pswrd);
//			preparedStatement.setString(2, fname);
//			preparedStatement.setString(3, lname);
//			preparedStatement.setString(4, role);
//			preparedStatement.setString(5, profileBG);
//			preparedStatement.setString(6, profileText);
//			preparedStatement.setString(7, usrname);
//			preparedStatement.executeUpdate();
//			JOptionPane.showMessageDialog(null,"Account successfully updated");
//		} catch (SQLException ex) {
//			System.err.println(ex.getMessage());
//		}
//	}

//	public void deleteAccount(){
//	 	if(searchAccount(username.getText())){
//			Alert alert = new Alert(Alert.AlertType.WARNING);
//			alert.setTitle("Change Account Status");
//			alert.setContentText("Inactive employees will be hidden from view");
//			ButtonType activeButton = new ButtonType("Mark as Active", ButtonBar.ButtonData.YES);
//			ButtonType inactiveButton = new ButtonType("Mark as Inactive", ButtonBar.ButtonData.NO);
//			ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
//			alert.getButtonTypes().setAll(activeButton, inactiveButton, cancelButton);
//			alert.showAndWait().ifPresent(type -> {
//				if (type == activeButton) {
//					String sql = "UPDATE accounts SET inactiveDate = ? WHERE username = ?";
//					try {
//						preparedStatement = con.prepareStatement(sql);
//						preparedStatement.setString(1, null);
//						preparedStatement.setString(2, username.getText());
//						preparedStatement.executeUpdate();
//						JOptionPane.showMessageDialog(null,"Account successfully updated");
//					} catch (SQLException ex) {
//						System.err.println(ex.getMessage());
//					}
//				} else if (type == inactiveButton) {
//					String sql = "UPDATE accounts SET inactiveDate = ? WHERE username = ?";
//					try {
//						preparedStatement = con.prepareStatement(sql);
//						preparedStatement.setString(1, String.valueOf(LocalDate.now()));
//						preparedStatement.setString(2, username.getText());
//						preparedStatement.executeUpdate();
//						JOptionPane.showMessageDialog(null,"Account successfully updated");
//					} catch (SQLException ex) {
//						System.err.println(ex.getMessage());
//					}
//				}else{
//				}
//			});
//		}
//	 	fill();
//	}


//	public void fillInfo(User selectedUser){
//		username.setText(selectedUser.getUsername());
//		password.setText(selectedUser.getPassword());
//		re_password.setText(selectedUser.getPassword());
//		firstName.setText(selectedUser.getFirst_name());
//		lastName.setText(selectedUser.getLast_name());
//		roleSelect.setText(selectedUser.getRole());
//		bgColourPicker.setValue(Color.web(selectedUser.getBgColour()));
//		textColourPicker.setValue(Color.web(selectedUser.getTextColour()));
//		if(selectedUser.getInactiveDate()!=null){
//			signUpError.setText("INACTIVE EMPLOYEE");
//		}else{
//			signUpError.setText("");
//		}
//
//		textChange();
//		backgroundChange();
//		labelChange();
//	}

//	public void mouseClick(){
//	 	if(selectedUser==null || accountsTable.getSelectionModel().getSelectedItem() == null || selectedUser.getUsername() != accountsTable.getSelectionModel().getSelectedItem().getUsername()) {
//			selectedUser = accountsTable.getSelectionModel().getSelectedItem();
//		}else{
//	 		fillInfo(selectedUser);
//		}
//
//	}

//	public void toggleInactive(){
//	 	if(inactiveToggle.isSelected()){
//	 		loadEmployees(true);
//		}else{
//	 		loadEmployees(false);
//		}
//	}

	
}
