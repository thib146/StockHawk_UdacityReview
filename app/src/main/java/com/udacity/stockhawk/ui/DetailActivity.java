package com.udacity.stockhawk.ui;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import com.udacity.stockhawk.R;

import java.util.Locale;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 08/03/2017.
 */

public class DetailActivity extends AppCompatActivity implements DetailFragment.OnStockNamePass {

    // Variable used for the window title
    private String mStockName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        if (mStockName != null) {
            getSupportActionBar().setTitle(mStockName); // Set the window title with the company name
        }

        // Get the device's orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're on a Tablet and we rotate the device from landscape to portrait, close this activity
        if (orientation == ORIENTATION_LANDSCAPE) {
            finish();
        }
    }

    // Receive the company name from the fragment
    @Override
    public void onStockNamePass(String stockName) {
        mStockName = stockName;
    }
}