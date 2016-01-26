package com.socketmint.cruzer.crud.create;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.socketmint.cruzer.crud.create.temp.Refuel;
import com.socketmint.cruzer.crud.create.vehicle.VehicleEssential;
import com.socketmint.cruzer.manage.Choices;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends FragmentPagerAdapter {
    private int pageChoice;
    private int total = 0;

    public Adapter(FragmentManager fm) {
        super(fm);
    }

    public void setPageChoice(int pageChoice) {
        this.pageChoice = pageChoice;
        switch (pageChoice) {
            case Choices.PAGE_VEHICLE:
                total = Choices.VEHICLE.values().length;
                break;
            case Choices.PAGE_EXPENSE:
                total = Choices.EXPENSES.values().length;
                break;
        }
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        switch (pageChoice) {
            case Choices.PAGE_VEHICLE:
                switch (Choices.VEHICLE.values()[position]) {
                    case ESSENTIAL_DETAILS:
                        return VehicleEssential.newInstance();
                }
                break;
            case Choices.PAGE_EXPENSE:
                switch (Choices.EXPENSES.values()[position]) {
                    case REFUEL:
                        return Refuel.newInstance();
                    case SERVICE:
                        return Refuel.newInstance();
                }
        }
        return null;
    }

    @Override
    public int getCount() {
        return total;
    }
}
