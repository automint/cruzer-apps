package com.socketmint.cruzer.crud.create;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;

public class Create extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private Login login = new Login();

    private int pageChoice;
    private String vehicleId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        pageChoice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        chooseTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        databaseHelper = new DatabaseHelper(getApplicationContext());
        login.initInstance(this);
        initializeViews();

        vehicleId = getIntent().getStringExtra(Constants.Bundle.VEHICLE_ID);
        vehicleId = (vehicleId != null) ? vehicleId : "";

        setContent();
    }

    private void initializeViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void setContent() {
        String title;
        Fragment target;
        int createIcon;
        switch (pageChoice) {
            case Choices.VEHICLE:
                title = getString(R.string.text_first_vehicle);
                target = Vehicle.newInstance();
                createIcon = R.drawable.ic_vehicle;
                break;
            case Choices.REFUEL:
                title = getString(R.string.title_refuel);
                target = Refuel.newInstance(vehicleId);
                createIcon = R.drawable.ic_refuel;
                break;
            case Choices.SERVICE:
                title = getString(R.string.title_service);
                target = Service.newInstance(vehicleId);
                createIcon = R.drawable.ic_service;
                break;
            case Choices.INSURANCE:
                title = getString(R.string.title_insurance);
                target = Insurance.newInstance(vehicleId);
                createIcon = R.drawable.ic_insurance;
                break;
            case Choices.PUC:
                title = getString(R.string.title_puc);
                target = PUC.newInstance(vehicleId);
                createIcon = R.drawable.ic_puc;
                break;
            default:
                title = getString(R.string.app_name);
                target = null;
                createIcon = R.mipmap.ic_launcher;
                break;
        }
        ((AppCompatTextView) findViewById(R.id.text_create_type)).setText(title);
        ((AppCompatImageView) findViewById(R.id.image_create_icon)).setImageResource(createIcon);
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_create_record, target).commit();
    }

    private void chooseTheme() {
        int theme;
        switch (pageChoice) {
            case Choices.VEHICLE:
                theme = R.style.AppTheme_Create;
                break;
            case Choices.REFUEL:
                theme = R.style.AppTheme_Refuel;
                break;
            case Choices.SERVICE:
                theme = R.style.AppTheme_Service;
                break;
            case Choices.INSURANCE:
                theme = R.style.AppTheme_Insurance;
                break;
            case Choices.PUC:
                theme = R.style.AppTheme_PUC;
                break;
            default:
                theme = R.style.AppTheme_Create;
                break;
        }
        setTheme(theme);
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
                        final Snackbar exitBar = Snackbar.make(Create.this.findViewById(android.R.id.content), getString(R.string.message_back_logout), Snackbar.LENGTH_INDEFINITE);
                        exitBar.show();
                        exit = true;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                exit = false;
                                exitBar.dismiss();
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
