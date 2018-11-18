/*
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wifirttscan;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;


/**
 * Displays ranging information about a particular access point chosen by the user. Uses {@link
 * Handler} to trigger new requests based on
 */
public class AccessPointRangingResultsActivity extends AppCompatActivity {
    private static final String TAG = "APRRActivity";

    public static final String SCAN_RESULT_EXTRA =
            "com.example.android.wifirttscan.extra.SCAN_RESULT";

    private static final int SAMPLE_SIZE_DEFAULT = 50;
    private static final int MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT = 1000;

    // UI Elements.
    private TextView mSsidTextView;
    private TextView mBssidTextView;

    private TextView mRangeTextView;
    private TextView mRangeSDTextView;
    private TextView mRssiTextView;

    private ToggleButton scanToggle;
    private Boolean scanning;

    private String writeData;
    private String data;

    // Non UI variables.
    private ScanResult mScanResult;
    private String mMAC;

    private int mMillisecondsDelayBeforeNewRangingRequest;

    // Max sample size to calculate average for
    // 1. Distance to device (getDistanceMm) over time
    // 2. Standard deviation of the measured distance to the device (getDistanceStdDevMm) over time
    // Note: A RangeRequest result already consists of the average of 7 readings from a burst,
    // so the average in (1) is the average of these averages.
    private int mSampleSize;


    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    // Triggers additional RangingRequests with delay (mMillisecondsDelayBeforeNewRangingRequest).
    final Handler mRangeRequestDelayHandler = new Handler();

    CsvManager mcsvmanager = new CsvManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_point_ranging_results);

        // Initializes UI elements.
        mSsidTextView = findViewById(R.id.ssid);
        mBssidTextView = findViewById(R.id.bssid);

        mRangeTextView = findViewById(R.id.range_value);
        mRangeSDTextView = findViewById(R.id.range_sd_value);
        mRssiTextView = findViewById(R.id.rssi_value);

        scanToggle = findViewById(R.id.toggle_button);

        // Retrieve ScanResult from Intent.
        Intent intent = getIntent();
        mScanResult = intent.getParcelableExtra(SCAN_RESULT_EXTRA);

        if (mScanResult == null) {
            finish();
        }

        mMAC = mScanResult.BSSID;

        mSsidTextView.setText(mScanResult.SSID);
        mBssidTextView.setText(mScanResult.BSSID);


        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);


        mRttRangingResultCallback = new RttRangingResultCallback();


        scanning = scanToggle.isChecked();


        startRangingRequest();

    }

    public void onToggleClicked(View view){
        if(((ToggleButton)view).isChecked()){
            scanning = true;
        }
        else{
            scanning = false;
        }

        return;
    }


    private void startRangingRequest() {
        // Permission for fine location should already be granted via MainActivity (you can't get
        // to this class unless you already have permission. If they get to this class, then disable
        // fine location permission, we kick them back to main activity.
        Log.d(TAG, "Enter startRangingRequest");
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finish();
        }

        if(ActivityCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            finish();
        }

        //if AP supports 80211mc
        writeData = mScanResult.SSID;
        writeData += ',' + mScanResult.BSSID;
        writeData += ',' + mScanResult.centerFreq0;
        writeData += ',' + mScanResult.centerFreq1;
        writeData += ',' + mScanResult.channelWidth;
        writeData += ',' + mScanResult.frequency;


        if(mScanResult.is80211mcResponder()){
            RangingRequest rangingRequest =
                    new RangingRequest.Builder().addAccessPoint(mScanResult).build();


            mWifiRttManager.startRanging(
                    rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);
        }

        else{

            Log.d(TAG, "TEsting string " + writeData);

            mRangeTextView.setText(writeData);

            if(scanning){
                mRangeSDTextView.setText("scanning");
            }
            else{
                mRangeSDTextView.setText("X");
            }


            mRssiTextView.setText(mScanResult.level + "");


            writeData += ',' + mScanResult.level;



            mcsvmanager.Write();

        }
    }


    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private class RttRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest() {
            mRangeRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            startRangingRequest();
                        }
                    },
                    mMillisecondsDelayBeforeNewRangingRequest);
        }

        @Override
        public void onRangingFailure(int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG, "onRangingResults(): " + list);

            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)
            if (list.size() == 1) {

                RangingResult rangingResult = list.get(0);


                if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {

                    mRangeTextView.setText((rangingResult.getDistanceMm() / 1000f) + "");

                    mRangeSDTextView.setText(
                            (rangingResult.getDistanceStdDevMm() / 1000f) + "");

                    mRssiTextView.setText(rangingResult.getRssi() + "");

                    // FIXME: CsvManager is changed
                    // mcsvmanager.Write(rangingResult);

                } else if (rangingResult.getStatus()
                        == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                    Log.d(TAG, "RangingResult failed (AP doesn't support IEEE80211 MC.");

                } else {
                    Log.d(TAG, "RangingResult failed.");
                }


            }
            //queueNextRangingRequest();
        }
    }
}
