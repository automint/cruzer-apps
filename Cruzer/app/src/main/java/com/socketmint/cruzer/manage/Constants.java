package com.socketmint.cruzer.manage;

import com.socketmint.cruzer.database.DatabaseSchema;

public class Constants {
    private static final String URL_SERVER = "http://10.1.1.104:8080";   // (server_ip:port) or (api.cruzer.io:8080)
    private static final String URL_API = "api";

    public static abstract class Url {
        public static final String OAUTH = URL_SERVER + "/" + URL_API + "/" + "oauth";
        public static final String AUTH = URL_SERVER + "/" + URL_API + "/" + "authenticate";
        public static final String USER = URL_SERVER + "/" + URL_API + "/" + "user";
        public static final String GET_MANU = URL_SERVER + "/" + URL_API + "/" + "manufacturers";
        public static final String MANU = URL_SERVER + "/" + URL_API + "/" + "manufacturer";
        public static final String GET_MODEL = URL_SERVER + "/" + URL_API + "/" + "models";
        public static final String MODEL = URL_SERVER + "/" + URL_API + "/" + "model";
        public static final String GET_VEHICLE = URL_SERVER + "/" + URL_API + "/" + "vehicles";
        public static final String PUT_VEHICLE = URL_SERVER + "/" + URL_API + "/" + "vehicle/";
        public static final String POST_VEHICLE = URL_SERVER + "/" + URL_API + "/" + "vehicle";
        public static final String GET_WORKSHOP = URL_SERVER + "/" + URL_API + "/" + "workshops";
        public static final String WORKSHOP = URL_SERVER + "/" + URL_API + "/" + "workshop";
        public static final String GET_REFUEL = URL_SERVER + "/" + URL_API + "/" + "refuels";
        public static final String PUT_REFUEL = URL_SERVER + "/" + URL_API + "/" + "refuel/";
        public static final String POST_REFUEL = URL_SERVER + "/" + URL_API + "/" + "refuel";
        public static final String GET_SERVICE = URL_SERVER + "/" + URL_API + "/" + "services";
        public static final String PUT_SERVICE = URL_SERVER + "/" + URL_API + "/" + "service/";
        public static final String POST_SERVICE = URL_SERVER + "/" + URL_API + "/" + "service";
        public static String GET_PROBLEMS(String serviceId) { return PUT_SERVICE + serviceId + "/problems"; }
        public static final String GCM = URL_SERVER + "/" + URL_API + "/" + "gcm";
    }

    public static abstract class VolleyRequest {
        public static final String METHOD_PUT = "PUT";
        public static final String METHOD_POST = "POST";
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
    }

    public static abstract class Gcm {
        public static final String FIELD_NEW_GCM = "newgcm";
        public static final String FIELD_OLD_GCM = "oldgcm";
        public static final String ACTION_GCM_REG_COMPLETE = "registrationComplete";

        public static final String KEY_DATA = "data";
    }

    public static abstract class Bundle {
        public static final String FORM_TYPE = "what";
        public static final String VEHICLE_ID = DatabaseSchema.COLUMN_VEHICLE_ID;
        public static final String PAGE_CHOICE = "view_type";
        public static final String ID = DatabaseSchema.COLUMN_ID;
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
        public static final String EVENT_PAGER = "Pager";
        public static final String EVENT_DIALOG = "Dialog";
        public static final String EVENT_WORKSHOP_DISPLAY = "Workshop";
    }
}
