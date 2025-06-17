package com.obana.ddnsclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
//import android.preference.Preference;
import android.os.SharedMemory;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

public class MainActivity extends AppCompatActivity {

    private static SharedPreferences mPrefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public void startDdnsService (boolean on) {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        if (on) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
                Log.i("MainActivity", "startForegroundService");
            } else {
                startService(serviceIntent);
                Log.i("MainActivity", "startService");
            }
        } else {
            stopService(serviceIntent);
        }
    }
    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            // 初始化所有Preference的summary
            initPreferenceSummary(findPreference("pref_service_provider"));
            initPreferenceSummary(findPreference("pref_domain_name"));
            initPreferenceSummary(findPreference("pref_user_name"));
            //initPreferenceSummary(findPreference("pref_user_token"));
            initPreferenceSummary(findPreference("pref_network_type"));
            initPreferenceSummary(findPreference("pref_update_period"));
            initPreferenceSummary(findPreference("pref_service_onoff"));

            boolean serviceEnable = mPrefs.getBoolean("pref_service_onoff", false);
            MainActivity activity = (MainActivity)getContext();
            activity.startDdnsService(serviceEnable);
        }

        private void initPreferenceSummary(Preference preference) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                preference.setSummary(listPreference.getEntry());
            } else if (preference instanceof EditTextPreference) {
                EditTextPreference editPreference = (EditTextPreference) preference;
                preference.setSummary(editPreference.getText());
            }
            if (preference != null) preference.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(newValue.toString());
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else if (preference instanceof EditTextPreference) {
                preference.setSummary(newValue.toString());
            } else if (preference instanceof SwitchPreference) {
                MainActivity activity = (MainActivity)getContext();
                activity.startDdnsService((boolean)newValue);
            }
            return true;
        }
    }
}