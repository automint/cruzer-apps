package com.socketmint.cruzer.crud.create;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UserInterface;

public class Create extends AppCompatActivity implements View.OnClickListener {

    private AppCompatTextView textOptionRefuel, textOptionService;
    private AppCompatImageView iconOptionRefuel, iconOptionService;
    private LinearLayoutCompat layoutOptionRefuel, layoutOptionService, layoutCOExpenses, layoutCOVehicles;
    private ViewPager pager;
    private Adapter adapter;

    private int pageChoice;

    private UserInterface userInterface = UserInterface.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_create_temp);

        userInterface.changeActivity(this);
        initializeViews();

        pageChoice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        adapter = new Adapter(getSupportFragmentManager());
    }

    @Override
    public void onStart() {
        super.onStart();

        adapter.setPageChoice(pageChoice);
        pager.setAdapter(adapter);

        setContent();
    }

    private void initializeViews() {
        textOptionRefuel = (AppCompatTextView) findViewById(R.id.text_option_refuel);
        textOptionService = (AppCompatTextView) findViewById(R.id.text_option_service);
        iconOptionRefuel = (AppCompatImageView) findViewById(R.id.icon_option_refuel);
        iconOptionService = (AppCompatImageView) findViewById(R.id.icon_option_service);
        layoutOptionRefuel = (LinearLayoutCompat) findViewById(R.id.layout_option_refuel);
        layoutOptionService = (LinearLayoutCompat) findViewById(R.id.layout_option_service);
        layoutCOExpenses = (LinearLayoutCompat) findViewById(R.id.layout_create_option_expenses);
        layoutCOVehicles = (LinearLayoutCompat) findViewById(R.id.layout_create_option_vehicle);
        pager = (ViewPager) findViewById(R.id.pager_create_record);

        textOptionRefuel.setTypeface(userInterface.font(UserInterface.font.roboto_light));
        textOptionService.setTypeface(userInterface.font(UserInterface.font.roboto_light));

        findViewById(R.id.button_back).setOnClickListener(this);
        layoutOptionRefuel.setOnClickListener(this);
        layoutOptionService.setOnClickListener(this);
    }

    private void setContent() {
        switch (pageChoice) {
            case Choices.PAGE_VEHICLE:
                layoutCOVehicles.setVisibility(View.VISIBLE);
                break;
            case Choices.PAGE_EXPENSE:
                findViewById(R.id.toolbar).setBackgroundColor(ContextCompat.getColor(this, R.color.white_darken_1));
                layoutCOExpenses.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_back:
                super.onBackPressed();
                break;
            case R.id.layout_option_refuel:
                refuel();
                break;
            case R.id.layout_option_service:
                service();
                break;
        }
    }

    private void refuel() {
        iconOptionRefuel.setColorFilter(Color.argb(0, 0, 0, 0));
        iconOptionService.setColorFilter(ContextCompat.getColor(this, R.color.dark_v1));
        textOptionRefuel.setTextColor(ContextCompat.getColor(this, R.color.refuel_color));
        textOptionService.setTextColor(ContextCompat.getColor(this, R.color.dark_v1));
        layoutOptionRefuel.setAlpha(1f);
        layoutOptionService.setAlpha(0.5f);
    }

    private void service() {
        iconOptionService.setColorFilter(Color.argb(0, 0, 0, 0));
        iconOptionRefuel.setColorFilter(ContextCompat.getColor(this, R.color.dark_v1));
        textOptionService.setTextColor(ContextCompat.getColor(this, R.color.service_color));
        textOptionRefuel.setTextColor(ContextCompat.getColor(this, R.color.dark_v1));
        layoutOptionService.setAlpha(1f);
        layoutOptionRefuel.setAlpha(0.5f);
    }
}
