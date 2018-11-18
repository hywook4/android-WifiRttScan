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
    private String delimiter = ",";
    private String fileName = "AnalysisData.csv";
    private FileWriter mFileWriter;
    private CSVWriter writer;

    public CsvManager() {
        // Do nothing
    }

    public CsvManager(String fileName) {
        this.fileName = fileName;
    }

    public CsvManager(String fileName, String delimiter) {
        this.fileName = fileName;
        this.delimiter = delimiter;
    }

    public void Write(String data) {
        String[] data_list = {data};
        this.Write(data_list);
    }

    public void Write(String[] data_list) {
        try {
            String baseDir = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath();
            String filePath = baseDir + File.separator + this.fileName;
            File f = new File(filePath);

            if(f.exists() && !f.isDirectory()){
                mFileWriter = new FileWriter(filePath , true);
                writer = new CSVWriter(mFileWriter);
            }
            else {
                mFileWriter = new FileWriter(filePath);
                writer = new CSVWriter(mFileWriter);
            }

            for(int i = 0; i < data_list.length; i++) {
                String data = data_list[i];
                writer.writeNext(data.split(","));
                writer.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
