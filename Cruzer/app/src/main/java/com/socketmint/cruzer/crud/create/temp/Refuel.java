package com.socketmint.cruzer.crud.create.temp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.ui.UserInterface;

public class Refuel extends Fragment {
    private AppCompatEditText editAmount, editOdometer, editDate, editVolume, editRate;

    private UserInterface userInterface = UserInterface.getInstance();

    public static Refuel newInstance() {
        Refuel fragment = new Refuel();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_create_refuel, container, false);

        userInterface.changeActivity(getActivity());

        initializeView(view);
        return view;
    }

    private void initializeView(View v) {
        ((AppCompatTextView) v.findViewById(R.id.label_amount)).setTypeface(userInterface.font(UserInterface.font.roboto_light));
        ((AppCompatTextView) v.findViewById(R.id.label_odometer)).setTypeface(userInterface.font(UserInterface.font.roboto_light));
        ((AppCompatTextView) v.findViewById(R.id.label_date)).setTypeface(userInterface.font(UserInterface.font.roboto_light));
        ((AppCompatTextView) v.findViewById(R.id.label_volume)).setTypeface(userInterface.font(UserInterface.font.roboto_light));
        ((AppCompatTextView) v.findViewById(R.id.label_refuel_rate)).setTypeface(userInterface.font(UserInterface.font.roboto_light));

        editAmount = (AppCompatEditText) v.findViewById(R.id.edit_amount);
        editOdometer = (AppCompatEditText) v.findViewById(R.id.edit_odometer);
        editDate = (AppCompatEditText) v.findViewById(R.id.edit_date);
        editVolume = (AppCompatEditText) v.findViewById(R.id.edit_volume);
        editRate = (AppCompatEditText) v.findViewById(R.id.edit_refuel_rate);

        editAmount.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        editOdometer.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        editDate.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        editVolume.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        editRate.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
    }
}
