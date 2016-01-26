package com.socketmint.cruzer.dataholder;

public class Service {
    private String id, sId, vehicleId, workshopId;
    public String date, cost, odo, details, status;

    public Service(String id, String sId, String vehicleId, String workshopId, String date, String cost, String odo, String details, String status) {
        this.id = id;
        this.sId = sId;
        this.vehicleId = vehicleId;
        this.workshopId = workshopId;
        this.date = date;
        this.cost = cost;
        this.odo = odo;
        this.details = details;
        this.status = status;
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
}
