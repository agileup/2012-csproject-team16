package kaistcs.android.dontkoala;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

public class DontKoalaActivity extends TabActivity implements OnTabChangeListener {
	
	// Variable Initialization
	private TabHost mTabHost = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mTabHost = getTabHost();
        
        mTabHost.addTab(mTabHost.newTabSpec("profile")
        						.setIndicator("Profile", getResources().getDrawable(R.drawable.ic_tab_profile))
        						.setContent(new Intent(this, ProfileTab.class)));
        mTabHost.addTab(mTabHost.newTabSpec("group")
        						.setIndicator("Group", getResources().getDrawable(R.drawable.ic_tab_group))
        						.setContent(new Intent(this, GroupTab.class)));
        mTabHost.addTab(mTabHost.newTabSpec("notification")
        						.setIndicator("Notifications", getResources().getDrawable(R.drawable.ic_tab_notification))
        						.setContent(new Intent(this, NotificationTab.class)));
        
        mTabHost.setOnTabChangedListener(this);
        mTabHost.setCurrentTab(0);
    }

	@Override
	public void onTabChanged(String tabId) {
		// 탭 체인지될 경우 해당 탭의 액티비티 호출과 상관없이 메인에서 처리할 부분 (딱히 없으면 그냥 주석)
    	if (tabId.equals("profile")) {
    		//Toast.makeText(this, "Profile tab", Toast.LENGTH_SHORT).show();
    	} else if (tabId.equals("group")) {
    		//Toast.makeText(this, "Group tab", Toast.LENGTH_SHORT).show();
    	} else if (tabId.equals("notification")) {
    		//Toast.makeText(this, "Notification tab", Toast.LENGTH_SHORT).show();
    	} else {
    		//
    	}	
    }
}