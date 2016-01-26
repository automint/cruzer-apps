package com.socketmint.cruzer.dataholder;

public class Error {
    private String id;
    public String code, message;

    public Error(String id, String code, String message) {
        this.id = id;
        this.code = code;
        this.message = message;
    }

    public String getId() {
        return id;
    }
}
