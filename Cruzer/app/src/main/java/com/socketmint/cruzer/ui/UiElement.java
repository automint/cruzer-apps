package com.socketmint.cruzer.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.TimePicker;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.LocData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Singleton class to define and access different parameters for manipulation of user interface
 * @author ndkcha
 * @since 18
 * @version 26
 */

public class UiElement implements View.OnClickListener, DialogInterface.OnDismissListener {
    private static final String TAG = "UiElement";

    //  keeping track of calling activity
    private Activity activity;

    //  different date format objects for different purposes
    private SimpleDateFormat createDateFormat, createTimeFormat, convertDateFormat, serverFormat, cardDateFormat, retrieveDateFormat;

    //  dialog boxes
    private Dialog payTmDialog;

    /**
     * Default constructor to initialize class object
     * It initializes different date formats based on current locale
     * It sets current calling activity to operate on current context
     * @param activity as calling activity
     */

    public UiElement(Activity activity) {
        this.activity = activity;
        createDateFormat = new SimpleDateFormat("dd/MM/yyyy", activity.getResources().getConfiguration().locale);
        cardDateFormat = new SimpleDateFormat("dd MMM", activity.getResources().getConfiguration().locale);
        retrieveDateFormat = new SimpleDateFormat("dd MMM yyyy", activity.getResources().getConfiguration().locale);
        convertDateFormat = new SimpleDateFormat("yyyy-MM-dd", activity.getResources().getConfiguration().locale);
        createTimeFormat = new SimpleDateFormat("hh:mm:ss", activity.getResources().getConfiguration().locale);
        serverFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", activity.getResources().getConfiguration().locale);
    }

    /**
     * Method to validate email address
     * @param email to be validated
     * @return boolean that confirms validation of email.
     */

    public boolean validateEmail(String email) {
        try {
            String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
            return email.matches(emailPattern);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * Method to initialize and display date picker dialog
     * It sets date to Edit Box supplied via parameter that will be displayed in form
     * @param result as edit box in which result date will be displayed
     */

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

    /**
     * Method to initialize and display time picker dialog
     * It sets time to Edit Box supplied via parameter that will be displayed in form
     * @param result as edit box in which result time will be displayed
     */

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

    /**
     * @return current date to be displayed in form
     */

    public String currentDate() {
        return (createDateFormat.format(Calendar.getInstance().getTime()));
    }

    /**
     * @return current time to be displayed in form
     */
    public String currentTime() {
        return (createTimeFormat.format(Calendar.getInstance().getTime()));
    }

    /**
     * Converts date in server format to form readable date
     * @param longDate as server timestamp
     * @return date that can be edited in form
     */

    public String date(String longDate) {
        try {
            return createDateFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    /**
     * Converts date in server format to a format to be displayed in card
     * @param longDate as server date
     * @return date that will be displayed in cards
     */

    public String cardDate(String longDate) {
        try {
            return cardDateFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    /**
     * Converts date in server format to a format to be displayed in "More Details" or "Retrieve" section
     * @param longDate as server date
     * @return human readable date
     */

    public String retrieveDate(String longDate) {
        try {
            return retrieveDateFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    /**
     * Converts date in server format to form readable time
     * @param longDate as server timestamp
     * @return time that can be edited in form
     */

    public String time(String longDate) {
        try {
            return createTimeFormat.format(serverFormat.parse(longDate));
        } catch (ParseException e) { return longDate; }
    }

    /**
     * Merges date and time into single timestamp
     * It can be used to store timestamp in database as well as server
     * @param date as form date
     * @param time as form time
     * @return timestamp to be stored in database
     */

    public String date(String date, String time) {
        try {
            String intermediate = convertDateFormat.format(createDateFormat.parseObject(date));
            return intermediate.concat(" " + time);
        } catch (ParseException e) { return null; }
    }

    /**
     * Method to hide keyboard from a particular view
     * @param view as currently focused view
     */

    public void hideKeyboard(View view) {
        InputMethodManager keyboardManager = (InputMethodManager) activity.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            keyboardManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to display Confirmation Dialog box for PayTm Offer
     */

    public void payTmConfirmationDialog() {
        //  Initialize payTmDialog box with custom theme
        payTmDialog = new Dialog(activity, R.style.AppTheme_PayTm_Dialog);
        payTmDialog.setContentView(R.layout.dialog_paytm);

        //  Set event listeners
        payTmDialog.findViewById(R.id.button_paytm_like).setOnClickListener(this);
        payTmDialog.setOnDismissListener(this);
        payTmDialog.setCancelable(true);

        //  Assign custom layout params
        payTmDialog.getWindow().setLayout(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        payTmDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        payTmDialog.show();
    }

    /**
     * Listen to click events on view
     * @param v as view that is been clicked
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_paytm_like:
                Intent httpIntent = new Intent(Intent.ACTION_VIEW);
                try {
                    activity.getPackageManager().getPackageInfo("com.facebook.katana", 0);
                    httpIntent.setData(Uri.parse("fb://page/470510013124932"));
                } catch (Exception e) {
                    httpIntent.setData(Uri.parse("https://facebook.com/CruzerApp"));
                }
                try {
                    payTmDialog.dismiss();
                } catch (Exception e) { Log.d(TAG, "problem in closing payTm Dialog box"); }
                activity.startActivity(httpIntent);
                break;
        }
    }

    /**
     * Listen to dismiss events of dialog box
     * @param dialog that is being dismissed
     */

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (payTmDialog != null && dialog == payTmDialog) {
            LocData locData = new LocData();
            locData.cruzerInstance(activity);
            locData.storePayTmLike(true);
        }
    }
}
