package com.socketmint.cruzer.dataholder;

public class User {
    private String id, sId, cityId;
    private String password;
    public String mobile, email, firstName, lastName;

    public User(String id, String sId, String password, String mobile, String email, String firstName, String lastName, String cityId) {
        this.id = id;
        this.sId = sId;
        this.password = password;
        this.mobile = mobile;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.cityId = cityId;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }

    public String getPassword() {
        return password;
    }

    public String getCityId() {
        return cityId;
    }
}
