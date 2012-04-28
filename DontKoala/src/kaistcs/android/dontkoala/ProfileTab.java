package kaistcs.android.dontkoala;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ProfileTab extends PreferenceActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.profile);
    }
}
