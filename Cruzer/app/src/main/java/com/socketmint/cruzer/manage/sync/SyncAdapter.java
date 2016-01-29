package com.socketmint.cruzer.manage.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
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
import com.socketmint.cruzer.dataholder.*;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

// THIS FILE HAS CONTENTS TO BE DELETED AFTER TESTING
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private Context syncContext;

    private RequestQueue requestQueue;
    private HashMap<String, String> headerParams = new HashMap<>();

    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Refuel> refuels = new ArrayList<>();
    private List<Service> services = new ArrayList<>();

    private ContentResolver contentResolver;
    private Account account;
    private Bundle extras;
    private String authority;
    private ContentProviderClient provider;
    private SyncResult syncResult;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        databaseHelper = new DatabaseHelper(context);
        requestQueue = Volley.newRequestQueue(context);
        contentResolver = context.getContentResolver();
        syncContext = context;
        locData.cruzerInstance(context);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        databaseHelper = new DatabaseHelper(context);
        requestQueue = Volley.newRequestQueue(context);
        contentResolver = context.getContentResolver();
        syncContext = context;
        locData.cruzerInstance(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        this.account = account;
        this.extras = extras;
        this.authority = authority;
        this.provider = provider;
        this.syncResult = syncResult;

        headerParams.clear();
        headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());

        try {
            List<String> syncStatusConstraint = new ArrayList<>();
            syncStatusConstraint.add(DatabaseSchema.SYNC_STATUS);
            List<String> idConstraint = new ArrayList<>();
            idConstraint.add(DatabaseSchema.COLUMN_ID);

            try {
                vehicles.clear();
                vehicles = databaseHelper.vehicles(DatabaseHelper.SyncStatus.NEW);
                if (vehicles != null) {
                    for (Vehicle vehicle : vehicles) {
                        String modelId = (vehicle.model != null) ? vehicle.model.getId() : "";
                        uploadVehicle(vehicle.getId(), vehicle.reg, databaseHelper.user().getsId(), modelId, vehicle.name);
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                return;
            }
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

            int size = (databaseHelper.statusList() != null) ? databaseHelper.statusList().size() : 0;
            if (size == 0) {
                getStatusTable();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void getStatusTable() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.STATUS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("sync", "get status = " + response);
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String id = object.getString(DatabaseSchema.Status.COLUMN_ID);
                        String details = object.optString(DatabaseSchema.Status.COLUMN_DETAILS);
                        databaseHelper.addStatus(id, details);
                    }
                } catch (JSONException e) { Log.d("sync", "get status is not in json"); }
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

    /*private void getRefuels() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.REFUEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("refuel get", response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e("refuel", "array"); }
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
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void getProblems(final String serviceId) {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.GET_PROBLEMS(serviceId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("problems get", response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e("problems", "array"); }
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
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void getServices() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.SERVICE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("service get", response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e("service", "array"); }
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
                        Vehicle vehicle = databaseHelper.vehicleBySid(vehicleId);
                        if (vehicle != null) {
                            Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_SID), new String[]{sId});
                            Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshopId});
                            String wId = (workshop != null) ? workshop.getId() : "";
                            if (service == null)
                                databaseHelper.addService(sId, vehicle.getId(), date, wId, cost, odo, details, status);
                            else
                                databaseHelper.updateService(sId, vehicle.getId(), date, wId, cost, odo, details, status);
                            getProblems(sId);
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void getWorkshops() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.WORKSHOP, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("workshop get", response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e("workshops", "array"); }
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
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void getModels() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.MODEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("model get", response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e("model", "array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String sId = object.getString(DatabaseSchema.COLUMN_ID);
                        String manuId = object.getString(DatabaseSchema.Models.COLUMN_MANU_ID);
                        String name = object.optString(DatabaseSchema.Models.COLUMN_NAME);
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
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void getManus() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.MANU, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        String message = object.optString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                            return;
                        }
                    } catch (JSONException e) { Log.e("manu", "array"); }
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
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }*/

    private void deleteService(final String sId, final String id) {
        Log.d("service delete", Constants.Url.SERVICE(sId));
        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.SERVICE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("service delete", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.Services.TABLE_NAME, id);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void deleteRefuel(final String sId, final String id) {
        Log.d("refuel delete", Constants.Url.REFUEL(sId));
        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.REFUEL(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("refuel delete", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.Refuels.TABLE_NAME, id);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void deleteVehicle(final String sId, final String id) {
        Log.d("vehicle delete", Constants.Url.VEHICLE(sId));
        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.VEHICLE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("vehicle delete", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.Vehicles.TABLE_NAME, id);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
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
        Log.d("service update", Constants.Url.SERVICE(sId));
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.SERVICE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Services.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
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
                return headerParams;
            }
        };
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
        Log.d("refuel put", bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.REFUEL(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("refuel put", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Refuels.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
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
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void updateVehicle(final String sId, final String id, final String reg, final String modelId, final String name) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Vehicles.COLUMN_REG, reg);
        bodyParams.put(DatabaseSchema.Vehicles.COLUMN_MODEL_ID, modelId);
        if (!modelId.isEmpty())
            bodyParams.put(DatabaseSchema.Vehicles.COLUMN_NAME, name);
        Log.d("vehicle update", bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.VEHICLE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("vehicle update", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Vehicles.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
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
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void updateUser(final String id, final String password, final String firstName, final String lastName, final String email) {
        Log.d("user update", Constants.Url.USER);
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, firstName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_LAST_NAME, lastName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_PASSWORD, password);
        bodyParams.put(DatabaseSchema.Users.COLUMN_EMAIL, email);
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.USER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("user update", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Users.TABLE_NAME);
                    else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
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
                return headerParams;
            }
        };
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
        Log.d("service post", bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.SERVICE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("service post", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        databaseHelper.syncRecord(id, sId, DatabaseSchema.Services.TABLE_NAME);
                    } else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
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
                return headerParams;
            }
        };
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
        Log.d("refuel post", bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.REFUEL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("refuel post", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        databaseHelper.syncRecord(id, sId, DatabaseSchema.Refuels.TABLE_NAME);
                    } else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
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
                return headerParams;
            }
        };
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
        Log.d("vehicle post", bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.VEHICLE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("vehicle post", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        databaseHelper.syncRecord(id, sId, DatabaseSchema.Vehicles.TABLE_NAME);
                    } else {
                        String message = object.getString(Constants.Json.MESSAGE);
                        if (message.equals(syncContext.getString(R.string.error_auth_fail))) {
                            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                                @Override
                                public boolean apply(Request<?> request) {
                                    return true;
                                }
                            });
                            authenticate();
                        }
                    }
                } catch (JSONException | NullPointerException e) { e.printStackTrace(); }
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
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void authenticate() {
        User user = databaseHelper.user();
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(Constants.VolleyRequest.AUTH_MOBILE_PARAM, user.mobile);
        final String authParam = Jwts.builder().setClaims(claimsMap).setHeaderParam("typ", "JWT").setIssuedAt(Calendar.getInstance().getTime()).setExpiration(new Date(Calendar.getInstance().getTime().getTime() + 10*60000)).signWith(SignatureAlgorithm.HS256, Base64.encodeToString(syncContext.getString(R.string.key_jwt).getBytes(), Base64.DEFAULT)).compact();
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.OAUTH, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("auth", response);
                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        locData.storeToken(authResponse.getString(Constants.Json.TOKEN));
                        onPerformSync(account, extras, authority, provider, syncResult);
                    }
                } catch (JSONException e) { e.printStackTrace(); }
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
