package kaistcs.android.dontkoala;

import java.text.ParseException;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;

public class ProfileTab extends PreferenceActivity {
	EditTextPreference mName;
	Preference mHomeLocation;
	Preference mEmergencyContacts;
	CheckBoxPreference mSendLocation;
	EditTextPreference mPresetText;
	
	private SharedPreferences sharedPrefs;
	private static final int REQ_HOME_LOCATION = 0;
	
	// helper class for reading/writing HomeLocation preference
	public static class HomeLocationPref {
		private int latE6;
		private int longE6;
		private String addr = "";
		
		public HomeLocationPref(int latE6, int longE6, String addr) {
			this.latE6 = latE6;
			this.longE6 = longE6;
			this.addr = addr;
		}
		
		public HomeLocationPref(String prefString) throws ParseException, NumberFormatException{
			String[] parts = prefString.split(",");
			
			if (parts.length == 3) {
				latE6 = Integer.parseInt(parts[0]);
				longE6 = Integer.parseInt(parts[1]);
				addr = parts[2];
			} else {
				throw new ParseException(prefString, prefString.length());
			}
		}
		
		@Override
		public String toString() {
			return latE6 + "," + longE6 + "," + addr;
		}
		
		public int getLatitudeE6() {
			return latE6;
		}
		
		public int getLongitudeE6() {
			return longE6;
		}
		
		public String getAddress() {
			return addr;
		}
	}
	
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
		
		mName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mName.setSummary((String) newValue);
				return true;
			}
		});
		
		mHomeLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(ProfileTab.this, HomeLocation.class);
				i.putExtra("HomeLocationPref", sharedPrefs.getString(mHomeLocation.getKey(), ""));
				startActivityForResult(i, REQ_HOME_LOCATION);
				
				return true;
			}
		});
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		mName.setSummary(mName.getText());
		
		try {
			HomeLocationPref parser = new HomeLocationPref(sharedPrefs.getString(mHomeLocation.getKey(), ""));
			
			// If address is not available, then show lat,long
			if (parser.getAddress().isEmpty() == true)
				mHomeLocation.setSummary( ((double)parser.getLatitudeE6()/1E6) + ", " + ((double)parser.getLongitudeE6()/1E6) );
			else
				mHomeLocation.setSummary(parser.getAddress());
		} catch (Exception e) {
			
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_HOME_LOCATION && resultCode == RESULT_OK) {
			String prefString = data.getStringExtra("HomeLocationPref");
			sharedPrefs.edit().putString(mHomeLocation.getKey(), prefString).apply();
		}
	}
}
