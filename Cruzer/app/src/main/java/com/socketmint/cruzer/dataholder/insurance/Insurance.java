package com.socketmint.cruzer.dataholder.insurance;

/**
 * Data holder for Insurance Details
 * @author ndkcha
 * @since 26
 * @version 26
 */

public class Insurance {
    private String id, sId, vehicleId, companyId;
    public String policyNo, startDate, endDate, premium, details;

    public Insurance(String id, String sId, String vehicleId, String companyId, String policyNo, String startDate, String endDate, String premium, String details) {
        this.id = id;
        this.sId = sId;
        this.vehicleId = vehicleId;
        this.companyId = companyId;
        this.policyNo = policyNo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.premium = premium;
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public String getsId() {
        return sId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getCompanyId() {
        return companyId;
    }
}
