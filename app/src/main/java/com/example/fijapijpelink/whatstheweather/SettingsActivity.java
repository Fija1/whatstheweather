package com.example.fijapijpelink.whatstheweather;


import android.os.Bundle;

public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String TEMPUNIT = "tempunit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


// Display the settings fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
