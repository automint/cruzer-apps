package com.socketmint.cruzer.dataholder;

public class Model {
    private String id, sId, manuId;
    public String name;
    public Manu manu;

    public Model(String id, String sId, String manuId, String name) {
        this.id = id;
        this.sId = sId;
        this.manuId = manuId;
        this.name = name;
    }

    public void setManu(Manu manu) {
        this.manu = manu;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }

    public String getManuId() {
        return manuId;
    }
}
