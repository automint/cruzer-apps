package com.socketmint.cruzer.dataholder;

public class Problem {
    private String id, sId, serviceId;
    public String details, lCost, pCost, qty, rate, type;

    public Problem(String id, String sId, String serviceId, String details, String lCost, String pCost, String qty, String rate, String type) {
        this.id = id;
        this.sId = sId;
        this.serviceId = serviceId;
        this.details = details;
        this.lCost = lCost;
        this.pCost = pCost;
        this.qty = qty;
        this.rate = rate;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }

    public String getServiceId() {
        return serviceId;
    }
}
