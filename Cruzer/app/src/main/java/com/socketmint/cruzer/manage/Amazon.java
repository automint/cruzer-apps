package com.socketmint.cruzer.manage;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.socketmint.cruzer.R;

import java.io.File;

public class Amazon {
    private static final String USER_BUCKET = "cruzer-receipts";

    private static Amazon instance;
    private Activity activity;
    private TransferUtility transferUtility;

    public static Amazon getInstance() {
        if (instance == null)
            instance = new Amazon();
        return instance;
    }

    public void initInstance(Activity activity) {
        this.activity = activity;
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(activity.getApplicationContext(), activity.getString(R.string.id_amazon_identity_pool), Regions.AP_NORTHEAST_1);
        AmazonS3 amazonS3 = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(amazonS3, activity.getApplicationContext());
    }

    public void uploadPUC(final String name, final File file) {
        final ProgressDialog progressDialog = new ProgressDialog(activity);

        new Thread(new Runnable() {
            @Override
            public void run() {
                TransferObserver observer = transferUtility.upload(USER_BUCKET, name, file);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setCancelable(true);
                        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Snackbar.make(activity.findViewById(android.R.id.content), activity.getString(R.string.message_perform_background), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                        progressDialog.setMessage(activity.getString(R.string.message_upload_ongoing));
                    }
                });

                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {

                    }

                    @Override
                    public void onProgressChanged(int id, final long bytesCurrent, final long bytesTotal) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                                progressDialog.setMessage(activity.getString(R.string.message_upload_ongoing) + percentage);
                                if (percentage == 100)
                                    progressDialog.dismiss();
                            }
                        });
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }).start();
    }

    public void uploadInsurance(final String name, final File file) {
        final ProgressDialog progressDialog = new ProgressDialog(activity);

        new Thread(new Runnable() {
            @Override
            public void run() {
                TransferObserver observer = transferUtility.upload(USER_BUCKET, name, file);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setCancelable(true);
                        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Snackbar.make(activity.findViewById(android.R.id.content), activity.getString(R.string.message_perform_background), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                        progressDialog.setMessage(activity.getString(R.string.message_upload_ongoing));
                    }
                });

                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {

                    }

                    @Override
                    public void onProgressChanged(int id, final long bytesCurrent, final long bytesTotal) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                                progressDialog.setMessage(activity.getString(R.string.message_upload_ongoing) + percentage);
                                if (percentage == 100)
                                    progressDialog.dismiss();
                            }
                        });
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }).start();
    }
}
