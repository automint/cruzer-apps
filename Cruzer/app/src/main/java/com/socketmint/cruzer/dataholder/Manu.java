package com.socketmint.cruzer.dataholder;

public class Manu {
    private String id, sId;
    public String name;

    public Manu(String id, String sId, String name) {
        this.id = id;
        this.sId = sId;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }
}
