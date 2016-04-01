package com.socketmint.cruzer.startup;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;

public class HelpFragment extends Fragment implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "HelpFragment";

    private Login login = Login.getInstance();
    private int choice;
    private String mobile;

    private AppCompatImageView imageHelp;

    private ProgressDialog progressDialog;

    public static HelpFragment newInstance(int position, String mobile) {
        HelpFragment fragment = new HelpFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.Bundle.PAGE_CHOICE, position);
        args.putString(Constants.Bundle.MOBILE, mobile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_help_screen, container, false);

        choice = getArguments().getInt(Constants.Bundle.PAGE_CHOICE);
        mobile = getArguments().getString(Constants.Bundle.MOBILE, "");
        mobile = (mobile == null) ? "" : mobile;

        imageHelp = (AppCompatImageView) view.findViewById(R.id.image_help);
        AppCompatButton buttonGetStarted = (AppCompatButton) view.findViewById(R.id.button_get_started);
        buttonGetStarted.setOnClickListener(this);

        selectHelpScreen();
        login.initInstance(getActivity());

        progressDialog = new ProgressDialog(getActivity());
        buttonGetStarted.setVisibility((choice == 3) ? View.VISIBLE : View.GONE);

        return view;
    }

    private void selectHelpScreen() {
        switch (choice) {
            case 0:
                imageHelp.setImageResource(R.drawable.help_1);
                break;
            /*case 1:
                imageHelp.setImageResource(R.drawable.help_2);
                break;*/
            case 1:
                imageHelp.setImageResource(R.drawable.help_3);
                break;
            case 2:
                imageHelp.setImageResource(R.drawable.help_5);
                break;
            case 3:
                imageHelp.setImageResource(R.drawable.help_7);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_get_started:
                if (login.login() > 0) {
                    setSeen();
                    startActivity(new Intent(getActivity(), Launcher.class));
                    getActivity().finish();
                } else {
                    GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
                    GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getActivity()).addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions).build();

                    startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(googleApiClient), 2);
                    progressDialog.setMessage(getString(R.string.message_wait_task_pending));
                    progressDialog.show();
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void setSeen() {
        LocData locData = new LocData();
        locData.cruzerInstance(getActivity());
        locData.storeHelpScreenSeen(true);
    }

    private void handleGoogleSignIn(GoogleSignInResult result) {
        Log.d(TAG, "Google Sign In - " + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
            if (account != null) {
                Log.d(TAG, "Google | email - " + account.getEmail() + " : display name - " + account.getDisplayName());
                setSeen();
                login.cruzerLogin(account, mobile);
            }
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
