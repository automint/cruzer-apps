package com.socketmint.cruzer.database;

import android.provider.BaseColumns;

public class DatabaseSchema {
    public DatabaseSchema() { }

    public static final String SYNC_STATUS = "sync_status";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SID = "s_id";
    public static final String COLUMN_VEHICLE_ID = "vehicle_id";
    public static final String COLUMN_USER_ID = "user_id";

    public static abstract class Users implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_FIRST_NAME = "firstname";
        public static final String COLUMN_LAST_NAME = "lastname";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_MOBILE = "mobile";
    }

    public static abstract class Manus implements BaseColumns {
        public static final String TABLE_NAME = "manus";
        public static final String COLUMN_NAME = "name";
    }

    public static abstract class Models implements BaseColumns {
        public static final String TABLE_NAME = "models";
        public static final String COLUMN_MANU_ID = "manu_id";
        public static final String COLUMN_NAME = "name";
    }

    public static abstract class Vehicles implements BaseColumns {
        public static final String TABLE_NAME = "vehicles";
        public static final String COLUMN_REG = "reg";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_USER_ID = DatabaseSchema.COLUMN_USER_ID;
        public static final String COLUMN_MODEL_ID = "model_id";
    }

    public static abstract class Workshops implements BaseColumns {
        public static final String TABLE_NAME = "workshops";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ADDRESS = "address";
        public static final String COLUMN_MANAGER = "manager";
        public static final String COLUMN_CONTACT = "contact";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_CITY = "city";
        public static final String COLUMN_AREA = "area";
        public static final String COLUMN_OFFERINGS = "offerings";
    }

    public static abstract class Services implements BaseColumns {
        public static final String TABLE_NAME = "services";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_WORKSHOP_ID = "workshop_id";
        public static final String COLUMN_COST = "cost";
        public static final String COLUMN_ODO = "odo";
        public static final String COLUMN_DETAILS = "details";
        public static final String COLUMN_STATUS = "status_id";
        public static final String COLUMN_USER_ID = DatabaseSchema.COLUMN_USER_ID;
        public static final String COLUMN_ROLE_ID = "role_id";
    }

    public static abstract class Refuels implements BaseColumns {
        public static final String TABLE_NAME = "refuels";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_RATE = "rate";
        public static final String COLUMN_VOLUME = "volume";
        public static final String COLUMN_COST = "cost";
        public static final String COLUMN_ODO = "odo";
    }

    public static abstract class Errors implements BaseColumns {
        public static final String TABLE_NAME = "messages";
        public static final String COLUMN_CODE = "code";
        public static final String COLUMN_MESSAGE = "message";
    }

    public static abstract class Problems implements BaseColumns {
        public static final String TABLE_NAME = "problems";
        public static final String COLUMN_SERVICE_ID = "service_id";
        public static final String COLUMN_DETAILS = "details";
        public static final String COLUMN_LCOST = "lcost";
        public static final String COLUMN_PCOST = "pcost";
        public static final String COLUMN_QTY = "qty";
    }

    public static abstract class Status implements BaseColumns {
        public static final String TABLE_NAME = "status";
        public static final String COLUMN_ID = DatabaseSchema.COLUMN_ID;
        public static final String COLUMN_DETAILS = "details";
    }
}
