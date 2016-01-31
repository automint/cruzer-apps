package com.socketmint.cruzer.startup;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.create.Create;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;

public class Launcher extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final String TAG = "Launcher";
    private static final String ACTION_LOGIN_PHONE = "Login Phone";
    private static final String ACTION_LOGIN_GOOGLE = "Login Google";

    private ProgressDialog progressDialog;

    private LoginDialog loginDialog = LoginDialog.getInstance();
    private Login login = Login.getInstance();
    private DatabaseHelper databaseHelper;

    private GoogleApiClient googleApiClient;
    private Tracker analyticsTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_launcher);

        analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();

        loginDialog.initInstance(this);
        login.initInstance(this);
        databaseHelper = new DatabaseHelper(getApplicationContext());
        preLogin();

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions).build();


        progressDialog = new ProgressDialog(this);

        initializeViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        Log.d(TAG, "analytics screen sent");
    }

    private void preLogin() {
        if (login.login() > 0) {
            if (databaseHelper.vehicleCount() == 0) {
                startActivity(new Intent(Launcher.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.VEHICLE));
                finish();
            } else {
                startActivity(new Intent(Launcher.this, ViewHistory.class));
                finish();
            }
        }
    }

    private void initializeViews() {
        findViewById(R.id.button_login_google).setOnClickListener(this);
        findViewById(R.id.fab_login_phone).setOnClickListener(this);
    }

    private boolean exit = false;

    @Override
    public void onBackPressed() {
        if (exit) {
            android.os.Process.killProcess(Process.myPid());
        } else {
            Snackbar.make(Launcher.this.findViewById(android.R.id.content), getString(R.string.message_back_exit), Snackbar.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void handleGoogleSignIn(GoogleSignInResult result) {
        Log.d(TAG, "Google Sign In - " + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
            if (account != null) {
                Log.d(TAG, "Google | email - " + account.getEmail() + " : display name - " + account.getDisplayName());
                login.cruzerLogin(account);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_login_phone:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_LOGIN_PHONE).build());
                loginDialog.show(true);
                break;
            case R.id.button_login_google:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_LOGIN_GOOGLE).build());
                startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(googleApiClient), 2);
                progressDialog.setMessage(getString(R.string.message_wait_task_pending));
                progressDialog.show();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);
        progressDialog.dismiss();
        switch (requestCode) {
            case 2:
                handleGoogleSignIn(Auth.GoogleSignInApi.getSignInResultFromIntent(data));
                break;
        }
    }
}
