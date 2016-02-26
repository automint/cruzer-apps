package com.socketmint.cruzer.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class UiElement {
    private Activity activity;

    private SimpleDateFormat createDateFormat, createTimeFormat, convertDateFormat, serverFormat, cardDateFormat, retrieveDateFormat;


    public UiElement(Activity activity) {
        this.activity = activity;
        createDateFormat = new SimpleDateFormat("dd/MM/yyyy", activity.getResources().getConfiguration().locale);
        cardDateFormat = new SimpleDateFormat("dd MMM", activity.getResources().getConfiguration().locale);
        retrieveDateFormat = new SimpleDateFormat("dd MMM yyyy", activity.getResources().getConfiguration().locale);
        convertDateFormat = new SimpleDateFormat("yyyy-MM-dd", activity.getResources().getConfiguration().locale);
        createTimeFormat = new SimpleDateFormat("hh:mm:ss", activity.getResources().getConfiguration().locale);
        serverFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", activity.getResources().getConfiguration().locale);
    }

    public boolean validateEmail(String email) {
        try {
            String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
            return email.matches(emailPattern);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public void datePickerDialog(final AppCompatEditText result) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();
                date.set(year, monthOfYear, dayOfMonth);
                result.setText(createDateFormat.format(date.getTime()));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(new Date().getTime());
        dialog.show();
    }

    public void timePickerDialog(final AppCompatEditText result) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String time = hourOfDay + ":" + minute + ":00";
                result.setText(time);
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        dialog.show();
    }

    public String currentDate() {
        return (createDateFormat.format(Calendar.getInstance().getTime()));
    }

    public String currentTime() {
        return (createTimeFormat.format(Calendar.getInstance().getTime()));
    }

    public String date(String longDate) {
        try {
            return createDateFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    public String cardDate(String longDate) {
        try {
            return cardDateFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    public String retrieveDate(String longDate) {
        try {
            return retrieveDateFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    public String time(String longDate) {
        try {
            return createTimeFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    public String date(String date, String time) {
        try {
            String intermediate = convertDateFormat.format(createDateFormat.parseObject(date));
            return intermediate.concat(" " + time);
        } catch (ParseException e) { return null; }
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
