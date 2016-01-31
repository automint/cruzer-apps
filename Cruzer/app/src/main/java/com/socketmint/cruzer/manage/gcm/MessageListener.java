package com.socketmint.cruzer.manage.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Problem;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.main.History;
import com.socketmint.cruzer.manage.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;

public class MessageListener extends GcmListenerService {

    private static final String TAG = "gcm";

    private DatabaseHelper databaseHelper;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d("GCM - From", from);
        Log.d("GCM - Data", data.toString());
        databaseHelper = new DatabaseHelper(getApplicationContext());
        try {
            JSONObject root = new JSONObject(data.getString(Constants.Gcm.KEY_DATA));
            Log.d(TAG, root.toString());
            String method = root.getString(Constants.Json.METHOD);
            String table = root.getString(Constants.Json.TABLE);
            String body = root.getString(Constants.Json.BODY);
            doOperation(method, table, body);
        } catch (JSONException e) { Log.e(TAG, "not in json"); }
    }

    private void doOperation(String method, String table, String body) {
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
                    String modelId = object.optString(DatabaseSchema.Vehicles.COLUMN_MODEL_ID);

                    Vehicle vehicle = databaseHelper.vehicleByReg(reg);
                    String vId;

                    if (vehicle != null) {
                        Log.d(TAG, "vehicle exists");
                        if (!reg.equals(vehicle.reg) || !modelId.equals(vehicle.getModelId()))
                            databaseHelper.updateVehicleFromGcm(vehicleId, reg, modelId);
                        vId = vehicle.getId();
                    } else {
                        Log.d(TAG, "new vehicle");
                        vId = databaseHelper.addVehicleFromGcm(reg, vehicleId, modelId);

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
                    String workshopId = object.optString(DatabaseSchema.Services.COLUMN_WORKSHOP_ID);
                    String uId = object.optString(DatabaseSchema.Services.COLUMN_USER_ID);
                    String roleId = object.optString(DatabaseSchema.Services.COLUMN_ROLE_ID);

                    Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{id});
                    Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshopId});
                    String serviceId;

                    if (service == null) {
                        Log.e(TAG, "no service. adding");
                        serviceId = databaseHelper.addService(id, vId, date, (workshop != null) ? workshop.getId() : "", cost, odo, details, status, uId, roleId);                           // workshop id set nathi thayu [IMP]
                    } else {
                        serviceId = service.getId();
                        if (databaseHelper.updateService(id, vId, date, (workshop != null) ? workshop.getId() : service.getWorkshopId(), cost, odo, details, status, uId, roleId)) {         // workshop id set nathi thayu [IMP]
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

//                    Add Listener Here
                    if (!method.equals(Constants.VolleyRequest.METHOD_PUT))
                        sendNotification("Service has been added for " + reg);
                } catch (JSONException e) { Log.e(TAG, "body is not json"); }
                break;
        }
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(this, History.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
