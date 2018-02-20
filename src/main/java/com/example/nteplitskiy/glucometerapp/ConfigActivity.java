package com.example.nteplitskiy.glucometerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class ConfigActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_config);
        addPreferencesFromResource(R.xml.preferences);

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        Preference sendMail = (Preference) findPreference("send_mail");
        sendMail.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {


                Intent intent = getIntent();
                Log.d("sendMail", "" + intent.getExtras().getString("json"));

                Intent mail = new Intent(Intent.ACTION_SEND);
                mail.setType("text/html");
                mail.putExtra(Intent.EXTRA_EMAIL, pref.getString("email", ""));
                mail.putExtra(Intent.EXTRA_SUBJECT, pref.getString("name_patient", ""));
                mail.putExtra(Intent.EXTRA_TEXT, intent.getExtras().getString("json"));

                startActivity(mail);
                //intent.putExtra()
                return false;
            }
        });
    }
}
