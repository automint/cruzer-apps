package com.socketmint.cruzer.crud;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.ListViewCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Manu;
import com.socketmint.cruzer.dataholder.Model;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChoiceDialog {
    private static final String TAG = "ChoiceDialog";
    private static final String SCREEN_MANU = " Manufacturer";
    private static final String SCREEN_MODEL = " Model";
    private static final String SCREEN_VEHICLE = " Vehicle";

    private Activity activity;
    private Dialog dialog;
    private ListViewCompat choiceList;

    private DatabaseHelper databaseHelper;
    private ArrayAdapter<String> adapter;
    private String returnName;
    private LocData locData = new LocData();
    private Tracker analyticsTracker;

    public ChoiceDialog(Activity activity) {
        this.activity = activity;
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        locData.formInstance(activity);
        analyticsTracker = ((CruzerApp) activity.getApplication()).getAnalyticsTracker();
    }

    private void createDialog() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_choice);
        choiceList = (ListViewCompat) dialog.findViewById(R.id.choice_list);
        ((AppCompatEditText) dialog.findViewById(R.id.edit_choice_filter)).addTextChangedListener(textFilter);
    }

    private void addListener(final AppCompatEditText result) {
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                try {
                    if (!returnName.isEmpty()) {
                        result.setText(returnName);
                        result.setSelection(returnName.length());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "no return name");
                }
            }
        });
    }

    public void chooseManufacturer(final AppCompatEditText result) {
        createDialog();
        addListener(result);
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_MANU).build());
        final List<Manu> manus = databaseHelper.manus();
        final List<String> list = new ArrayList<>();
        returnName = null;
        for (Manu item : manus) {
            list.add(item.name);
        }
        adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, android.R.id.text1, list);
        choiceList.setAdapter(adapter);
        choiceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                returnName = textView.getText().toString();
                int pos = list.indexOf(returnName);
                locData.storeManuId(manus.get(pos).getId());
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    public void chooseModel(final AppCompatEditText result, final boolean update) {
        createDialog();
        addListener(result);
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_MODEL).build());
        final List<Model> models = databaseHelper.models(Collections.singletonList(DatabaseSchema.Models.COLUMN_MANU_ID), new String[]{locData.manuId()});
        returnName = null;
        final List<String> list = new ArrayList<>();
        for (Model item : models) {
            list.add(item.name);
        }
        if (!update)
            list.add(activity.getString(R.string.label_other_option));
        adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, android.R.id.text1, list);
        choiceList.setAdapter(adapter);
        choiceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                returnName = textView.getText().toString();
                int pos = list.indexOf(returnName);
                locData.storeModelId(pos < models.size() ? models.get(pos).getId() : "");
                Log.d(TAG, "modelId = " + (pos < models.size() ? models.get(pos).getId() : ""));
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void chooseVehicle() {
        createDialog();
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_VEHICLE).build());
        final List<Vehicle> vehicles = databaseHelper.vehicles();
        returnName = null;
        final List<String> list = new ArrayList<>();
        for (Vehicle item : vehicles) {
            String string;
            if (item.name == null || item.name.isEmpty()) {
                if (item.model != null)
                    string = item.model.name + ", " + item.model.manu.name;
                else
                    string = item.reg;
            } else
                string = item.name;
            list.add(string);
        }
        list.add("All Vehicles");
        adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, android.R.id.text1, list);
        choiceList.setAdapter(adapter);
        choiceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                returnName = textView.getText().toString();
                int pos = list.indexOf(returnName);
                if (pos >= vehicles.size())
                    activity.startActivity(activity.getIntent().putExtra(Constants.Bundle.VEHICLE_ID, "all"));
                else
                    activity.startActivity(activity.getIntent().putExtra(Constants.Bundle.VEHICLE_ID, vehicles.get(pos).getId()));
                activity.finish();
            }
        });
        dialog.show();
    }

    TextWatcher textFilter = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            adapter.getFilter().filter(s);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}
