package com.socketmint.cruzer.dataholder;

public class Workshop {
    private String id, sId, cityId, workshopTypeId;
    public String name, address, manager, contact, latitude, longitude, area, offerings;

    public Workshop(String id, String sId, String name, String address, String manager, String contact, String latitude, String longitude, String cityId, String area, String offerings, String workshopTypeId) {
        this.id = id;
        this.sId = sId;
        this.name = name;
        this.address = address;
        this.manager = manager;
        this.contact = contact;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cityId = cityId;
        this.area = area;
        this.offerings = offerings;
        this.workshopTypeId = workshopTypeId;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }

    public String getCityId() {
        return cityId;
    }

    public String getWorkshopTypeId() {
        return workshopTypeId;
    }
}
