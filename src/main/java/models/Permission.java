package models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Permission {
	private int permissionID;

	private String permissionName;

	public Permission(ResultSet resultSet) {
		try {
			if(resultSet.getString("permissionID")!=null)
				this.permissionID = resultSet.getInt("permissionID");
			if(resultSet.getString("permissionName")!=null)
				this.permissionName = resultSet.getString("permissionName");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getPermissionID() {
		return permissionID;
	}

	public void setPermissionID(int permissionID) {
		this.permissionID = permissionID;
	}

	public String getPermissionName() {
		return permissionName;
	}

	public void setPermissionName(String permissionName) {
		this.permissionName = permissionName;
	}

	@Override
	public String toString() {
		return permissionName;
	}
}
