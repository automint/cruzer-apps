package com.socketmint.cruzer.dataholder.expense.service;

public class Service {
    private String id, sId, vehicleId, workshopId, userId, roleId;
    public String date, cost, odo, details, status, vat;

    public Service(String id, String sId, String vehicleId, String workshopId, String date, String cost, String odo, String details, String status, String userId, String roleId, String vat) {
        this.id = id;
        this.sId = sId;
        this.vehicleId = vehicleId;
        this.workshopId = workshopId;
        this.date = date;
        this.cost = cost;
        this.odo = odo;
        this.details = details;
        this.status = status;
        this.userId = userId;
        this.roleId = roleId;
        this.vat = vat;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getWorkshopId() {
        return workshopId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRoleId() {
        return roleId;
    }
}
