package com.socketmint.cruzer.crud.update;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;

public class Update extends AppCompatActivity {
    private int pageChoice;
    private String id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        pageChoice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        chooseTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        initializeViews();

        id = getIntent().getStringExtra(Constants.Bundle.ID);
        id = (id != null) ? id : "";

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
                title = getString(R.string.drawer_item_vehicle);
                target = Vehicle.newInstance(id);
                createIcon = R.drawable.ic_vehicle;
                break;
            case Choices.REFUEL:
                title = getString(R.string.title_refuel);
                target = Refuel.newInstance(id);
                createIcon = R.drawable.ic_refuel;
                break;
            case Choices.SERVICE:
                title = getString(R.string.title_service);
                target = Service.newInstance(id);
                createIcon = R.drawable.ic_service;
                break;
            case Choices.USER:
                title = getString(R.string.title_user);
                target = User.newInstance();
                createIcon = R.drawable.ic_user;
                break;
            case Choices.INSURANCE:
                title = getString(R.string.title_insurance);
                target = Insurance.newInstance(id);
                createIcon = R.drawable.ic_insurance;
                break;
            case Choices.PUC:
                title = getString(R.string.title_puc);
                target = PUC.newInstance(id);
                createIcon = R.drawable.ic_puc;
                break;
            default:
                title = getString(R.string.app_name);
                target = null;
                createIcon = R.mipmap.ic_launcher;
                break;
        }
        ((AppCompatTextView) findViewById(R.id.text_update_type)).setText(title);
        ((AppCompatImageView) findViewById(R.id.image_update_icon)).setImageResource(createIcon);
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_update_record, target).commit();
    }

    private void chooseTheme() {
        int theme;
        switch (pageChoice) {
            case Choices.VEHICLE:
                theme = R.style.AppTheme_Update;
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
            default:
                theme = R.style.AppTheme_Update;
                break;
        }
        setTheme(theme);
    }
}
