package models;

public class Store {
	private int storeID;

	private String storeName;

	public Store() {
	}

	public Store(String storeName){
		this.storeName = storeName;
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

	@Override
	public String toString() {
		return storeName;
	}
}
