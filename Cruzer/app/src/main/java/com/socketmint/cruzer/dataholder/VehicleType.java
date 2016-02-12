package com.socketmint.cruzer.dataholder;

public class VehicleType {
    private String id;
    public String type;

    public VehicleType(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }
}
