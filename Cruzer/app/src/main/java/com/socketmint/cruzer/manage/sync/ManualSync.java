package com.socketmint.cruzer.manage.sync;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
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
import com.socketmint.cruzer.dataholder.PUC;
import com.socketmint.cruzer.dataholder.insurance.Insurance;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.location.City;
import com.socketmint.cruzer.dataholder.location.Country;
import com.socketmint.cruzer.dataholder.expense.Refuel;
import com.socketmint.cruzer.dataholder.expense.service.Service;
import com.socketmint.cruzer.dataholder.expense.service.Status;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.dataholder.workshop.Workshop;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.interfaces.DSAKey;
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
    private List<PUC> pucs = new ArrayList<>();
    private List<Insurance> insurances = new ArrayList<>();
    private String locality, nation;

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

                    pucs.clear();
                    pucs = databaseHelper.pucList(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.NEW});
                    if(pucs != null){
                        for(PUC puc : pucs){
                            uploadPUC(puc.getId(), puc.getVehicleId(), puc.pucNom, puc.startDate, puc.endDate, puc.fees, puc.details);
                        }
                    }

                    insurances.clear();
                    insurances = databaseHelper.insurances(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.NEW});
                    if(insurances != null){
                        for(Insurance insurance : insurances){
                            uploadInsurance(insurance.getId(), insurance.getVehicleId(), insurance.getCompanyId(), insurance.policyNo, insurance.startDate, insurance.endDate, insurance.premium, insurance.details);
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
                    insurances.clear();
                    insurances = databaseHelper.insurances(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.UPDATE});
                    if(insurances != null){
                        for(Insurance insurance : insurances){
                            updateInsurance(insurance.getId(), insurance.getsId(), insurance.getCompanyId(), insurance.policyNo, insurance.startDate, insurance.endDate, insurance.premium, insurance.details);
                        }
                    }
                    pucs.clear();
                    pucs = databaseHelper.pucList(syncStatusConstraint, new String[]{DatabaseHelper.SyncStatus.UPDATE});
                    if(pucs != null){
                        for(PUC puc : pucs){
                            updatePUC(puc.getId(), puc.getsId(), puc.pucNom, puc.startDate, puc.endDate, puc.fees, puc.details);
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
                    pucs.clear();
                    pucs = databaseHelper.deletedPUC();
                    if(pucs != null){
                        for(PUC puc : pucs){
                            deletePUC(puc.getsId(), puc.getId());
                        }
                    }
                    insurances.clear();
                    insurances = databaseHelper.deletedInsurance();
                    if(insurances != null){
                        for(Insurance insurance : insurances){
                            deleteInsurance(insurance.getsId(), insurance.getId());
                        }
                    }
                    List<Status> statusList = databaseHelper.statusList();
                    if (((statusList != null) ? statusList.size() : 0) == 0)
                        getStatusTable();

                    locality = syncBundle.getString(Constants.Bundle.CITY);
                    nation = syncBundle.getString(Constants.Bundle.COUNTRY);
                    notifyUserCity(false, false);

                    //  when activity starts
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        syncThread.start();
    }

    private void uploadInsurance(final String id, String vehicleId, String companyId, String policyNo, String startDate, String endDate, String premium, String details) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        if(!vehicleId.isEmpty())
            bodyParams.put(DatabaseSchema.PUC.COLUMN_VEHICLE_ID, databaseHelper.vehicle(vehicleId).getsId());
        if (!policyNo.isEmpty())
            bodyParams.put(DatabaseSchema.Insurances.COLUMN_POLICY_NO, policyNo);
        if (!companyId.isEmpty())
            bodyParams.put(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID, companyId);
        if (!startDate.isEmpty())
            bodyParams.put(DatabaseSchema.Insurances.COLUMN_START_DATE, startDate);
        if (!endDate.isEmpty())
            bodyParams.put(DatabaseSchema.Insurances.COLUMN_END_DATE, endDate);
        if (!premium.isEmpty())
            bodyParams.put(DatabaseSchema.Insurances.COLUMN_PREMIUM, premium);
        if (!details.isEmpty())
            bodyParams.put(DatabaseSchema.Insurances.COLUMN_DETAILS, details);
        Log.d("insurance post", bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.INSURANCE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("insurance post", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        Log.e(TAG, "sync Record for PUC" + databaseHelper.syncRecord(id, sId, DatabaseSchema.Insurances.TABLE_NAME));
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

    private void uploadPUC(final String id, String vehicleId, String pucNom, String startDate, String endDate, String fees, String details) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        if(!vehicleId.isEmpty())
            bodyParams.put(DatabaseSchema.PUC.COLUMN_VEHICLE_ID, databaseHelper.vehicle(vehicleId).getsId());
        if (!pucNom.isEmpty())
            bodyParams.put(DatabaseSchema.PUC.COLUMN_PUC_NO, pucNom);
        if (!startDate.isEmpty())
            bodyParams.put(DatabaseSchema.PUC.COLUMN_START_DATE, startDate);
        if (!endDate.isEmpty())
            bodyParams.put(DatabaseSchema.PUC.COLUMN_END_DATE, endDate);
        if (!fees.isEmpty())
            bodyParams.put(DatabaseSchema.PUC.COLUMN_FEES, fees);
        if (!details.isEmpty())
            bodyParams.put(DatabaseSchema.PUC.COLUMN_DETAILS, details);
        Log.d("puc post", bodyParams.toString());
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.PUC, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("puc post", response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        String sId = object.getString(Constants.Json.ID);
                        Log.e(TAG, "sunc record for insurance " + databaseHelper.syncRecord(id, sId, DatabaseSchema.PUC.TABLE_NAME));
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

    private void notifyUserCity(final boolean citySynced, final boolean countrySynced) {
        if (locality == null || nation == null) {
            getCountries(false);
            return;
        }
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
                Intent getCityIntent = new Intent(Constants.IntentFilters.CITY);
                getCityIntent.putExtra(Constants.IntentFilters.FLAG_CITY_STATUS, true);
                LocalBroadcastManager.getInstance(activity).sendBroadcast(getCityIntent);
            } else if (!citySynced)
                getCities(countrySynced, true);
            else {
                Intent getCityIntent = new Intent(Constants.IntentFilters.CITY);
                getCityIntent.putExtra(Constants.IntentFilters.FLAG_CITY_STATUS, false);
                LocalBroadcastManager.getInstance(activity).sendBroadcast(getCityIntent);
            }
        } else if (!countrySynced)
            getCountries(true);
        else {
            Intent getCityIntent = new Intent(Constants.IntentFilters.CITY);
            getCityIntent.putExtra(Constants.IntentFilters.FLAG_CITY_STATUS, false);
            LocalBroadcastManager.getInstance(activity).sendBroadcast(getCityIntent);
        }
    }

    public void getCities(final boolean countrySynced, final boolean notifyUserCityEnabled) {
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
                    if (notifyUserCityEnabled)
                        notifyUserCity(true, countrySynced);
                    else {
                        Intent getCityIntent = new Intent(Constants.IntentFilters.CITY);
                        getCityIntent.putExtra(Constants.IntentFilters.FLAG_CITY_STATUS, true);
                        LocalBroadcastManager.getInstance(activity).sendBroadcast(getCityIntent);
                    }
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

    public void getCountries(final boolean notifyUserCityEnabled) {
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
                    if (notifyUserCityEnabled)
                        notifyUserCity(false, true);
                    else
                        getCities(true, false);
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

    private void deletePUC(final String sId, final String id) {
        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.PUC(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.PUC.TABLE_NAME, id);
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

    private void deleteInsurance(final String sId, final String id) {
        StringRequest request = new StringRequest(Request.Method.DELETE, Constants.Url.INSURANCE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.delete(DatabaseSchema.Insurances.TABLE_NAME, id);
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

    private void updatePUC(final String id, final String sId, String pucNom, String startDate, String endDate, String fees, String details){
        final HashMap<String, String> bodyParams = new HashMap<>();
        bodyParams.put(DatabaseSchema.PUC.COLUMN_PUC_NO, pucNom);
        bodyParams.put(DatabaseSchema.PUC.COLUMN_START_DATE, startDate);
        bodyParams.put(DatabaseSchema.PUC.COLUMN_END_DATE, endDate);
        bodyParams.put(DatabaseSchema.PUC.COLUMN_FEES, fees);
        bodyParams.put(DatabaseSchema.PUC.COLUMN_DETAILS, details);
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.PUC(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "update puc response = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.PUC.TABLE_NAME);
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
        }){
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

    private void updateInsurance(final String id, final String sId, String companyId, String policyNo, String startDate, String endDate, String premium, String details) {
        final HashMap<String, String> bodyParams = new HashMap<>();
        if(!companyId.isEmpty())
            bodyParams.put(DatabaseSchema.Insurances.COLUMN_INSURANCE_COMPANY_ID, companyId);
        bodyParams.put(DatabaseSchema.Insurances.COLUMN_POLICY_NO, policyNo);
        bodyParams.put(DatabaseSchema.Insurances.COLUMN_START_DATE, startDate);
        bodyParams.put(DatabaseSchema.Insurances.COLUMN_END_DATE, endDate);
        bodyParams.put(DatabaseSchema.Insurances.COLUMN_PREMIUM, premium);
        bodyParams.put(DatabaseSchema.Insurances.COLUMN_DETAILS, details);
        Log.d(TAG, "update Insurance = " + bodyParams.toString());
        Log.d(TAG, "update insurance url = " + Constants.Url.INSURANCE(sId));
        StringRequest request = new StringRequest(Request.Method.PUT, Constants.Url.INSURANCE(sId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "update insurance response = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean(Constants.Json.SUCCESS);
                    if (success)
                        databaseHelper.syncRecord(id, DatabaseSchema.Insurances.TABLE_NAME);
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
        }){
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
