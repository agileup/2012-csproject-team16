package kaistcs.android.dontkoala;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;

public class ProfileTab extends PreferenceActivity {
	private EditTextPreference mName;
	private Preference mHomeLocation;
	private Preference mEmergencyContacts;
	private CheckBoxPreference mSendLocation;
	private EditTextPreference mPresetText;
	
	private SharedPreferences sharedPrefs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.profile);
		
		mName = (EditTextPreference)findPreference("profile_name");
		mHomeLocation = (Preference)findPreference("profile_home_location");
		mEmergencyContacts = (Preference)findPreference("profile_emergency_contacts");
		mSendLocation = (CheckBoxPreference)findPreference("profile_send_location");
		mPresetText = (EditTextPreference)findPreference("profile_preset_text");
		
		mEmergencyContacts.setIntent(new Intent(this, EmergencyContacts.class));
		mHomeLocation.setIntent(new Intent(this, HomeLocation.class));
		
		mName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mName.setSummary((String) newValue);
				return true;
			}
		});
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		mName.setSummary(mName.getText());
		mHomeLocation.setSummary(sharedPrefs.getString(mHomeLocation.getKey(), ""));	// TODO: display address instead of "latitude,longitude". 
	}
}
