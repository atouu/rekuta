package com.rekuta.mashi;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.EditText;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.checkThemePreference(this);
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        private String customBgmFile;
        private boolean darkMode;
        private EditText customBgmFileEditText;
        private SwitchPreference customBgmPref;
        private EditTextPreference recordStartPref;
        private EditTextPreference recordEndPref;
        private Preference customBgmFilePref;
        private SharedPreferences sharedPrefs;
        private SwitchPreference darkModePref;
        
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            initialize();
        }
        
        private void initialize() {
            sharedPrefs = getPreferenceManager().getDefaultSharedPreferences(getActivity());
            customBgmFilePref = (Preference) findPreference("customBGMFile");
            customBgmPref = (SwitchPreference) findPreference("customBGM");
            recordStartPref = (EditTextPreference) findPreference("recordStart");
            recordEndPref = (EditTextPreference) findPreference("recordEnd");
            darkModePref = (SwitchPreference) findPreference("darkMode");
            customBgmFile = sharedPrefs.getString("customBGMFile", "None");
            darkMode = customBgmPref.isChecked();
            
            if (!customBgmFile.equals("None")) {
                customBgmFilePref.setSummary(Utils.getFileName(getActivity(), Uri.parse(customBgmFile)));
            }
            
            customBgmFilePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent pickFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pickFile.setType("audio/*");
                    startActivityForResult(pickFile, 0);
                    return true;
                }
            });
            
            customBgmPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (customBgmFile.equals("None")) {
                        Utils.showMessage(getActivity(), R.string.specify_custom_bgm_first);
                        return false;
                    }
                    return true;
                }
            });
            
            recordStartPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().isEmpty()) {
                        recordStartPref.setText("0");
                        return false;
                    }
                    return true;
                }
            });
            
            recordEndPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().isEmpty()) {
                        recordEndPref.setText("0");
                        return false;
                    }
                    return true;
                }
            });
            
            darkModePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (darkMode == (boolean) newValue) {
                        getActivity().setResult(Activity.RESULT_CANCELED);
                    } else {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("toggleTheme", true);
                        getActivity().setResult(Activity.RESULT_OK, returnIntent);
                    }
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 0 && resultCode == RESULT_OK) {
                customBgmFile = data.getData().toString();
                customBgmFilePref.setSummary(Utils.getFileName(getActivity(), data.getData()));
                
                ContentResolver resolver = getActivity().getContentResolver();
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                resolver.takePersistableUriPermission(data.getData(), takeFlags);
                
                sharedPrefs.edit().putString("customBGMFile", data.getData().toString()).commit();
            }
        }
    }

}