package com.socketmint.cruzer.startup;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Model;
import com.socketmint.cruzer.dataholder.Problem;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.ui.UiElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LoginDialog {
    private static LoginDialog instance;

    private static final String TAG = "LoginDialog";

    private final int TYPE_DIRECT_LOGIN = 1;
    private final int TYPE_LOGIN_AR = 2;

    private UiElement uiElement;
    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();
    private Login sLogin = Login.getInstance();

    private Activity activity;
    private Dialog dialog;
    private AppCompatTextView textMessage;
    private AppCompatEditText editMobile, editPassword, editFirstName, editLastName, editEmail;
    private AppCompatButton btnDone;
    private LinearLayoutCompat layoutName, layoutEmail;
    private ProgressDialog progressDialog;

    private RequestQueue requestQueue;

    private Thread loginThread;

    public static LoginDialog getInstance() {
        if (instance == null)
            instance = new LoginDialog();
        return instance;
    }

    public void initInstance(Activity activity) {
        this.activity = activity;
        progressDialog = new ProgressDialog(activity);
        uiElement = new UiElement(activity);
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        locData.cruzerInstance(activity);
        sLogin.initInstance(activity);
    }

    public void show(boolean cancelable) {
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_login);
        setContent();
        runChoice();
        dialog.getWindow().setLayout(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(cancelable);
        dialog.show();
    }

    private void setContent() {
        layoutName = (LinearLayoutCompat) dialog.findViewById(R.id.layout_name);
        layoutEmail = (LinearLayoutCompat) dialog.findViewById(R.id.layout_email);
        textMessage = (AppCompatTextView) dialog.findViewById(R.id.text_message);
        editMobile = (AppCompatEditText) dialog.findViewById(R.id.edit_mobile);
        editPassword = (AppCompatEditText) dialog.findViewById(R.id.edit_password);
        editFirstName = (AppCompatEditText) dialog.findViewById(R.id.edit_first_name);
        editLastName = (AppCompatEditText) dialog.findViewById(R.id.edit_last_name);
        editEmail = (AppCompatEditText) dialog.findViewById(R.id.edit_email);
        btnDone = (AppCompatButton) dialog.findViewById(R.id.button_login_phone);
        AppCompatCheckBox checkBoxShowPassword = (AppCompatCheckBox) dialog.findViewById(R.id.checkbox_show_password);

        checkBoxShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    editPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                else
                    editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                editPassword.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                editPassword.setSelection(editPassword.length());
            }
        });

    }

    private void runChoice() {
        layoutName.setVisibility(View.GONE);
        layoutEmail.setVisibility(View.GONE);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emptyFields()) {
                    Snackbar.make(dialog.findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                uiElement.hideKeyboard(btnDone);

                loginThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        login(editMobile.getText().toString(), editPassword.getText().toString(), TYPE_DIRECT_LOGIN);
                    }
                });
                loginThread.start();
            }
        });
    }

    private void setFields(String phoneNumber, String password) {
        editMobile.setText(phoneNumber);
        editPassword.setText(password);
        editMobile.setSelection(editMobile.length());
        editPassword.setSelection(editPassword.length());
    }

    private void blockButtons() {
        btnDone.setEnabled(false);
    }

    private void openButtons() {
        btnDone.setEnabled(true);
    }

    private boolean emptyFields() {
        return (editMobile.getText().toString().isEmpty() || editPassword.getText().toString().isEmpty());
    }

    private void login(final String mobile, final String password, final int type) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.setMessage(activity.getString(R.string.message_authenticating));
                    progressDialog.show();
                    dialog.setCancelable(false);
                    textMessage.setTextColor(ContextCompat.getColor(activity, R.color.dark_v1));
                    textMessage.setText(R.string.message_authenticating);
                    blockButtons();
                }
            }
        });

        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_MOBILE, mobile);
        bodyParams.put(DatabaseSchema.Users.COLUMN_PASSWORD, password);

        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.AUTH, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "auth = " + response);
                openButtons();
                dialog.setCancelable(true);

                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        locData.storeToken(authResponse.getString(Constants.Json.TOKEN));
                        if (type == TYPE_LOGIN_AR) {
                            successfulRegister(mobile, password, false);
                            return;
                        }

                        JSONObject info = new JSONObject(authResponse.getString(Constants.Json.INFO));
                        String id = info.optString(DatabaseSchema.COLUMN_ID);
                        String mobile = info.optString(DatabaseSchema.Users.COLUMN_MOBILE);
                        String password = info.optString(DatabaseSchema.Users.COLUMN_PASSWORD);
                        String email = info.optString(DatabaseSchema.Users.COLUMN_EMAIL);
                        String firstName = info.optString(DatabaseSchema.Users.COLUMN_FIRST_NAME);
                        String lastName = info.optString(DatabaseSchema.Users.COLUMN_LAST_NAME);

                        sLogin.login(Login.LoginType.PHONE);

                        databaseHelper.addUser(id, mobile, password, firstName, lastName, email);
                        if (databaseHelper.vehicleCount() > 0) {
                            for (Vehicle vehicle : databaseHelper.vehicles()) {
                                databaseHelper.updateVehicle(vehicle.getId(), databaseHelper.user().getId());
                            }
                        }

                        loginThread.interrupt();

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                if (progressDialog != null)
                                    progressDialog.setMessage(activity.getString(R.string.message_getting_data));
                                dialog.setCancelable(false);
                            }
                            @Override
                            protected Void doInBackground(Void... params) {
                                try {
                                    databaseHelper.insertFromFile(R.raw.automint);
                                } catch (IOException e) { Log.e(TAG, "insert from file - IOException Caught"); }
                                return null;
                            }
                            @Override
                            public void onPostExecute(Void result) {
                                super.onPostExecute(result);
                                loginThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getStatusTable();
                                        vehicleFromServer();
                                    }
                                });
                                loginThread.start();
                            }
                        }. execute();
                    } else {
                        String message = authResponse.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_server_password))) {
                            loginThread.interrupt();

                            if (progressDialog != null)
                                progressDialog.dismiss();
                            setFields(mobile, password);
                            textMessage.setTextColor(ContextCompat.getColor(activity, R.color.dark_v1));
                            textMessage.setText(R.string.message_login_fail);
                        } else {
                            register(editMobile.getText().toString(), editPassword.getText().toString());
                        }
                    }
                } catch (JSONException e) { Log.e(TAG, "auth : is not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loginThread.interrupt();

                if (progressDialog != null)
                    progressDialog.dismiss();
                dialog.setCancelable(true);
                openButtons();
                setFields(mobile, password);
                textMessage.setTextColor(ContextCompat.getColor(activity, R.color.volley_error_message));
                textMessage.setText(R.string.message_volley_error_response);
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

    private void register(final String mobile, final String password) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null)
                    progressDialog.setMessage(activity.getString(R.string.message_registering));
                dialog.setCancelable(false);
                blockButtons();
                textMessage.setTextColor(ContextCompat.getColor(activity, R.color.dark_v1));
                textMessage.setText(R.string.message_registering);
            }
        });

        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_MOBILE, mobile);
        bodyParams.put(DatabaseSchema.Users.COLUMN_PASSWORD, password);

        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.USER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setCancelable(true);
                        openButtons();
                    }
                });

                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        login(mobile, password, TYPE_LOGIN_AR);
                    else {
                        loginThread.interrupt();

                        setFields(mobile, password);
                        textMessage.setTextColor(ContextCompat.getColor(activity, R.color.dark_v1));
                        textMessage.setText(R.string.message_signup_fail);
                        if (progressDialog != null)
                            progressDialog.dismiss();
                    }
                } catch (JSONException e) { Log.e(TAG, "registration : is not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loginThread.interrupt();

                if (progressDialog != null)
                    progressDialog.dismiss();
                dialog.setCancelable(true);
                openButtons();
                setFields(mobile, password);
                textMessage.setTextColor(ContextCompat.getColor(activity, R.color.volley_error_message));
                textMessage.setText(R.string.message_volley_error_response);
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

    private void successfulRegister(final String mobile, final String password, final boolean withPassword) {
        loginThread.interrupt();

        if (progressDialog != null)
            progressDialog.dismiss();
        editMobile.setFocusable(false);
        if (withPassword)
            editPassword.setFocusable(false);
        else
            editPassword.setFocusable(true);
        textMessage.setText(R.string.message_welcome);

        layoutName.setVisibility(View.VISIBLE);
        layoutEmail.setVisibility(View.VISIBLE);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editFirstName.getText().toString().isEmpty() || !editLastName.getText().toString().isEmpty() || !editEmail.getText().toString().isEmpty()) {
                    if (!editEmail.getText().toString().isEmpty() && !uiElement.validateEmail(editEmail.getText().toString())) {
                        Snackbar.make(activity.findViewById(android.R.id.content), activity.getString(R.string.message_invalid_email), Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    loginThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            updateUser(mobile, password, editFirstName.getText().toString(), editLastName.getText().toString(), editEmail.getText().toString());
                        }
                    });
                    loginThread.start();
                } else
                    loginThread.run();
            }
        });
    }

    private void updateUser(final String mobile, final String password, String firstName, String lastName, String email) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.setMessage(activity.getString(R.string.message_wait_task_pending));
                    progressDialog.show();
                }
                dialog.setCancelable(false);
            }
        });

        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, firstName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_LAST_NAME, lastName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_EMAIL, email);

        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.USER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setCancelable(true);
                    }
                });
                login(mobile, password, TYPE_DIRECT_LOGIN);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null)
                            progressDialog.dismiss();
                        dialog.setCancelable(true);
                    }
                });
                login(mobile, password, TYPE_DIRECT_LOGIN);
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

    private void vehicleFromServer() {

        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.VEHICLE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                dialog.setCancelable(true);

                try {
                    JSONArray array = new JSONArray(response);
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
                } catch (JSONException e) { Log.e(TAG, "vehicle : is not json"); }

                getWorkshops();
                getRefuels();

                loginThread.interrupt();
                if (progressDialog != null)
                    progressDialog.dismiss();

                activity.startActivity(new Intent(activity, ViewHistory.class));
                activity.finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loginThread.interrupt();
                dialog.setCancelable(true);
                if (progressDialog != null)
                    progressDialog.dismiss();

                activity.startActivity(new Intent(activity, ViewHistory.class));
                activity.finish();
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
                    JSONArray array = new JSONArray(response);
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
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "refuels not in json"); }
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
                    JSONArray array = new JSONArray(response);
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
                    getServices();
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "workshops not in json"); }
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
}
