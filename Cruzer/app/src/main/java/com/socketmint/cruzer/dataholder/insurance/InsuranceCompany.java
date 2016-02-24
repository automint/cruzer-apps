package com.socketmint.cruzer.dataholder.insurance;

/**
 * Data holder for Insurance Company Details
 * @author ndkcha
 * @since 26
 * @version 26
 */

public class InsuranceCompany {
    private String id;
    public String company;

    public InsuranceCompany(String id, String company) {
        this.id = id;
        this.company = company;
    }

    public String getId() {
        return id;
    }
}
