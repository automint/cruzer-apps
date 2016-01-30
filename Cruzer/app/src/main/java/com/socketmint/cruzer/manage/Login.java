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
import com.socketmint.cruzer.dataholder.Manu;
import com.socketmint.cruzer.dataholder.Model;
import com.socketmint.cruzer.dataholder.Problem;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.startup.Launcher;
import com.socketmint.cruzer.ui.UiElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class Login {
    public static Login instance;
    private static final String TAG = "Login";
    private int currentLoginType = 0;

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

    public void create() {
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_login);
        setUIElements();
        dialog.getWindow().setLayout(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    public void show(final String email, final String firstName, final String lastName) {
        create();
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emptyFields()) {
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
        login(account.getEmail(), firstName, lastName, "");
    }

    private void initFail() {
        if (progressDialog != null)
            progressDialog.dismiss();

        Snackbar.make(activity.findViewById(android.R.id.content), R.string.message_init_fail, Snackbar.LENGTH_SHORT).show();
    }

    private void volleyFail() {
        if (progressDialog != null)
            progressDialog.dismiss();

        Snackbar.make(activity.findViewById(android.R.id.content), R.string.message_volley_error_response, Snackbar.LENGTH_LONG).show();
    }

    private void moveForward() {
        if (progressDialog != null)
            progressDialog.dismiss();

        login(currentLoginType);
        activity.startActivity(new Intent(activity, ViewHistory.class));
        activity.finish();
    }

    private void login(final String email, final String firstName, final String lastName, final String mobile) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.setMessage(activity.getString(R.string.message_authenticating));
                    progressDialog.show();
                }
            }
        });

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
                        String password = info.optString(DatabaseSchema.Users.COLUMN_PASSWORD);
                        String contact = info.optString(DatabaseSchema.Users.COLUMN_MOBILE);

                        currentLoginType = LoginType.GOOGLE;

                        databaseHelper.addUser(id, (mobile.isEmpty()) ? contact : mobile, password, firstName, lastName, email);
                        if (databaseHelper.vehicleCount() > 0) {
                            for (Vehicle vehicle : databaseHelper.vehicles()) {
                                databaseHelper.updateVehicle(vehicle.getId(), databaseHelper.user().getId());
                            }
                        }

                        if (!mobile.isEmpty())
                            putUser(email, firstName, lastName, mobile);
                        getManus();

                        if (progressDialog != null)
                            progressDialog.setMessage(activity.getString(R.string.message_getting_data));

                    } else {
                        String message = authResponse.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_no_user))) {
                            if (progressDialog != null)
                                progressDialog.dismiss();

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
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null)
                    progressDialog.setMessage(activity.getString(R.string.message_registering));
            }
        });

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
                    else {
                        initFail();
                    }
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
                            } catch (JSONException e) { cancel(true); initFail();  }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            getStatusTable();
                            if (isCancelled())
                                initFail();
                            else
                                getModels();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) {
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
                            } catch (JSONException e) { cancel(true); initFail(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            if (isCancelled())
                                initFail();
                            else
                                vehicleFromServer();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) {
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
        requestQueue.add(request);
    }

    private void vehicleFromServer() {

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
                                    if (model != null)
                                        databaseHelper.addVehicle(sId, reg, name, databaseHelper.user().getId(), model.getId());
                                    else
                                        databaseHelper.addVehicleFromServer(sId, reg, name, databaseHelper.user().getId());
                                }
                            } catch (JSONException e) { cancel(true); moveForward(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            if (isCancelled())
                                moveForward();
                            else
                                getRefuels();
                        }
                    }.execute();
                } catch (JSONException e) {
                    Log.e(TAG, "vehicle : is not json");
                    moveForward();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                moveForward();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                params.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return params;
            }
        };
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
                                    String volume = object.getString(DatabaseSchema.Refuels.COLUMN_VOLUME);
                                    Refuel refuel = databaseHelper.refuel(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                                    Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                                    if (vehicle != null) {
                                        if (refuel == null)
                                            databaseHelper.addRefuel(sId, vehicle.getId(), date, rate, volume, cost, odo);
                                    }
                                }
                            } catch (JSONException e) { cancel(true); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            getWorkshops();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) {
                    Log.e(TAG, "refuels not in json");
                    getWorkshops();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                moveForward();
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
        requestQueue.add(request);
    }

    private void getServices() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.SERVICE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "service response = " + response);
                try {
                    JSONArray array = new JSONArray(response);
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
                        Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                        if (vehicle != null) {
                            Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                            if (service == null) {
                                Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshopId});
                                String wId = "";
                                if (workshop != null)
                                    wId = workshop.getId();
                                databaseHelper.addService(sId, vehicle.getId(), date, wId, cost, odo, details, status, userId, roleId);
                            }
                            getProblems(sId);
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "services not in json"); }
                moveForward();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                moveForward();
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
        requestQueue.add(request);
    }

    private void getProblems(final String serviceId) {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.GET_PROBLEMS(serviceId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "problem response " + response);
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i =0; i < array.length(); i++) {
                        JSONObject problem = array.getJSONObject(i);
                        String sId = problem.getString(DatabaseSchema.COLUMN_ID);
                        String lCost = problem.optString(DatabaseSchema.Problems.COLUMN_LCOST);
                        String pCost = problem.optString(DatabaseSchema.Problems.COLUMN_PCOST);
                        String details = problem.optString(DatabaseSchema.Problems.COLUMN_DETAILS);
                        String qty = problem.optString(DatabaseSchema.Problems.COLUMN_QTY);
                        Problem item = databaseHelper.problem(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                        if (item == null) {
                            Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{serviceId});
                            if (service != null)
                                databaseHelper.addProblem(sId, service.getId(), details, lCost, pCost, qty);
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "problems not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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
                                    String city = object.optString(DatabaseSchema.Workshops.COLUMN_CITY);
                                    String area = object.optString(DatabaseSchema.Workshops.COLUMN_AREA);
                                    String offerings = object.optString(DatabaseSchema.Workshops.COLUMN_OFFERINGS);
                                    Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{sId});
                                    Log.e(TAG, "workshop != null - " + (workshop != null));
                                    if (workshop != null)
                                        databaseHelper.updateWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings);
                                    else
                                        databaseHelper.addWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings);
                                }
                            } catch (JSONException e) { cancel(true); moveForward(); }
                            return null;
                        }
                        @Override
                        public void onPostExecute(Void result) {
                            getServices();
                        }
                    }.execute();
                } catch (JSONException | NullPointerException e) {
                    Log.e(TAG, "workshops not in json");
                    getServices();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                moveForward();
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
        requestQueue.add(request);
    }

    public void getStatusTable() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.STATUS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "get status = " + response);
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String id = object.getString(DatabaseSchema.Status.COLUMN_ID);
                        String details = object.optString(DatabaseSchema.Status.COLUMN_DETAILS);
                        databaseHelper.addStatus(id, details);
                    }
                } catch (JSONException e) { Log.d(TAG, "get status is not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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
        requestQueue.add(request);
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

    private boolean emptyFields() {
        return (editMobile.getText().toString().isEmpty());
    }

    public void login(int type) {
        locData.storeLoginType(type);
    }

    public int login() {
        return locData.loginType();
    }

    public void logout() {
        locData.clearData();
        databaseHelper.logout();
        activity.startActivity(new Intent(activity, Launcher.class));
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
        public static final int PHONE = 2;
        public static final int GOOGLE = 3;
    }
}
