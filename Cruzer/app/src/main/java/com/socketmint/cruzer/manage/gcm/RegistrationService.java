package com.socketmint.cruzer.manage.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegistrationService extends IntentService {
    private static final String TAG = "GCM Intent";
    private LocData locData = new LocData();

    public RegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        locData.cruzerInstance(getApplicationContext());
        try {
            InstanceID instanceID = InstanceID.getInstance(this);

            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(TAG, "GCM Registration Token: " + token);

            sendRegistrationToServer(token);

            locData.storeGcmSentStatus(true);
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            locData.storeGcmSentStatus(false);
        }

        Intent registrationComplete = new Intent(Constants.Gcm.ACTION_GCM_REG_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void sendRegistrationToServer(final String token) {
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        final LocData locData = new LocData();
        locData.cruzerInstance(getApplicationContext());
        Log.d(TAG, "locData - " + locData.gcm());
        int method = (!token.equals(locData.gcm())) ? Request.Method.PUT : Request.Method.POST;
        method = (locData.gcm().isEmpty()) ? Request.Method.POST : method;
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(Constants.Gcm.FIELD_NEW_GCM, token);
        if (method == Request.Method.PUT)
            bodyParams.put(Constants.Gcm.FIELD_OLD_GCM, locData.gcm());
        else {
            if (!locData.gcm().isEmpty())
                return;
        }
        Log.d(TAG, "Method | Body = " + method + " | " + bodyParams.toString());
        StringRequest request = new StringRequest(method, Constants.Url.GCM, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "server response = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.optBoolean(Constants.Json.SUCCESS);
                    if (success)
                        locData.storeGcm(token);
                } catch (JSONException e) { Log.e(TAG, " can not parse response"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headerParams = new HashMap<>();
                headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return headerParams;
            }
        };
        requestQueue.add(request);
    }
}
