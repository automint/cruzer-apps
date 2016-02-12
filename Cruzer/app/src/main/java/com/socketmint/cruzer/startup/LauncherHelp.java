package com.socketmint.cruzer.startup;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;

import com.socketmint.cruzer.R;

public class LauncherHelp extends AppCompatActivity {
    private static final String TAG = "LauncherHelp";

    private AppCompatImageView imageHelp1, imageHelp2, imageHelp3, imageHelp4, imageHelp5;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_help);

        imageHelp1 = (AppCompatImageView) findViewById(R.id.image_help_item_1);
        imageHelp2 = (AppCompatImageView) findViewById(R.id.image_help_item_2);
        imageHelp3 = (AppCompatImageView) findViewById(R.id.image_help_item_3);
        imageHelp4 = (AppCompatImageView) findViewById(R.id.image_help_item_4);
        imageHelp5 = (AppCompatImageView) findViewById(R.id.image_help_item_5);

        ViewPager pager = (ViewPager) findViewById(R.id.pager_help);
        Adapter adapter = new Adapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(pageChangeListener);
    }

    private void setIndicators(int position) {
        Log.d(TAG, "indicator position = " + position);
        switch (position) {
            case 0:
                imageHelp1.setImageResource(R.drawable.ic_indicator_selected);
                imageHelp2.setImageResource(R.drawable.ic_indicator);
                imageHelp3.setImageResource(R.drawable.ic_indicator);
                imageHelp4.setImageResource(R.drawable.ic_indicator);
                imageHelp5.setImageResource(R.drawable.ic_indicator);
                break;
            case 1:
                imageHelp1.setImageResource(R.drawable.ic_indicator);
                imageHelp2.setImageResource(R.drawable.ic_indicator_selected);
                imageHelp3.setImageResource(R.drawable.ic_indicator);
                imageHelp4.setImageResource(R.drawable.ic_indicator);
                imageHelp5.setImageResource(R.drawable.ic_indicator);
                break;
            case 2:
                imageHelp1.setImageResource(R.drawable.ic_indicator);
                imageHelp2.setImageResource(R.drawable.ic_indicator);
                imageHelp3.setImageResource(R.drawable.ic_indicator_selected);
                imageHelp4.setImageResource(R.drawable.ic_indicator);
                imageHelp5.setImageResource(R.drawable.ic_indicator);
                break;
            case 3:
                imageHelp1.setImageResource(R.drawable.ic_indicator);
                imageHelp2.setImageResource(R.drawable.ic_indicator);
                imageHelp3.setImageResource(R.drawable.ic_indicator);
                imageHelp4.setImageResource(R.drawable.ic_indicator_selected);
                imageHelp5.setImageResource(R.drawable.ic_indicator);
                break;
            case 4:
                imageHelp1.setImageResource(R.drawable.ic_indicator);
                imageHelp2.setImageResource(R.drawable.ic_indicator);
                imageHelp3.setImageResource(R.drawable.ic_indicator);
                imageHelp4.setImageResource(R.drawable.ic_indicator);
                imageHelp5.setImageResource(R.drawable.ic_indicator_selected);
                break;
        }
    }

    private class Adapter extends FragmentPagerAdapter {
        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return HelpFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 5;
        }
    }

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            Log.d(TAG, "position = " + position);
            setIndicators(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private boolean exit = false;

    @Override
    public void onBackPressed() {
        if (exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            Snackbar.make(LauncherHelp.this.findViewById(android.R.id.content), getString(R.string.message_back_exit), Snackbar.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);

        }
    }
}
