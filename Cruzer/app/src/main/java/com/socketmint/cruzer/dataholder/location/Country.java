package com.socketmint.cruzer.dataholder.location;

public class Country {
    private String id;
    public String country;

    public Country(String id, String country) {
        this.id = id;
        this.country = country;
    }

    public String getId() {
        return id;
    }
}
