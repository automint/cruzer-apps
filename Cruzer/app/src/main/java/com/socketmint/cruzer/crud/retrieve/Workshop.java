package com.socketmint.cruzer.crud.retrieve;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.manage.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class Workshop extends Fragment implements View.OnClickListener {
    private static final String TAG = "RetrieveWorkshop";
    private AppCompatTextView textWorkshopName, textManager, textMobile, textAddress, textCity, textArea, textOfferings;

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

        String id = getArguments().getString(Constants.Bundle.ID);
        initializeViews(view);
        setDetails(view, id);

        return view;
    }

    private void initializeViews(View v) {
        textWorkshopName = (AppCompatTextView) v.findViewById(R.id.text_workshop_name);
        textManager = (AppCompatTextView) v.findViewById(R.id.text_workshop_manager);
        textMobile = (AppCompatTextView) v.findViewById(R.id.text_workshop_mobile);
        textAddress = (AppCompatTextView) v.findViewById(R.id.text_workshop_address);
        textCity = (AppCompatTextView) v.findViewById(R.id.text_workshop_city);
        textArea = (AppCompatTextView) v.findViewById(R.id.text_workshop_area);
        textOfferings = (AppCompatTextView) v.findViewById(R.id.text_workshop_offerings);

        v.findViewById(R.id.button_claim_workshop).setOnClickListener(this);
    }

    private void setDetails(View v, String id) {
        try {
            JSONObject object = new JSONObject(id);
            String name = object.getString(DatabaseSchema.Workshops.COLUMN_NAME);
            String manager = object.getString(DatabaseSchema.Workshops.COLUMN_MANAGER);
            String contact = object.getString(DatabaseSchema.Workshops.COLUMN_CONTACT);
            String address = object.getString(DatabaseSchema.Workshops.COLUMN_ADDRESS);
            String city = object.getString(DatabaseSchema.Workshops.COLUMN_CITY_ID);
            String area = object.getString(DatabaseSchema.Workshops.COLUMN_AREA);
            String offerings = object.getString(DatabaseSchema.Workshops.COLUMN_OFFERINGS);

            analyticsTracker.setScreenName(Constants.GoogleAnalytics.EVENT_WORKSHOP_DISPLAY + " : " + name);
            analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());

            textWorkshopName.setText(name);
            textManager.setText(manager);
            textMobile.setText(contact);
            textAddress.setText(address);
            textCity.setText(city);
            textArea.setText(area);
            textOfferings.setText(offerings);
            v.findViewById(R.id.layout_workshop_manager).setVisibility((manager.isEmpty() || manager.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_contact).setVisibility((contact.isEmpty() || contact.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_address).setVisibility((address.isEmpty() || address.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_city).setVisibility((city.isEmpty() || city.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_area).setVisibility((area.isEmpty() || area.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_offerings).setVisibility((offerings.isEmpty() || offerings.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
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
}
