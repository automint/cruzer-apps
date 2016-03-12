package com.socketmint.cruzer.startup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.LocData;

public class SplashScreen extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        LocData locData = new LocData();
        locData.cruzerInstance(this);

        if (locData.helpScreenSeen()) {
            startActivity(new Intent(SplashScreen.this, Launcher.class));
            finish();
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreen.this, PayTm.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        }, 2000);
    }
}
