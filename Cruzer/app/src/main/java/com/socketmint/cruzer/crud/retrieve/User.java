package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;

public class User extends Fragment {
    private static final String TAG = "RetrieveUser";
    private AppCompatTextView textUserName, textMobile, textEmail;
    private LinearLayoutCompat layoutUserName, layoutMobile, layoutEmail;

    private DatabaseHelper databaseHelper;

    private Tracker analyticsTracker;

    public static User newInstance() {
        User fragment = new User();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_view_user, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

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
        textUserName = (AppCompatTextView) v.findViewById(R.id.text_user_name);
        textMobile = (AppCompatTextView) v.findViewById(R.id.text_mobile);
        textEmail = (AppCompatTextView) v.findViewById(R.id.text_email);

        layoutUserName = (LinearLayoutCompat) v.findViewById(R.id.layout_user_name);
        layoutMobile = (LinearLayoutCompat) v.findViewById(R.id.layout_mobile);
        layoutEmail = (LinearLayoutCompat) v.findViewById(R.id.layout_email);
    }

    private void setContent() {
        com.socketmint.cruzer.dataholder.User user = databaseHelper.user();

        textUserName.setText(getString(R.string.text_user_name, user.firstName, user.lastName));
        textMobile.setText(user.mobile);
        textEmail.setText(user.email);

        layoutUserName.setVisibility((user.firstName.isEmpty() && user.lastName.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutMobile.setVisibility((user.mobile.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutEmail.setVisibility((user.email.isEmpty()) ? View.GONE : View.VISIBLE);
    }
}
