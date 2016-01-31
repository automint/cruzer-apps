package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;

public class Retrieve extends AppCompatActivity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_retrieve_new);

        findViewById(R.id.button_back).setOnClickListener(this);

        String id = getIntent().getStringExtra(Constants.Bundle.ID);
        int choice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        if (choice == 0) {
            Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT);
            onBackPressed();
        }

        switch (choice) {
            case Choices.WORKSHOP:
                ((AppCompatTextView) findViewById(R.id.toolbar_title)).setText(R.string.label_workshop);
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_retrieve, Workshop.newInstance(id)).commit();
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_back:
                onBackPressed();
                break;
        }
    }
}
