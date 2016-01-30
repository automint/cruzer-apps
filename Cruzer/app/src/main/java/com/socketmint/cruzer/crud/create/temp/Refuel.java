package com.socketmint.cruzer.crud.create.temp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.socketmint.cruzer.R;

public class Refuel extends Fragment {
    private AppCompatEditText editAmount, editOdometer, editDate, editVolume, editRate;

    public static Refuel newInstance() {
        Refuel fragment = new Refuel();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_create_refuel, container, false);

        initializeView(view);
        return view;
    }

    private void initializeView(View v) {
        editAmount = (AppCompatEditText) v.findViewById(R.id.edit_amount);
        editOdometer = (AppCompatEditText) v.findViewById(R.id.edit_odometer);
        editDate = (AppCompatEditText) v.findViewById(R.id.edit_date);
        editVolume = (AppCompatEditText) v.findViewById(R.id.edit_volume);
        editRate = (AppCompatEditText) v.findViewById(R.id.edit_refuel_rate);
    }
}
