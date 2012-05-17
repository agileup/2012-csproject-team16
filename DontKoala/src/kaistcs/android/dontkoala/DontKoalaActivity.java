package kaistcs.android.dontkoala;

import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

// TODO: change black UI to white
public class DontKoalaActivity extends TabActivity implements OnTabChangeListener {
	
	// Variable Initialization
	private TabHost mTabHost = null;
	int intro = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 처음 로딩 페이지 띄우기
        startActivity(new Intent(this, SplashActivity.class));
        
        Intent lostIntent = new Intent(this, DetectionService.class);
        lostIntent.setAction(DetectionService.ACTION_START_LOST_PHONE);
        startService(lostIntent);
        
        // 실제 탭 화면
        setContentView(R.layout.main);
        
        mTabHost = getTabHost();
        
        mTabHost.addTab(mTabHost.newTabSpec("profile")
        						.setIndicator("Profile", getResources().getDrawable(R.drawable.ic_tab_profile))
        						.setContent(new Intent(this, ProfileTab.class)));
        mTabHost.addTab(mTabHost.newTabSpec("group")
        						.setIndicator("Group", getResources().getDrawable(R.drawable.ic_tab_group))
        						.setContent(new Intent(this, GroupTab.class)));
        Intent notiIntent = new Intent(this, NotificationTab.class);
        notiIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mTabHost.addTab(mTabHost.newTabSpec("notification")
        						.setIndicator("Notifications", getResources().getDrawable(R.drawable.ic_tab_notification))
        						.setContent(notiIntent));
        
        mTabHost.setOnTabChangedListener(this);
        mTabHost.setCurrentTab(1);
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
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Toast.makeText(getApplicationContext(), "onResume()", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		String tag = intent.getStringExtra("tag");
		if (tag.equals("status")) {
			Toast.makeText(getApplicationContext(), "status", Toast.LENGTH_SHORT).show();
			mTabHost.setCurrentTab(2);
		} else if (tag.equals("dialog")) {
			Toast.makeText(getApplicationContext(), "dialog", Toast.LENGTH_SHORT).show();
			mTabHost.setCurrentTab(2);
		}
	}
}