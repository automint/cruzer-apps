package com.socketmint.cruzer.manage;

import com.socketmint.cruzer.database.DatabaseSchema;

public class Constants {
    private static final String URL_SERVER = "http://10.70.0.50:8080";         // (server_ip:8080)
    private static final String URL_API_VERSION = "0.2";
    private static final String URL_API_DIR = "api";
    private static final String URL_API = URL_API_DIR + "/" + URL_API_VERSION;

    public static abstract class Url {
        public static final String OAUTH = URL_SERVER + "/" + URL_API + "/" + "oauth";
        public static final String AUTH = URL_SERVER + "/" + URL_API + "/" + "authenticate";
        public static final String USER = URL_SERVER + "/" + URL_API + "/" + "users";
        public static final String MANU = URL_SERVER + "/" + URL_API + "/" + "manufacturers";
        public static final String MODEL = URL_SERVER + "/" + URL_API + "/" + "models";
        public static String MODEL(String id) { return MODEL + "/" + id; }
        public static final String VEHICLE = URL_SERVER + "/" + URL_API + "/" + "vehicles";
        public static String VEHICLE(String id) { return VEHICLE + "/" + id; }
        public static final String WORKSHOP = URL_SERVER + "/" + URL_API + "/" + "workshops";
        public static String WORKSHOP(String workshopId) { return WORKSHOP + "/" + workshopId; }
        public static final String REFUEL = URL_SERVER + "/" + URL_API + "/" + "refuels";
        public static String REFUEL(String id) { return REFUEL + "/" + id; }
        public static final String SERVICE = URL_SERVER + "/" + URL_API + "/" + "services";
        public static String SERVICE(String id) { return SERVICE + "/" + id; }
        public static String GET_PROBLEMS(String serviceId) { return SERVICE(serviceId) + "/problems"; }
        public static final String GCM = URL_SERVER + "/" + URL_API + "/" + "gcm";
        public static final String STATUS = URL_SERVER + "/" + URL_API + "/" + "status";
        public static final String CITIES = URL_SERVER + "/" + URL_API + "/" + "cities";
        public static final String COUNTRIES = URL_SERVER + "/" + URL_API + "/" + "countries";
        public static final String WORKSHOP_TYPES = URL_SERVER + "/" + URL_API + "/" + "workshoptypes";
        public static String WORKSHOP_CITY(String cityId) { return WORKSHOP + "/cities/" + cityId; }
        public static String WORKSHOP_CITY_VEHICLE_TYPE(String cityId, String vehicleTypeId) { return WORKSHOP_CITY(cityId) + "/vehicletypes/" + vehicleTypeId; }
        public static String WORKSHOP_CITY_OFFERING(String cityId, String offeringId) { return WORKSHOP_CITY(cityId) + "/offerings/" + offeringId; }
        public static String WORKSHOP_CITY_VEHICLE_TYPE_OFFERING(String cityId, String vehicleTypeId, String offeringId) { return WORKSHOP_CITY_VEHICLE_TYPE(cityId, vehicleTypeId) + "/offerings/" + offeringId; }
    }

    public static abstract class VolleyRequest {
        public static final String METHOD_PUT = "PUT";
        public static final String ACCESS_TOKEN = "x-access-token";
        public static final String AUTH_TOKEN = "x-auth-token";
        public static final String AUTH_MOBILE_PARAM = DatabaseSchema.Users.COLUMN_MOBILE;
        public static final String AUTH_EMAIL_PARAM = DatabaseSchema.Users.COLUMN_EMAIL;
    }

    public static abstract class Json {
        public static final String SUCCESS = "success";
        public static final String TOKEN = "token";
        public static final String INFO = "info";
        public static final String MESSAGE = "message";
        public static final String ID = DatabaseSchema.COLUMN_ID;
        public static final String METHOD = "method";
        public static final String TABLE = "table";
        public static final String BODY = "body";
        public static final String MODEL_NAME = "model_name";
        public static final String MANU_NAME = "manu_name";
    }

    public static abstract class Gcm {
        public static final String FIELD_NEW_GCM = "newgcm";
        public static final String FIELD_OLD_GCM = "oldgcm";
        public static final String INTENT_GCM = "GcmIntentFilter";
        public static final String MESSAGE_UPDATE = "update";
        public static final String MESSAGE_TOKEN_SENT = "tokenSent";

        public static final String KEY_DATA = "data";
    }

    public static abstract class Bundle {
        public static final String VEHICLE_ID = DatabaseSchema.COLUMN_VEHICLE_ID;
        public static final String PAGE_CHOICE = "view_type";
        public static final String ID = DatabaseSchema.COLUMN_ID;
        public static final String OFFERING_FILTER = "offeringFilter";
        public static final String VEHICLE_TYPE_FILTER = "vehicleTypeFilter";

        public static final String CITY = "city";
        public static final String COUNTRY = "country";
    }

    public static abstract class Sync {
        public static final long SYNC_INTERVAL = 60L * 60L; // (hours * minutes * seconds)
    }

    public static abstract class GooglePlay {
        public static final int RESOLUTION_REQUEST = 9000;
    }

    public static abstract class RequestCodes {
        public static final int PERMISSION_MAPS_CURRENT_LOCATION = 2;
    }

    public static abstract class GoogleAnalytics {
        public static final String EVENT_CLICK = "OnClick";
        public static final String EVENT_DIALOG = "Dialog";
        public static final String EVENT_WORKSHOP_DISPLAY = "Workshop";
    }

    public static abstract class IntentFilters {
        public static final String GCM = Gcm.INTENT_GCM;
        public static final String CITY = "getCityIntentFilter";

        public static final String FLAG_CITY_STATUS = "getCityStatus";
    }
}
