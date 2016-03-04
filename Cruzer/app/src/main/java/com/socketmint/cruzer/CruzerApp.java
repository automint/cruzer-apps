package com.socketmint.cruzer;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Application class to define certain parameters at global level
 * @see Application to get more details about Application classes
 */

public class CruzerApp extends Application {
    //  Tracker for Google Analytics that will be passed through entire application
    private Tracker analyticsTracker;

    //  Empty default constructor
    public CruzerApp() { }

    /**
     * Installs Multidex support if needed for current Android VM
     * @param base context of current application
     */
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * Initializes Google Analysis Tracker using singleton method
     * Tracker is opted out if current configuration is debug
     * Method is synchronized in order to prevent race conditions while accessing from different activity classes
     * @return analyticsTracker initialized for entire application
     */

    synchronized public Tracker getAnalyticsTracker() {
        if (analyticsTracker == null)
            analyticsTracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker);
        GoogleAnalytics.getInstance(this).setAppOptOut(BuildConfig.DEBUG);
        analyticsTracker.enableExceptionReporting(true);
        return analyticsTracker;
    }
}
