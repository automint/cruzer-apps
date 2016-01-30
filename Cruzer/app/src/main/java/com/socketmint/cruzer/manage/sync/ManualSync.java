package com.socketmint.cruzer.manage.sync;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Manu;
import com.socketmint.cruzer.dataholder.Model;
import com.socketmint.cruzer.dataholder.Problem;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.history.RefuelFragment;
import com.socketmint.cruzer.history.ServiceFragment;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.ui.UserInterface;

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

public class ManualSync {
    private static ManualSync instance;

    private Activity activity;
    private final String TAG = "ManualSync";

    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Refuel> refuels = new ArrayList<>();
    private List<Service> services = new ArrayList<>();

    private RequestQueue requestQueue;
    private int pendingRequests;
    private HashMap<String, String> headerParams = new HashMap<>();

    private UserInterface userInterface = UserInterface.getInstance();

    private Thread syncThread;

    public static ManualSync getInstance() {
        if (instance == null)
            instance = new ManualSync();
        return instance;
    }

    public void initInstance(Activity activity) {
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        this.activity = activity;
        locData.cruzerInstance(activity);
        userInterface.changeActivity(activity);
    }

    public void syncUser(Activity activity, String id, String password, String firstName, String lastName, String email) {
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        this.activity = activity;
        locData.cruzerInstance(activity);
        userInterface.changeActivity(activity);
        headerParams.clear();
        headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
        updateUser(id, password, firstName, lastName, email);
    }

    private void checkStop() {
        if (pendingRequests <= 0) {
            Log.d("pendingRequests", String.valueOf(pendingRequests));
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try { activity.findViewById(R.id.button_sync).clearAnimation(); } catch (NullPointerException e) { Log.d(TAG, "can't stop animation"); }
                }
            });
        }
    }

    public void performSync() {
        syncThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pendingRequests = 0;
                locData.cruzerInstance(activity);

                headerParams.clear();
                headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            (activity.findViewById(R.id.button_sync)).startAnimation(userInterface.animation(UserInterface.animation.rotate));
                        } catch (NullPointerException e) { Log.d(TAG, "can't start animation"); }
                    }
                });

                try {
                    List<String> syncStatusConstraint = new ArrayList<>();
                    syncStatusConstraint.add(DatabaseSchema.SYNC_STATUS);
                    List<String> idConstraint = new ArrayList<>();
                    idConstraint.add(DatabaseSchema.COLUMN_ID);

                    try {
                        vehicles.clear();
                        vehicles = databaseHelper.vehicles(DatabaseHelper.SyncStatus.NEW);
                        if (vehicles != null) {
                            if (vehicles.isEmpty())
                                for (Vehicle vehicle : vehicles) {
                                    String modelId = (vehicle.model != null) ? vehicle.model.getId() : "";
                                    uploadVehicle(vehicle.getId(), vehicle.reg, databaseHelper.user().getsId(), modelId, vehicle.name);
                                }
                        }
                    } catch (NullPointerException e) { Log.d(TAG, "vehicle = Null Pointer, syncing stopped"); return; }

                    refuels.clear();
                    refuels = databaseHelper.refuels(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.NEW});
                    if (refuels != null) {
                        for (Refuel refuel : refuels) {
                            uploadRefuel(refuel.getId(), databaseHelper.vehicle(refuel.getVehicleId()).getsId(), refuel.date, refuel.rate, refuel.volume, refuel.cost, refuel.odo);
                        }
                    }

                    services.clear();
                    services = databaseHelper.services(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.NEW});
                    if (services != null) {
                        for (Service service : services) {
                            Workshop workshop = databaseHelper.workshop(idConstraint, new String[]{service.getWorkshopId()});
                            String workshopId = (workshop != null) ? workshop.getsId() : "";
                            uploadService(service.getId(), databaseHelper.vehicle(service.getVehicleId()).getsId(), service.date, workshopId, service.cost, service.odo, service.details);
                        }
                    }

                    User user = databaseHelper.user(DatabaseHelper.SyncStatus.UPDATE);
                    if (user != null) {
                        updateUser(user.getId(), user.getPassword(), user.firstName, user.lastName, user.email);
                    }

                    vehicles.clear();
                    vehicles = databaseHelper.vehicles(DatabaseHelper.SyncStatus.UPDATE);
                    if (vehicles != null) {
                        for (Vehicle vehicle : vehicles) {
                            String modelId = (vehicle.model != null) ? vehicle.model.getId() : "";
                            updateVehicle(vehicle.getsId(), vehicle.getId(), vehicle.reg, modelId, vehicle.name);
                        }
                    }

                    refuels.clear();
                    refuels = databaseHelper.refuels(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.UPDATE});
                    if (refuels != null) {
                        for (Refuel refuel : refuels) {
                            updateRefuel(refuel.getsId(), refuel.getId(), refuel.date, refuel.rate, refuel.volume, refuel.cost, refuel.odo);
                        }
                    }

                    services.clear();
                    services = databaseHelper.services(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.UPDATE});
                    if (services != null) {
                        for (Service service : services) {
                            Workshop workshop = databaseHelper.workshop(idConstraint, new String[]{service.getWorkshopId()});
                            String workshopId = (workshop != null) ? workshop.getsId() : "";
                            updateService(service.getsId(), service.getId(), service.date, workshopId, service.cost, service.odo, service.details);
                        }
                    }

                    refuels.clear();
                    refuels = databaseHelper.deletedRefuels();
                    if (refuels != null) {
                        for (Refuel refuel : refuels) {
                            deleteRefuel(refuel.getsId(), refuel.getId());
                        }
                    }

                    services.clear();
                    services = databaseHelper.deletedServices();
                    if (services != null) {
                        for (Service service : services) {
                            deleteService(service.getsId(), service.getId());
                        }
                    }

                    vehicles.clear();
                    vehicles = databaseHelper.vehicles(DatabaseHelper.SyncStatus.DELETE);
                    if (vehicles != null) {
                        for (Vehicle vehicle : vehicles) {
                            deleteVehicle(vehicle.getsId(), vehicle.getId());
                        }
                    }

                    getRefuels();
                    getWorkshops();
                    getManus();
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        syncThread.start();
    }

    private void getRefuels() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.REFUEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "refuel response = " + response);
                pendingRequests--;
                checkStop();
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e(TAG, "refuel array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String sId = object.getString(DatabaseSchema.COLUMN_ID);
                        String vehicleId = object.getString(DatabaseSchema.COLUMN_VEHICLE_ID);
                        String date = object.optString(DatabaseSchema.Services.COLUMN_DATE);
                        String cost = object.optString(DatabaseSchema.Services.COLUMN_COST);
                        String odo = object.optString(DatabaseSchema.Services.COLUMN_ODO);
                        String rate = object.optString(DatabaseSchema.Refuels.COLUMN_RATE);
                        String volume = object.optString(DatabaseSchema.Refuels.COLUMN_VOLUME);
                        Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                        if (vehicle != null) {
                            Refuel refuel = databaseHelper.refuel(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                            if (refuel == null)
                                databaseHelper.addRefuel(sId, vehicle.getId(), date, rate, volume, cost, odo);
                            else
                                databaseHelper.updateRefuel(sId, vehicle.getId(), date, rate, volume, cost, odo);
                        }
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try { RefuelFragment.addData(); } catch (Exception e) { e.printStackTrace(); }
                        }
                    });
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "refuel : not json or not added"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void getServices() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.SERVICE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "service response = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e(TAG, "service array"); }
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
                            Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshopId});
                            String wId = (workshop != null) ? workshop.getId() : "";
                            if (service == null)
                                databaseHelper.addService(sId, vehicle.getId(), date, wId, cost, odo, details, status, userId, roleId);
                            else
                                databaseHelper.updateService(sId, vehicle.getId(), date, wId, cost, odo, details, status, userId, roleId);
                            getProblems(sId);
                        }
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try { ServiceFragment.addData(); } catch (Exception e) { e.printStackTrace(); }
                        }
                    });
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "service : not added or not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void getProblems(final String serviceId) {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.GET_PROBLEMS(serviceId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "problems response = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e(TAG, "problems array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i =0; i < array.length(); i++) {
                        JSONObject problem = array.getJSONObject(i);
                        String sId = problem.getString(DatabaseSchema.COLUMN_ID);
                        String lCost = problem.optString(DatabaseSchema.Problems.COLUMN_LCOST);
                        String pCost = problem.optString(DatabaseSchema.Problems.COLUMN_PCOST);
                        String details = problem.optString(DatabaseSchema.Problems.COLUMN_DETAILS);
                        String qty = problem.optString(DatabaseSchema.Problems.COLUMN_QTY);
                        Problem item = databaseHelper.problem(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                        Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{serviceId});
                        if (service != null) {
                            if (item == null)
                                databaseHelper.addProblem(sId, service.getId(), details, lCost, pCost, qty);
                            else
                                databaseHelper.updateProblem(sId, service.getId(), details, lCost, pCost, qty);
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "problems : not added or not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void getWorkshops() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.WORKSHOP, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "workshop response = " + response);
                databaseHelper.delete(DatabaseSchema.Workshops.TABLE_NAME);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e(TAG, "workshop array"); }
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
                        if (workshop != null) {
                            databaseHelper.updateWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings);
                        } else
                            databaseHelper.addWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings);
                    }
                    getServices();
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "workshop : not added or not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void getModels() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.MODEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "model response = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e(TAG, "model array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
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
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "model : not added or not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void getManus() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.MANU, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e(TAG, "manus array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
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
                    getModels();
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "manus : not added or not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void deleteService(final String sId, final String id) {
        Log.d(TAG, "delete service = " + Constants.Url.SERVICE(sId));

        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.SERVICE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "delete service = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.Services.TABLE_NAME, id);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);

                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "service : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void deleteRefuel(final String sId, final String id) {
        Log.d(TAG, "delete refuel = " + Constants.Url.REFUEL(sId));
        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.REFUEL(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "delete refuel = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.Refuels.TABLE_NAME, id);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "refuel : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void deleteVehicle(final String sId, final String id) {
        Log.d(TAG, "delete vehicle = " + Constants.Url.VEHICLE(sId));
        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.VEHICLE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "delete vehicle = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.Vehicles.TABLE_NAME, id);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "vehicle : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void updateUser(final String id, final String password, final String firstName, final String lastName, final String email) {
        Log.d(TAG, "put user = " + Constants.Url.USER);
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_PASSWORD, password);
        bodyParams.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, firstName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_LAST_NAME, lastName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_EMAIL, email);
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.USER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "put user = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Users.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "user : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void updateService(final String sId, final String id, final String date, final String workshopId, final String cost, final String odo, final String details) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Services.COLUMN_DATE, date);
        if (!workshopId.isEmpty())
            bodyParams.put(DatabaseSchema.Services.COLUMN_WORKSHOP_ID, workshopId);
        bodyParams.put(DatabaseSchema.Services.COLUMN_COST, cost);
        bodyParams.put(DatabaseSchema.Services.COLUMN_ODO, odo);
        bodyParams.put(DatabaseSchema.Services.COLUMN_DETAILS, details);
        Log.d(TAG, "put service = " + Constants.Url.SERVICE(sId));
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.SERVICE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "put service = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Services.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "service :  not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void updateRefuel(final String sId, final String id, final String date, final String rate, final String volume, final String cost, final String odo) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        if (!date.isEmpty())
            bodyParams.put(DatabaseSchema.Refuels.COLUMN_DATE, date);
        if (!rate.isEmpty())
            bodyParams.put(DatabaseSchema.Refuels.COLUMN_RATE, rate);
        if (!volume.isEmpty())
            bodyParams.put(DatabaseSchema.Refuels.COLUMN_VOLUME, volume);
        if (!cost.isEmpty())
            bodyParams.put(DatabaseSchema.Refuels.COLUMN_COST, cost);
        if (!odo.isEmpty())
            bodyParams.put(DatabaseSchema.Refuels.COLUMN_ODO, odo);
        Log.d(TAG, "put refuel = " + bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.REFUEL(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "put refuel = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Refuels.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "refuel : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
            @Override
            public Map<String, String> getHeaders() {
                Log.e("header", headerParams.toString());
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void updateVehicle(final String sId, final String id, final String reg, final String modelId, final String name) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Vehicles.COLUMN_REG, reg);
        bodyParams.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
        if (!modelId.isEmpty())
            bodyParams.put(DatabaseSchema.Vehicles.COLUMN_NAME, name);
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.VEHICLE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "put vehicle = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Vehicles.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "vehicle : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void uploadService(final String id, final String vehicleId, final String date, final String workshopId, final String cost, final String odo, final String details) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Services.COLUMN_DATE, date);
        bodyParams.put(DatabaseSchema.COLUMN_VEHICLE_ID, vehicleId);
        if (!workshopId.isEmpty())
            bodyParams.put(DatabaseSchema.Services.COLUMN_WORKSHOP_ID, workshopId);
        if (!cost.isEmpty())
            bodyParams.put(DatabaseSchema.Services.COLUMN_COST, cost);
        if (!odo.isEmpty())
            bodyParams.put(DatabaseSchema.Services.COLUMN_ODO, odo);
        if (!details.isEmpty())
            bodyParams.put(DatabaseSchema.Services.COLUMN_DETAILS, details);
        Log.d(TAG, "post service = " + bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.SERVICE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "post service = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        databaseHelper.syncRecord(id, sId, DatabaseSchema.Services.TABLE_NAME);
                    } else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "service : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void uploadRefuel(final String id, final String vehicleId, final String date, final String rate, final String volume, final String cost, final String odo) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.COLUMN_VEHICLE_ID, vehicleId);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_DATE, date);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_RATE, rate);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_VOLUME, volume);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_COST, cost);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_ODO, odo);
        Log.d(TAG, "post refuel = " + bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.REFUEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "post refuel = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        databaseHelper.syncRecord(id, sId, DatabaseSchema.Refuels.TABLE_NAME);
                    } else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "refuel : not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void uploadVehicle(final String id, final String reg, final String uId, final String modelId, final String name) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Vehicles.COLUMN_REG, reg);
        bodyParams.put(DatabaseSchema.Vehicles.COLUMN_USER_ID, uId);
        if (!modelId.isEmpty())
            bodyParams.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
        if (!name.isEmpty())
            bodyParams.put(DatabaseSchema.Vehicles.COLUMN_NAME, name);
        Log.d(TAG, "post vehicle = " + bodyParams.toString());
        final StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.VEHICLE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingRequests--;
                checkStop();
                Log.d(TAG, "post vehicle = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        databaseHelper.syncRecord(id, sId, DatabaseSchema.Vehicles.TABLE_NAME);
                    } else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(activity.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { Log.e(TAG, "vehicle :  not json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingRequests--;
                checkStop();
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return bodyParams;
            }
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingRequests++;
        requestQueue.add(request);
    }

    private void authenticate() {
        User user = databaseHelper.user();
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(Constants.VolleyRequest.AUTH_MOBILE_PARAM, user.mobile);
        final String authParam = Jwts.builder().setClaims(claimsMap).setHeaderParam("typ", "JWT").setIssuedAt(Calendar.getInstance().getTime()).setExpiration(new Date(Calendar.getInstance().getTime().getTime() + 10*60000)).signWith(SignatureAlgorithm.HS256, Base64.encodeToString(activity.getString(R.string.key_jwt).getBytes(), Base64.DEFAULT)).compact();
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.OAUTH, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Auth = " + response);
                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        locData.cruzerInstance(activity);
                        locData.storeToken(authResponse.getString(Constants.Json.TOKEN));
                        syncThread.interrupt();
                        syncThread.run();
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
}
