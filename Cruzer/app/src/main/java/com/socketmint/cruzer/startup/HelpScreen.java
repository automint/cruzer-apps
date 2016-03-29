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
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.manage.Constants;

public class HelpScreen extends AppCompatActivity {
//    private static final String TAG = "HelpScreen";

    private AppCompatImageView imageHelp1, imageHelp2, imageHelp3, imageHelp4, imageHelp5;
    private String mobile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_help);

        imageHelp1 = (AppCompatImageView) findViewById(R.id.image_help_item_1);
        imageHelp2 = (AppCompatImageView) findViewById(R.id.image_help_item_2);
        imageHelp3 = (AppCompatImageView) findViewById(R.id.image_help_item_3);
        imageHelp4 = (AppCompatImageView) findViewById(R.id.image_help_item_4);
        imageHelp5 = (AppCompatImageView) findViewById(R.id.image_help_item_5);
        imageHelp5.setVisibility(View.GONE);

        mobile = getIntent().getStringExtra(Constants.Bundle.MOBILE);
        mobile = (mobile == null) ? "" : mobile;

        ViewPager pager = (ViewPager) findViewById(R.id.pager_help);
        Adapter adapter = new Adapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(pageChangeListener);
    }

    private void setIndicators(int position) {
        switch (position) {
            case 0:
                imageHelp1.setImageResource(R.drawable.ic_indicator_selected);
                imageHelp2.setImageResource(R.drawable.ic_indicator);
                imageHelp3.setImageResource(R.drawable.ic_indicator);
                imageHelp4.setImageResource(R.drawable.ic_indicator);
//                imageHelp5.setImageResource(R.drawable.ic_indicator);
                break;
            case 1:
                imageHelp1.setImageResource(R.drawable.ic_indicator);
                imageHelp2.setImageResource(R.drawable.ic_indicator_selected);
                imageHelp3.setImageResource(R.drawable.ic_indicator);
                imageHelp4.setImageResource(R.drawable.ic_indicator);
//                imageHelp5.setImageResource(R.drawable.ic_indicator);
                break;
            case 2:
                imageHelp1.setImageResource(R.drawable.ic_indicator);
                imageHelp2.setImageResource(R.drawable.ic_indicator);
                imageHelp3.setImageResource(R.drawable.ic_indicator_selected);
                imageHelp4.setImageResource(R.drawable.ic_indicator);
//                imageHelp5.setImageResource(R.drawable.ic_indicator);
                break;
            case 3:
                imageHelp1.setImageResource(R.drawable.ic_indicator);
                imageHelp2.setImageResource(R.drawable.ic_indicator);
                imageHelp3.setImageResource(R.drawable.ic_indicator);
                imageHelp4.setImageResource(R.drawable.ic_indicator_selected);
//                imageHelp5.setImageResource(R.drawable.ic_indicator);
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
            return HelpFragment.newInstance(position, mobile);
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            setIndicators(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    public void onDestroy() {
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
        super.onDestroy();
    }

    private boolean exit = false;

    @Override
    public void onBackPressed() {
        if (exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            Snackbar.make(HelpScreen.this.findViewById(android.R.id.content), getString(R.string.message_back_exit), Snackbar.LENGTH_SHORT).show();
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
