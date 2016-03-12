package com.socketmint.cruzer.dataholder.location;

public class City {
    private String id, countryId;
    public String city;

    public City(String id, String countryId, String city) {
        this.id = id;
        this.countryId = countryId;
        this.city = city;
    }

    public String getId() {
        return id;
    }

    public String getCountryId() {
        return countryId;
    }
}
