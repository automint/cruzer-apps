package com.socketmint.cruzer.drawer;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.socketmint.cruzer.R;

import java.util.ArrayList;
import java.util.List;

public class DrawerAdapter extends BaseAdapter {
    private Activity drawerActivity;

    protected List<DrawerData> drawerDataList = new ArrayList<>();

    public DrawerAdapter(List<DrawerData> drawerDataList, Activity activity) {
        this.drawerDataList = drawerDataList;
        drawerActivity = activity;
    }


    @Override
    public int getCount() {
        return drawerDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return drawerDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = (convertView == null) ? ((LayoutInflater) drawerActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.drawerbar_item, parent, false) : convertView;

        AppCompatTextView count = (AppCompatTextView) view.findViewById(R.id.text_drawer_item_count);

        DrawerData data = drawerDataList.get(position);
        count.setVisibility((data.titleCount == 0) ? View.INVISIBLE : View.VISIBLE);
        count.setText(String.valueOf(data.titleCount));
        ((AppCompatTextView) view.findViewById(R.id.text_drawer_item)).setText(data.itemTitle);
        ((AppCompatImageView) view.findViewById(R.id.icon_drawer_item)).setImageResource(data.drawerIcon);

        return view;
    }
}
