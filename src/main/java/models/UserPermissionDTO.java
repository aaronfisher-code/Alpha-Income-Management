package models;

public class UserPermissionDTO {
    private int userID;
    private int permissionID;

    public UserPermissionDTO() {}

    public UserPermissionDTO(int userID, int permissionID) {
        this.userID = userID;
        this.permissionID = permissionID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getPermissionID() {
        return permissionID;
    }

    public void setPermissionID(int permissionID) {
        this.permissionID = permissionID;
    }
}
