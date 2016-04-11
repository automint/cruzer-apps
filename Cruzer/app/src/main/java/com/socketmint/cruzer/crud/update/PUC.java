package com.socketmint.cruzer.crud.update;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.vehicle.*;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PUC extends Fragment implements View.OnClickListener{

    private UiElement uiElement;
    private DatabaseHelper databaseHelper;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<String> vehicleList = new ArrayList<>();
    private AppCompatEditText editStartDate, editEndDate, editPucNo, editFees, editDetails;
    private String vehicleId;
    private String id;

    public static PUC newInstance(String id) {
        PUC fragment = new PUC();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_puc, container, false);

        uiElement = new UiElement(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        id = getArguments().getString(Constants.Bundle.ID, "");
        initializeViews(v);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        setContens();
    }

    private void setContens() {
        com.socketmint.cruzer.dataholder.PUC puc = databaseHelper.puc(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
        editPucNo.setText(puc.pucNom);
        editStartDate.setText(uiElement.date(puc.startDate));
        editEndDate.setText(uiElement.date(puc.endDate));
        editFees.setText(puc.fees);
        editDetails.setText(puc.details);
    }

    private void initializeViews(View v) {
        editPucNo = (AppCompatEditText) v.findViewById(R.id.edit_puc_no);
        editStartDate = (AppCompatEditText) v.findViewById(R.id.edit_puc_start_date);
        editEndDate = (AppCompatEditText) v.findViewById(R.id.edit_puc_end_date);
        editFees = (AppCompatEditText) v.findViewById(R.id.edit_puc_fees);
        editDetails = (AppCompatEditText) v.findViewById(R.id.edit_puc_notes);

        v.findViewById(R.id.button_create_record).setOnClickListener(this);
        v.findViewById(R.id.edit_puc_start_date).setOnClickListener(this);
        v.findViewById(R.id.edit_puc_end_date).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.edit_puc_start_date:
                uiElement.datePickerDialog(editStartDate);
                break;

            case R.id.edit_puc_end_date:
                uiElement.datePickerDialog(editEndDate);
                break;

            case R.id.button_create_record:
                if(editFees.getText().toString().isEmpty() || editFees.getText().toString().equals("0")){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_amount, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (editPucNo.getText().toString().isEmpty() || editPucNo.getText().toString().equals("0")){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_puc_no, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(editStartDate.getText().toString().isEmpty() || editEndDate.getText().toString().isEmpty()){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_date, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                String fees = editFees.getText().toString().replaceAll("[^0-9.]+","").trim();
                String pucNo = editPucNo.getText().toString().replaceAll("[^0-9.]+", "").trim();
                if(databaseHelper.updatePUC(id, pucNo, uiElement.date(editStartDate.getText().toString(), uiElement.currentTime()), editEndDate.getText().toString(), fees, editDetails.getText().toString()))
                    getActivity().onBackPressed();
                break;
        }
    }
}
