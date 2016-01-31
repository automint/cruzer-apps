package com.socketmint.cruzer.crud.create;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;

public class Create extends AppCompatActivity implements View.OnClickListener {
    private DatabaseHelper databaseHelper;
    private Login login = new Login();

    private int pageChoice;
    private String vehicleId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_create_temp);

        databaseHelper = new DatabaseHelper(getApplicationContext());
        login.initInstance(this);
        initializeViews();

        pageChoice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        vehicleId = getIntent().getStringExtra(Constants.Bundle.VEHICLE_ID);
        vehicleId = (vehicleId != null) ? vehicleId : "";

        setContent();
    }

    private void initializeViews() {
        findViewById(R.id.button_back).setOnClickListener(this);
    }

    private void setContent() {
        String title;
        Fragment target;
        int color, createIcon;
        switch (pageChoice) {
            case Choices.VEHICLE:
                title = getString(R.string.text_first_vehicle);
                target = Vehicle.newInstance();
                color = ContextCompat.getColor(this, R.color.cruzer_blue_v1);
                createIcon = R.mipmap.ic_launcher;
                break;
            case Choices.REFUEL:
                title = getString(R.string.label_refuel);
                target = Refuel.newInstance(vehicleId);
                color = ContextCompat.getColor(this, R.color.refuel_color);
                createIcon = R.drawable.ic_refuel;
                break;
            case Choices.SERVICE:
                title = getString(R.string.label_service);
                target = Service.newInstance(vehicleId);
                color = ContextCompat.getColor(this, R.color.service_color);
                createIcon = R.drawable.ic_service;
                break;
            default:
                title = getString(R.string.app_name);
                target = null;
                color = ContextCompat.getColor(this, R.color.cruzer_blue_v1);
                createIcon = R.mipmap.ic_launcher;
                break;
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_create_record, target).commit();
        findViewById(R.id.toolbar).setBackgroundColor(color);
        ((AppCompatImageView) findViewById(R.id.image_create_icon)).setImageResource(createIcon);
        ((AppCompatTextView) findViewById(R.id.text_create_type)).setText(title);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_back:
                onBackPressed();
                break;
        }
    }

    private boolean exit = false;

    @Override
    public void onBackPressed() {
        switch (pageChoice) {
            case Choices.VEHICLE:
                if (databaseHelper.vehicleCount() == 0) {
                    if (exit)
                        login.logout();
                    else {
                        Snackbar.make(Create.this.findViewById(android.R.id.content), getString(R.string.message_back_logout), Snackbar.LENGTH_SHORT).show();
                        exit = true;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                exit = false;
                            }
                        }, 3 * 1000);
                    }
                } else
                    super.onBackPressed();
                break;
            default:
                super.onBackPressed();
                break;
        }
    }
}
