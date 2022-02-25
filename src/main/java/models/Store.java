package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class Store {
	private int storeID;

	private String storeName;

	public Store(ResultSet resultSet) {
		try {
			if(resultSet.getString("storeID")!=null)
				this.storeID = resultSet.getInt("storeID");
			if(resultSet.getString("storeName")!=null)
				this.storeName = resultSet.getString("storeName");
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
