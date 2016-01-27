package com.socketmint.cruzer;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class CruzerApp extends Application {
    private Tracker analyticsTracker;
    public CruzerApp() { }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    synchronized public Tracker getAnalyticsTracker() {
        if (analyticsTracker == null)
            analyticsTracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker);
        GoogleAnalytics.getInstance(this).setAppOptOut(BuildConfig.DEBUG);
        analyticsTracker.enableExceptionReporting(true);
        return analyticsTracker;
    }
}
