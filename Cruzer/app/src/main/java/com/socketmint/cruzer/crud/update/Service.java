package com.socketmint.cruzer.crud.update;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.Collections;

public class Service extends Fragment implements View.OnClickListener {
    private static final String TAG = "UpdateService ";
    private static final String ACTION_DATE = "Select Date";
    private static final String ACTION_UPDATE_SERVICE = "Update Service";
    private AppCompatEditText editAmount, editOdometer, editDate, editTime, editNotes;

    private UiElement uiElement;
    private DatabaseHelper databaseHelper;

    private String id;
    private Tracker analyticsTracker;

    public static Service newInstance(String id) {
        Service fragment = new Service();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_create_service, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        uiElement = new UiElement(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        id = getArguments().getString(Constants.Bundle.ID, "");

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG.trim());
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setContent();
    }

    private void initializeViews(View v) {
        editAmount = (AppCompatEditText) v.findViewById(R.id.edit_amount);
        editOdometer = (AppCompatEditText) v.findViewById(R.id.edit_odometer);
        editDate = (AppCompatEditText) v.findViewById(R.id.edit_date);
        editTime = (AppCompatEditText) v.findViewById(R.id.edit_time);
        editNotes = (AppCompatEditText) v.findViewById(R.id.edit_notes);

        editAmount.addTextChangedListener(amountWatcher);
        editOdometer.addTextChangedListener(odometerWatcher);

        editDate.setOnClickListener(this);
        editTime.setOnClickListener(this);
        v.findViewById(R.id.button_create_record).setOnClickListener(this);
        v.findViewById(R.id.layout_service_vehicle).setVisibility(View.GONE);
    }

    private void setContent() {
        com.socketmint.cruzer.dataholder.expense.service.Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
        editAmount.setText(service.cost);
        editOdometer.setText(service.odo);
        editDate.setText(uiElement.date(service.date));
        editTime.setText(uiElement.time(service.date));
        editNotes.setText(service.details);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_date:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(TAG + ACTION_DATE).build());
                uiElement.datePickerDialog(editDate, true);
                break;
            case R.id.edit_time:
                uiElement.timePickerDialog(editTime);
                break;
            case R.id.button_create_record:
                if (editAmount.getText().toString().isEmpty() || editAmount.getText().toString().equals("0")) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_amount, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_UPDATE_SERVICE).build());
                String amount = editAmount.getText().toString().replaceAll("[^0-9.]+","").trim();
                String odometer = editOdometer.getText().toString().replaceAll("[^0-9.]+", "").trim();
                if (databaseHelper.updateService(id, uiElement.date(editDate.getText().toString(), editTime.getText().toString()), amount, odometer, editNotes.getText().toString()))
                    getActivity().onBackPressed();
                break;
        }
    }

    TextWatcher amountWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!s.toString().startsWith(getString(R.string.text_rupee)) && !s.toString().isEmpty()) {
                String original = s.toString().replaceAll("[^0-9.]+","").trim();
                editAmount.setText((original.isEmpty()) ? "" : getString(R.string.text_rupee).concat(original));
                Selection.setSelection(editAmount.getText(), editAmount.getText().length());
            }
        }
    };

    TextWatcher odometerWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!s.toString().endsWith(getString(R.string.text_odometer_suffix)) && !s.toString().isEmpty()) {
                String original = s.toString().replaceAll("[^0-9.]+", "").trim();
                editOdometer.setText((original.isEmpty()) ? "" : original.concat(getString(R.string.text_odometer_suffix)));
                Selection.setSelection(editOdometer.getText(), original.length());
            }
        }
    };
}
