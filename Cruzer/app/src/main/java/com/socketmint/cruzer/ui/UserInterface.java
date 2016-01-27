package com.socketmint.cruzer.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.LocData;

import java.io.ByteArrayOutputStream;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class UserInterface {
    private static UserInterface instance;

    public static abstract class font {
        public static final int copse = 0;
        public static final int roboto_black = 1;
        public static final int roboto_black_italic = 2;
        public static final int roboto_bold = 3;
        public static final int roboto_bold_italic = 4;
        public static final int roboto_italic = 5;
        public static final int roboto_light = 6;
        public static final int roboto_light_italic = 7;
        public static final int roboto_medium = 8;
        public static final int roboto_medium_italic = 9;
        public static final int roboto_regular = 10;
        public static final int roboto_thin = 11;
        public static final int roboto_thin_italic = 12;
    }

    public static abstract class animation {
        public static final int bounce = 0;
        public static final int fade_in = 1;
        public static final int fade_out = 2;
        public static final int slide_from_down = 3;
        public static final int slide_from_left = 4;
        public static final int slide_from_up = 5;
        public static final int slide_from_right = 6;
        public static final int slide_to_down = 7;
        public static final int slide_to_left = 8;
        public static final int slide_to_right = 9;
        public static final int slide_to_up = 10;
        public static final int rotate = 11;
    }

    private Activity activity;

    private Typeface copse, roboto_black, roboto_black_italic, roboto_bold,  roboto_bold_italic, roboto_italic, roboto_light, roboto_light_italic, roboto_medium,
            roboto_medium_italic, roboto_regular, roboto_thin, roboto_thin_italic;

    private Animation bounce, fade_in, fade_out, slide_from_down, slide_from_left, slide_from_up, slide_from_right, slide_to_down, slide_to_left, slide_to_right,
            slide_to_up, rotate;

    private SimpleDateFormat displayDateFormat, serverDateFormat;

    private DatePickerDialog datePickerDialog;

    private LocData locData = new LocData();

    public static UserInterface getInstance() {
        if (instance == null)
            instance = new UserInterface();
        return instance;
    }

    public void initInstance(Activity activity) {
        this.activity = activity;
        locData.formInstance(activity);
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", activity.getResources().getConfiguration().locale);
        serverDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", activity.getResources().getConfiguration().locale);
        defineAnimations();
        defineFonts();
    }

    public void changeActivity(Activity activity) {
        this.activity = activity;
        locData.formInstance(activity);
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", activity.getResources().getConfiguration().locale);
        serverDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", activity.getResources().getConfiguration().locale);
    }

    private void defineFonts() {
        copse = Typeface.createFromAsset(activity.getAssets(), "fonts/Copse-Regular.ttf");
        roboto_black = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Black.ttf");
        roboto_black_italic = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-BlackItalic.ttf");
        roboto_bold = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Bold.ttf");
        roboto_italic = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Italic.ttf");
        roboto_bold_italic = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-BoldItalic.ttf");
        roboto_light = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Light.ttf");
        roboto_light_italic = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-LightItalic.ttf");
        roboto_medium = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Medium.ttf");
        roboto_medium_italic = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-MediumItalic.ttf");
        roboto_regular = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Regular.ttf");
        roboto_thin = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Thin.ttf");
        roboto_thin_italic = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-ThinItalic.ttf");
    }

    private void defineAnimations() {
        slide_from_left = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_fromleft);
        slide_from_down = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_fromdown);
        slide_from_right = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_fromright);
        slide_from_up = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_fromup);
        slide_to_down = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_todown);
        slide_to_left = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_toleft);
        slide_to_right = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_toright);
        slide_to_up = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_toup);
        bounce = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.bounce);
        fade_in = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.fadein);
        fade_out = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.fadeout);
        rotate = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.rotate_clockwise);

    }

    public Typeface font(int fontType) {
        switch (fontType) {
            case font.copse:
                return copse;
            case font.roboto_black:
                return roboto_black;
            case font.roboto_black_italic:
                return roboto_black_italic;
            case font.roboto_bold:
                return roboto_bold;
            case font.roboto_bold_italic:
                return roboto_bold_italic;
            case font.roboto_light:
                return roboto_light;
            case font.roboto_light_italic:
                return roboto_light_italic;
            case font.roboto_medium:
                return roboto_medium;
            case font.roboto_medium_italic:
                return roboto_medium_italic;
            case font.roboto_regular:
                return roboto_regular;
            case font.roboto_thin:
                return roboto_thin;
            case font.roboto_thin_italic:
                return roboto_thin_italic;
            case font.roboto_italic:
                return roboto_italic;
            default:
                return null;
        }
    }

    public Animation animation(int animationType) {
        switch (animationType) {
            case animation.slide_from_left:
                return slide_from_left;
            case animation.slide_from_down:
                return slide_from_down;
            case animation.slide_from_right:
                return slide_from_right;
            case animation.slide_from_up:
                return slide_from_up;
            case animation.slide_to_left:
                return slide_to_left;
            case animation.slide_to_right:
                return slide_to_right;
            case animation.slide_to_up:
                return slide_to_up;
            case animation.slide_to_down:
                return slide_to_down;
            case animation.bounce:
                return bounce;
            case animation.fade_in:
                return fade_in;
            case animation.fade_out:
                return fade_out;
            case animation.rotate:
                return rotate;
            default:
                return null;
        }
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

    public void hideKeyboard(View view) {
        InputMethodManager keyboardManager = (InputMethodManager) activity.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            keyboardManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
