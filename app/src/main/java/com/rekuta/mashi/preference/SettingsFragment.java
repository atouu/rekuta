package com.rekuta.mashi.preference;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.rekuta.mashi.R;
import com.rekuta.mashi.utilities.Utils;

import java.io.FileNotFoundException;
import java.util.HashMap;

public class SettingsFragment extends PreferenceFragmentCompat {
    private IntegerEditTextPreference recordStartPref;
    private IntegerEditTextPreference recordEndPref;
    private FilePickerPreference backgroundMusicPref;

    private ListPreference themePref;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recordStartPref = findPreference(R.string.pref_record_start);
        recordEndPref = findPreference(R.string.pref_record_end);
        backgroundMusicPref = findPreference(R.string.pref_background_music);
        themePref = findPreference(R.string.pref_theme);


        backgroundMusicPref.setExtensions("wav", "mp3");
        backgroundMusicPref.setSummaryProvider(preference -> backgroundMusicPref.getText());
        backgroundMusicPref.setOnPreferenceChangeListener((preference, newValue) -> {
            String newVal = newValue.toString();

            if (newVal.equals("Default")) {
                recordStartPref.setText("4200");
                recordEndPref.setText("9600");
                return true;
            }

            String bgmParamFile = newVal.substring(0, newVal.lastIndexOf(".")).concat(".txt");
            HashMap<String, Integer> bgmParam;

            try {
                bgmParam = Utils.parseBGMParam(bgmParamFile);
            } catch (Exception e) {
                if (e instanceof FileNotFoundException) {
                    Utils.showMessage(getActivity(), "BGM param file not found.");
                    return true;
                }

                Utils.showMessage(getActivity(), "Invalid BGM param file.");
                return true;
            }

            recordStartPref.setText(String.valueOf(bgmParam.get("rStart")));
            recordEndPref.setText(String.valueOf(bgmParam.get("rStop")));

            return true;
        });

        themePref.setOnPreferenceChangeListener((preference, newValue) -> {
            switch (newValue.toString()) {
                case "dark":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case "light":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }

            return true;
        });

        recordStartPref.setSummaryProvider(preference -> recordStartPref.getText().concat(" ms"));
        recordEndPref.setSummaryProvider(preference -> recordEndPref.getText().concat(" ms"));

    }

    private <T extends Preference> T findPreference(int resId) {
        return findPreference(getString(resId));
    }

}