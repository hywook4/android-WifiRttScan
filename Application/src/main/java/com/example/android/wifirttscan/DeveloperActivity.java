package com.example.android.wifirttscan;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class DeveloperActivity extends AppCompatActivity {
    private static final String TAG = "DeveloperActivity";

    private CsvManager mCsvManager;

    private EditText testCsvEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mCsvManager = new CsvManager("test.csv");

        testCsvEditText = (EditText)findViewById(R.id.test_csv_edittext);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    public void onClickWriteCsv(View view) {
        Log.d(TAG, "onClickDeveloperConsole");

        String data = testCsvEditText.getText().toString();
        mCsvManager.Write(data);
    }
}
