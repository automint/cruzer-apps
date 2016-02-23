package com.socketmint.cruzer.crud.retrieve;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.main.Vehicles;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Workshop extends Fragment implements View.OnClickListener {
    private static final String TAG = "RetrieveWorkshop";
    private AppCompatTextView textWorkshopName, textManager, textMobile, textAddress, textCity, textOfferings;
    private AppCompatButton buttonBookService;

    private String id;

    private RequestQueue requestQueue;
    private Tracker analyticsTracker;

    public static Workshop newInstance(String id) {
        Workshop fragment = new Workshop();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_workshop, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());

        String workshop = getArguments().getString(Constants.Bundle.ID);
        initializeViews(view);
        setDetails(view, workshop);

        return view;
    }

    private void initializeViews(View v) {
        textWorkshopName = (AppCompatTextView) v.findViewById(R.id.text_workshop_name);
        textManager = (AppCompatTextView) v.findViewById(R.id.text_workshop_manager);
        textMobile = (AppCompatTextView) v.findViewById(R.id.text_workshop_mobile);
        textAddress = (AppCompatTextView) v.findViewById(R.id.text_workshop_address);
        textCity = (AppCompatTextView) v.findViewById(R.id.text_workshop_city);
        textOfferings = (AppCompatTextView) v.findViewById(R.id.text_workshop_offerings);
        buttonBookService = (AppCompatButton) v.findViewById(R.id.button_book_service);

        v.findViewById(R.id.button_claim_workshop).setOnClickListener(this);
        buttonBookService.setOnClickListener(this);
    }

    private void setDetails(View v, String workshop) {
        try {
            JSONObject object = new JSONObject(workshop);
            Log.d(TAG, "w-id = " + object.optString(DatabaseSchema.COLUMN_ID));
            id = object.optString(DatabaseSchema.COLUMN_ID);
            Log.d(TAG, "id = " + id);
            String name = object.getString(DatabaseSchema.Workshops.COLUMN_NAME);
            String manager = object.getString(DatabaseSchema.Workshops.COLUMN_MANAGER);
            String contact = object.getString(DatabaseSchema.Workshops.COLUMN_CONTACT);
            String address = object.getString(DatabaseSchema.Workshops.COLUMN_ADDRESS);
            String city = object.getString(DatabaseSchema.Workshops.COLUMN_CITY_ID);
            String offerings = object.getString(DatabaseSchema.Workshops.COLUMN_OFFERINGS);
            String bookingFlag = object.optString(Constants.Json.BOOKING_FLAG);
            bookingFlag = (bookingFlag == null) ? "" : bookingFlag;
            bookingFlag = (bookingFlag.isEmpty()) ? "0" : bookingFlag;

            analyticsTracker.setScreenName(Constants.GoogleAnalytics.EVENT_WORKSHOP_DISPLAY + " : " + name);
            analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());

            textWorkshopName.setText(name);
            textManager.setText(manager);
            textMobile.setText(contact);
            textAddress.setText(address);
            textCity.setText(city);
            textOfferings.setText(offerings);
            v.findViewById(R.id.layout_workshop_manager).setVisibility((manager.isEmpty() || manager.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_contact).setVisibility((contact.isEmpty() || contact.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_address).setVisibility((address.isEmpty() || address.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_city).setVisibility((city.isEmpty() || city.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_offerings).setVisibility((offerings.isEmpty() || offerings.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            buttonBookService.setVisibility((bookingFlag.equals("1")) ? View.VISIBLE : View.GONE);
        } catch (JSONException e) {
            Log.e(TAG, "can not parse workshop json");
            getActivity().onBackPressed();
        }
    }

    private void callAdmin() {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:+919427800160"));
        startActivity(callIntent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_claim_workshop:
                if (getActivity().checkPermission(Manifest.permission.CALL_PHONE, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, Constants.RequestCodes.PERMISSION_CALL_PHONE);
                        return;
                    }
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_claim_workshop_permission_fail, Snackbar.LENGTH_LONG);
                    return;
                }
                callAdmin();
                break;
            case R.id.button_book_service:
                Login login = new Login();
                login.initInstance(getActivity());
                if (!login.isNetworkAvailable()) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_volley_error_response, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                bookService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.RequestCodes.PERMISSION_CALL_PHONE:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        callAdmin();
                }
                break;
        }
    }

    public void bookService() {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Services.COLUMN_WORKSHOP_ID, id);
        Log.d(TAG, "book service - " + bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.BOOK_SERVICE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "book service = " + response);
                buttonBookService.setEnabled(false);
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.message_book_service, textWorkshopName.getText().toString()))
                        .setPositiveButton(R.string.label_ok, null)
                        .show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_volley_error_response, Snackbar.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getParams() {
                return bodyParams;
            }

            @Override
            public Map<String, String> getHeaders() {
                LocData locData = new LocData();
                locData.cruzerInstance(getActivity());
                HashMap<String, String> headerParams = new HashMap<>();
                headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return headerParams;
            }
        };
        requestQueue.add(request);
    }
}
