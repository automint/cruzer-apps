package com.socketmint.cruzer.dataholder.vehicle;

public class Vehicle {
    private String id, sId, userId, modelId;
    public String reg, name;
    public Model model;

    public Vehicle(String id, String sId, String userId, String modelId, String reg, String name) {
        this.id = id;
        this.sId = sId;
        this.userId = userId;
        this.modelId = modelId;
        this.reg = reg;
        this.name = name;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }

    public String getUserId() {
        return userId;
    }

    public String getModelId() {
        return modelId;
    }
}
