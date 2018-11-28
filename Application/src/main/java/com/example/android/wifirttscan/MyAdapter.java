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

import android.net.wifi.ScanResult;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;

import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.app.Application;

import java.util.ArrayList;
import java.util.List;
/**
 * Displays the ssid and bssid from a list of {@link ScanResult}s including a header at the top of
 * the {@link RecyclerView} to label the data.
 */
public class MyAdapter extends RecyclerView.Adapter<ViewHolder> {

    private static final String TAG = "MyAdapter";
    private static final int HEADER_POSITION = 0;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private static ScanResultClickListener sScanResultClickListener;

    private List<ScanResult> mWifiAccessPointsWithRtt;
    private SparseBooleanArray mAPSelectedArray;

    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    public int mRssi;
    public int mRtt;

    public Application mApplication;
    public int num;

    public List<ScanResult> returnSelectedAPInfo() {
        ArrayList<ScanResult> results = new ArrayList<>();
        for(int i = 0; i < mWifiAccessPointsWithRtt.size(); i++) {
            if (!mAPSelectedArray.get(i + 1, false)) {
                continue;
            } else {
                results.add(mWifiAccessPointsWithRtt.get(i));
            }
        }
        return results;
    }


    public MyAdapter(List<ScanResult> list, ScanResultClickListener scanResultClickListener, Application app) {
        mWifiAccessPointsWithRtt = list;
        mAPSelectedArray = new SparseBooleanArray(mWifiAccessPointsWithRtt.size());
        sScanResultClickListener = scanResultClickListener;
        mApplication = app;
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {
        public ViewHolderHeader(View view) {
            super(view);
        }
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView mSsidTextView;
        public TextView rssiTextView;
        public TextView rttTextView;
        public CheckBox apSelected;


        public ViewHolderItem(View view) {
            super(view);
            view.setOnClickListener(this);
            Log.d(TAG, "Enter ViewHolderItem");
            mSsidTextView = view.findViewById(R.id.ssid_text_view);
            rssiTextView = view.findViewById(R.id.rssi_text_view);
            rttTextView = view.findViewById(R.id.rtt_text_view);
            apSelected = view.findViewById(R.id.ap_checkbox);
            apSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    int adapterPosition = getAdapterPosition();
                    if(!mAPSelectedArray.get(adapterPosition, false)) {
                        apSelected.setChecked(true);
                        mAPSelectedArray.put(adapterPosition, true);
                    } else {
                        apSelected.setChecked(false);
                        mAPSelectedArray.put(adapterPosition, false);
                    }
                }
            });

            Log.d(TAG, "before wifirttmanager");
            //mWifiRttManager = (WifiRttManager) view.getContext().getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
            //mRttRangingResultCallback = new RttRangingResultCallback();

            Log.d(TAG, "after wifirttmanager");
        }

        @Override
        public void onClick(View view) {
            sScanResultClickListener.onScanResultItemClick(getItem(getAdapterPosition()));
        }

        public void bind(int position) {
            if(!mAPSelectedArray.get(position, false)) {
                apSelected.setChecked(false);
            } else {
                apSelected.setChecked(true);
            }
        }
    }

    public void swapData(List<ScanResult> list) {

        // Always clear with any update, as even an empty list means no WifiRtt devices were found.
        mWifiAccessPointsWithRtt.clear();

        if ((list != null) && (list.size() > 0)) {
            mWifiAccessPointsWithRtt.addAll(list);
        }

        mAPSelectedArray = new SparseBooleanArray(mWifiAccessPointsWithRtt.size());

        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "conCreatViewHolder() : ");
        ViewHolder viewHolder;

        if (viewType == TYPE_HEADER) {
            viewHolder =
                    new ViewHolderHeader(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.recycler_row_header, parent, false));

        } else if (viewType == TYPE_ITEM) {
            viewHolder =
                    new ViewHolderItem(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.recycler_row_item, parent, false));
        } else {
            throw new RuntimeException(viewType + " isn't a valid view type.");
        }


        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        Log.d(TAG, "Enter the onBindViewHolder");
        if (viewHolder instanceof ViewHolderHeader) {
            // No updates need to be made to header view (defaults remain same).

        } else if (viewHolder instanceof ViewHolderItem) {
            ViewHolderItem viewHolderItem = (ViewHolderItem) viewHolder;
            ScanResult currentScanResult = getItem(position);

            viewHolderItem.mSsidTextView.setText(currentScanResult.SSID);
            viewHolderItem.bind(position);

            if(currentScanResult.is80211mcResponder()){

                viewHolderItem.rttTextView.setText("O");
                viewHolderItem.rssiTextView.setText(currentScanResult.level + "");
                //RangingRequest rangingRequest = new RangingRequest.Builder().addAccessPoint(currentScanResult).build();

                //mWifiRttManager.startRanging(rangingRequest, mApplication.getMainExecutor(), mRttRangingResultCallback);

                //viewHolderItem.rttTextView.setText(mRtt + "");
                //viewHolderItem.rssiTextView.setText(mRssi + "");
            }

            else{
                mRssi = currentScanResult.level;
                viewHolderItem.rssiTextView.setText(mRssi + "");
                viewHolderItem.rttTextView.setText("X");
            }

        } else {
            throw new RuntimeException(viewHolder + " isn't a valid view holder.");
        }

    }

    /*
     * Because we added a header item to the list, we need to decrement the position by one to get
     * the proper place in the list.
     */
    private ScanResult getItem(int position) {
        return mWifiAccessPointsWithRtt.get(position - 1);
    }

    // Returns size of list plus the header item (adds extra item).
    @Override
    public int getItemCount() {
        return mWifiAccessPointsWithRtt.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == HEADER_POSITION) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }
    }

    // Used to inform the class containing the RecyclerView that one of the ScanResult items in the
    // list was clicked.
    public interface ScanResultClickListener {
        void onScanResultItemClick(ScanResult scanResult);
    }


    public class RttRangingResultCallback extends RangingResultCallback {

        @Override
        public void onRangingFailure(int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
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

                    //rttTextView.setText(rangingResult.getDistanceMm() + "");

                    //rssiTextView.setText(rangingResult.getRssi() + "");


                } else if (rangingResult.getStatus()
                        == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                    Log.d(TAG, "RangingResult failed (AP doesn't support IEEE80211 MC.");

                } else {
                    Log.d(TAG, "RangingResult failed.");
                }

            }
        }
    }




}
