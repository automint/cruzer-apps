package com.socketmint.cruzer.manage;

import android.content.Context;
import android.content.SharedPreferences;

public class LocData {
    private SharedPreferences sharedPreferences;

    private static abstract class Constants {
        public static final String COMMON_PREFERENCES_FILE = "cruzer_preferences";
        public static final String FORM_PREFERENCES_FILE = "form_preferences";

        public static final String HELP_SCREEN = "helpScreen";
        public static final String TOKEN = "token";
        public static final String MANU_ID = "manu-id";
        public static final String MODEL_ID = "model-id";
        public static final String LOGIN_TYPE = "login_type";
        public static final String GCM = "gcm";
        public static final String PAYTM_LIKE = "paytm_like";
    }

    public void cruzerInstance(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.COMMON_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public void formInstance(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.FORM_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public void storePayTmLike(boolean seen) {
        sharedPreferences.edit().putBoolean(Constants.PAYTM_LIKE, seen).apply();
    }

    public boolean payTmLike() {
        return sharedPreferences.getBoolean(Constants.PAYTM_LIKE, false);
    }

    public void storeHelpScreenSeen(boolean seen) {
        sharedPreferences.edit().putBoolean(Constants.HELP_SCREEN, seen).apply();
    }

    public boolean helpScreenSeen() {
        return sharedPreferences.getBoolean(Constants.HELP_SCREEN, false);
    }

    public void storeToken(String token) {
        sharedPreferences.edit().putString(Constants.TOKEN, token).apply();
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

    public void clearData() {
        sharedPreferences.edit().clear().apply();
    }
}
