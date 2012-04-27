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
        
        // 일단 탭 아이콘이 프로필 밖에 없어서 전부 통일, 나중에 이미지만 변경
        mTabHost.addTab(mTabHost.newTabSpec("tab1").setIndicator("Profile", getResources().getDrawable(R.drawable.profile)).setContent(new Intent(this, ProfileTab.class)));
        mTabHost.addTab(mTabHost.newTabSpec("tab2").setIndicator("Group", getResources().getDrawable(R.drawable.profile)).setContent(new Intent(this, GroupTab.class)));
        mTabHost.addTab(mTabHost.newTabSpec("tab3").setIndicator("Notification", getResources().getDrawable(R.drawable.profile)).setContent(new Intent(this, NotificationTab.class)));
        
        mTabHost.setOnTabChangedListener(this);
        mTabHost.setCurrentTab(0);
    }

	@Override
	public void onTabChanged(String tabId) {
		// 탭 체인지될 경우 해당 탭의 액티비티 호출과 상관없이 메인에서 처리할 부분 (딱히 없으면 그냥 주석)
    	if (tabId.equals("tab1")) {
    		//Toast.makeText(this, "Profile tab", Toast.LENGTH_SHORT).show();
    	} else if (tabId.equals("tab2")) {
    		//Toast.makeText(this, "Group tab", Toast.LENGTH_SHORT).show();
    	} else if (tabId.equals("tab3")) {
    		//Toast.makeText(this, "Notification tab", Toast.LENGTH_SHORT).show();
    	} else {
    		//
    	}	
    }
}