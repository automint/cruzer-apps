package com.socketmint.cruzer.dataholder;

/**
 * Data Holder for PUC Details
 * @author ndkcha
 * @since 26
 * @version 26
 */

public class PUC {
    private String id, sId, vehicleId, workshopId;
    public String pucNom, startDate, endDate, fees, details;

    public PUC(String id, String sId, String vehicleId, String workshopId, String pucNom, String startDate, String endDate, String fees, String details) {
        this.id = id;
        this.sId = sId;
        this.vehicleId = vehicleId;
        this.workshopId = workshopId;
        this.pucNom = pucNom;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fees = fees;
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

    public String getWorkshopId() {
        return workshopId;
    }
}
