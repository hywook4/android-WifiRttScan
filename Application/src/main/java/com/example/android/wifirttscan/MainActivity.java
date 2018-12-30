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
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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

import com.example.android.wifirttscan.MyAdapter.ScanResultClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;

import static com.example.android.wifirttscan.AccessPointRangingResultsActivity.SCAN_RESULT_EXTRA;

/**
 * Displays list of Access Points enabled with WifiRTT (to check distance). Requests location
 * permissions if they are not approved via secondary splash screen explaining why they are needed.
 */
public class MainActivity extends AppCompatActivity implements ScanResultClickListener {

    private static final String TAG = "MainActivity";

    private boolean mLocationPermissionApproved = false;
    private boolean mExternalStoragePermissionApproved = false;
    private boolean mInternalStoragePermissionApproved = false;

    List<ScanResult> mAccessPointsSupporting80211mc;
    List<ScanResult> mAccessPoints;

    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;

    private TextView mOutputTextView;
    private RecyclerView mRecyclerView;

    private MyAdapter mAdapter;

    //variables for RTT range requesting and data writing

    ScanResult mScanResult;
    List<ScanResult> mcScanResults;

    private TextView mCsvFileName;
    private TextView mScanDelay;
    private Chronometer mTimer;

    private Boolean scanning = false;

    private String writeData;
    private Map<String, String> buffer;

    private Date date;
    private SimpleDateFormat timeStamp;
    private int mMillisecondDelay = 1000;
    private long mStartScheduleTime;

    final Handler mRangeRequestDelayHandler = new Handler();
    final Handler mRequestDelayer = new Handler();

    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    private CsvManager mCsvManager;
    private CsvManager debugWriter = new CsvManager("debug.csv");
    private int number = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mOutputTextView = findViewById(R.id.access_point_summary_text_view);
        mRecyclerView = findViewById(R.id.recycler_view);

        mScanDelay = findViewById(R.id.scan_delay);
        mCsvFileName = findViewById(R.id.csv_file_name);
        mTimer = findViewById(R.id.main_timer);

        // Improve performance if you know that changes in content do not change the layout size
        // of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mAccessPointsSupporting80211mc = new ArrayList<>();
        mAccessPoints = new ArrayList<>();

        mAdapter = new MyAdapter(mAccessPoints, this, getApplication());
        mRecyclerView.setAdapter(mAdapter);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanReceiver = new WifiScanReceiver();

        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        mRttRangingResultCallback = new RttRangingResultCallback();

        Map<String, String> mBuffer;
        buffer = new HashMap<>();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        mExternalStoragePermissionApproved = ActivityCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        mInternalStoragePermissionApproved = ActivityCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        registerReceiver(
                mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        unregisterReceiver(mWifiScanReceiver);
    }

    private void logToUi(final String message) {
        if (!message.isEmpty()) {
            Log.d(TAG, message);
            mOutputTextView.setText(message);
        }
    }

    @Override
    public void onScanResultItemClick(ScanResult scanResult) {
        Log.d(TAG, "onScanResultItemClick(): ssid: " + scanResult.SSID);

        Intent intent = new Intent(this, AccessPointRangingResultsActivity.class);
        intent.putExtra(SCAN_RESULT_EXTRA, scanResult);
        startActivity(intent);
    }

    public void onClickFindDistancesToAccessPoints(View view) {
        buffer.clear();
        if (mLocationPermissionApproved && mExternalStoragePermissionApproved && mInternalStoragePermissionApproved) {
            logToUi(getString(R.string.retrieving_access_points));
            mWifiManager.startScan();

        } else {
            // On 23+ (M+) devices, fine location permission not granted. Request permission.
            Intent startIntent = new Intent(this, LocationPermissionRequestActivity.class);
            startActivity(startIntent);
        }
    }

    public void onClickRangeSelectedAP(View view) {
        Log.d(TAG, "onClickRangeSelectedAP");


        if(((ToggleButton)view).isChecked()){
            startTimer();
            scanning = true;

            mcScanResults = mAdapter.returnSelectedAPInfo();

            Log.d(TAG, mcScanResults.size() + " : this is size of list");
            debugWriter.Write("Size of the List : " + mcScanResults.size());


            String fileName = mCsvFileName.getText().toString() + ".csv";
            mMillisecondDelay = Integer.parseInt(mScanDelay.getText().toString());
            mCsvManager = new CsvManager(fileName);
            number = 0;

            startRangingRequest();
        }
        else{
            scanning = false;
            stopTimer();
        }
        return;
    }

    public void onClickSaveRttAps(View view) {
        Log.d(TAG, "onClickSaveRttAps");

    }

    public void onClickLoadRttAps(View view) {
        Log.d(TAG, "onClickLoadRttAps");

    }

    public void onClickClearRttAps(View view) {
        Log.d(TAG, "onClickClearRttAps");

    }

    private void delayRequest(){
        mStartScheduleTime += mMillisecondDelay;
        int nextDelay = Math.max((int)(mStartScheduleTime - System.currentTimeMillis()), 0);
        while (nextDelay == 0) {
            mStartScheduleTime += mMillisecondDelay;
            nextDelay = Math.max((int)(mStartScheduleTime - System.currentTimeMillis()), 0);
        }
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

        debugWriter.Write("startRangingRequest: " + mcScanResults.size());
        for(int i = 0 ; i < mcScanResults.size() ; i++){
            mScanResult = mcScanResults.get(i);

            writeData = mScanResult.SSID;
            writeData += ',' + mScanResult.BSSID;
            writeData += ',' + String.valueOf(mScanResult.centerFreq0);
            writeData += ',' + String.valueOf(mScanResult.centerFreq1);
            writeData += ',' + String.valueOf(mScanResult.channelWidth);
            writeData += ',' + String.valueOf(mScanResult.frequency);

            //Add to buffer as a BSSID
            buffer.put(mScanResult.BSSID, writeData);
            debugWriter.Write(mScanResult.BSSID + ": " + writeData);

            date = new Date(Calendar.getInstance().getTimeInMillis());
            timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

            /*
            //if AP supports 80211mc
            if(mScanResult.is80211mcResponder()){
                RangingRequest rangingRequest =
                        new RangingRequest.Builder().addAccessPoint(mScanResult).build();

                mWifiRttManager.startRanging(
                        rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);
            }*/
            /*
            else{
                writeData += ',' + String.valueOf(number);
                writeData += ',' + String.valueOf(mScanResult.level);
                writeData += ',' + timeStamp.format(date);

                number++;

                mCsvManager.Write(writeData);
            }*/
        }

        if(mWifiRttManager != null) {
            RangingRequest rangingRequest =
                    new RangingRequest.Builder().addAccessPoints(mcScanResults).build();

            mWifiRttManager.startRanging(
                    rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);
        }

        if(scanning)
            delayRequest();
    }

    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private class RttRangingResultCallback extends RangingResultCallback {
        @Override
        public void onRangingFailure(int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
            debugWriter.Write("onRangingFailure() code: " + code);
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG, "onRangingResults(): " + list);
            debugWriter.Write("onRangingResults() " + list.size());

            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)

            //if (list.size() == 1) {
            for(int i = 0; i<list.size(); i++){

                RangingResult rangingResult = list.get(i);

                if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                    String key = rangingResult.getMacAddress().toString();
                    debugWriter.Write("Integer.toHexString(rangingResult.hashCode()): " + key);
                    String data = buffer.get(key);
                    if(data != null) {
                        data += ',' + String.valueOf(rangingResult.getStatus());
                        data += ',' + String.valueOf(rangingResult.getDistanceMm());
                        data += ',' + String.valueOf(rangingResult.getDistanceStdDevMm());
                        data += ',' + String.valueOf(rangingResult.getRssi());

                        data += ',' + timeStamp.format(date);

                        data += ',' + String.valueOf(rangingResult.getNumAttemptedMeasurements());
                        data += ',' + String.valueOf(rangingResult.getNumSuccessfulMeasurements());

                        mCsvManager.Write(data);
                    }
                } else if (rangingResult.getStatus()
                        == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                    Log.d(TAG, "RangingResult failed (AP doesn't support IEEE80211 MC.");
                    debugWriter.Write("RangingResult failed (AP doesn't support IEEE80211 MC.");
                } else {
                    Log.d(TAG, "RangingResult failed.");
                    debugWriter.Write("RangingResult failed.");
                }

            }
        }
    }




    public void onClickDeveloperConsole(View view) {
        Log.d(TAG, "onClickDeveloperConsole");

        Intent intent = new Intent(this, DeveloperActivity.class);
        startActivity(intent);
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        private List<ScanResult> find80211mcSupportedAccessPoints(
                @NonNull List<ScanResult> originalList) {
            List<ScanResult> newList = new ArrayList<>();

            for (ScanResult scanResult : originalList) {

                if (scanResult.is80211mcResponder()) {
                    newList.add(scanResult);
                }

                if (newList.size() >= RangingRequest.getMaxPeers()) {
                    break;
                }
            }
            return newList;
        }

        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {

            List<ScanResult> scanResults = mWifiManager.getScanResults();

            if (scanResults != null) {

                if (mLocationPermissionApproved) {
                    List<ScanResult> newList = scanResults;
                    for(ScanResult ap : mAccessPoints) {
                        boolean flag = false;
                        for(ScanResult sr : scanResults) {
                            if(ap.BSSID.equals(sr.BSSID)) {
                                flag = true;
                            }
                        }
                        if(!flag) {
                            newList.add(0, ap);
                            if (newList.size() >= RangingRequest.getMaxPeers()) {
                                break;
                            }
                        }
                    }
                    mAccessPointsSupporting80211mc = find80211mcSupportedAccessPoints(newList);
                    mAccessPoints = newList;

                    mAdapter.swapData(mAccessPoints);

                    logToUi(
                            mAccessPoints.size()
                                    + " APs discovered, "
                                    + mAccessPointsSupporting80211mc.size()
                                    + " RTT capable.");

                } else {
                    // TODO (jewalker): Add Snackbar regarding permissions
                    Log.d(TAG, "Permissions not allowed.");
                }
            }
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
