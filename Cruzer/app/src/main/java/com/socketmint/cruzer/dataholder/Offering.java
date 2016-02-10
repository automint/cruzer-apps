package com.socketmint.cruzer.dataholder;

public class Offering {
    private String id;
    public String offering;

    public Offering(String id, String offering) {
        this.id = id;
        this.offering = offering;
    }

    public String getId() {
        return id;
    }
}
