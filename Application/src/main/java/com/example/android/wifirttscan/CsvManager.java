package com.example.android.wifirttscan;

import android.icu.text.SimpleDateFormat;

import java.io.File;
import java.io.FileWriter;

public class CsvWriter {
    public CsvWriter() {

    }

    public void WriteData() {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "AnalysisData.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath );
        CSVWriter writer;
        // File exist
        if(f.exists() && !f.isDirectory()){
            mFileWriter = new FileWriter(filePath , true);
            writer = new CSVWriter(mFileWriter);
        }
        else {
            writer = new CSVWriter(new FileWriter(filePath));
        }
        String[] data = {"Ship Name","Scientist Name", "...",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").formatter.format(date)});

        writer.writeNext(data);

        writer.close();
    }
}
