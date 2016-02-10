package com.socketmint.cruzer.dataholder;

public class VehicleSubType {
    private String id, vehicleTypeId;
    public String subType;

    public VehicleSubType(String id, String vehicleTypeId, String subType) {
        this.id = id;
        this.vehicleTypeId = vehicleTypeId;
        this.subType = subType;
    }

    public String getId() {
        return id;
    }

    public String getVehicleTypeId() {
        return vehicleTypeId;
    }
}
