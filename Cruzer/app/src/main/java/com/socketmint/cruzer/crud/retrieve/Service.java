package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.expense.service.Problem;
import com.socketmint.cruzer.dataholder.expense.service.Status;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Service extends Fragment {
    private static final String TAG = "RetrieveService";

    private AppCompatTextView textVehicleName, textWorkshopName, textAmount, textOdometer, textDate, textProblems, textStatus, textNotes, textVat;
    private LinearLayoutCompat layoutOdometer, layoutDate, layoutProblems, layoutStatus, layoutNotes, layoutVat;

    private DatabaseHelper databaseHelper;
    private UiElement uiElement;

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
        View view = layoutInflater.inflate(R.layout.fragment_view_service, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        uiElement = new UiElement(getActivity());
        id = getArguments().getString(Constants.Bundle.ID, "");

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setContent();
    }

    private void initializeViews(View v) {
        textVehicleName = (AppCompatTextView) v.findViewById(R.id.text_vehicle_name);
        textWorkshopName = (AppCompatTextView) v.findViewById(R.id.text_workshop_name);
        textAmount = (AppCompatTextView) v.findViewById(R.id.text_amount);
        textOdometer = (AppCompatTextView) v.findViewById(R.id.text_odometer);
        textDate = (AppCompatTextView) v.findViewById(R.id.text_date);
        textNotes = (AppCompatTextView) v.findViewById(R.id.text_notes);
        textProblems = (AppCompatTextView) v.findViewById(R.id.text_problems);
        textStatus = (AppCompatTextView) v.findViewById(R.id.text_status);
        textVat = (AppCompatTextView) v.findViewById(R.id.text_vat);

        layoutOdometer = (LinearLayoutCompat) v.findViewById(R.id.layout_odometer);
        layoutDate = (LinearLayoutCompat) v.findViewById(R.id.layout_date);
        layoutProblems = (LinearLayoutCompat) v.findViewById(R.id.layout_problems);
        layoutStatus = (LinearLayoutCompat) v.findViewById(R.id.layout_service_status);
        layoutNotes = (LinearLayoutCompat) v.findViewById(R.id.layout_notes);
        layoutVat = (LinearLayoutCompat) v.findViewById(R.id.layout_vat);
    }

    private void setContent() {
        com.socketmint.cruzer.dataholder.expense.service.Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});

        textVehicleName.setText(vehicleName(databaseHelper.vehicle(service.getVehicleId())));
        com.socketmint.cruzer.dataholder.workshop.Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{service.getWorkshopId()});
        String workshopName = (workshop != null) ? getString(R.string.text_workshop_name, workshop.name) : "";
        textWorkshopName.setText(workshopName);
        textAmount.setText(Html.fromHtml(getString(R.string.text_amount, service.cost)));
        textOdometer.setText(Html.fromHtml(getString(R.string.text_odometer, service.odo)));
        textDate.setText(Html.fromHtml(uiElement.retrieveDate(service.date)));
        List<Problem> problemList = databaseHelper.problems(Collections.singletonList(DatabaseSchema.Problems.COLUMN_SERVICE_ID), new String[]{service.getId()});
        if (problemList != null) {
            String problems = "";
            for (int i = 0; i < problemList.size(); i++) {
                Problem item = problemList.get(i);
                double lCost = 0;
                double pCost = 0;
                try {
                    lCost = Double.parseDouble(item.lCost);
                } catch (NumberFormatException | NullPointerException e) { Log.d(TAG, "number format lCost"); }
                try {
                    pCost = Double.parseDouble(item.pCost);
                } catch (NumberFormatException | NullPointerException e) { Log.d(TAG, "number format pCost"); }
                double total = lCost + pCost;
                try {
                    total += Double.parseDouble(item.rate);
                } catch (NumberFormatException | NullPointerException e) { Log.d(TAG, "number format rate"); }
                problems = problems.concat((i + 1) + ". " + item.details + (!item.qty.isEmpty() ? " <small><i>(" + item.qty + ")</i></small>, " : ", ") +  "<small>Rs.</small>" + total + "<br>");
            }
            textProblems.setText(Html.fromHtml(problems));
        } else
            problemList = new ArrayList<>();
        String status;
        List<Status> statusList = databaseHelper.statusList();
        if (service.status != null) {
            try {
                status = statusList.get(Integer.parseInt(service.status)-1).details;
            } catch (NumberFormatException e) { status = ""; }
        } else
            status = "";
        textStatus.setText(status);
        textNotes.setText(service.details);
        String vat = (service.vat == null) ? "0" : service.vat;
        textVat.setText(vat);

        layoutOdometer.setVisibility((service.odo.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutDate.setVisibility((service.date.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutNotes.setVisibility((service.details.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutProblems.setVisibility((problemList.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutStatus.setVisibility((status.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutVat.setVisibility((vat.equals("0") || vat.isEmpty()) ? View.GONE : View.VISIBLE);
        textWorkshopName.setVisibility((workshopName.isEmpty()) ? View.GONE : View.VISIBLE);
    }

    private SpannableString vehicleName(Vehicle vehicle) {
        if (vehicle.name == null) {
            if (vehicle.model != null) {
                String t = vehicle.model.name + ", " + vehicle.model.manu.name;
                SpannableString title = new SpannableString(t);
                title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                return title;
            } else
                return (new SpannableString(vehicle.reg));
        } else {
            if (vehicle.name.isEmpty()) {
                if (vehicle.model != null) {
                    String t = vehicle.model.name + ", " + vehicle.model.manu.name;
                    SpannableString title = new SpannableString(t);
                    title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                    return title;
                } else
                    return (new SpannableString(vehicle.reg));
            } else
                return (new SpannableString(vehicle.name));
        }
    }
}
