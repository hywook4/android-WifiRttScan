package com.example.android.wifirttscan;

import android.net.wifi.rtt.RangingResult;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;

public class CsvManager {
    FileWriter mFileWriter;
    CSVWriter writer;

    public CsvManager() {

    }

    public void Write(){
        try{
            Log.d("Write", "startwrite");

            String baseDir = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath();
            String fileName = "AnalysisData.csv";
            String filePath = baseDir + File.separator + fileName;
            File f = new File(filePath);
            // File exist
            if(f.exists() && !f.isDirectory()){
                Log.d("yes folder", "use dir");
                Log.d("write on", filePath);
                mFileWriter = new FileWriter(filePath , true);
                writer = new CSVWriter(mFileWriter);
            }
            else {
                Log.d("no folder", "create dir");
                Log.d("write on", filePath);
                writer = new CSVWriter(new FileWriter(filePath));
            }
            // FIXME: convert RangingResult to proper String

            String[] record  = {"test1",
                    "test2",
                    "test3",
                    "test4",
                    "test5",
                    "test6"};

            writer.writeNext(record);
            Log.d("after write", "write success?");
            writer.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Write(RangingResult data) {
        try{
            String baseDir = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath();
            String fileName = "AnalysisData.csv";
            String filePath = baseDir + File.separator + fileName;
            File f = new File(filePath );
            // File exist

            if(f.exists() && !f.isDirectory()){
                mFileWriter = new FileWriter(filePath , true);
                writer = new CSVWriter(mFileWriter);
            }
            else {
                writer = new CSVWriter(new FileWriter(filePath));
            }
            // FIXME: convert RangingResult to proper String

            String[] record  = {data.getMacAddress()+"",
                    data.getStatus()+"",
                    data.getDistanceMm()+"",
                    data.getDistanceStdDevMm()+"",
                    data.getRssi()+"",
                    data.getRangingTimestampMillis()+"",
                    data.getNumAttemptedMeasurements()+"",
                    data.getNumSuccessfulMeasurements()+""};

            writer.writeNext(record);

            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
