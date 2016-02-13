package com.socketmint.cruzer.manage.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GcmListenerService;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Manu;
import com.socketmint.cruzer.dataholder.Model;
import com.socketmint.cruzer.dataholder.Problem;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.main.History;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class MessageListener extends GcmListenerService {

    private static final String TAG = "GCM Listener";

    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();

    private RequestQueue requestQueue;
    private Thread gcmOperation, networkOperation;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        Log.d("GCM - From", from);
        Log.d("GCM - Data", data.toString());
        locData.cruzerInstance(getApplicationContext());
        databaseHelper = new DatabaseHelper(getApplicationContext());
        try {
            JSONObject root = new JSONObject(data.getString(Constants.Gcm.KEY_DATA));
            Log.d(TAG, root.toString());
            final String method = root.getString(Constants.Json.METHOD);
            final String table = root.getString(Constants.Json.TABLE);
            final String body = root.getString(Constants.Json.BODY);
            gcmOperation = new Thread(new Runnable() {
                @Override
                public void run() {
                    doOperation(method, table, body);
                }
            });
            gcmOperation.start();
        } catch (JSONException e) { Log.e(TAG, "not in json"); }
    }

    private void doOperation(final String method, final String table, final String body) {
        switch (table) {
            case DatabaseSchema.Services.TABLE_NAME:
                try {
                    JSONObject object = new JSONObject(body);

                    String userMobile = object.optString(DatabaseSchema.Users.COLUMN_MOBILE);
                    User user = databaseHelper.user();

                    if (!user.mobile.equals(userMobile)) {
                        Log.e(TAG, "user not found. exiting | local - " + user.mobile + ", server - " + userMobile);
                        return;
                    }

                    String userId = object.optString(DatabaseSchema.Vehicles.COLUMN_USER_ID);
                    String firstName = object.optString(DatabaseSchema.Users.COLUMN_FIRST_NAME);
                    String lastName = object.optString(DatabaseSchema.Users.COLUMN_LAST_NAME);
                    String email = object.optString(DatabaseSchema.Users.COLUMN_EMAIL);

                    if (!userId.equals(user.getsId()) || !firstName.equals(user.firstName) || !lastName.equals(user.lastName))
                        databaseHelper.updateUser(userMobile, firstName, lastName, email);

                    String vehicleId = object.optString(DatabaseSchema.COLUMN_VEHICLE_ID);
                    String reg = object.optString(DatabaseSchema.Vehicles.COLUMN_REG);
                    final String modelId = object.optString(DatabaseSchema.Vehicles.COLUMN_MODEL_ID);

                    Model model = databaseHelper.model(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{modelId});
                    Log.d(TAG, "(model == null) : " + (model == null));
                    if (model == null) {
                        gcmOperation.interrupt();
                        networkOperation = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "network operation");
                                fetchModel(modelId);
                            }
                        });
                        networkOperation.start();
                        return;
                    }

                    Vehicle vehicle = databaseHelper.vehicleByReg(reg);
                    String vId;

                    if (vehicle != null) {
                        Log.d(TAG, "vehicle exists");
                        if (!reg.equals(vehicle.reg) || !modelId.equals(vehicle.getModelId()))
                            databaseHelper.updateVehicle(vehicleId, reg, modelId);
                        vId = vehicle.getId();
                    } else {
                        Log.d(TAG, "new vehicle");
                        vId = databaseHelper.addVehicle(reg, vehicleId, modelId);

                        if (vId == null) {
                            Log.e(TAG, "can not insert vehicle. exiting");
                            return;
                        }
                    }

                    String id = object.optString(DatabaseSchema.COLUMN_ID);
                    String date = object.optString(DatabaseSchema.Services.COLUMN_DATE);
                    String cost = object.optString(DatabaseSchema.Services.COLUMN_COST);
                    String odo = object.optString(DatabaseSchema.Services.COLUMN_ODO);
                    String details = object.optString(DatabaseSchema.Services.COLUMN_DETAILS);
                    String status = object.optString(DatabaseSchema.Services.COLUMN_STATUS);
                    final String workshopId = object.optString(DatabaseSchema.Services.COLUMN_WORKSHOP_ID);
                    String uId = object.optString(DatabaseSchema.Services.COLUMN_USER_ID);
                    String roleId = object.optString(DatabaseSchema.Services.COLUMN_ROLE_ID);

                    Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{id});
                    Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshopId});
                    if (workshop == null) {
                        gcmOperation.interrupt();
                        networkOperation = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                fetchWorkshop(workshopId);
                            }
                        });
                        if (networkOperation.getState() == Thread.State.NEW)
                            networkOperation.start();
                        else
                            networkOperation.run();
                    }
                    String serviceId;

                    if (service == null) {
                        Log.d(TAG, "no service. adding");
                        serviceId = databaseHelper.addService(id, vId, date, (workshop != null) ? workshop.getId() : "", cost, odo, details, status, uId, roleId);
                    } else {
                        serviceId = service.getId();
                        if (databaseHelper.updateService(id, vId, date, (workshop != null) ? workshop.getId() : service.getWorkshopId(), cost, odo, details, status, uId, roleId)) {
                            Log.d(TAG, "service updated");
                        } else
                            Log.d(TAG, "could not update service");
                    }

                    try {
                        JSONArray array = object.getJSONArray(DatabaseSchema.Problems.TABLE_NAME);
                        for (int i =0; i < array.length(); i++) {
                            JSONObject item = array.optJSONObject(i);
                            String problemId = item.optString(DatabaseSchema.COLUMN_ID);
                            String lCost = item.optString(DatabaseSchema.Problems.COLUMN_LCOST);
                            String pCost = item.optString(DatabaseSchema.Problems.COLUMN_PCOST);
                            String problemDetails = item.optString(DatabaseSchema.Problems.COLUMN_DETAILS);
                            String qty = item.optString(DatabaseSchema.Problems.COLUMN_QTY);

                            Problem problem = databaseHelper.problem(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{problemId});
                            Log.d(TAG, "problem existing (problem == null) - " + (problem == null));

                            if (problem == null)
                                databaseHelper.addProblem(problemId, serviceId, problemDetails, lCost, pCost, qty);
                            else
                                databaseHelper.updateProblem(problemId, serviceId, problemDetails, lCost, pCost, qty);
                        }
                    } catch (JSONException e) { Log.e(TAG, "problems is not json"); }
                    Intent updateIntent = new Intent(Constants.IntentFilters.GCM);
                    updateIntent.putExtra(Constants.Gcm.MESSAGE_UPDATE, true);
                    LocalBroadcastManager.getInstance(MessageListener.this).sendBroadcast(updateIntent);
                    if (!method.equals(Constants.VolleyRequest.METHOD_PUT))
                        sendNotification("Service has been added for " + reg);
                } catch (JSONException e) { Log.e(TAG, "body is not json"); }
                break;
        }
    }

    private void fetchWorkshop(String id) {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.WORKSHOP(id), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "workshop = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
                            String message = object.getString(Constants.Json.MESSAGE);
                            if (message.equals(getString(R.string.error_auth_fail))) {
                                requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                    @Override
                                    public boolean apply(Request<?> request) {
                                        return true;
                                    }
                                });
                                authenticate();
                            }
                        }
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "model is array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String id = object.getString(DatabaseSchema.COLUMN_ID);
                        String name = object.optString(DatabaseSchema.Workshops.COLUMN_NAME);
                        String address = object.optString(DatabaseSchema.Workshops.COLUMN_ADDRESS);
                        String manager = object.optString(DatabaseSchema.Workshops.COLUMN_MANAGER);
                        String contact = object.optString(DatabaseSchema.Workshops.COLUMN_CONTACT);
                        String latitude = object.optString(DatabaseSchema.Workshops.COLUMN_LATITUDE);
                        String longitude = object.optString(DatabaseSchema.Workshops.COLUMN_LONGITUDE);
                        String cityId = object.optString(DatabaseSchema.Workshops.COLUMN_CITY_ID);
                        String area = object.optString(DatabaseSchema.Workshops.COLUMN_AREA);
                        String offerings = object.optString(DatabaseSchema.Workshops.COLUMN_OFFERINGS);
                        String workshopTypeId = object.optString(DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID);

                        Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
                        if (workshop == null)
                            databaseHelper.addWorkshop(id, name, address, manager, contact, latitude, longitude, cityId, area, offerings, workshopTypeId);
                        else
                            databaseHelper.updateWorkshop(id, name, address, manager, contact, latitude, longitude, cityId, area, offerings, workshopTypeId);
                    }
                    gcmOperation.run();
                } catch (JSONException e) { Log.d(TAG, "workshops not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headerParams = new HashMap<>();
                headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void fetchModel(String id) {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.MODEL(id), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "model = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
                            String message = object.getString(Constants.Json.MESSAGE);
                            if (message.equals(getString(R.string.error_auth_fail))) {
                                requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                    @Override
                                    public boolean apply(Request<?> request) {
                                        return true;
                                    }
                                });
                                authenticate();
                            }
                        }
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "model is array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String modelId = object.optString(DatabaseSchema.Vehicles.COLUMN_MODEL_ID);
                        String manuId = object.optString(DatabaseSchema.Models.COLUMN_MANU_ID);
                        String modelName = object.optString(Constants.Json.MODEL_NAME);
                        String manuName = object.optString(Constants.Json.MANU_NAME);

                        Manu manu = databaseHelper.manu(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{manuId});
                        if (manu == null)
                            databaseHelper.addManu(manuId, manuName);
                        Model model = databaseHelper.model(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{modelId});
                        if (model == null)
                            databaseHelper.addModel(modelId, manuId, modelName);
                    }
                    gcmOperation.run();
                } catch (JSONException e) { Log.d(TAG, "models not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headerParams = new HashMap<>();
                headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void authenticate() {
        User user = databaseHelper.user();
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(Constants.VolleyRequest.AUTH_MOBILE_PARAM, user.mobile);
        final String authParam = Jwts.builder().setClaims(claimsMap).setHeaderParam("typ", "JWT").setIssuedAt(Calendar.getInstance().getTime()).setExpiration(new Date(Calendar.getInstance().getTime().getTime() + 10*60000)).signWith(SignatureAlgorithm.HS256, Base64.encodeToString(getString(R.string.key_jwt).getBytes(), Base64.DEFAULT)).compact();
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.OAUTH, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Auth = " + response);
                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        locData.storeToken(authResponse.getString(Constants.Json.TOKEN));
                        networkOperation.interrupt();
                        networkOperation.run();
                    }
                } catch (JSONException e) { Log.e(TAG, " auth : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headerParams = new HashMap<>();
                headerParams.put(Constants.VolleyRequest.AUTH_TOKEN, authParam);
                return headerParams;
            }
        };
        requestQueue.add(request);
    }



    private void sendNotification(String message) {
        Intent intent = new Intent(this, History.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
