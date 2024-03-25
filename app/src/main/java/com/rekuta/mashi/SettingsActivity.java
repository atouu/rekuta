package com.rekuta.mashi;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppUtil.checkThemePreference(this);
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        private String customBgmFile;
        private boolean darkMode;
        private SwitchPreference customBgmPref;
        private EditTextPreference recordDelayPref;
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
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            customBgmFilePref = (Preference) findPreference("customBGMFile");
            customBgmPref = (SwitchPreference) findPreference("customBGM");
            recordDelayPref = (EditTextPreference) findPreference("recordDelay");
            darkModePref = (SwitchPreference) findPreference("darkMode");
            customBgmFile = sharedPrefs.getString("customBGMFile", "None");
            darkMode = sharedPrefs.getBoolean("darkMode", false);
            
            if (customBgmFile.equals("None")) {
                customBgmFilePref.setSummary("None");
            } else {
                customBgmFilePref.setSummary(AppUtil.getFileName(getActivity(), Uri.parse(customBgmFile)));
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
                        AppUtil.showMessage(getActivity(), R.string.specify_custom_bgm_first);
                        return false;
                    }
                    return true;
                }
            });
            
            recordDelayPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("")) {
                        recordDelayPref.setText("0");
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
                customBgmFilePref.setSummary(AppUtil.getFileName(getActivity(), data.getData()));
                
                ContentResolver resolver = getActivity().getContentResolver();
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                resolver.takePersistableUriPermission(data.getData(), takeFlags);
                
                sharedPrefs.edit().putString("customBGMFile",data.getData().toString()).commit();
            }
        }
    }

}