package models;

public class Store {
	private int storeID;
	private String storeName;
	private double storeHours;

	public Store() {
	}

	public Store(String storeName, double storeHours) {
		this.storeName = storeName;
		this.storeHours = storeHours;
	}

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public double getStoreHours() {
		return storeHours;
	}

	public void setStoreHours(double storeHours) {
		this.storeHours = storeHours;
	}

	@Override
	public String toString() {
		return storeName;
	}
}
