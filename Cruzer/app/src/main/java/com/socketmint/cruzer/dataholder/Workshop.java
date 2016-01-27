package com.socketmint.cruzer.dataholder;

public class Workshop {
    private String id, sId;
    public String name, address, manager, contact, latitude, longitude, city, area, offerings;

    public Workshop(String id, String sId, String name, String address, String manager, String contact, String latitude, String longitude, String city, String area, String offerings) {
        this.id = id;
        this.sId = sId;
        this.name = name;
        this.address = address;
        this.manager = manager;
        this.contact = contact;
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.area = area;
        this.offerings = offerings;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }
}
