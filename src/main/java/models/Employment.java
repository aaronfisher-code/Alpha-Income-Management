package models;

public class Employment {

	private int employmentID;
	private int userID;
	private int storeID;

	public Employment() {}

	public int getEmploymentID() {
		return employmentID;
	}

	public void setEmploymentID(int employmentID) {
		this.employmentID = employmentID;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}
}
