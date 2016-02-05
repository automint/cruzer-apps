package com.socketmint.cruzer.crud.update;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.sync.ManualSync;

public class User extends Fragment implements View.OnClickListener {
    private static final String TAG = "UpdateUser ";
    private static final String ACTION_UPDATE_USER = "Update User";
    private static final String ACTION_VIEW_PASSWORD = "View Password";
    private AppCompatEditText editMobile, editEmail, editFirstName, editLastName, editPassword;

    private DatabaseHelper databaseHelper;
    private ManualSync manualSync;

    private Tracker analyticsTracker;

    public static User newInstance() {
        User fragment = new User();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_update_user, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        manualSync = new ManualSync(getActivity());

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG.trim());
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setContent();
    }

    private void initializeViews(View v) {
        editMobile = (AppCompatEditText) v.findViewById(R.id.edit_mobile);
        editEmail = (AppCompatEditText) v.findViewById(R.id.edit_email);
        editFirstName = (AppCompatEditText) v.findViewById(R.id.edit_first_name);
        editLastName = (AppCompatEditText) v.findViewById(R.id.edit_last_name);
        editPassword = (AppCompatEditText) v.findViewById(R.id.edit_password);

        ((AppCompatCheckBox) v.findViewById(R.id.checkbox_show_password)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(TAG + ACTION_VIEW_PASSWORD).build());
                if (isChecked)
                    editPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                else
                    editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                editPassword.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                editPassword.setSelection(editPassword.length());
            }
        });

        v.findViewById(R.id.button_create_record).setOnClickListener(this);
    }

    private void setContent() {
        com.socketmint.cruzer.dataholder.User user = databaseHelper.user();
        editMobile.setText(user.mobile);
        editEmail.setText(user.email);
        editFirstName.setText(user.firstName);
        editLastName.setText(user.lastName);
        editPassword.setText(user.getPassword());

        editEmail.setFocusable(user.email.isEmpty());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_create_record:
                com.socketmint.cruzer.dataholder.User user = databaseHelper.user();
                if (editPassword.getText().toString().isEmpty() && !user.getPassword().isEmpty()) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_password, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_UPDATE_USER).build());
                if (databaseHelper.updateUser(user.getId(), editPassword.getText().toString(), editFirstName.getText().toString(), editLastName.getText().toString(), editEmail.getText().toString())) {
                    manualSync.syncUser();
                    getActivity().onBackPressed();
                }
                break;
        }
    }
}
