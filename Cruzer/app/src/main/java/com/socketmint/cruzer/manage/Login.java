package com.socketmint.cruzer.manage;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.PUC;
import com.socketmint.cruzer.dataholder.insurance.Insurance;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.location.City;
import com.socketmint.cruzer.dataholder.location.Country;
import com.socketmint.cruzer.dataholder.vehicle.Manu;
import com.socketmint.cruzer.dataholder.vehicle.Model;
import com.socketmint.cruzer.dataholder.expense.service.Problem;
import com.socketmint.cruzer.dataholder.expense.Refuel;
import com.socketmint.cruzer.dataholder.expense.service.Service;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.dataholder.workshop.Workshop;
import com.socketmint.cruzer.dataholder.workshop.WorkshopType;
import com.socketmint.cruzer.main.History;
import com.socketmint.cruzer.startup.Launcher;
import com.socketmint.cruzer.ui.UiElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class Login {
    public static Login instance;
    private static final String TAG = "Login";
    private int currentLoginType = 0;
    private int pendingManus, pendingModels, pendingVehicles, pendingRefuels, pendingPUCs, pendingCountries, pendingCities, pendingInsurances, pendingWorkshopTypes, pendingServiceStatuses, pendingWorkshops, pendingServices, pendingProblems;
    private boolean vehicleRequested, workshopTypeRequested, workshopRequested, serviceStatusRequested, cityRequested;

    private Activity activity;

    private Dialog dialog;
    private AppCompatEditText editMobile;
    private AppCompatButton btnDone;
    private ProgressDialog progressDialog;

    private UiElement uiElement;
    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();

    private RequestQueue requestQueue;

    public static Login getInstance() {
        if (instance == null)
            instance = new Login();
        return instance;
    }

    public void initInstance(Activity activity) {
        this.activity = activity;
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        locData.cruzerInstance(activity);
        progressDialog = new ProgressDialog(activity);
        progressDialog.setCancelable(false);
        uiElement = new UiElement(activity);
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
    }

    private void create() {
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_login);
        setUIElements();
        dialog.getWindow().setLayout(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    private void setUIElements() {
        editMobile = (AppCompatEditText) dialog.findViewById(R.id.edit_mobile);
        dialog.findViewById(R.id.text_message).setVisibility(View.GONE);
        dialog.findViewById(R.id.edit_password).setVisibility(View.GONE);
        dialog.findViewById(R.id.checkbox_show_password).setVisibility(View.GONE);
        dialog.findViewById(R.id.layout_name).setVisibility(View.GONE);
        dialog.findViewById(R.id.layout_email).setVisibility(View.GONE);
        btnDone = (AppCompatButton) dialog.findViewById(R.id.button_login_phone);
    }

    public void show(final String email, final String firstName, final String lastName) {
        create();
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editMobile.getText().toString().isEmpty()) {
                    Snackbar.make(dialog.findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                uiElement.hideKeyboard(btnDone);

                login(email, firstName, lastName, editMobile.getText().toString());
            }
        });
        dialog.show();
    }

    public void cruzerLogin(final GoogleSignInAccount account) {
        String name = account.getDisplayName();
        int lastSpace = (name != null) ? name.lastIndexOf(" ") : -1;
        String firstName = (lastSpace > 0) ? name.substring(0, lastSpace) : ((name != null) ? name : "");
        String lastName = (lastSpace > 0) ? name.substring(lastSpace + 1) : "";
        Log.d(TAG, "firstName = " + firstName + " | lastName = " + lastName);
        login(account.getEmail(), firstName, lastName, "");
    }

    private void initFail() {
        if (progressDialog != null)
            progressDialog.dismiss();

        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        Snackbar.make(activity.findViewById(android.R.id.content), R.string.message_init_fail, Snackbar.LENGTH_SHORT).show();
    }

    private void volleyFail() {
        if (progressDialog != null)
            progressDialog.dismiss();

        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        Snackbar.make(activity.findViewById(android.R.id.content), R.string.message_volley_error_response, Snackbar.LENGTH_LONG).show();
    }

    private void moveForward() {
        showPendingReqLogs();
        int total = pendingManus + pendingModels + pendingVehicles + pendingRefuels + pendingPUCs + pendingCountries + pendingCities + pendingInsurances + pendingWorkshopTypes + pendingServiceStatuses + pendingWorkshops + pendingServices + pendingProblems;
        Log.d(TAG, "pendingTotal = " + total + " | gotWhatIsRequested = " + gotWhatIsRequested());
        if (total > 0 || !gotWhatIsRequested())
            return;

        if (progressDialog != null)
            progressDialog.dismiss();

        login(currentLoginType);
        activity.startActivity(new Intent(activity, History.class));
        activity.finish();
    }

    private void showPendingReqLogs() {
        if (pendingManus > 0)
            Log.d(TAG, "pendingManus : " + pendingManus);
        if (pendingModels > 0)
            Log.d(TAG, "pendingModels : " + pendingModels);
        if (pendingVehicles > 0)
            Log.d(TAG, "pendingVehicles : " + pendingVehicles);
        if (pendingRefuels > 0)
            Log.d(TAG, "pendingManus : " + pendingRefuels);
        if (pendingPUCs > 0)
            Log.d(TAG, "pendingPUCs : " + pendingPUCs);
        if (pendingCountries > 0)
            Log.d(TAG, "pendingCountries : " + pendingCountries);
        if (pendingCities > 0)
            Log.d(TAG, "pendingCities : " + pendingCities);
        if (pendingInsurances > 0)
            Log.d(TAG, "pendingInsurances : " + pendingInsurances);
        if (pendingWorkshopTypes > 0)
            Log.d(TAG, "pendingWorkshopTypes : " + pendingWorkshopTypes);
        if (pendingWorkshops > 0)
            Log.d(TAG, "pendingWorkshops : " + pendingWorkshops);
        if (pendingServiceStatuses > 0)
            Log.d(TAG, "pendingServiceStatuses : " + pendingServiceStatuses);
        if (pendingServices > 0)
            Log.d(TAG, "pendingServices : " + pendingServices);
        if (pendingProblems > 0)
            Log.d(TAG, "pendingProblems : " + pendingProblems);
        if (!vehicleRequested)
            Log.d(TAG, "vehicle is not requested");
        if (!workshopRequested)
            Log.d(TAG, "workshop is not requested");
        if (!serviceStatusRequested)
            Log.d(TAG, "serviceStatus is not requested");
        if (!cityRequested)
            Log.d(TAG, "city is not requested");
        if (!workshopTypeRequested)
            Log.d(TAG, "workshopType is not requested");
    }

    private void initializePendingCount() {
        pendingManus = 0;
        pendingModels = 0;
        pendingVehicles = 0;
        pendingRefuels = 0;
        pendingPUCs = 0;
        pendingCountries = 0;
        pendingCities = 0;
        pendingInsurances = 0;
        pendingWorkshopTypes = 0;
        pendingServiceStatuses = 0;
        pendingWorkshops = 0;
        pendingServices = 0;
        pendingProblems = 0;

        vehicleRequested = false;
        workshopRequested = false;
        serviceStatusRequested = false;
        cityRequested = false;
        workshopTypeRequested = false;
    }

    private boolean gotWhatIsRequested() {
        return (vehicleRequested && workshopRequested && serviceStatusRequested && cityRequested);
    }

    private void login(final String email, final String firstName, final String lastName, final String mobile) {
        if (progressDialog != null) {
            progressDialog.setMessage(activity.getString(R.string.message_authenticating));
            progressDialog.show();
        }

        Map<String, Object> claimsMap = new HashMap<>();
        if (mobile.isEmpty())
            claimsMap.put(Constants.VolleyRequest.AUTH_EMAIL_PARAM, email);
        else
            claimsMap.put(Constants.VolleyRequest.AUTH_MOBILE_PARAM, mobile);
        final String authParam = Jwts.builder().setClaims(claimsMap).setHeaderParam("typ", "JWT").setIssuedAt(Calendar.getInstance().getTime()).setExpiration(new Date(Calendar.getInstance().getTime().getTime() + 10*60000)).signWith(SignatureAlgorithm.HS256, Base64.encodeToString(activity.getString(R.string.key_jwt).getBytes(), Base64.DEFAULT)).compact();
        Log.d(TAG, "jwt(" + claimsMap.toString() + ") - " + authParam);

        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.OAUTH, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "auth = " + response);

                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        locData.storeToken(authResponse.getString(Constants.Json.TOKEN));

                        JSONObject info = new JSONObject(authResponse.getString(Constants.Json.INFO));
                        String id = info.optString(DatabaseSchema.COLUMN_ID);
                        String fName = info.optString(DatabaseSchema.Users.COLUMN_FIRST_NAME);
                        String lName = info.optString(DatabaseSchema.Users.COLUMN_LAST_NAME);
                        String password = info.optString(DatabaseSchema.Users.COLUMN_PASSWORD);
                        String contact = info.optString(DatabaseSchema.Users.COLUMN_MOBILE);
                        String cityId = info.optString(DatabaseSchema.Users.COLUMN_CITY_ID);

                        currentLoginType = LoginType.GOOGLE;

                        Log.d(TAG, "firstName = " + firstName + " | lastName = " + lastName);
                        databaseHelper.addUser(id, (mobile.isEmpty()) ? contact : mobile, password, (firstName.isEmpty() ? fName : firstName), (lastName.isEmpty() ? lName : lastName), email, cityId);
                        if (databaseHelper.vehicleCount() > 0) {
                            for (Vehicle vehicle : databaseHelper.vehicles()) {
                                databaseHelper.updateVehicle(vehicle.getId(), databaseHelper.user().getId());
                            }
                        }

                        if (!mobile.isEmpty())
                            putUser(email, firstName, lastName, mobile);

                        initializePendingCount();
                        getManus();
                        getCountries();
                        getWorkshopTypes();
                        getServiceStatuses();

                        if (progressDialog != null)
                            progressDialog.setMessage(activity.getString(R.string.message_getting_data));

                    } else {
                        if (progressDialog != null)
                            progressDialog.dismiss();
                        String message = authResponse.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_no_user))) {
                            if (mobile.isEmpty())
                                show(email, firstName, lastName);
                            else
                                register(email, firstName, lastName, mobile);
                        }
                    }
                } catch (JSONException e) { Log.e(TAG, "auth : is not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (progressDialog != null)
                    progressDialog.dismiss();

                Snackbar.make(activity.findViewById(android.R.id.content), R.string.message_volley_error_response, Snackbar.LENGTH_LONG).show();
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

    private void register(final String email, final String firstName, final String lastName, final String mobile) {
        if (progressDialog != null)
            progressDialog.setMessage(activity.getString(R.string.message_registering));

        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_MOBILE, mobile);
        bodyParams.put(DatabaseSchema.Users.COLUMN_EMAIL, email);
        bodyParams.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, firstName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_LAST_NAME, lastName);

        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.USER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "register = " + response);
                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        login(email, firstName, lastName, mobile);
                    else
                        initFail();
                } catch (JSONException e) { Log.e(TAG, "registration : is not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
        };
        requestQueue.add(request);
    }

    private void putUser(final String email, final String firstName, final String lastName, final String mobile) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_MOBILE, mobile);
        bodyParams.put(DatabaseSchema.Users.COLUMN_EMAIL, email);
        bodyParams.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, firstName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_LAST_NAME, lastName);

        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.USER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "put user - " + response);
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
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void getManus() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.MANU, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "manu - " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    if (isCancelled())
                                        break;
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.COLUMN_ID);
                                    String name = object.getString(DatabaseSchema.Manus.COLUMN_NAME);
                                    Manu manu = databaseHelper.manu(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{sId});
                                    if (manu != null) {
                                        if (!manu.name.equals(name))
                                            databaseHelper.updateManu(sId, name);
                                    } else
                                        databaseHelper.addManu(sId, name);
                                }
                            } catch (JSONException e) { cancel(true); Log.d(TAG, "trouble in manu loop"); initFail();  }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingManus--;
                            if (isCancelled())
                                initFail();
                            else
                                getModels();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) {
                    pendingManus--;
                    Log.e(TAG, "manus : not added or not json");
                    initFail();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingManus++;
        requestQueue.add(request);
    }

    private void getModels() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.MODEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "model response = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    if (isCancelled())
                                        break;
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.COLUMN_ID);
                                    String manuId = object.getString(DatabaseSchema.Models.COLUMN_MANU_ID);
                                    String name = object.getString(DatabaseSchema.Models.COLUMN_NAME);
                                    Manu manu = databaseHelper.manu(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{manuId});
                                    if (manu != null) {
                                        Model model = databaseHelper.model(Arrays.asList(DatabaseSchema.Models.COLUMN_MANU_ID, DatabaseSchema.Models.COLUMN_NAME), new String[]{manu.getId(), name});
                                        if (model != null) {
                                            if (!model.getManuId().equals(manu.getId()) || !model.name.equals(name))
                                                databaseHelper.updateModel(sId, manu.getId(), name);
                                        } else
                                            databaseHelper.addModel(sId, manu.getId(), name);
                                    }
                                }
                            } catch (JSONException e) { cancel(true); Log.d(TAG, "trouble in model loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingModels--;
                            if (isCancelled())
                                initFail();
                            else
                                vehicles();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) {
                    pendingModels--;
                    Log.e(TAG, "model : not added or not json");
                    initFail();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingModels++;
        requestQueue.add(request);
    }

    private void vehicles() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.VEHICLE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "vehicle response = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.COLUMN_ID);
                                    String reg = object.optString(DatabaseSchema.Vehicles.COLUMN_REG);
                                    String name = object.optString(DatabaseSchema.Vehicles.COLUMN_NAME);
                                    String modelId = object.optString(DatabaseSchema.Vehicles.COLUMN_MODEL_ID);
                                    Model model = databaseHelper.model(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{modelId});
                                    modelId = (model != null) ? model.getId() : "";
                                    databaseHelper.addVehicle(sId, reg, name, databaseHelper.user().getId(), modelId);
                                }
                            } catch (JSONException e) { cancel(true); Log.d(TAG, "trouble in vehicle loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            vehicleRequested = true;
                            pendingVehicles--;
                            if (isCancelled())
                                initFail();
                            else {
                                getRefuels();
                                getInsurances();
                                if (pendingWorkshops == 0 && workshopRequested) {
                                    getPUC();
                                    if (pendingServiceStatuses == 0 && serviceStatusRequested)
                                        getServices();
                                }
                            }
                        }
                    }.execute();
                } catch (JSONException e) { vehicleRequested = true; pendingVehicles--; Log.e(TAG, "vehicle : is not json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingVehicles++;
        requestQueue.add(request);
    }

    private void getRefuels() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.REFUEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "refuel response = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.COLUMN_ID);
                                    String vehicleId = object.optString(DatabaseSchema.COLUMN_VEHICLE_ID);
                                    String date = object.optString(DatabaseSchema.Services.COLUMN_DATE);
                                    String cost = object.optString(DatabaseSchema.Services.COLUMN_COST);
                                    String odo = object.optString(DatabaseSchema.Services.COLUMN_ODO);
                                    String rate = object.optString(DatabaseSchema.Refuels.COLUMN_RATE);
                                    String volume = object.optString(DatabaseSchema.Refuels.COLUMN_VOLUME);
                                    Refuel refuel = databaseHelper.refuel(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                                    Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                                    if (vehicle != null) {
                                        if (refuel == null)
                                            databaseHelper.addRefuel(sId, vehicle.getId(), date, rate, volume, cost, odo);
                                    }
                                }
                            } catch (JSONException e) { cancel(true); Log.d(TAG, "trouble in refuel loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingRefuels--;
                            moveForward();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) { pendingRefuels--; Log.e(TAG, "refuels not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingRefuels++;
        requestQueue.add(request);
    }

    private void getPUC() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.PUC, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "PUC = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.PUC.COLUMN_ID);
                                    String vehicleId = object.getString(DatabaseSchema.PUC.COLUMN_VEHICLE_ID);
                                    String workshopId = object.getString(DatabaseSchema.PUC.COLUMN_WORKSHOP_ID);
                                    String pucNo = object.optString(DatabaseSchema.PUC.COLUMN_PUC_NO);
                                    String startDate = object.optString(DatabaseSchema.PUC.COLUMN_START_DATE);
                                    String endDate = object.optString(DatabaseSchema.PUC.COLUMN_END_DATE);
                                    String fees = object.optString(DatabaseSchema.PUC.COLUMN_FEES);
                                    String details = object.optString(DatabaseSchema.PUC.COLUMN_DETAILS);

                                    Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                                    Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshopId});
                                    if (vehicle == null || workshop == null)
                                        continue;

                                    PUC item = databaseHelper.puc(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                                    if (item == null)
                                        databaseHelper.addPUC(sId, vehicle.getId(), workshop.getId(), pucNo, startDate, endDate, fees, details);
                                    else
                                        databaseHelper.updatePUC(sId, vehicle.getId(), workshop.getId(), pucNo, startDate, endDate, fees, details);
                                }
                            } catch (JSONException e) { Log.e(TAG, "trouble in PUC loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingPUCs--;
                            moveForward();
                        }
                    }.execute();
                } catch (JSONException e) { pendingPUCs--; Log.e(TAG, "PUC not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
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
        pendingPUCs++;
        requestQueue.add(request);
    }

    private void getCountries() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.COUNTRIES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "fetch countries = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String id = object.getString(DatabaseSchema.COLUMN_ID);
                                    String country = object.optString(DatabaseSchema.Countries.COLUMN_COUNTRY);
                                    Country item = databaseHelper.country(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
                                    if (item == null)
                                        databaseHelper.addCountry(id, country);
                                    else if (!item.country.equals(country))
                                        databaseHelper.updateCountry(id, country);
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in country loop."); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingCountries--;
                            if (!isCancelled())
                                getCities();
                        }
                    }.execute();
                } catch (JSONException e) { pendingCountries--; Log.d(TAG, "countries not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
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
        pendingCountries++;
        requestQueue.add(request);
    }

    private void getCities() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.CITIES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "fetch cities = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String id = object.getString(DatabaseSchema.COLUMN_ID);
                                    String city = object.optString(DatabaseSchema.Cities.COLUMN_CITY);
                                    String countryId = object.optString(DatabaseSchema.Cities.COLUMN_COUNTRY_ID);
                                    City item = databaseHelper.city(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
                                    if (item == null)
                                        databaseHelper.addCity(id, city, countryId);
                                    else if (!item.city.equals(city))
                                        databaseHelper.updateCity(id, city, countryId);
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in cities loop"); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            cityRequested = true;
                            pendingCities--;
                            if (!isCancelled() && (pendingWorkshopTypes == 0 && workshopTypeRequested))
                                getWorkshops();
                        }
                    }.execute();
                } catch (JSONException e) { pendingCities--; cityRequested = true; Log.d(TAG, "cities not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
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
        pendingCities++;
        requestQueue.add(request);
    }

    private void getInsurances() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.INSURANCE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Insurances = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.Insurances.COLUMN_ID);
                                    String vehicleId = object.getString(DatabaseSchema.Insurances.COLUMN_VEHICLE_ID);
                                    String companyId = object.getString(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID);
                                    String policyNo = object.optString(DatabaseSchema.Insurances.COLUMN_POLICY_NO);
                                    String startDate = object.optString(DatabaseSchema.Insurances.COLUMN_START_DATE);
                                    String endDate = object.optString(DatabaseSchema.Insurances.COLUMN_END_DATE);
                                    String premium = object.optString(DatabaseSchema.Insurances.COLUMN_PREMIUM);
                                    String details = object.optString(DatabaseSchema.Insurances.COLUMN_DETAILS);

                                    String company = object.optString(DatabaseSchema.InsuranceCompanies.COLUMN_COMPANY);

                                    Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                                    InsuranceCompany insuranceCompany = databaseHelper.insuranceCompany(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{companyId});

                                    if (insuranceCompany == null)
                                        databaseHelper.addInsuranceCompany(companyId, company);
                                    else
                                        databaseHelper.updateInsuranceCompany(companyId, company);

                                    if (vehicle == null)
                                        continue;

                                    Insurance item = databaseHelper.insurance(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                                    if (item == null)
                                        databaseHelper.addInsurance(sId, vehicle.getId(), companyId, policyNo, startDate, endDate, premium, details);
                                    else
                                        databaseHelper.updateInsurance(sId, vehicle.getId(), companyId, policyNo, startDate, endDate, premium, details);
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in insurance loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingInsurances--;
                            moveForward();
                        }
                    }.execute();
                } catch (JSONException e) { pendingInsurances--; Log.e(TAG, "Insurances not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
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
        pendingInsurances++;
        requestQueue.add(request);
    }

    private void getWorkshopTypes() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.WORKSHOP_TYPES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "workshop types = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String id = object.getString(DatabaseSchema.COLUMN_ID);
                                    String type = object.optString(DatabaseSchema.WorkshopTypes.COLUMN_TYPE);
                                    WorkshopType workshopType = databaseHelper.workshopType(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
                                    if (workshopType == null)
                                        databaseHelper.addWorkshopType(id, type);
                                    else if (!workshopType.type.equals(type))
                                        databaseHelper.updateWorkshopType(id, type);
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in workshop types loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            workshopTypeRequested = true;
                            pendingWorkshopTypes--;
                            if (!isCancelled() && (cityRequested && pendingCities == 0))
                                getWorkshops();
                        }
                    }.execute();
                } catch (JSONException e) { workshopRequested = true; pendingWorkshopTypes--; Log.e(TAG, "workshop type is not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingWorkshopTypes++;
        requestQueue.add(request);
    }

    private void getWorkshops() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.WORKSHOP, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "workshop response = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.COLUMN_ID);
                                    String name = object.optString(DatabaseSchema.Workshops.COLUMN_NAME);
                                    String address = object.optString(DatabaseSchema.Workshops.COLUMN_ADDRESS);
                                    String manager = object.optString(DatabaseSchema.Workshops.COLUMN_MANAGER);
                                    String contact = object.optString(DatabaseSchema.Workshops.COLUMN_CONTACT);
                                    String latitude = object.optString(DatabaseSchema.Workshops.COLUMN_LATITUDE);
                                    String longitude = object.optString(DatabaseSchema.Workshops.COLUMN_LONGITUDE);
                                    String city = object.optString(DatabaseSchema.Workshops.COLUMN_CITY_ID);
                                    String area = object.optString(DatabaseSchema.Workshops.COLUMN_AREA);
                                    String offerings = object.optString(DatabaseSchema.Workshops.COLUMN_OFFERINGS);
                                    String workshopTypeId = object.optString(DatabaseSchema.Workshops.COLUMN_WORKSHOP_TYPE_ID);

                                    Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{sId});
                                    if (workshop != null)
                                        databaseHelper.updateWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings, workshopTypeId);
                                    else
                                        databaseHelper.addWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings, workshopTypeId);
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in workshops loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            workshopRequested = true;
                            pendingWorkshops--;
                            if (!isCancelled()) {
                                if (pendingVehicles == 0 && vehicleRequested) {
                                    getPUC();
                                    if (pendingServiceStatuses == 0 && serviceStatusRequested)
                                        getServices();
                                }
                            }

                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) { workshopRequested = true; pendingWorkshops--; Log.e(TAG, "workshops not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingWorkshops++;
        requestQueue.add(request);
    }

    public void getServiceStatuses() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.STATUS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "get service status = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String id = object.getString(DatabaseSchema.ServiceStatus.COLUMN_ID);
                                    String details = object.optString(DatabaseSchema.ServiceStatus.COLUMN_DETAILS);
                                    databaseHelper.addStatus(id, details);
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in service status loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            serviceStatusRequested = true;
                            pendingServiceStatuses--;
                            if (!isCancelled() && (pendingWorkshops == 0 && workshopRequested) && (pendingVehicles == 0 && vehicleRequested))
                                getServices();
                        }
                    }.execute();
                } catch (JSONException e) {
                    serviceStatusRequested = true;
                    pendingServiceStatuses--;
                    Log.d(TAG, "get service status is not in json");
                    moveForward();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingServiceStatuses++;
        requestQueue.add(request);
    }

    private void getServices() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.SERVICE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "service response = " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    final List<String> serviceIdTrackerList = new ArrayList<>();
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = array.getJSONObject(i);
                                    String sId = object.getString(DatabaseSchema.COLUMN_ID);
                                    String vehicleId = object.getString(DatabaseSchema.COLUMN_VEHICLE_ID);
                                    String date = object.optString(DatabaseSchema.Services.COLUMN_DATE);
                                    String workshopId = object.optString(DatabaseSchema.Services.COLUMN_WORKSHOP_ID);
                                    String cost = object.optString(DatabaseSchema.Services.COLUMN_COST);
                                    String odo = object.optString(DatabaseSchema.Services.COLUMN_ODO);
                                    String details = object.optString(DatabaseSchema.Services.COLUMN_DETAILS);
                                    String status = object.optString(DatabaseSchema.Services.COLUMN_STATUS);
                                    String userId = object.optString(DatabaseSchema.Services.COLUMN_USER_ID);
                                    String roleId = object.optString(DatabaseSchema.Services.COLUMN_ROLE_ID);
                                    String vat = object.optString(DatabaseSchema.Services.COLUMN_VAT);

                                    Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                                    if (vehicle != null) {
                                        Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                                        if (service == null) {
                                            Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshopId});
                                            String wId = (workshop == null) ? "" : workshop.getId();
                                            databaseHelper.addService(sId, vehicle.getId(), date, wId, cost, odo, details, status, userId, roleId, vat);
                                        }
                                        serviceIdTrackerList.add(sId);
                                    }
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in service loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingServices--;
                            if (isCancelled())
                                return;
                            for (String sId : serviceIdTrackerList) {
                                getProblems(sId);
                            }
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) { pendingServices--; Log.e(TAG, "services not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingServices++;
        requestQueue.add(request);
    }

    private void getProblems(final String serviceId) {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.GET_PROBLEMS(serviceId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "problem response " + response);
                try {
                    final JSONArray array = new JSONArray(response);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                for (int i =0; i < array.length(); i++) {
                                    JSONObject problem = array.getJSONObject(i);
                                    String sId = problem.getString(DatabaseSchema.COLUMN_ID);
                                    String lCost = problem.optString(DatabaseSchema.Problems.COLUMN_LCOST);
                                    String pCost = problem.optString(DatabaseSchema.Problems.COLUMN_PCOST);
                                    String details = problem.optString(DatabaseSchema.Problems.COLUMN_DETAILS);
                                    String qty = problem.optString(DatabaseSchema.Problems.COLUMN_QTY);
                                    String rate = problem.optString(DatabaseSchema.Problems.COLUMN_RATE);
                                    String type = problem.optString(DatabaseSchema.Problems.COLUMN_TYPE);
                                    Problem item = databaseHelper.problem(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                                    if (item == null) {
                                        Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{serviceId});
                                        if (service != null)
                                            databaseHelper.addProblem(sId, service.getId(), details, lCost, pCost, qty, rate, type);
                                    }
                                }
                            } catch (JSONException e) { cancel(true); Log.e(TAG, "trouble in problems loop"); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            pendingProblems--;
                            if (!isCancelled())
                                moveForward();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) { pendingProblems--; Log.e(TAG, "problems not in json"); moveForward(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                volleyFail();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
        pendingProblems++;
        requestQueue.add(request);
    }

    public void login(int type) {
        locData.storeLoginType(type);
    }

    public int login() {
        return locData.loginType();
    }

    public void logout() {
        locData.clearData();
        activity.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        activity.startActivity(new Intent(activity, Launcher.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }

    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public static abstract class LoginType {
        public static final int TRIAL = 1;
//        public static final int PHONE = 2;
        public static final int GOOGLE = 3;
    }
}
