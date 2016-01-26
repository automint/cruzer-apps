package com.socketmint.cruzer.drawer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.ui.UserInterface;

import java.util.ArrayList;
import java.util.List;

public class DrawerAdapter extends BaseAdapter {
    private Activity drawerActivity;
    private UserInterface userInterface = UserInterface.getInstance();

    protected List<DrawerData> drawerDataList = new ArrayList<>();

    public DrawerAdapter(List<DrawerData> drawerDataList, Activity activity) {
        this.drawerDataList = drawerDataList;
        drawerActivity = activity;
        userInterface.changeActivity(activity);
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
        View view;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) drawerActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.drawerbar_item, parent, false);
        } else
            view = convertView;

        AppCompatTextView title = (AppCompatTextView) view.findViewById(R.id.text_drawer_item);
        AppCompatTextView count = (AppCompatTextView) view.findViewById(R.id.text_drawer_item_count);
        AppCompatImageView icon = (AppCompatImageView) view.findViewById(R.id.icon_drawer_item);

        title.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        count.setTypeface(userInterface.font(UserInterface.font.roboto_light));

        DrawerData data = drawerDataList.get(position);
        title.setText(data.itemTitle);
        if (data.titleCount == 0)
            count.setVisibility(View.INVISIBLE);
        else
            count.setVisibility(View.VISIBLE);
        count.setText(String.valueOf(data.titleCount));
        icon.setImageResource(data.drawerIcon);

        return view;
    }
}
