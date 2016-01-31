package com.socketmint.cruzer.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;

import com.socketmint.cruzer.manage.LocData;

import java.io.ByteArrayOutputStream;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class UiElement {
    private static UiElement instance;

    private Activity activity;

    private SimpleDateFormat displayDateFormat, serverDateFormat;

    private DatePickerDialog datePickerDialog;

    private LocData locData = new LocData();

    /** @deprecated Switch to non singleton */
    @Deprecated
    public static UiElement getInstance() {
        if (instance == null)
            instance = new UiElement();
        return instance;
    }

    /** @deprecated Switch to non singleton */
    @Deprecated
    public void initInstance(Activity activity) {
        this.activity = activity;
        locData.formInstance(activity);
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", activity.getResources().getConfiguration().locale);
        serverDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", activity.getResources().getConfiguration().locale);
    }

    /** @deprecated Switch to non singleton */
    @Deprecated
    public void changeActivity(Activity activity) {
        this.activity = activity;
        locData.formInstance(activity);
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", activity.getResources().getConfiguration().locale);
        serverDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", activity.getResources().getConfiguration().locale);
    }

    /** @deprecated Switch to non singleton */
    @Deprecated
    public UiElement() { }

    public UiElement(Activity activity) {
        this.activity = activity;
        locData.formInstance(activity);
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", activity.getResources().getConfiguration().locale);
        serverDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", activity.getResources().getConfiguration().locale);
    }

    public boolean validateEmail(String email) {
        try {
            String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
            return email.matches(emailPattern);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public String imgToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] b = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public Bitmap base64ToImg(String string) {
        byte[] b = Base64.decode(string, 0);
        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }

    public void setDatePickerDialog(String title, final EditText editDate) {
        Calendar calendar = Calendar.getInstance();
        datePickerDialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();
                date.set(year, monthOfYear, dayOfMonth);
                Date longDate = date.getTime();
                locData.storeLongDate(serverDateFormat.format(longDate));
                editDate.setText(displayDateFormat.format(longDate));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
        datePickerDialog.setTitle(title);
    }

    public void showDatePickerDialog() {
        datePickerDialog.show();
    }

    public int getDateFromString(String date) {
        int result = 0;
        final Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(displayDateFormat.parse(date));
            result = calendar.get(Calendar.DAY_OF_MONTH);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String month(String date) {
        String result = "";
        final Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(displayDateFormat.parse(date));
            int month = calendar.get(Calendar.MONTH);
            result = new DateFormatSymbols().getMonths()[month - 1];
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int year(String date) {
        int result = 0;
        final Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(displayDateFormat.parse(date));
            result = calendar.get(Calendar.YEAR);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String currentDate() {
        try {
            Calendar calendar = Calendar.getInstance();
            Date date = calendar.getTime();
            locData.storeLongDate(serverDateFormat.format(date));
            return (displayDateFormat.format(date));
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public String date(String longDate) {
        try {
            locData.storeLongDate(longDate);
            return displayDateFormat.format(serverDateFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    public String date(String date, String time) {
        return null;
    }

    public void hideKeyboard(View view) {
        InputMethodManager keyboardManager = (InputMethodManager) activity.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            keyboardManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
