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

import android.content.Context;
import android.net.wifi.ScanResult;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    public TextView mSsidTextView;
    public TextView rssiTextView;
    public TextView rttTextView;
    public Context mContext;




    public MyAdapter(List<ScanResult> list, ScanResultClickListener scanResultClickListener) {
        mWifiAccessPointsWithRtt = list;
        sScanResultClickListener = scanResultClickListener;
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {
        public ViewHolderHeader(View view) {
            super(view);
        }
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder implements View.OnClickListener {



        public ViewHolderItem(View view) {
            super(view);
            view.setOnClickListener(this);
            Log.d(TAG, "Enter ViewHolderItem");
            mSsidTextView = view.findViewById(R.id.ssid_text_view);
            rssiTextView = view.findViewById(R.id.rssi_text_view);
            rttTextView = view.findViewById(R.id.rtt_text_view);



        }

        @Override
        public void onClick(View view) {
            sScanResultClickListener.onScanResultItemClick(getItem(getAdapterPosition()));
        }
    }

    public void swapData(List<ScanResult> list) {

        // Always clear with any update, as even an empty list means no WifiRtt devices were found.
        mWifiAccessPointsWithRtt.clear();

        if ((list != null) && (list.size() > 0)) {
            mWifiAccessPointsWithRtt.addAll(list);
        }

        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Log.d(TAG, "conCreatViewHolder()");
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

            mSsidTextView.setText(currentScanResult.SSID);

            if(currentScanResult.is80211mcResponder()){

                rttTextView.setText("O");
                rssiTextView.setText(currentScanResult.level + "");

                //WifiRttManager mWifiRttManager = (WifiRttManager) mContext.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
                //MyAdapter.RttRangingResultCallback mRttRangingResultCallback = new MyAdapter.RttRangingResultCallback();

                //RangingRequest rangingRequest = new RangingRequest.Builder().addAccessPoint(currentScanResult).build();


                //mWifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);



            }


            else{
                rssiTextView.setText(currentScanResult.level + "");
                rttTextView.setText("X");
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

                    rttTextView.setText(rangingResult.getDistanceMm() + "");

                    rssiTextView.setText(rangingResult.getRssi() + "");


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
