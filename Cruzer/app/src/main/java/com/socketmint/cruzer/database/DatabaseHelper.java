package com.socketmint.cruzer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.socketmint.cruzer.dataholder.PUC;
import com.socketmint.cruzer.dataholder.insurance.Insurance;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.location.City;
import com.socketmint.cruzer.dataholder.location.Country;
import com.socketmint.cruzer.dataholder.vehicle.Manu;
import com.socketmint.cruzer.dataholder.vehicle.Model;
import com.socketmint.cruzer.dataholder.expense.service.Problem;
import com.socketmint.cruzer.dataholder.expense.Refuel;
import com.socketmint.cruzer.dataholder.expense.service.Service;
import com.socketmint.cruzer.dataholder.expense.service.Status;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.dataholder.workshop.Workshop;
import com.socketmint.cruzer.dataholder.workshop.WorkshopType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "automint.db";
    private static final String CREATE_TABLE = "create table if not exists ";
    private static final String FOREIGN_KEY = " foreign key ";
    private static final String DELETE_CASCADE = " on delete cascade";
    private static final String CONFLICT = " on conflict abort";
    public static final String DROP_TABLE = "drop table if exists ";
    public static final String SELECT_ALL = "select * from ";
    public static final String[] ALTER_TABLE = {"alter table ", " add column "};


    public static abstract class SyncStatus {
        public static final String NEW = "new";
        public static final String UPDATE = "updated";
        public static final String DELETE = "deleted";
        public static final String SYNCED = "synced";
        public static final String HIDE = "hide";
    }

    private static abstract class CreateStrings {
        public static final String USERS = CREATE_TABLE + DatabaseSchema.Users.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.Users.COLUMN_EMAIL + " text, "
                + DatabaseSchema.Users.COLUMN_FIRST_NAME + " text, "
                + DatabaseSchema.Users.COLUMN_LAST_NAME + " text, "
                + DatabaseSchema.Users.COLUMN_PASSWORD + " text, "
                + DatabaseSchema.Users.COLUMN_CITY_ID + " text, "
                + DatabaseSchema.Users.COLUMN_MOBILE + " text unique" + CONFLICT + ", "
                + DatabaseSchema.SYNC_STATUS + " text" + ")";
        public static final String MANUS = CREATE_TABLE + DatabaseSchema.Manus.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.Manus.COLUMN_NAME + " text unique" + CONFLICT + ", "
                + DatabaseSchema.SYNC_STATUS + " text" + ")";
        public static final String MODELS = CREATE_TABLE + DatabaseSchema.Models.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.Models.COLUMN_MANU_ID + " text, "
                + DatabaseSchema.Models.COLUMN_NAME + " text, "
                + DatabaseSchema.SYNC_STATUS + " text," + FOREIGN_KEY + "(" + DatabaseSchema.Models.COLUMN_MANU_ID + ") references " + DatabaseSchema.Manus.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ","
                + " unique (" + DatabaseSchema.Models.COLUMN_MANU_ID + ", " + DatabaseSchema.Models.COLUMN_NAME + ")" + CONFLICT + ")";
        public static final String VEHICLES = CREATE_TABLE + DatabaseSchema.Vehicles.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.Vehicles.COLUMN_REG + " text, "
                + DatabaseSchema.Vehicles.COLUMN_NAME + " text, "
                + DatabaseSchema.Vehicles.COLUMN_USER_ID + " text, "
                + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " text, "
                + DatabaseSchema.SYNC_STATUS + " text,"
                + FOREIGN_KEY + "(" + DatabaseSchema.Vehicles.COLUMN_USER_ID + ") references " + DatabaseSchema.Users.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ","
                + FOREIGN_KEY + "(" + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + ") references " + DatabaseSchema.Models.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ","
                + " unique (" + DatabaseSchema.Vehicles.COLUMN_REG + ", " + DatabaseSchema.Vehicles.COLUMN_USER_ID + ")" + CONFLICT + ")";
        public static final String WORKSHOPS  = CREATE_TABLE + DatabaseSchema.Workshops.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.Workshops.COLUMN_NAME + " text, "
                + DatabaseSchema.Workshops.COLUMN_ADDRESS + " text, "
                + DatabaseSchema.Workshops.COLUMN_MANAGER + " text, "
                + DatabaseSchema.Workshops.COLUMN_CONTACT + " text, "
                + DatabaseSchema.Workshops.COLUMN_LATITUDE + " text, "
                + DatabaseSchema.Workshops.COLUMN_LONGITUDE + " text, "
                + DatabaseSchema.Workshops.COLUMN_CITY_ID + " text, "
                + DatabaseSchema.Workshops.COLUMN_AREA + " text, "
                + DatabaseSchema.Workshops.COLUMN_OFFERINGS + " text, "
                + DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID + " text, "
                + DatabaseSchema.SYNC_STATUS + " text, "
                + FOREIGN_KEY + "(" + DatabaseSchema.Workshops.COLUMN_CITY_ID + ") references " + DatabaseSchema.Cities.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ","
                + FOREIGN_KEY + "(" + DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID + ") references " + DatabaseSchema.WorkshopTypes.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ")";
        public static final String SERVICES = CREATE_TABLE + DatabaseSchema.Services.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.COLUMN_VEHICLE_ID + " text, "
                + DatabaseSchema.Services.COLUMN_DATE + " text, "
                + DatabaseSchema.Services.COLUMN_WORKSHOP_ID + " text, "
                + DatabaseSchema.Services.COLUMN_COST + " text, "
                + DatabaseSchema.Services.COLUMN_ODO + " text, "
                + DatabaseSchema.Services.COLUMN_DETAILS + " text, "
                + DatabaseSchema.Services.COLUMN_STATUS + " text, "
                + DatabaseSchema.Services.COLUMN_USER_ID + " text, "
                + DatabaseSchema.Services.COLUMN_ROLE_ID + " text, "
                + DatabaseSchema.Services.COLUMN_VAT + " text, "
                + DatabaseSchema.SYNC_STATUS + " text,"
                + FOREIGN_KEY + "(" + DatabaseSchema.COLUMN_VEHICLE_ID + ") references " + DatabaseSchema.Vehicles.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ","
                + FOREIGN_KEY + "(" + DatabaseSchema.Services.COLUMN_WORKSHOP_ID + ") references " + DatabaseSchema.Workshops.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ")";
        public static final String REFUELS = CREATE_TABLE + DatabaseSchema.Refuels.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.COLUMN_VEHICLE_ID + " text, "
                + DatabaseSchema.Refuels.COLUMN_DATE + " text, "
                + DatabaseSchema.Refuels.COLUMN_RATE + " text, "
                + DatabaseSchema.Refuels.COLUMN_VOLUME + " text, "
                + DatabaseSchema.Refuels.COLUMN_COST + " text, "
                + DatabaseSchema.Refuels.COLUMN_ODO + " text, "
                + DatabaseSchema.SYNC_STATUS + " text,"
                + FOREIGN_KEY + "(" + DatabaseSchema.COLUMN_VEHICLE_ID + ") references " + DatabaseSchema.Vehicles.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ")";
        public static final String PROBLEMS = CREATE_TABLE + DatabaseSchema.Problems.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + " text primary key, "
                + DatabaseSchema.COLUMN_SID + " text unique" + CONFLICT + ", "
                + DatabaseSchema.Problems.COLUMN_SERVICE_ID + " text, "
                + DatabaseSchema.Problems.COLUMN_DETAILS + " text, "
                + DatabaseSchema.Problems.COLUMN_LCOST + " text, "
                + DatabaseSchema.Problems.COLUMN_PCOST + " text, "
                + DatabaseSchema.Problems.COLUMN_RATE + " text, "
                + DatabaseSchema.Problems.COLUMN_TYPE + " text, "
                + DatabaseSchema.SYNC_STATUS + " text, "
                + DatabaseSchema.Problems.COLUMN_QTY + " text, "
                + FOREIGN_KEY + "(" + DatabaseSchema.Problems.COLUMN_SERVICE_ID + ") references " + DatabaseSchema.Services.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ")";
        public static final String SERVICE_STATUS = CREATE_TABLE + DatabaseSchema.ServiceStatus.TABLE_NAME + "(" + DatabaseSchema.ServiceStatus.COLUMN_ID + " text primary key, "
                + DatabaseSchema.ServiceStatus.COLUMN_DETAILS + " text" + ")";
        public static final String CITIES = CREATE_TABLE + DatabaseSchema.Cities.TABLE_NAME + "(" + DatabaseSchema.Cities.COLUMN_ID + " text primary key, "
                + DatabaseSchema.Cities.COLUMN_CITY + " text, "
                + DatabaseSchema.Cities.COLUMN_COUNTRY_ID + " text,"
                + FOREIGN_KEY + "(" + DatabaseSchema.Cities.COLUMN_COUNTRY_ID + ") references " + DatabaseSchema.Countries.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ")";
        public static final String COUNTRIES = CREATE_TABLE + DatabaseSchema.Countries.TABLE_NAME + "(" + DatabaseSchema.Countries.COLUMN_ID + " text primary key, "
                + DatabaseSchema.Countries.COLUMN_COUNTRY + " text" + ")";
        public static final String WORKSHOP_TYPE = CREATE_TABLE + DatabaseSchema.WorkshopTypes.TABLE_NAME + "(" + DatabaseSchema.WorkshopTypes.COLUMN_ID + " text primary key, "
                + DatabaseSchema.WorkshopTypes.COLUMN_TYPE + " text" + ")";
        public static final String INSURANCE_COMPANIES = CREATE_TABLE + DatabaseSchema.InsuranceCompanies.TABLE_NAME + "(" + DatabaseSchema.InsuranceCompanies.COLUMN_ID + " text primary key, "
                + DatabaseSchema.InsuranceCompanies.COLUMN_COMPANY + " text" + ")";
        public static final String INSURANCE = CREATE_TABLE + DatabaseSchema.Insurances.TABLE_NAME + "(" + DatabaseSchema.InsuranceCompanies.COLUMN_ID + " text primary key, "
                + DatabaseSchema.Insurances.COLUMN_SID + " text, "
                + DatabaseSchema.Insurances.COLUMN_VEHICLE_ID + " text, "
                + DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID + " text, "
                + DatabaseSchema.Insurances.COLUMN_POLICY_NO + " text, "
                + DatabaseSchema.Insurances.COLUMN_START_DATE + " text, "
                + DatabaseSchema.Insurances.COLUMN_END_DATE + " text, "
                + DatabaseSchema.Insurances.COLUMN_PREMIUM + " text, "
                + DatabaseSchema.Insurances.COLUMN_DETAILS + " text, "
                + DatabaseSchema.SYNC_STATUS + " text, "
                + FOREIGN_KEY + "(" + DatabaseSchema.Insurances.COLUMN_VEHICLE_ID + ") references " + DatabaseSchema.Vehicles.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ", "
                + FOREIGN_KEY + "(" + DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID + ") references " + DatabaseSchema.InsuranceCompanies.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ")";
        public static final String PUC = CREATE_TABLE + DatabaseSchema.PUC.TABLE_NAME + "(" + DatabaseSchema.PUC.COLUMN_ID + " text primary key, "
                + DatabaseSchema.PUC.COLUMN_SID + " text, "
                + DatabaseSchema.PUC.COLUMN_VEHICLE_ID + " text, "
                + DatabaseSchema.PUC.COLUMN_WORKSHOP_ID + " text, "
                + DatabaseSchema.PUC.COLUMN_PUC_NO + " text, "
                + DatabaseSchema.PUC.COLUMN_START_DATE + " text, "
                + DatabaseSchema.PUC.COLUMN_END_DATE + " text, "
                + DatabaseSchema.PUC.COLUMN_FEES + " text, "
                + DatabaseSchema.PUC.COLUMN_DETAILS + " text, "
                + DatabaseSchema.SYNC_STATUS + " text, "
                + FOREIGN_KEY + "(" + DatabaseSchema.PUC.COLUMN_VEHICLE_ID + ") references " + DatabaseSchema.Vehicles.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ", "
                + FOREIGN_KEY + "(" + DatabaseSchema.PUC.COLUMN_WORKSHOP_ID + ") references " + DatabaseSchema.Workshops.TABLE_NAME + "(" + DatabaseSchema.COLUMN_ID + ")" + DELETE_CASCADE + ")";
    }

    private static abstract class Versions {
        public static final int VC_14 = 1;
        public static final int VC_15 = 2;
        public static final int VC_16 = 3;
        public static final int VC_17 = 4;
        public static final int VC_19 = 5;
        public static final int VC_22 = 6;
        public static final int VC_25 = 7;
        public static final int VC_26 = 8;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, Versions.VC_26);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // extras
        try { db.execSQL(CreateStrings.CITIES); } catch (SQLException e) { e.printStackTrace(); }
        try { db.execSQL(CreateStrings.COUNTRIES); } catch (SQLException e) { e.printStackTrace(); }

        // users
        try { db.execSQL(CreateStrings.USERS); } catch (SQLException e) { e.printStackTrace(); }

        // vehicle
        try { db.execSQL(CreateStrings.MANUS); } catch (SQLException e) { e.printStackTrace(); }
        try { db.execSQL(CreateStrings.MODELS); } catch (SQLException e) { e.printStackTrace(); }
        try { db.execSQL(CreateStrings.VEHICLES); } catch (SQLException e) { e.printStackTrace(); }

        // refuels
        try { db.execSQL(CreateStrings.REFUELS); } catch (SQLException e) { e.printStackTrace(); }

        // workshops
        try { db.execSQL(CreateStrings.WORKSHOP_TYPE); } catch (SQLException e) { e.printStackTrace(); }
        try { db.execSQL(CreateStrings.WORKSHOPS); } catch (SQLException e) { e.printStackTrace(); }

        //services
        try { db.execSQL(CreateStrings.SERVICE_STATUS); } catch (SQLException e) { e.printStackTrace(); }
        try { db.execSQL(CreateStrings.SERVICES); } catch (SQLException e) { e.printStackTrace(); }
        try { db.execSQL(CreateStrings.PROBLEMS); } catch (SQLException e) { e.printStackTrace(); }

        // insurance
        try { db.execSQL(CreateStrings.INSURANCE_COMPANIES); } catch (SQLException e) { e.printStackTrace(); }
        try { db.execSQL(CreateStrings.INSURANCE); } catch (SQLException e) { e.printStackTrace(); }

        // puc
        try { db.execSQL(CreateStrings.PUC); } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case Versions.VC_14:
                try { db.execSQL(CreateStrings.PROBLEMS); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Problems.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Problems.COLUMN_QTY + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Services.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Services.COLUMN_STATUS + " text"); } catch (SQLException e) { e.printStackTrace(); }
            case Versions.VC_15:
                try {
                    db.execSQL("PRAGMA writable_schema=1");
                    db.execSQL("UPDATE sqlite_master SET sql='" + CreateStrings.MODELS + "' WHERE type='table' AND name='" + DatabaseSchema.Models.TABLE_NAME + "';");
                    db.execSQL("UPDATE sqlite_master SET sql='" + CreateStrings.VEHICLES + "' WHERE type='table' AND name='" + DatabaseSchema.Vehicles.TABLE_NAME + "';");
                    db.execSQL("UPDATE sqlite_master SET sql='" + CreateStrings.SERVICES + "' WHERE type='table' AND name='" + DatabaseSchema.Services.TABLE_NAME + "';");
                    db.execSQL("UPDATE sqlite_master SET sql='" + CreateStrings.REFUELS + "' WHERE type='table' AND name='" + DatabaseSchema.Refuels.TABLE_NAME + "';");
                    db.execSQL("UPDATE sqlite_master SET sql='" + CreateStrings.PROBLEMS + "' WHERE type='table' AND name='" + DatabaseSchema.Problems.TABLE_NAME + "';");
                    db.execSQL("PRAGMA writable_schema=0");
                } catch (SQLException e) { e.printStackTrace(); }
            case Versions.VC_16:
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Workshops.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Workshops.COLUMN_LATITUDE + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Workshops.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Workshops.COLUMN_LONGITUDE + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Workshops.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Workshops.COLUMN_CITY_ID + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Workshops.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Workshops.COLUMN_AREA + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Workshops.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Workshops.COLUMN_OFFERINGS + " text"); } catch (SQLException e) { e.printStackTrace(); }
            case Versions.VC_17:
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Services.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Services.COLUMN_USER_ID + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Services.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Services.COLUMN_ROLE_ID + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.SERVICE_STATUS); } catch (SQLException e) { e.printStackTrace(); }
            case Versions.VC_19:
                try { db.execSQL(DROP_TABLE + "messages"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.CITIES); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.COUNTRIES); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.WORKSHOP_TYPE); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Users.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Users.COLUMN_CITY_ID + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try {
                    db.execSQL("PRAGMA writable_schema=1");
                    db.execSQL("UPDATE sqlite_master SET sql='" + CreateStrings.WORKSHOPS + "' WHERE type='table' AND name='" + DatabaseSchema.Workshops.TABLE_NAME + "';");
                    db.execSQL("PRAGMA writable_schema=0");
                } catch (SQLException e) { e.printStackTrace(); }
            case Versions.VC_22:
                try { db.execSQL(CreateStrings.INSURANCE_COMPANIES); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.INSURANCE); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.PUC); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Problems.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Problems.COLUMN_RATE + " text"); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Problems.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Problems.COLUMN_TYPE + " text"); } catch (SQLException e) { e.printStackTrace(); }
            case Versions.VC_25:
                try { db.execSQL(DROP_TABLE + DatabaseSchema.Insurances.TABLE_NAME); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(DROP_TABLE + DatabaseSchema.PUC.TABLE_NAME); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.INSURANCE); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(CreateStrings.PUC); } catch (SQLException e) { e.printStackTrace(); }
                try { db.execSQL(ALTER_TABLE[0] + DatabaseSchema.Services.TABLE_NAME + ALTER_TABLE[1] + DatabaseSchema.Services.COLUMN_VAT + " text"); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public String generateId(String tableName) {
        try {
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + tableName, null);
            if (cursor.getCount() > 0) {
                cursor.moveToLast();
                String prevId = cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID));
                String newId = String.valueOf(Integer.valueOf(prevId) + 1);
                cursor.close();
                return (newId);
            } else
                return "0";
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { e.printStackTrace(); return null; }
    }

    public String syncStatus(String tableName, List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(tableName, new String[]{"*"}, conString, values, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String syncState = cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS));
                cursor.close();
                return syncState;
            } else
                return null;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public boolean addUser(String sId, String mobile, String password, String firstName, String lastName, String email, String cityId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, generateId(DatabaseSchema.Users.TABLE_NAME));
            values.put(DatabaseSchema.COLUMN_SID, sId);
            values.put(DatabaseSchema.Users.COLUMN_MOBILE, (mobile.equalsIgnoreCase("null")) ? "" : mobile);
            values.put(DatabaseSchema.Users.COLUMN_PASSWORD, (password.equalsIgnoreCase("null")) ? "" : password);
            values.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, (firstName.equalsIgnoreCase("null")) ? "" : firstName);
            values.put(DatabaseSchema.Users.COLUMN_LAST_NAME, (lastName.equalsIgnoreCase("null")) ? "" : lastName);
            values.put(DatabaseSchema.Users.COLUMN_EMAIL, (email.equalsIgnoreCase("null")) ? "" : email);
            cityId = (cityId.equalsIgnoreCase("null") ? "" : cityId);
            if (!cityId.isEmpty())
                values.put(DatabaseSchema.Users.COLUMN_CITY_ID, cityId);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Users.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateUser(String id, String password, String firstName, String lastName, String email) {
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseSchema.Users.COLUMN_PASSWORD, password);
            values.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, (firstName.equalsIgnoreCase("null")) ? "" : firstName);
            values.put(DatabaseSchema.Users.COLUMN_LAST_NAME, (lastName.equalsIgnoreCase("null")) ? "" : lastName);
            values.put(DatabaseSchema.Users.COLUMN_EMAIL, (email.equalsIgnoreCase("null")) ? "" : email);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.UPDATE);
            getWritableDatabase().update(DatabaseSchema.Users.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateUser(String mobile, String firstName, String lastName, String email) {
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, (firstName.equalsIgnoreCase("null")) ? "" : firstName);
            values.put(DatabaseSchema.Users.COLUMN_LAST_NAME, (lastName.equalsIgnoreCase("null")) ? "" : lastName);
            values.put(DatabaseSchema.Users.COLUMN_EMAIL, (email.equalsIgnoreCase("null")) ? "" : email);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.UPDATE);
            getWritableDatabase().update(DatabaseSchema.Users.TABLE_NAME, values, DatabaseSchema.Users.COLUMN_MOBILE + "=?", new String[]{mobile});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateUserCity(String id, String cityId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Users.COLUMN_CITY_ID, cityId);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.UPDATE);

            getWritableDatabase().update(DatabaseSchema.Users.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public User user(String syncStatus) {
        try {
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Users.TABLE_NAME, new String[]{"*"}, DatabaseSchema.SYNC_STATUS + "=?", new String[]{syncStatus}, null, null, null);
            cursor.moveToFirst();

            User user = new User(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_PASSWORD)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_MOBILE)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_EMAIL)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_FIRST_NAME)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_LAST_NAME)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_CITY_ID)));
            cursor.close();
            return user;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public User user() {
        try {
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Users.TABLE_NAME, null);
            cursor.moveToFirst();

            User user = new User(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_PASSWORD)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_MOBILE)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_EMAIL)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_FIRST_NAME)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_LAST_NAME)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Users.COLUMN_CITY_ID)));
            cursor.close();
            return user;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public boolean addManu(String id, String name) {
        try {
            ContentValues values = new ContentValues();

            if (id.equals("null") || name.equals("null"))
                return false;
            values.put(DatabaseSchema.COLUMN_ID, id);
            values.put(DatabaseSchema.Manus.COLUMN_NAME, name);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Manus.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateManu(String id, String name) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, id);
            values.put(DatabaseSchema.Manus.COLUMN_NAME, name);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.Manus.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public List<Manu> manus() {
        try {
            List<Manu> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Manus.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                Manu object = new Manu(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Manus.COLUMN_NAME)));
                list.add(object);
                cursor.moveToNext();
            }
            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public Manu manu(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Manus.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            Manu object = new Manu(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Manus.COLUMN_NAME)));
            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public boolean addModel(String id, String manuId, String name) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, id);
            if (manuId.equals("null") || name.equals("null"))
                return false;
            values.put(DatabaseSchema.Models.COLUMN_MANU_ID, manuId);
            values.put(DatabaseSchema.Models.COLUMN_NAME, name);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Models.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateModel(String id, String manuId, String name) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, id);
            values.put(DatabaseSchema.Models.COLUMN_MANU_ID, manuId);
            values.put(DatabaseSchema.Models.COLUMN_NAME, name);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.Models.TABLE_NAME, values, DatabaseSchema.Models.COLUMN_NAME + "=? and " + DatabaseSchema.Models.COLUMN_MANU_ID + "=?", new String[]{name, manuId});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public List<Model> models(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            List<Model> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Models.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                Model object = new Model(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_MANU_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_NAME)));
                list.add(object);
                cursor.moveToNext();
            }
            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public Model model(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Models.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            Model object = new Model(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_MANU_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_NAME)));
            cursor.close();
            return object;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean addVehicle(String sId, String reg, String name, String uId, String modelId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, generateId(DatabaseSchema.Vehicles.TABLE_NAME));
            values.put(DatabaseSchema.COLUMN_SID, sId);
            values.put(DatabaseSchema.Vehicles.COLUMN_REG, (reg.equalsIgnoreCase("null")) ? "" : reg);
            values.put(DatabaseSchema.Vehicles.COLUMN_NAME, (name.equalsIgnoreCase("null")) ? "" : name);
            values.put(DatabaseSchema.Vehicles.COLUMN_USER_ID, uId);
            modelId = (modelId.equalsIgnoreCase("null")) ? "" : modelId;
            if (!modelId.isEmpty())
                values.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Vehicles.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean addVehicle(String reg, String name, String uId, String modelId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, generateId(DatabaseSchema.Vehicles.TABLE_NAME));
            values.put(DatabaseSchema.Vehicles.COLUMN_REG, reg);
            values.put(DatabaseSchema.Vehicles.COLUMN_NAME, name);
            values.put(DatabaseSchema.Vehicles.COLUMN_USER_ID, uId);
            if (!modelId.isEmpty())
                values.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.NEW);

            getWritableDatabase().insert(DatabaseSchema.Vehicles.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { e.printStackTrace(); return false; }
    }

    public String addVehicle(String reg, String sId, String modelId) {
        try {
            ContentValues values = new ContentValues();

            String id = generateId(DatabaseSchema.Vehicles.TABLE_NAME);
            values.put(DatabaseSchema.COLUMN_ID, id);
            values.put(DatabaseSchema.COLUMN_SID, sId);
            values.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
            values.put(DatabaseSchema.Vehicles.COLUMN_REG, reg);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Vehicles.TABLE_NAME, null, values);
            return id;
        } catch (SQLiteConstraintException e) { e.printStackTrace(); return null; }
    }

    public boolean updateVehicle(String id, String uId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Vehicles.COLUMN_USER_ID, uId);
            if (syncStatus(DatabaseSchema.Vehicles.TABLE_NAME, Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id}).equals(SyncStatus.NEW))
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.NEW);
            else
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.UPDATE);

            getWritableDatabase().update(DatabaseSchema.Vehicles.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateVehicle(String id, String reg, String name, String modelId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Vehicles.COLUMN_REG, reg);
            values.put(DatabaseSchema.Vehicles.COLUMN_NAME, name);
            if (!modelId.isEmpty())
                values.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
            if (syncStatus(DatabaseSchema.Vehicles.TABLE_NAME, Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id}).equals(SyncStatus.NEW))
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.NEW);
            else
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.UPDATE);

            getWritableDatabase().update(DatabaseSchema.Vehicles.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateVehicle(String sId, String reg, String modelId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Vehicles.COLUMN_REG, reg);
            values.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.Vehicles.TABLE_NAME, values, DatabaseSchema.COLUMN_SID + "=?", new String[]{sId});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public List<Vehicle> vehicles() {
        try {
            List<Vehicle> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery("select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_NAME + " as model_name, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as model_sid, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + " as manu_id, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as manu_sid, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.Manus.COLUMN_NAME + " as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME + " inner join "
                    + DatabaseSchema.Models.TABLE_NAME + " on " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + "="
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " inner join " + DatabaseSchema.Manus.TABLE_NAME + " on "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + "=" + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID
                    + " union select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + " '' as model_name, '' as model_sid, '' as " + DatabaseSchema.Models.COLUMN_MANU_ID + ", '' as manu_sid, '' as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME
                    + " where " + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " = '' or " + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " is null", null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                try {
                    if (cursor.getString(cursor.getColumnIndex("sync_status")).equals(SyncStatus.DELETE)) {
                        cursor.moveToNext();
                        continue;
                    }
                } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { e.printStackTrace(); break; }
                Vehicle object = new Vehicle(cursor.getString(cursor.getColumnIndex("vehicle_id")),
                        cursor.getString(cursor.getColumnIndex("vehicle_sid")),
                        cursor.getString(cursor.getColumnIndex("user_id")),
                        cursor.getString(cursor.getColumnIndex("model_id")),
                        cursor.getString(cursor.getColumnIndex("vehicle_reg")),
                        cursor.getString(cursor.getColumnIndex("vehicle_name")));
                String manuId = cursor.getString(cursor.getColumnIndex("manu_id"));
                if (!manuId.isEmpty()) {
                    Manu manu = new Manu(manuId, cursor.getString(cursor.getColumnIndex("manu_sid")), cursor.getString(cursor.getColumnIndex("manu_name")));
                    Model model = new Model(cursor.getString(cursor.getColumnIndex("model_id")), cursor.getString(cursor.getColumnIndex("model_sid")), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_MANU_ID)), cursor.getString(cursor.getColumnIndex("model_name")));
                    model.manu = manu;
                    object.model = model;
                }
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public List<Vehicle> vehicles(String syncStatus) {
        try {
            List<Vehicle> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery("select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_NAME + " as model_name, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as model_sid, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + " as manu_id, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as manu_sid, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.Manus.COLUMN_NAME + " as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME + " inner join "
                    + DatabaseSchema.Models.TABLE_NAME + " on " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + "="
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " inner join " + DatabaseSchema.Manus.TABLE_NAME + " on "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + "=" + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID
                    + " where " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + "=?" + " union select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + " '' as model_name, '' as model_sid, '' as " + DatabaseSchema.Models.COLUMN_MANU_ID + ", '' as manu_sid, '' as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME
                    + " where (" + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " = '' or " + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " is null) and "
                    + DatabaseSchema.SYNC_STATUS + "=?", new String[]{syncStatus, syncStatus});
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                Vehicle object = new Vehicle(cursor.getString(cursor.getColumnIndex("vehicle_id")),
                        cursor.getString(cursor.getColumnIndex("vehicle_sid")),
                        cursor.getString(cursor.getColumnIndex("user_id")),
                        cursor.getString(cursor.getColumnIndex("model_id")),
                        cursor.getString(cursor.getColumnIndex("vehicle_reg")),
                        cursor.getString(cursor.getColumnIndex("vehicle_name")));
                String manuId = cursor.getString(cursor.getColumnIndex("manu_id"));
                if (!manuId.isEmpty()) {
                    Manu manu = new Manu(manuId, cursor.getString(cursor.getColumnIndex("manu_sid")), cursor.getString(cursor.getColumnIndex("manu_name")));
                    Model model = new Model(cursor.getString(cursor.getColumnIndex("model_id")), cursor.getString(cursor.getColumnIndex("model_sid")), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_MANU_ID)), cursor.getString(cursor.getColumnIndex("model_name")));
                    model.manu = manu;
                    object.model = model;
                }
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public Vehicle vehicle(String id) {
        try {
            Cursor cursor = getReadableDatabase().rawQuery("select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_NAME + " as model_name, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as model_sid, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + " as manu_id, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as manu_sid, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.Manus.COLUMN_NAME + " as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME + " inner join "
                    + DatabaseSchema.Models.TABLE_NAME + " on " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + "="
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " inner join " + DatabaseSchema.Manus.TABLE_NAME + " on "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + "=" + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID
                    + " where " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + "=?" + " union select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + " '' as model_name, '' as model_sid, '' as " + DatabaseSchema.Models.COLUMN_MANU_ID + ", '' as manu_sid, '' as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME
                    + " where (" + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " = '' or " + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " is null) and "
                    + DatabaseSchema.COLUMN_ID + "=?", new String[]{id, id});
            cursor.moveToFirst();

            Vehicle object = new Vehicle(cursor.getString(cursor.getColumnIndex("vehicle_id")),
                    cursor.getString(cursor.getColumnIndex("vehicle_sid")),
                    cursor.getString(cursor.getColumnIndex("user_id")),
                    cursor.getString(cursor.getColumnIndex("model_id")),
                    cursor.getString(cursor.getColumnIndex("vehicle_reg")),
                    cursor.getString(cursor.getColumnIndex("vehicle_name")));
            String manuId = cursor.getString(cursor.getColumnIndex("manu_id"));
            if (!manuId.isEmpty()) {
                Manu manu = new Manu(manuId, cursor.getString(cursor.getColumnIndex("manu_sid")), cursor.getString(cursor.getColumnIndex("manu_name")));
                Model model = new Model(cursor.getString(cursor.getColumnIndex("model_id")), cursor.getString(cursor.getColumnIndex("model_sid")), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_MANU_ID)), cursor.getString(cursor.getColumnIndex("model_name")));
                model.manu = manu;
                object.model = model;
            }
            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public Vehicle vehicleByReg(String reg) {
        try {
            Cursor cursor = getReadableDatabase().rawQuery("select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_NAME + " as model_name, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as model_sid, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + " as manu_id, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as manu_sid, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.Manus.COLUMN_NAME + " as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME + " inner join "
                    + DatabaseSchema.Models.TABLE_NAME + " on " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + "="
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " inner join " + DatabaseSchema.Manus.TABLE_NAME + " on "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + "=" + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID
                    + " where " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + "=?" + " union select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + " '' as model_name, '' as model_sid, '' as " + DatabaseSchema.Models.COLUMN_MANU_ID + ", '' as manu_sid, '' as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME
                    + " where (" + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " = '' or " + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " is null) and "
                    + DatabaseSchema.Vehicles.COLUMN_REG + "=?", new String[]{reg, reg});
            cursor.moveToFirst();

            Vehicle object = new Vehicle(cursor.getString(cursor.getColumnIndex("vehicle_id")),
                    cursor.getString(cursor.getColumnIndex("vehicle_sid")),
                    cursor.getString(cursor.getColumnIndex("user_id")),
                    cursor.getString(cursor.getColumnIndex("model_id")),
                    cursor.getString(cursor.getColumnIndex("vehicle_reg")),
                    cursor.getString(cursor.getColumnIndex("vehicle_name")));
            String manuId = cursor.getString(cursor.getColumnIndex("manu_id"));
            if (!manuId.isEmpty()) {
                Manu manu = new Manu(manuId, cursor.getString(cursor.getColumnIndex("manu_sid")), cursor.getString(cursor.getColumnIndex("manu_name")));
                Model model = new Model(cursor.getString(cursor.getColumnIndex("model_id")), cursor.getString(cursor.getColumnIndex("model_sid")), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_MANU_ID)), cursor.getString(cursor.getColumnIndex("model_name")));
                model.manu = manu;
                object.model = model;
            }
            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public Vehicle vehicleBySid(String sId) {
        try {
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Vehicles.TABLE_NAME, new String[]{"*"}, DatabaseSchema.COLUMN_SID + "=?", new String[]{sId}, null, null, null);
            cursor.moveToFirst();

            Vehicle object = new Vehicle(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Vehicles.COLUMN_USER_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Vehicles.COLUMN_MODEL_ID)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Vehicles.COLUMN_REG)), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Vehicles.COLUMN_NAME)));
            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public int vehicleCount() {
        try {
            int count = 0;
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Vehicles.TABLE_NAME, null);
            cursor.moveToFirst();

            while(!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)) {
                    cursor.moveToNext();
                    continue;
                }
                count++;
                cursor.moveToNext();
            }

            cursor.close();
            return count;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { e.printStackTrace(); return 0; }
    }

    public Vehicle firstVehicle() {
        try {
            Cursor cursor = getReadableDatabase().rawQuery("select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_NAME + " as model_name, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as model_sid, "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + " as manu_id, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as manu_sid, "
                    + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.Manus.COLUMN_NAME + " as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME + " inner join "
                    + DatabaseSchema.Models.TABLE_NAME + " on " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + "="
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " inner join " + DatabaseSchema.Manus.TABLE_NAME + " on "
                    + DatabaseSchema.Models.TABLE_NAME + "." + DatabaseSchema.Models.COLUMN_MANU_ID + "=" + DatabaseSchema.Manus.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID
                    + " union select " + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_ID + " as vehicle_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.COLUMN_SID + " as vehicle_sid, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_REG + " as vehicle_reg, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_NAME + " as vehicle_name, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_USER_ID + " as user_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " as model_id, "
                    + DatabaseSchema.Vehicles.TABLE_NAME + "." + DatabaseSchema.SYNC_STATUS + " as sync_status, "
                    + " '' as model_name, '' as model_sid, '' as " + DatabaseSchema.Models.COLUMN_MANU_ID + ", '' as manu_sid, '' as manu_name from " + DatabaseSchema.Vehicles.TABLE_NAME
                    + " where " + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " = '' or " + DatabaseSchema.Vehicles.COLUMN_MODEL_ID + " is null", null);
            cursor.moveToFirst();
            if (cursor.getString(cursor.getColumnIndex("sync_status")).equals(SyncStatus.DELETE)) {
                cursor.moveToNext();
            }
            Vehicle object = new Vehicle(cursor.getString(cursor.getColumnIndex("vehicle_id")),
                    cursor.getString(cursor.getColumnIndex("vehicle_sid")),
                    cursor.getString(cursor.getColumnIndex("user_id")),
                    cursor.getString(cursor.getColumnIndex("model_id")),
                    cursor.getString(cursor.getColumnIndex("vehicle_reg")),
                    cursor.getString(cursor.getColumnIndex("vehicle_name")));
            String manuId = cursor.getString(cursor.getColumnIndex("manu_id"));
            if (!manuId.isEmpty()) {
                Manu manu = new Manu(manuId, cursor.getString(cursor.getColumnIndex("manu_sid")), cursor.getString(cursor.getColumnIndex("manu_name")));
                Model model = new Model(cursor.getString(cursor.getColumnIndex("model_id")), cursor.getString(cursor.getColumnIndex("model_sid")), cursor.getString(cursor.getColumnIndex(DatabaseSchema.Models.COLUMN_MANU_ID)), cursor.getString(cursor.getColumnIndex("model_name")));
                model.manu = manu;
                object.model = model;
            }
            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { e.printStackTrace();  return null;}
    }

    public boolean addRefuel(String sId, String vehicleId, String date, String rate, String volume, String cost, String odo) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, generateId(DatabaseSchema.Refuels.TABLE_NAME));
            values.put(DatabaseSchema.COLUMN_SID, sId);
            values.put(DatabaseSchema.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.Refuels.COLUMN_DATE, (date.equalsIgnoreCase("null")) ? "" : date);
            values.put(DatabaseSchema.Refuels.COLUMN_RATE, (rate.equalsIgnoreCase("null")) ? "" : rate);
            values.put(DatabaseSchema.Refuels.COLUMN_VOLUME, (volume.equalsIgnoreCase("null")) ? "" : volume);
            values.put(DatabaseSchema.Refuels.COLUMN_COST, (cost.equalsIgnoreCase("null")) ? "" : cost);
            values.put(DatabaseSchema.Refuels.COLUMN_ODO, (odo.equalsIgnoreCase("null")) ? "" : odo);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Refuels.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean addRefuel(String vehicleId, String date, String rate, String volume, String cost, String odo) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_ID, generateId(DatabaseSchema.Refuels.TABLE_NAME));
            values.put(DatabaseSchema.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.Refuels.COLUMN_DATE, date);
            values.put(DatabaseSchema.Refuels.COLUMN_RATE, rate);
            values.put(DatabaseSchema.Refuels.COLUMN_VOLUME, volume);
            values.put(DatabaseSchema.Refuels.COLUMN_COST, cost);
            values.put(DatabaseSchema.Refuels.COLUMN_ODO, odo);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.NEW);

            getWritableDatabase().insert(DatabaseSchema.Refuels.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateRefuel(String id, String date, String rate, String volume, String cost, String odo) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Refuels.COLUMN_DATE, date);
            values.put(DatabaseSchema.Refuels.COLUMN_RATE, rate);
            values.put(DatabaseSchema.Refuels.COLUMN_VOLUME, volume);
            values.put(DatabaseSchema.Refuels.COLUMN_COST, cost);
            values.put(DatabaseSchema.Refuels.COLUMN_ODO, odo);
            if (syncStatus(DatabaseSchema.Refuels.TABLE_NAME, Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id}).equals(SyncStatus.NEW))
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.NEW);
            else
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.UPDATE);

            getWritableDatabase().update(DatabaseSchema.Refuels.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public List<Refuel> refuels() {
        try {
            List<Refuel> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Refuels.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                        || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                Refuel object = new Refuel(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_RATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_VOLUME)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_COST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_ODO)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public List<Refuel> refuels(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            List<Refuel> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Refuels.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                        || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                Refuel object = new Refuel(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_RATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_VOLUME)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_COST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_ODO)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public List<Refuel> deletedRefuels() {
        try {
            List<Refuel> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Refuels.TABLE_NAME, new String[]{"*"}, DatabaseSchema.SYNC_STATUS + "=?", new String[]{SyncStatus.DELETE}, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                Refuel object = new Refuel(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_RATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_VOLUME)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_COST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_ODO)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public Refuel refuel(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Refuels.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                    || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                cursor.moveToNext();
            }
            Refuel object = new Refuel(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_DATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_RATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_VOLUME)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_COST)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Refuels.COLUMN_ODO)));

            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public String addService(String sId, String vehicleId, String date, String workshopId, String cost, String odo, String details, String status, String userId, String roleId, String vat) {
        try {
            ContentValues values = new ContentValues();

            String id = generateId(DatabaseSchema.Services.TABLE_NAME);
            values.put(DatabaseSchema.COLUMN_ID, id);
            values.put(DatabaseSchema.COLUMN_SID, sId);
            values.put(DatabaseSchema.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.Services.COLUMN_DATE, (date.equalsIgnoreCase("null")) ? "" : date);
            workshopId = (date.equalsIgnoreCase("null")) ? "" : workshopId;
            if (!workshopId.isEmpty())
                values.put(DatabaseSchema.Services.COLUMN_WORKSHOP_ID, workshopId);
            values.put(DatabaseSchema.Services.COLUMN_COST, (cost.equalsIgnoreCase("null")) ? "" : cost);
            values.put(DatabaseSchema.Services.COLUMN_ODO, (odo.equalsIgnoreCase("null")) ? "" : odo);
            values.put(DatabaseSchema.Services.COLUMN_STATUS, status);
            values.put(DatabaseSchema.Services.COLUMN_DETAILS, (details.equalsIgnoreCase("null")) ? "" : details);
            values.put(DatabaseSchema.Services.COLUMN_USER_ID, userId);
            values.put(DatabaseSchema.Services.COLUMN_ROLE_ID, roleId);
            values.put(DatabaseSchema.Services.COLUMN_VAT, (vat.equalsIgnoreCase("null")) ? "" : vat);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Services.TABLE_NAME, null, values);
            return id;
        } catch (SQLiteConstraintException e) { return null; }
    }

    public String addService(String vehicleId, String date, String cost, String odo, String details) {
        try {
            ContentValues values = new ContentValues();

            String id = generateId(DatabaseSchema.Services.TABLE_NAME);
            values.put(DatabaseSchema.COLUMN_ID, id);
            values.put(DatabaseSchema.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.Services.COLUMN_DATE, date);
            values.put(DatabaseSchema.Services.COLUMN_COST, cost);
            values.put(DatabaseSchema.Services.COLUMN_ODO, odo);
            values.put(DatabaseSchema.Services.COLUMN_DETAILS, details);
            values.put(DatabaseSchema.Services.COLUMN_USER_ID, user().getsId());
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.NEW);

            getWritableDatabase().insert(DatabaseSchema.Services.TABLE_NAME, null, values);
            return id;
        } catch (SQLiteConstraintException e) { return null; }
    }

    public boolean updateService(String id, String date, String cost, String odo, String details) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Services.COLUMN_DATE, date);
            values.put(DatabaseSchema.Services.COLUMN_COST, cost);
            values.put(DatabaseSchema.Services.COLUMN_ODO, odo);
            values.put(DatabaseSchema.Services.COLUMN_DETAILS, details);
            if (syncStatus(DatabaseSchema.Services.TABLE_NAME, Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id}).equals(SyncStatus.NEW))
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.NEW);
            else
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.UPDATE);

            getWritableDatabase().update(DatabaseSchema.Services.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateService(String sId, String vehicleId, String date, String workshopId, String cost, String odo, String details, String status, String userId, String roleId, String vat) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.Services.COLUMN_DATE, (date.equalsIgnoreCase("null")) ? "" : date);
            workshopId = (date.equalsIgnoreCase("null")) ? "" : workshopId;
            if (!workshopId.isEmpty())
                values.put(DatabaseSchema.Services.COLUMN_WORKSHOP_ID, workshopId);
            values.put(DatabaseSchema.Services.COLUMN_COST, (cost.equalsIgnoreCase("null")) ? "" : cost);
            values.put(DatabaseSchema.Services.COLUMN_ODO, (odo.equalsIgnoreCase("null")) ? "" : odo);
            values.put(DatabaseSchema.Services.COLUMN_DETAILS, (details.equalsIgnoreCase("null")) ? "" : details);
            values.put(DatabaseSchema.Services.COLUMN_STATUS, status);
            values.put(DatabaseSchema.Services.COLUMN_USER_ID, userId);
            values.put(DatabaseSchema.Services.COLUMN_ROLE_ID, roleId);
            values.put(DatabaseSchema.Services.COLUMN_VAT, (vat.equalsIgnoreCase("null")) ? "" : vat);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.Services.TABLE_NAME, values, DatabaseSchema.COLUMN_SID + "=?", new String[]{sId});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public List<Service> services() {
        try {
            List<Service> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Services.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                        || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                Service object = new Service(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_WORKSHOP_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_COST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ODO)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DETAILS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_STATUS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ROLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_VAT)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public List<Service> deletedServices() {
        try {
            List<Service> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Services.TABLE_NAME, new String[]{"*"}, DatabaseSchema.SYNC_STATUS + "=?", new String[]{SyncStatus.DELETE}, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                Service object = new Service(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_WORKSHOP_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_COST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ODO)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DETAILS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_STATUS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ROLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_VAT)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public List<Service> services(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            List<Service> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Services.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                        || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                Service object = new Service(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_WORKSHOP_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_COST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ODO)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DETAILS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_STATUS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ROLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_VAT)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public Service service(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Services.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                    || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                cursor.moveToNext();
            }

            Service object = new Service(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_VEHICLE_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_WORKSHOP_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_COST)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ODO)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_DETAILS)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_STATUS)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_ROLE_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Services.COLUMN_VAT)));
            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public boolean addProblem(String sId, String serviceId, String details, String lCost, String pCost, String qty, String rate, String type) {
        try {
            ContentValues values = new ContentValues();

            String id = generateId(DatabaseSchema.Problems.TABLE_NAME);
            values.put(DatabaseSchema.COLUMN_ID, id);
            values.put(DatabaseSchema.COLUMN_SID, sId);
            values.put(DatabaseSchema.Problems.COLUMN_SERVICE_ID, serviceId);
            values.put(DatabaseSchema.Problems.COLUMN_DETAILS, (details.equalsIgnoreCase("null")) ? "" : details);
            values.put(DatabaseSchema.Problems.COLUMN_LCOST, (lCost.equalsIgnoreCase("null")) ? "" : lCost);
            values.put(DatabaseSchema.Problems.COLUMN_PCOST, (pCost.equalsIgnoreCase("null")) ? "" : pCost);
            values.put(DatabaseSchema.Problems.COLUMN_QTY, (qty.equalsIgnoreCase("null")) ? "" : qty);
            values.put(DatabaseSchema.Problems.COLUMN_RATE, (rate.equalsIgnoreCase("null")) ? "" : rate);
            values.put(DatabaseSchema.Problems.COLUMN_TYPE, (type.equalsIgnoreCase("null")) ? "" : type);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Problems.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateProblem(String sId, String serviceId, String details, String lCost, String pCost, String qty, String rate, String type) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Problems.COLUMN_SERVICE_ID, serviceId);
            values.put(DatabaseSchema.Problems.COLUMN_DETAILS, (details.equalsIgnoreCase("null")) ? "" : details);
            values.put(DatabaseSchema.Problems.COLUMN_LCOST, (lCost.equalsIgnoreCase("null")) ? "" : lCost);
            values.put(DatabaseSchema.Problems.COLUMN_PCOST, (pCost.equalsIgnoreCase("null")) ? "" : pCost);
            values.put(DatabaseSchema.Problems.COLUMN_QTY, (qty.equalsIgnoreCase("null")) ? "" : qty);
            values.put(DatabaseSchema.Problems.COLUMN_RATE, (rate.equalsIgnoreCase("null")) ? "" : rate);
            values.put(DatabaseSchema.Problems.COLUMN_TYPE, (type.equalsIgnoreCase("null")) ? "" : type);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.Problems.TABLE_NAME, values, DatabaseSchema.COLUMN_SID + "=?", new String[]{sId});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public Problem problem(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Problems.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                    || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                cursor.moveToNext();
            }

            Problem object = new Problem(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_SERVICE_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_DETAILS)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_LCOST)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_PCOST)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_QTY)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_RATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_TYPE)));
            cursor.close();
            return object;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<Problem> problems(List<String> constraints, String[] values) {
        try {
            List<Problem> list = new ArrayList<>();
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Problems.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)
                        || cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                Problem object = new Problem(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_SERVICE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_DETAILS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_LCOST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_PCOST)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_QTY)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_RATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Problems.COLUMN_TYPE)));
                list.add(object);
                cursor.moveToNext();
            }
            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean addWorkshop(String id, String name, String address, String manager, String contact, String latitude, String longitude, String cityId, String area, String offerings, String workshopTypeId) {
        try {
            ContentValues values = new ContentValues();

            String city = (cityId.equalsIgnoreCase("null") ? "" : cityId);
            String workshopType = (workshopTypeId.equalsIgnoreCase("null") ? "" : workshopTypeId);

            values.put(DatabaseSchema.COLUMN_ID, id);
            if (name.equals("null"))
                return false;
            values.put(DatabaseSchema.Workshops.COLUMN_NAME, (name.equalsIgnoreCase("null")) ? "" : name);
            values.put(DatabaseSchema.Workshops.COLUMN_ADDRESS, (address.equalsIgnoreCase("null")) ? "" : address);
            values.put(DatabaseSchema.Workshops.COLUMN_MANAGER, (manager.equalsIgnoreCase("null")) ? "" : manager);
            values.put(DatabaseSchema.Workshops.COLUMN_CONTACT, (contact.equalsIgnoreCase("null")) ? "" : contact);
            values.put(DatabaseSchema.Workshops.COLUMN_LATITUDE, (latitude.equals("null")) ? "" : latitude);
            values.put(DatabaseSchema.Workshops.COLUMN_LONGITUDE, (longitude.equals("null")) ? "" : longitude);
            if (!city.isEmpty())
                values.put(DatabaseSchema.Workshops.COLUMN_CITY_ID, city);
            values.put(DatabaseSchema.Workshops.COLUMN_AREA, (area.equalsIgnoreCase("null")) ? "" : area);
            values.put(DatabaseSchema.Workshops.COLUMN_OFFERINGS, (offerings.equalsIgnoreCase("null")) ? "" : offerings);
            if (!workshopType.isEmpty())
                values.put(DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID, workshopType);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Workshops.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) {  return false;  }
    }

    public boolean updateWorkshop(String id, String name, String address, String manager, String contact, String latitude, String longitude, String cityId, String area, String offerings, String workshopTypeId) {
        try {
            ContentValues values = new ContentValues();

            String city = (cityId.equalsIgnoreCase("null") ? "" : cityId);
            String workshopType = (workshopTypeId.equalsIgnoreCase("null") ? "" : workshopTypeId);

            if (name.equals("null"))
                return false;
            values.put(DatabaseSchema.Workshops.COLUMN_NAME, (name.equalsIgnoreCase("null")) ? "" : name);
            values.put(DatabaseSchema.Workshops.COLUMN_ADDRESS, (address.equalsIgnoreCase("null")) ? "" : address);
            values.put(DatabaseSchema.Workshops.COLUMN_MANAGER, (manager.equalsIgnoreCase("null")) ? "" : manager);
            values.put(DatabaseSchema.Workshops.COLUMN_CONTACT, (contact.equalsIgnoreCase("null")) ? "" : contact);
            values.put(DatabaseSchema.Workshops.COLUMN_LATITUDE, (latitude.equals("null")) ? "" : latitude);
            values.put(DatabaseSchema.Workshops.COLUMN_LONGITUDE, (longitude.equals("null")) ? "" : longitude);
            if (!city.isEmpty())
                values.put(DatabaseSchema.Workshops.COLUMN_CITY_ID, city);
            values.put(DatabaseSchema.Workshops.COLUMN_AREA, (area.equalsIgnoreCase("null")) ? "" : area);
            values.put(DatabaseSchema.Workshops.COLUMN_OFFERINGS, (offerings.equalsIgnoreCase("null")) ? "" : offerings);
            if (!workshopType.isEmpty())
                values.put(DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID, workshopType);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.Workshops.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public List<Workshop> workshops() {
        try {
            List<Workshop> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Workshops.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE)) {
                    cursor.moveToNext();
                    continue;
                }
                Workshop object = new Workshop(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_ADDRESS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_MANAGER)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_CONTACT)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_LATITUDE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_LONGITUDE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_CITY_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_AREA)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_OFFERINGS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { e.printStackTrace(); return null; }
    }

    public Workshop workshop(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Workshops.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            Workshop object = new Workshop(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_ADDRESS)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_MANAGER)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_CONTACT)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_LATITUDE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_LONGITUDE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_CITY_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_AREA)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_OFFERINGS)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID)));

            cursor.close();
            return object;
        } catch (CursorIndexOutOfBoundsException | IllegalArgumentException | IllegalStateException e) { return null; }
    }

    public boolean addStatus(String id, String details) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.ServiceStatus.COLUMN_ID, id);
            values.put(DatabaseSchema.ServiceStatus.COLUMN_DETAILS, details);

            getWritableDatabase().insert(DatabaseSchema.ServiceStatus.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public List<Status> statusList() {
        try {
            List<Status> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.ServiceStatus.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                Status object = new Status(cursor.getString(cursor.getColumnIndex(DatabaseSchema.ServiceStatus.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.ServiceStatus.COLUMN_DETAILS)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean addCity(String id, String city, String countryId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Cities.COLUMN_ID, id);
            values.put(DatabaseSchema.Cities.COLUMN_CITY, city);
            values.put(DatabaseSchema.Cities.COLUMN_COUNTRY_ID, countryId);

            getWritableDatabase().insert(DatabaseSchema.Cities.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateCity(String id, String city, String countryId) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Cities.COLUMN_CITY, city);
            values.put(DatabaseSchema.Cities.COLUMN_COUNTRY_ID, countryId);

            getWritableDatabase().update(DatabaseSchema.Cities.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean addCountry(String id, String country) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Countries.COLUMN_ID, id);
            values.put(DatabaseSchema.Countries.COLUMN_COUNTRY, country);

            getWritableDatabase().insert(DatabaseSchema.Countries.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateCountry(String id, String country) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Countries.COLUMN_COUNTRY, country);

            getWritableDatabase().update(DatabaseSchema.Countries.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public City city(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Cities.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            City object = new City(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Cities.COLUMN_COUNTRY_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Cities.COLUMN_CITY)));

            cursor.close();
            return object;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<City> cities() {
        try {
            List<City> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Cities.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                City object = new City(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Cities.COLUMN_COUNTRY_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Cities.COLUMN_CITY)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<City> cities(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            List<City> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Cities.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                City object = new City(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Cities.COLUMN_COUNTRY_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Cities.COLUMN_CITY)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public Country country(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Countries.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            Country object = new Country(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Countries.COLUMN_COUNTRY)));

            cursor.close();
            return object;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<Country> countries() {
        try {
            List<Country> list = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Countries.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                Country object = new Country(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Countries.COLUMN_COUNTRY)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean addWorkshopType(String id, String type) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.WorkshopTypes.COLUMN_ID, id);
            values.put(DatabaseSchema.WorkshopTypes.COLUMN_TYPE, type);

            getWritableDatabase().insert(DatabaseSchema.WorkshopTypes.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateWorkshopType(String id, String type) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.WorkshopTypes.COLUMN_TYPE, type);

            getWritableDatabase().update(DatabaseSchema.WorkshopTypes.TABLE_NAME, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public WorkshopType workshopType(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getWritableDatabase().query(DatabaseSchema.WorkshopTypes.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            WorkshopType object = new WorkshopType(cursor.getString(cursor.getColumnIndex(DatabaseSchema.WorkshopTypes.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.WorkshopTypes.COLUMN_TYPE)));

            cursor.close();
            return object;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<WorkshopType> workshopTypes() {
        try {
            List<WorkshopType> list = new ArrayList<>();
            Cursor cursor = getWritableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.WorkshopTypes.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                WorkshopType object = new WorkshopType(cursor.getString(cursor.getColumnIndex(DatabaseSchema.WorkshopTypes.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.WorkshopTypes.COLUMN_TYPE)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean addInsuranceCompany(String id, String company) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.InsuranceCompanies.COLUMN_ID, id);
            values.put(DatabaseSchema.InsuranceCompanies.COLUMN_COMPANY, company);

            getWritableDatabase().insert(DatabaseSchema.InsuranceCompanies.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateInsuranceCompany(String id, String company) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.InsuranceCompanies.COLUMN_COMPANY, company);

            getWritableDatabase().update(DatabaseSchema.InsuranceCompanies.TABLE_NAME, values, DatabaseSchema.InsuranceCompanies.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public InsuranceCompany insuranceCompany(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }
            Cursor cursor = getReadableDatabase().query(DatabaseSchema.InsuranceCompanies.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            InsuranceCompany object = new InsuranceCompany(cursor.getString(cursor.getColumnIndex(DatabaseSchema.InsuranceCompanies.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.InsuranceCompanies.COLUMN_COMPANY)));

            cursor.close();
            return object;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<InsuranceCompany> insuranceCompanies() {
        try {
            List<InsuranceCompany> list = new ArrayList<>();

            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.InsuranceCompanies.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                InsuranceCompany object = new InsuranceCompany(cursor.getString(cursor.getColumnIndex(DatabaseSchema.InsuranceCompanies.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.InsuranceCompanies.COLUMN_COMPANY)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean addInsurance(String sId, String vehicleId, String companyId, String policyNo, String startDate, String endDate, String premium, String details) {
        try {
            ContentValues values = new ContentValues();

            String id = generateId(DatabaseSchema.Insurances.TABLE_NAME);
            values.put(DatabaseSchema.Insurances.COLUMN_ID, id);
            values.put(DatabaseSchema.Insurances.COLUMN_SID, sId);
            values.put(DatabaseSchema.Insurances.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID, companyId);
            values.put(DatabaseSchema.Insurances.COLUMN_POLICY_NO, policyNo);
            values.put(DatabaseSchema.Insurances.COLUMN_START_DATE, startDate);
            values.put(DatabaseSchema.Insurances.COLUMN_END_DATE, endDate);
            values.put(DatabaseSchema.Insurances.COLUMN_PREMIUM, premium);
            values.put(DatabaseSchema.Insurances.COLUMN_DETAILS, details);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.Insurances.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updateInsurance(String sId, String vehicleId, String companyId, String policyNo, String startDate, String endDate, String premium, String details) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.Insurances.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID, companyId);
            values.put(DatabaseSchema.Insurances.COLUMN_POLICY_NO, policyNo);
            values.put(DatabaseSchema.Insurances.COLUMN_START_DATE, startDate);
            values.put(DatabaseSchema.Insurances.COLUMN_END_DATE, endDate);
            values.put(DatabaseSchema.Insurances.COLUMN_PREMIUM, premium);
            values.put(DatabaseSchema.Insurances.COLUMN_DETAILS, details);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.Insurances.TABLE_NAME, values, DatabaseSchema.COLUMN_SID + "=?", new String[]{sId});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public Insurance insurance(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }

            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Insurances.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            Insurance object = new Insurance(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_VEHICLE_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_POLICY_NO)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_START_DATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_END_DATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_PREMIUM)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_DETAILS)));

            cursor.close();
            return object;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<Insurance> insurances(List<String> constraints, String[] values) {
        try {
            List<Insurance> list = new ArrayList<>();
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }

            Cursor cursor = getReadableDatabase().query(DatabaseSchema.Insurances.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE) ||
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                Insurance object = new Insurance(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_POLICY_NO)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_START_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_END_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_PREMIUM)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_DETAILS)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<Insurance> insurances() {
        try {
            List<Insurance> list = new ArrayList<>();

            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.Insurances.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE) ||
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                Insurance object = new Insurance(cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_POLICY_NO)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_START_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_END_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_PREMIUM)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.Insurances.COLUMN_DETAILS)));
                list.add(object);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean addPUC(String sId, String vehicleId, String workshopId, String pucNo, String startDate, String endDate, String fees, String details) {
        try {
            ContentValues values = new ContentValues();

            String id = generateId(DatabaseSchema.PUC.TABLE_NAME);
            values.put(DatabaseSchema.PUC.COLUMN_ID, id);
            values.put(DatabaseSchema.PUC.COLUMN_SID, sId);
            values.put(DatabaseSchema.PUC.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.PUC.COLUMN_WORKSHOP_ID, workshopId);
            values.put(DatabaseSchema.PUC.COLUMN_PUC_NO, pucNo);
            values.put(DatabaseSchema.PUC.COLUMN_START_DATE, startDate);
            values.put(DatabaseSchema.PUC.COLUMN_END_DATE, endDate);
            values.put(DatabaseSchema.PUC.COLUMN_FEES, fees);
            values.put(DatabaseSchema.PUC.COLUMN_DETAILS, details);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().insert(DatabaseSchema.PUC.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean updatePUC(String sId, String vehicleId, String workshopId, String pucNo, String startDate, String endDate, String fees, String details) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.PUC.COLUMN_VEHICLE_ID, vehicleId);
            values.put(DatabaseSchema.PUC.COLUMN_WORKSHOP_ID, workshopId);
            values.put(DatabaseSchema.PUC.COLUMN_PUC_NO, pucNo);
            values.put(DatabaseSchema.PUC.COLUMN_START_DATE, startDate);
            values.put(DatabaseSchema.PUC.COLUMN_END_DATE, endDate);
            values.put(DatabaseSchema.PUC.COLUMN_FEES, fees);
            values.put(DatabaseSchema.PUC.COLUMN_DETAILS, details);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(DatabaseSchema.PUC.TABLE_NAME, values, DatabaseSchema.COLUMN_SID, new String[]{sId});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public PUC puc(List<String> constraints, String[] values) {
        try {
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }

            Cursor cursor = getReadableDatabase().query(DatabaseSchema.PUC.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            PUC puc = new PUC(cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_SID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_VEHICLE_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_WORKSHOP_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_PUC_NO)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_START_DATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_END_DATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_FEES)),
                    cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_DETAILS)));

            cursor.close();
            return puc;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<PUC> pucList(List<String> constraints, String[] values) {
        try {
            List<PUC> list = new ArrayList<>();
            String conString = constraints.get(0) + "=?";
            if (constraints.size() > 1) {
                for (int i = 1; i < constraints.size(); i++) {
                    conString = conString.concat(" and " + constraints.get(i) + "=?");
                }
            }

            Cursor cursor = getReadableDatabase().query(DatabaseSchema.PUC.TABLE_NAME, new String[]{"*"}, conString, values, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE) ||
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                PUC puc = new PUC(cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_WORKSHOP_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_PUC_NO)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_START_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_END_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_FEES)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_DETAILS)));
                list.add(puc);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public List<PUC> pucList() {
        try {
            List<PUC> list = new ArrayList<>();

            Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL + DatabaseSchema.PUC.TABLE_NAME, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                if (cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.DELETE) ||
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.SYNC_STATUS)).equals(SyncStatus.HIDE)) {
                    cursor.moveToNext();
                    continue;
                }
                PUC puc = new PUC(cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_SID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_VEHICLE_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_WORKSHOP_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_PUC_NO)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_START_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_END_DATE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_FEES)),
                        cursor.getString(cursor.getColumnIndex(DatabaseSchema.PUC.COLUMN_DETAILS)));
                list.add(puc);
                cursor.moveToNext();
            }

            cursor.close();
            return list;
        } catch (IllegalArgumentException | IllegalStateException | CursorIndexOutOfBoundsException e) { return null; }
    }

    public boolean deleteLocal(String tableName, String id) {
        try {
            if (syncStatus(tableName, Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id}).equals(SyncStatus.NEW))
                return delete(tableName, id);
            else {
                ContentValues values = new ContentValues();
                values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.DELETE);
                getWritableDatabase().update(tableName, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
                if (tableName.equals(DatabaseSchema.Vehicles.TABLE_NAME)) {
                    hide(DatabaseSchema.Refuels.TABLE_NAME, id);
                    hide(DatabaseSchema.Services.TABLE_NAME, id);
                }
                return true;
            }
        } catch (SQLiteConstraintException e) { return false; }
    }

    private boolean hide(String tableName, String vId) {
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.HIDE);
            getWritableDatabase().update(tableName, values, DatabaseSchema.COLUMN_VEHICLE_ID + "=?", new String[]{vId});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean delete(String tableName, String id) {
        try {
            getWritableDatabase().delete(tableName, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean syncRecord(String id, String sId, String tableName) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.COLUMN_SID, sId);
            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(tableName, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }

    public boolean syncRecord(String id, String tableName) {
        try {
            ContentValues values = new ContentValues();

            values.put(DatabaseSchema.SYNC_STATUS, SyncStatus.SYNCED);

            getWritableDatabase().update(tableName, values, DatabaseSchema.COLUMN_ID + "=?", new String[]{id});
            return true;
        } catch (SQLiteConstraintException e) { return false; }
    }
}
