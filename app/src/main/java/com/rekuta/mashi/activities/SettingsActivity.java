package com.rekuta.mashi.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.rekuta.mashi.preference.SettingsFragment;
import com.rekuta.mashi.utilities.Utils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.checkThemePreference(this);
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

}