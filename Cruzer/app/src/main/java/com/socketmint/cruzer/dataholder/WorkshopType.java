package com.socketmint.cruzer.dataholder;

public class WorkshopType {
    private String id;
    public String type;

    public WorkshopType(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }
}
