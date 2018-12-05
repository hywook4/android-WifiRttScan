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
import android.icu.util.Calendar;
import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


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

    private TextView mCsvFileName;
    private TextView mScanDelay;

    private Chronometer mTimer;

    private Boolean scanning;

    private String startData;
    private String writeData;

    private Date date;
    private SimpleDateFormat timeStamp;

    // Non UI variables.
    private ScanResult mScanResult;
    private String mMAC;

    private int number = 0;
    private int mMillisecondDelay = 1000;
    private long mStartScheduleTime;

    // Max sample size to calculate average for
    // 1. Distance to device (getDistanceMm) over time
    // 2. Standard deviation of the measured distance to the device (getDistanceStdDevMm) over time
    // Note: A RangeRequest result already consists of the average of 7 readings from a burst,
    // so the average in (1) is the average of these averages.
    private int mSampleSize;

    private Calendar currentTime;

    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    // Triggers additional RangingRequests with delay (mMillisecondsDelayBeforeNewRangingRequest).
    final Handler mRangeRequestDelayHandler = new Handler();
    final Handler mRequestDelayer = new Handler();

    CsvManager mCsvManager;

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

        mScanDelay = findViewById(R.id.scan_delay);
        mCsvFileName = findViewById(R.id.csv_file_name);
        mTimer = findViewById(R.id.sub_timer);

        //mScanDelay.setText(mMillisecondDelay + "");

        // Retrieve ScanResult from Intent.
        Intent intent = getIntent();
        mScanResult = intent.getParcelableExtra(SCAN_RESULT_EXTRA);

        if (mScanResult == null) {
            finish();
        }

        mMAC = mScanResult.BSSID;

        startData = mScanResult.SSID;
        startData += ',' + mScanResult.BSSID;
        startData += ',' + String.valueOf(mScanResult.centerFreq0);
        startData += ',' + String.valueOf(mScanResult.centerFreq1);
        startData += ',' + String.valueOf(mScanResult.channelWidth);
        startData += ',' + String.valueOf(mScanResult.frequency);


        mSsidTextView.setText(mScanResult.SSID);
        mBssidTextView.setText(mScanResult.BSSID);

        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        mRttRangingResultCallback = new RttRangingResultCallback();

    }

    public void onToggleClicked(View view){
        String fileName = mCsvFileName.getText().toString() + ".csv";
        mMillisecondDelay = Integer.parseInt(mScanDelay.getText().toString());

        Log.d(TAG, "delay time : "+mMillisecondDelay);
        mCsvManager = new CsvManager(fileName);
        if(((ToggleButton)view).isChecked()){
            scanning = true;
            mStartScheduleTime = System.currentTimeMillis();
            startTimer();
            startRangingRequest();
        }
        else{
            scanning = false;
            stopTimer();
        }
        return;
    }

    private void delayRequest(){
        mStartScheduleTime += mMillisecondDelay;
        int nextDelay = Math.max((int)(mStartScheduleTime - System.currentTimeMillis()), 0);
        mRequestDelayer.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if(scanning)
                            startRangingRequest();
                    }
                },
                nextDelay);
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

        //clear previous write data
        writeData = startData;

        date = new Date(Calendar.getInstance().getTimeInMillis());
        timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


        //if AP supports 80211mc
        if(mScanResult.is80211mcResponder()){
            RangingRequest rangingRequest =
                    new RangingRequest.Builder().addAccessPoint(mScanResult).build();

            mWifiRttManager.startRanging(
                    rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);
        }

        else{
            mRangeTextView.setText("" + number);
            mRangeSDTextView.setText("X");
            mRssiTextView.setText(mScanResult.level + "");
            writeData += ',' + String.valueOf(number);
            writeData += ',' + "X";
            writeData += ',' + String.valueOf(mScanResult.level);
            writeData += ',' + timeStamp.format(date);

            number++;

            mCsvManager.Write(writeData);

            if(scanning)
                delayRequest();
        }
    }


    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private class RttRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest() {
            mStartScheduleTime += mMillisecondDelay;
            int nextDelay = Math.max((int)(mStartScheduleTime - System.currentTimeMillis()), 0);
            mRangeRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if(scanning)
                                startRangingRequest();
                        }
                    },
                    nextDelay);
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

                    writeData += ',' + String.valueOf(rangingResult.getStatus());
                    writeData += ',' + String.valueOf(rangingResult.getDistanceMm());
                    writeData += ',' + String.valueOf(rangingResult.getDistanceStdDevMm());
                    writeData += ',' + String.valueOf(rangingResult.getRssi());

                    writeData += ',' + timeStamp.format(date);

                    writeData += ',' + String.valueOf(rangingResult.getNumAttemptedMeasurements());
                    writeData += ',' + String.valueOf(rangingResult.getNumSuccessfulMeasurements());

                    mCsvManager.Write(writeData);

                } else if (rangingResult.getStatus()
                        == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                    Log.d(TAG, "RangingResult failed (AP doesn't support IEEE80211 MC.");

                } else {
                    Log.d(TAG, "RangingResult failed.");
                }

            }
            if(scanning)
                queueNextRangingRequest();
        }
    }

    public void startTimer() {
        mTimer.setBase(SystemClock.elapsedRealtime());
        mTimer.start();
    }

    public void stopTimer() {
        mTimer.setBase(SystemClock.elapsedRealtime());
        mTimer.stop();
    }
}
