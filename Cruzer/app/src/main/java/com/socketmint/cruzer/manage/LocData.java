package com.socketmint.cruzer.manage;

import android.content.Context;
import android.content.SharedPreferences;

import com.socketmint.cruzer.R;

public class LocData {
    private SharedPreferences sharedPreferences;

    private static abstract class Constants {
        public static final String COMMON_PREFERENCES_FILE = "cruzer_preferences";
        public static final String FORM_PREFERENCES_FILE = "form_preferences";

        public static final String TOKEN = "token";
        public static final String LONG_DATE = "long-date";
        public static final String MANU_ID = "manu-id";
        public static final String MODEL_ID = "model-id";
        public static final String VEHICLE_ID = "v-id";
        public static final String WORKSHOP_ID = "workshop-id";
        public static final String LOGIN_TYPE = "login_type";
        public static final String GCM = "gcm";
        public static final String GCM_SENT = "sentTokenToServer";
    }

    public void cruzerInstance(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.COMMON_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public void formInstance(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.FORM_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public void storeToken(String token) {
        sharedPreferences.edit().putString(Constants.TOKEN, token).apply();
    }

    public void storeLongDate(String longDate) {
        sharedPreferences.edit().putString(Constants.LONG_DATE, longDate).apply();
    }

    public String longDate() {
        return sharedPreferences.getString(Constants.LONG_DATE, "");
    }

    public String token() {
        return sharedPreferences.getString(Constants.TOKEN, "");
    }

    public void storeManuId(String id) {
        sharedPreferences.edit().putString(Constants.MANU_ID, id).apply();
    }

    public String manuId() {
        return sharedPreferences.getString(Constants.MANU_ID, "0");
    }

    public void storeModelId(String id) {
        sharedPreferences.edit().putString(Constants.MODEL_ID, id).apply();
    }

    public String modelId() {
        return sharedPreferences.getString(Constants.MODEL_ID, "0");
    }

    public void storeVId(String id) {
        sharedPreferences.edit().putString(Constants.VEHICLE_ID, id).apply();
    }

    public String vId() {
        return sharedPreferences.getString(Constants.VEHICLE_ID, null);
    }

    public void storeWorkshopId(String id) {
        sharedPreferences.edit().putString(Constants.WORKSHOP_ID, id).apply();
    }

    public String workshopId() {
        return sharedPreferences.getString(Constants.WORKSHOP_ID, null);
    }

    public void storeLoginType(int type) {
        sharedPreferences.edit().putInt(Constants.LOGIN_TYPE, type).apply();
    }

    public int loginType() {
        return sharedPreferences.getInt(Constants.LOGIN_TYPE, 0);
    }

    public void storeGcm(String gcm) {
        sharedPreferences.edit().putString(Constants.GCM, gcm).apply();
    }

    public String gcm() {
        return sharedPreferences.getString(Constants.GCM, "");
    }

    public void storeGcmSentStatus(boolean status) {
        sharedPreferences.edit().putBoolean(Constants.GCM_SENT, status).apply();
    }

    public boolean gcmSentStatus() {
        return sharedPreferences.getBoolean(Constants.GCM_SENT, false);
    }

    public void clearData() {
        sharedPreferences.edit().clear().apply();
    }
}
