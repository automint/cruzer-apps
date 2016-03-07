package com.socketmint.cruzer.crud.create;

/**
 * Fragment for creating an PUC entry for particular vehicle
 * @author ndkcha
 * @since 26
 * @version 26
 */

import android.Manifest;
import android.app.Activity;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.android.gms.analytics.HitBuilders;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Amazon;
import com.socketmint.cruzer.manage.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PUC extends Fragment implements View.OnClickListener {
    private static final String TAG = "CreatePUC";

    private AppCompatSpinner spinnerVehicle;

    private DatabaseHelper databaseHelper;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<String> vehicleList = new ArrayList<>();

    private String vehicleId, uploadVehicleId;
    private String fileName;
    private File imageFile;

    private Amazon amazon = Amazon.getInstance();

    public static PUC newInstance(String vehicleId) {
        PUC fragment = new PUC();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.VEHICLE_ID, vehicleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_puc, container, false);

        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        amazon.initInstance(getActivity());

        vehicleId = getArguments().getString(Constants.Bundle.VEHICLE_ID, "");
        if (vehicleId.isEmpty()) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_init_fail, Snackbar.LENGTH_SHORT).show();
            getActivity().finish();
            return null;
        }

        adjustVehicleId();
        vehicles = databaseHelper.vehicles();

        initializeViews(v);
        setDefaultValues();
        return v;
    }

    private void adjustVehicleId() {
        com.socketmint.cruzer.dataholder.vehicle.Vehicle vehicle = databaseHelper.vehicle(vehicleId);
        vehicle = (vehicle != null) ? vehicle : databaseHelper.firstVehicle();
        vehicleId = vehicle.getId();
    }

    private void initializeViews(View v) {
        spinnerVehicle = (AppCompatSpinner) v.findViewById(R.id.spinner_vehicle);

        v.findViewById(R.id.button_puc_photo).setOnClickListener(this);
    }

    private void setDefaultValues() {
        vehicleList.clear();
        String original = "";
        for (Vehicle item : vehicles) {
            String string;
            if (item.name == null || item.name.isEmpty()) {
                if (item.model != null)
                    string = item.model.name + ", " + item.model.manu.name;
                else
                    string = item.reg;
            } else
                string = item.name;
            if (item.getId().equals(vehicleId))
                original = string;
            vehicleList.add(string);
        }
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner_item, R.id.text_spinner_item, vehicleList);
        spinnerVehicle.setAdapter(vehicleAdapter);
        spinnerVehicle.setSelection(vehicleList.indexOf(original));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_puc_photo:
                int vehicleIndex = vehicleList.indexOf(spinnerVehicle.getSelectedItem().toString());
                uploadVehicleId = vehicles.get(vehicleIndex).getsId();
                Log.d(TAG, "vehicleIndex = " + vehicleIndex + " | uploadVehicleId = " + uploadVehicleId);
                uploadChooser();
                break;
        }
    }

    private void uploadChooser() {
        final CharSequence[] items = {"Take new photo", "Choose from existing"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (items[which].equals("Take new photo")) {
                    if ((getActivity().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED)
                            && (getActivity().checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, android.os.Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.RequestCodes.PERMISSION_CAMERA);
                            return;
                        }
                        Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_storage_access_fail), Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    cameraPhoto();
                } else if (items[which].equals("Choose from existing")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select File"), Constants.RequestCodes.GALLERY_REQUEST_PHOTO);
                } else
                    dialog.dismiss();
            }
        });
        builder.show();
    }

    private void cameraPhoto() {
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/" + getString(R.string.directory_cruzer));
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                Log.d(TAG, "folder.mkdir - " + folder.mkdir());
                Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_storage_access_fail), Snackbar.LENGTH_SHORT).show();
                return;
            }
        }
        fileName = uploadVehicleId + "-puc-" + System.currentTimeMillis();
        try {
            imageFile = File.createTempFile(fileName, ".jpg", folder);
        } catch (IOException e) {
            e.printStackTrace();
            Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_storage_access_fail), Snackbar.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile)), Constants.RequestCodes.CAMERA_REQUEST_PHOTO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Constants.RequestCodes.CAMERA_REQUEST_PHOTO:
                    amazon.uploadPUC(fileName, imageFile);
                    Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_upload_confirm), Snackbar.LENGTH_SHORT).show();
                    break;
                case Constants.RequestCodes.GALLERY_REQUEST_PHOTO:
                    Uri selectedImageUri = data.getData();
                    String[] projection = {MediaStore.MediaColumns.DATA};
                    CursorLoader cursorLoader = new CursorLoader(getActivity(), selectedImageUri, projection, null, null, null);
                    Cursor cursor = cursorLoader.loadInBackground();
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    cursor.moveToFirst();
                    String selectedImagePath = cursor.getString(columnIndex);
                    File file = new File(selectedImagePath);
                    String fileName = file.getName();
                    if (!fileName.startsWith(uploadVehicleId + "-puc-"))
                        fileName = uploadVehicleId + "-puc-" + System.currentTimeMillis() + ".jpg";
                    amazon.uploadPUC(fileName, file);
                    Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_upload_confirm), Snackbar.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.RequestCodes.PERMISSION_CAMERA:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                        cameraPhoto();
                }
                break;
        }
    }
}
