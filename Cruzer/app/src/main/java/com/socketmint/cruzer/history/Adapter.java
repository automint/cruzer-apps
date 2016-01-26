package com.socketmint.cruzer.history;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.socketmint.cruzer.manage.Choices;

public class Adapter extends FragmentPagerAdapter {
    private String vId;

    public Adapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    public void setVehicleId(String vId) {
        this.vId = vId;
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        switch (Choices.EXPENSES.values()[position]) {
            case REFUEL:
                return RefuelFragment.newInstance(vId);
            case SERVICE:
                return ServiceFragment.newInstance(vId);
            default:
                return RefuelFragment.newInstance(vId);
        }
    }

    @Override
    public int getCount() {
        return Choices.EXPENSES.values().length;
    }
}
