package com.socketmint.cruzer.dataholder;

public class Status {
    private String id;
    public String details;

    public Status(String id, String details) {
        this.id = id;
        this.details = details;
    }

    public String getId() {
        return id;
    }
}
