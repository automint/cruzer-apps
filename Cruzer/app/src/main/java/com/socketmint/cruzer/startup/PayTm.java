package com.socketmint.cruzer.startup;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.Constants;

public class PayTm extends AppCompatActivity implements View.OnClickListener {
    private AppCompatEditText editMobile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paytm);

        editMobile = (AppCompatEditText) findViewById(R.id.edit_mobile);
        findViewById(R.id.button_paytm_done).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_paytm_done:
                String mobile = editMobile.getText().toString();
                if (mobile.isEmpty()) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(PayTm.this, HelpScreen.class).putExtra(Constants.Bundle.MOBILE, mobile));
                finish();
                break;
        }
    }

    @Override
    public void onDestroy() {
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
        super.onDestroy();
    }
}
