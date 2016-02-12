package com.socketmint.cruzer.manage.sync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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
import com.socketmint.cruzer.dataholder.City;
import com.socketmint.cruzer.dataholder.Country;
import com.socketmint.cruzer.dataholder.Offering;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.Status;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.VehicleSubType;
import com.socketmint.cruzer.dataholder.VehicleType;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.dataholder.WorkshopType;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

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
    private Activity activity;
    private final String TAG = "ManualSync";

    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();

    private RequestQueue requestQueue;
    private HashMap<String, String> headerParams = new HashMap<>();

    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Refuel> refuels = new ArrayList<>();
    private List<Service> services = new ArrayList<>();
    private String locality, nation;
    private int pendingOfferings, pendingVehicleSubTypes, pendingVehicleTypes, pendingWorkshopTypes;
    private boolean workshopDependSuccess = true;

    private Thread syncThread;

    public ManualSync(Activity activity) {
        this.activity = activity;
        databaseHelper = new DatabaseHelper(activity.getApplicationContext());
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        locData.cruzerInstance(activity);
    }

    public void syncUser() {
        headerParams.clear();
        headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());

        syncThread = new Thread(new Runnable() {
            @Override
            public void run() {
                User user = databaseHelper.user(DatabaseHelper.SyncStatus.UPDATE);
                updateUser(user.getId(), user.getPassword(), user.firstName, user.lastName, user.email, user.getCityId());
            }
        });
        syncThread.start();
    }

    public void syncEverything(final Bundle syncBundle) {
        syncThread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                        updateUser(user.getId(), user.getPassword(), user.firstName, user.lastName, user.email, user.getCityId());
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

                    List<Status> statusList = databaseHelper.statusList();
                    if (((statusList != null) ? statusList.size() : 0) == 0)
                        getStatusTable();

                    locality = syncBundle.getString(Constants.Bundle.CITY);
                    nation = syncBundle.getString(Constants.Bundle.COUNTRY);
                    notifyUserCity(false, false);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        syncThread.start();
    }

    /*private void getVehicleTypes() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.VEHICLE_TYPES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "vehicle types = " + response);
                pendingVehicleTypes--;
                checkWorkshopDataUpdate();
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
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
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "vehicle types is array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String id = object.getString(DatabaseSchema.VehicleTypes.COLUMN_ID);
                        String type = object.optString(DatabaseSchema.VehicleTypes.COLUMN_TYPE);
                        VehicleType vehicleType = databaseHelper.vehicleType(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
                        if (vehicleType == null)
                            databaseHelper.addVehicleType(id, type);
                        else if (!vehicleType.type.equals(type))
                            databaseHelper.updateVehicleType(id, type);
                    }
                } catch (JSONException e) { Log.e(TAG, "vehicle types not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                pendingVehicleTypes--;
                workshopDependSuccess = false;
                checkWorkshopDataUpdate();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingVehicleTypes++;
        requestQueue.add(request);
    }

    private void getVehicleSubTypes() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.VEHICLE_SUB_TYPES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "vehicle sub types = " + response);
                pendingVehicleSubTypes--;
                checkWorkshopDataUpdate();
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
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
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "cities is array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String id = object.getString(DatabaseSchema.VehicleSubTypes.COLUMN_ID);
                        String subTypes = object.optString(DatabaseSchema.VehicleSubTypes.COLUMN_SUB_TYPE);
                        String vehicleTypeId = object.optString(DatabaseSchema.VehicleSubTypes.COLUMN_TYPE_ID);
                        VehicleSubType vehicleSubType = databaseHelper.vehicleSubType(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
                        if (vehicleSubType == null)
                            databaseHelper.addVehicleSubTypes(id, subTypes, vehicleTypeId);
                        else if (!vehicleSubType.subType.equals(subTypes) || !vehicleSubType.getVehicleTypeId().equals(vehicleTypeId))
                            databaseHelper.updateVehicleSubTypes(id, subTypes, vehicleTypeId);
                    }
                } catch (JSONException e) { Log.e(TAG, "vehicle sub types not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingVehicleSubTypes--;
                workshopDependSuccess = false;
                checkWorkshopDataUpdate();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingVehicleSubTypes++;
        requestQueue.add(request);
    }

    private void getOfferings() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.OFFERINGS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                pendingOfferings--;
                checkWorkshopDataUpdate();
                Log.d(TAG, "offerings = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
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
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "cities is array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String id = object.getString(DatabaseSchema.Offerings.COLUMN_ID);
                        String offering = object.optString(DatabaseSchema.Offerings.COLUMN_OFFERING);
                        Offering item = databaseHelper.offering(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
                        if (item == null)
                            databaseHelper.addOffering(id, offering);
                        else if (!item.offering.equals(offering))
                            databaseHelper.updateOffering(id, offering);
                    }
                } catch (JSONException e) { Log.e(TAG, "offerings not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pendingOfferings--;
                workshopDependSuccess = false;
                checkWorkshopDataUpdate();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingOfferings++;
        requestQueue.add(request);
    }

    private void getWorkshopTypes() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.WORKSHOP_TYPES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "workshop types = " + response);
                pendingWorkshopTypes--;
                checkWorkshopDataUpdate();
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
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
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "workshop type is array"); }
                    JSONArray array = new JSONArray(response);
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
                } catch (JSONException e) { Log.e(TAG, "workshop type is not in json"); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                pendingWorkshopTypes--;
                workshopDependSuccess = false;
                checkWorkshopDataUpdate();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };
        pendingWorkshopTypes++;
        requestQueue.add(request);
    }*/

    private void notifyUserCity(final boolean citySynced, final boolean countrySynced) {
        if (locality == null || nation == null)
            return;
        final Country country = databaseHelper.country(Collections.singletonList(DatabaseSchema.Countries.COLUMN_COUNTRY), new String[]{nation});
        Log.d(TAG, "country != null : " + (country != null) + " | citySynced : " + citySynced + " | countrySynced : " + countrySynced);
        if (country != null) {
            final City city = databaseHelper.city(Arrays.asList(DatabaseSchema.Cities.COLUMN_CITY, DatabaseSchema.Cities.COLUMN_COUNTRY_ID), new String[]{locality, country.getId()});
            Log.d(TAG, "city != null : " + (city != null) + " | citySynced : " + citySynced + " | countrySynced : " + countrySynced);
            if (city != null) {
                User user = databaseHelper.user();
                Log.d(TAG, "user.getChildId == null : " + (user.getCityId() == null) + " | !user.getCityId().equals(city.getId()) : " + ((user.getCityId() !=  null) ? !user.getCityId().equals(city.getId()) : "null"));
                if (user.getCityId() == null || !user.getCityId().equals(city.getId()))
                    databaseHelper.updateUserCity(databaseHelper.user().getId(), city.getId());
            } else if (!citySynced)
                getCities(countrySynced);
            else
                uploadCity(locality, country.getId(), countrySynced);
        } else if (!countrySynced)
            getCountries();
        else
            uploadCountry(nation);
    }

    public void getCities(final boolean countrySynced) {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.CITIES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "fetch cities = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
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
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "cities is array"); }
                    JSONArray array = new JSONArray(response);
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
                    notifyUserCity(true, countrySynced);
                } catch (JSONException e) { Log.d(TAG, "cities not in json"); }
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

    public void getCountries() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.COUNTRIES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "fetch countries = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
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
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "countries is array"); }
                    JSONArray array = new JSONArray(response);
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
                    notifyUserCity(false, true);
                } catch (JSONException e) { Log.d(TAG, "countries not in json"); }
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

    public void uploadCity(final String city, final String countryId, final boolean countrySynced) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Cities.COLUMN_CITY, city);
        bodyParams.put(DatabaseSchema.Cities.COLUMN_COUNTRY_ID, countryId);
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.CITIES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "upload city = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String id = object.getString(Constants.Json.ID);
                        databaseHelper.addCity(id, city, countryId);
                        notifyUserCity(true, countrySynced);
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
                } catch (JSONException e) { Log.d(TAG, "upload city not in json"); }
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
            @Override
            public Map<String, String> getParams() {
                return bodyParams;
            }
        };
        requestQueue.add(request);
    }

    public void uploadCountry(final String country) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Countries.COLUMN_COUNTRY, country);
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.COUNTRIES, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "upload country = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String id = object.getString(Constants.Json.ID);
                        databaseHelper.addCountry(id, country);
                        notifyUserCity(false, true);
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
                } catch (JSONException e) { Log.d(TAG, "upload city not in json"); }
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
            @Override
            public Map<String, String> getParams() {
                return bodyParams;
            }
        };
        requestQueue.add(request);
    }

    private void getStatusTable() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.STATUS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("sync", "get status = " + response);
                try {
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean success = object.getBoolean(Constants.Json.SUCCESS);
                        if (!success) {
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
                    } catch (JSONException | NullPointerException e) { Log.e(TAG, "status is array"); }
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String id = object.getString(DatabaseSchema.ServiceStatus.COLUMN_ID);
                        String details = object.optString(DatabaseSchema.ServiceStatus.COLUMN_DETAILS);
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
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void updateUser(final String id, final String password, final String firstName, final String lastName, final String email, final String cityId) {
        Log.d(TAG, "put user = " + Constants.Url.USER);
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.Users.COLUMN_PASSWORD, password);
        bodyParams.put(DatabaseSchema.Users.COLUMN_FIRST_NAME, firstName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_LAST_NAME, lastName);
        bodyParams.put(DatabaseSchema.Users.COLUMN_EMAIL, email);
        bodyParams.put(DatabaseSchema.Users.COLUMN_CITY_ID, cityId);
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.USER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
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
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_DATE, date);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_RATE, rate);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_VOLUME, volume);
        bodyParams.put(DatabaseSchema.Refuels.COLUMN_COST, cost);
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
