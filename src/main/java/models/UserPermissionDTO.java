package models;

import java.time.LocalDate;

public class UserPermissionDTO {
    private int userID;
    private int permissionID;
    private LocalDate startDate;
    private LocalDate endDate;

    public UserPermissionDTO() {}

    public UserPermissionDTO(int userID, int permissionID, LocalDate startDate, LocalDate endDate) {
        this.userID = userID;
        this.permissionID = permissionID;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public LocalDate getStartDate() {return startDate;}

    public void setStartDate(LocalDate startDate) {this.startDate = startDate;}

    public LocalDate getEndDate() {return endDate;}

    public void setEndDate(LocalDate endDate) {this.endDate = endDate;}
}
