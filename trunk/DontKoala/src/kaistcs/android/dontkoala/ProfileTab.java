package kaistcs.android.dontkoala;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

public class ProfileTab extends PreferenceActivity implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
	private Preference mName;
	private Preference mHomeLocation;
	private Preference mEmergencyContacts;
	private CheckBoxPreference mSendLocation;
	private Preference mPresetText;
	
	private static final int NameDialog = 0;
	private static final int PresetTextDialog = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.profile);
		
		mName = (Preference)findPreference("profile_name");
		mHomeLocation = (Preference)findPreference("profile_home_location");
		mEmergencyContacts = (Preference)findPreference("profile_emergency_contacts");
		mSendLocation = (CheckBoxPreference)findPreference("profile_send_location");
		mPresetText = (Preference)findPreference("profile_preset_text");
		
		mName.setOnPreferenceClickListener(this);
		mHomeLocation.setOnPreferenceClickListener(this);
		mEmergencyContacts.setOnPreferenceClickListener(this);
		mSendLocation.setOnPreferenceChangeListener(this);
		mPresetText.setOnPreferenceClickListener(this);
    }
	
	@Override
	public void onStart() {
		super.onStart();
		mPresetText.setEnabled(mSendLocation.isChecked());
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		AlertDialog.Builder ret = null;
		
		if (id == NameDialog) {
			final LinearLayout layout = new LinearLayout(this);
	        layout.setOrientation(LinearLayout.VERTICAL);
	        layout.setGravity(Gravity.CENTER_HORIZONTAL);
	        layout.setPadding(10, 0, 10, 0);
	        
	        final EditText inputName = new EditText(this);
	        layout.addView(inputName);
			
			ret = new AlertDialog.Builder(this)
				    .setTitle("Input your name")
				    .setView(layout)
				    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				    	@Override
				        public void onClick(DialogInterface dialog, int whichButton) {
				            mName.setSummary(inputName.getText().toString());
				            
				            // TODO: save name into the database
				        }
				    }).setNegativeButton("Cancel", null);
		}
		else if (id == PresetTextDialog) {
			final LinearLayout layout = new LinearLayout(this);
	        layout.setOrientation(LinearLayout.VERTICAL);
	        layout.setGravity(Gravity.CENTER_HORIZONTAL);
	        layout.setPadding(10, 0, 10, 0);
	        
	        final EditText inputPresetText = new EditText(this);
	        layout.addView(inputPresetText);
	        
			ret = new AlertDialog.Builder(this)
				    .setTitle("Input the preset text")
				    .setView(layout)
				    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				    	@Override
				        public void onClick(DialogInterface dialog, int whichButton) {
				            // TODO: save preset text into the database
				        }
				    }).setNegativeButton("Cancel", null);
		}
		
		return ret.create();
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		if (preference == mName) {
			showDialog(NameDialog);
		}
		else if (preference == mPresetText)
		{
			showDialog(PresetTextDialog);
		}
		
		return true;
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		if (preference == mSendLocation) {
			mPresetText.setEnabled((Boolean) newValue);
		}
		
		return true;
	}
}
