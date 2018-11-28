package com.example.android.wifirttscan;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class DeveloperActivity extends AppCompatActivity {
    private static final String TAG = "DeveloperActivity";

    public static boolean configOnlyMCCheckbox = true;

    private CsvManager mCsvManager;

    private EditText testCsvEditText;
    private Switch onlyMCCheckboxSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        testCsvEditText = (EditText)findViewById(R.id.test_csv_edittext);
        onlyMCCheckboxSwitch = (Switch)findViewById(R.id.only_mc_checkbox_switch);

        onlyMCCheckboxSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("[DEBUG]", "isCHecked: " + isChecked);
                configOnlyMCCheckbox = isChecked;
                Log.d("[DEBUG]", "configOnlyMCCheckbox: " + configOnlyMCCheckbox);
            }
        });

        mCsvManager = new CsvManager("test.csv");
        onlyMCCheckboxSwitch.setChecked(configOnlyMCCheckbox);
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
