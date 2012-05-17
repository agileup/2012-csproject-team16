package kaistcs.android.dontkoala;

import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

// TODO: change black UI to white
public class DontKoalaActivity extends TabActivity implements OnTabChangeListener {
	
	// Variable Initialization
	private TabHost mTabHost = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 처음 로딩 페이지 띄우고
        startActivity(new Intent(this, SplashActivity.class));
        
        // 실제 탭 화면
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
        
        //mTabHost.getTabWidget().setLeftStripDrawable(drawable);
        //mTabHost.getTabWidget().setStripEnabled(true);
        
        mTabHost.setOnTabChangedListener(this);
        mTabHost.setCurrentTab(0);
        //mTabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);
        
        for (int i=0; i<mTabHost.getTabWidget().getChildCount(); i++) {
			mTabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.WHITE);
		}
		mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#FFD200"));
    }

	@Override
	public void onTabChanged(String tabId) {
		
		for (int i=0; i<mTabHost.getTabWidget().getChildCount(); i++) {
			mTabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.WHITE);
		}
		mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#FFD200"));
/*		
		// 탭 체인지될 경우 해당 탭의 액티비티 호출과 상관없이 메인에서 처리할 부분 (딱히 없으면 그냥 주석)
    	if (tabId.equals("profile")) {
    		mTabHost.getTabWidget().getChildAt(0).setBackgroundColor(Color.parseColor("#FFD200"));
    	} else if (tabId.equals("group")) {
    		mTabHost.getTabWidget().getChildAt(1).setBackgroundColor(Color.parseColor("#FFD200"));
    	} else if (tabId.equals("notification")) {
    		mTabHost.getTabWidget().getChildAt(2).setBackgroundColor(Color.parseColor("#FFD200"));
    	} 
*/	}
}