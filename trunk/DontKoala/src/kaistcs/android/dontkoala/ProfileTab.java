package kaistcs.android.dontkoala;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.content.Intent;

public class ProfileTab extends PreferenceActivity {

	EditTextPreference mName;
	Preference mHomeLocation;
	Preference mEmergencyContacts;
	CheckBoxPreference mSendLocation;
	EditTextPreference mPresetText;
	
	private UserInfo userInfo;
	private static final int REQ_HOME_LOCATION = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.profile);
		
		mName = (EditTextPreference)findPreference("profile_name");
		mHomeLocation = (Preference)findPreference("profile_home_location");
		mEmergencyContacts = (Preference)findPreference("profile_emergency_contacts");
		mSendLocation = (CheckBoxPreference)findPreference("profile_send_location");
		mPresetText = (EditTextPreference)findPreference("profile_preset_text");
		userInfo = new UserInfo(this);
		
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
				i.putExtra("HomeLocationIn", userInfo.getHomeLocation());
				
				startActivityForResult(i, REQ_HOME_LOCATION);
				
				return true;
			}
		});
    }
	
	@Override
	public void onResume() {
		super.onResume();
		mName.setSummary(mName.getText());
		
		UserInfo.HomeLocationInfo home = userInfo.getHomeLocation();
		
		if (home != null) {
			// If address is not available, then show lat,long
			if (home.getAddress().isEmpty() == true)
				mHomeLocation.setSummary( ((double)home.getLatitudeE6()/1E6) + ", " + ((double)home.getLongitudeE6()/1E6) );
			else
				mHomeLocation.setSummary(home.getAddress());
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_HOME_LOCATION && resultCode == RESULT_OK) {
			UserInfo.HomeLocationInfo homeLocOut = data.getParcelableExtra("HomeLocationOut");
			userInfo.setHomeLocation(homeLocOut);
		}
	}
}
