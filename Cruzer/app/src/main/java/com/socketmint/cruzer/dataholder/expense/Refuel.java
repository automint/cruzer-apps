package com.socketmint.cruzer.dataholder.expense;

public class Refuel {
    private String id, sId, vehicleId;
    public String date, rate, volume, cost, odo;

    public Refuel(String id, String sId, String vehicleId, String date, String rate, String volume, String cost, String odo) {
        this.id = id;
        this.sId = sId;
        this.vehicleId = vehicleId;
        this.date = date;
        this.rate = rate;
        this.volume = volume;
        this.cost = cost;
        this.odo = odo;
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
}
