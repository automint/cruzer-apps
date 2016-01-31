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
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChoiceDialog {
    public static ChoiceDialog instance;
    private static final String TAG = "ChoiceDialog";
    private static final String SCREEN_MANU = " Manufacturer";
    private static final String SCREEN_MODEL = " Model";
    private static final String SCREEN_VEHICLE = " Vehicle";
    private static final String SCREEN_WORKSHOP = " Workshop";

    private Activity activity;
    private Dialog dialog;
    private ListViewCompat choiceList;
    private UiElement uiElement;

    private DatabaseHelper databaseHelper;
    private List<String> list;
    private ArrayAdapter<String> adapter;
    private String returnName;
    private LocData locData = new LocData();
    private Tracker analyticsTracker;

    /** @deprecated Use non-static methods instead*/
    @Deprecated
    public static ChoiceDialog getInstance() {
        if (instance == null)
            instance = new ChoiceDialog();
        return instance;
    }

    /** @deprecated Use non-static methods instead*/
    @Deprecated
    public ChoiceDialog() { }

    /** @deprecated Use non-static methods instead*/
    @Deprecated
    public void initInstance(Activity activity) {
        this.activity = activity;
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        locData.formInstance(activity);
        uiElement = new UiElement(activity);
        list = new ArrayList<>();
        analyticsTracker = ((CruzerApp) activity.getApplication()).getAnalyticsTracker();
    }

    /** @deprecated Use non-static methods instead*/
    @Deprecated
    public void setDialog(int which) {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_choice);
        choiceList = (ListViewCompat) dialog.findViewById(R.id.choice_list);
        AppCompatEditText editChoice = (AppCompatEditText) dialog.findViewById(R.id.edit_choice_filter);
        switch (which) {
            case CrudChoices.MANU:
                manu();
                break;
            case CrudChoices.MODEL:
                model();
                break;
            case CrudChoices.VEHICLE:
                vehicle();
                break;
            case CrudChoices.WORKSHOP:
                workshop();
                break;
        }
        editChoice.addTextChangedListener(textFilter);
    }

    /** @deprecated Use non-static method */
    @Deprecated
    public void show(final AppCompatEditText editText) {
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                try {
                    if (!returnName.isEmpty()) {
                        editText.setText(returnName);
                        editText.setSelection(returnName.length());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "no return name");
                }
            }
        });
    }

    /** @deprecated Use non-static method */
    @Deprecated
    public void show() {
        dialog.show();
    }

    public ChoiceDialog(Activity activity) {
        this.activity = activity;
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        locData.formInstance(activity);
        uiElement = new UiElement(activity);
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

    public void chooseModel(final AppCompatEditText result) {
        createDialog();
        addListener(result);
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_MODEL).build());
        final List<Model> models = databaseHelper.models(Collections.singletonList(DatabaseSchema.Models.COLUMN_MANU_ID), new String[]{locData.manuId()});
        returnName = null;
        final List<String> list = new ArrayList<>();
        for (Model item : models) {
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
                locData.storeModelId(models.get(pos).getId());
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
                    activity.startActivity(activity.getIntent().putExtra(Constants.Bundle.VEHICLE_ID, "all").putExtra(Constants.Bundle.PAGE_CHOICE, Choices.EXPENSES.REFUEL.ordinal()));
                else
                    activity.startActivity(activity.getIntent().putExtra(Constants.Bundle.VEHICLE_ID, vehicles.get(pos).getId()).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.EXPENSES.REFUEL.ordinal()));
                activity.finish();
            }
        });
        dialog.show();
    }

    private void workshop() {
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_WORKSHOP).build());
        final List<Workshop> workshops = databaseHelper.workshops();
        list.clear();
        returnName = null;
        for (Workshop item : workshops) {
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
                locData.storeWorkshopId(workshops.get(pos).getId());
                dialog.dismiss();
            }
        });
    }

    private void manu() {
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_MANU).build());
        final List<Manu> manus = databaseHelper.manus();
        list.clear();
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
    }

    private void model() {
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_MODEL).build());
        final List<Model> models = databaseHelper.models(Arrays.asList(DatabaseSchema.Models.COLUMN_MANU_ID), new String[]{locData.manuId()});
        returnName = null;
        list.clear();
        for (Model item : models) {
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
                locData.storeModelId(models.get(pos).getId());
                dialog.dismiss();
            }
        });
    }

    private void vehicle() {
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_DIALOG).setAction(SCREEN_VEHICLE).build());
        final List<Vehicle> vehicles = databaseHelper.vehicles();
        returnName = null;
        list.clear();
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
                    activity.startActivity(activity.getIntent().putExtra(Constants.Bundle.VEHICLE_ID, "all").putExtra(Constants.Bundle.PAGE_CHOICE, Choices.EXPENSES.REFUEL.ordinal()));
                else
                    activity.startActivity(activity.getIntent().putExtra(Constants.Bundle.VEHICLE_ID, vehicles.get(pos).getId()).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.EXPENSES.REFUEL.ordinal()));
                activity.finish();
            }
        });
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
