package kaistcs.android.dontkoala;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class NotificationActivity extends Activity {
	
	private NotificationManager nm = null;
	CustomizeDialog customizeDialog = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
    	PushWakeLock.acquireCpuWakeLock(getBaseContext());
	    
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

	    // AlertDialog 띄우기
		customizeDialog = new CustomizeDialog(this);
		customizeDialog.setTitle("DON'T KOALA");
		customizeDialog.setMessage("최우혁님이 안전하게 귀가했습니다");
		customizeDialog.show();

		Intent notiIntent = new Intent(this, DontKoalaActivity.class);
		notiIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		notiIntent.putExtra("tag", "status");
		
		// Status bar에 알림 띄우기
	    nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    // PendingIntent를 등록하고, noti를 클릭시에 어떤 클래스를 호출 할 것인지 등록
	    PendingIntent intent = PendingIntent.getActivity(NotificationActivity.this, 0, notiIntent, 0);
	    // status bar에 등록될 메세지 (Ticker, icon, time)
	    Notification notification = new Notification(R.drawable.ic_launcher, "<돈꽐라> 알림입니다", System.currentTimeMillis());
	    // List에 표시될 항목
	    notification.setLatestEventInfo(NotificationActivity.this, "DON'T KOALA", "새로운 알림이 있습니다", intent);
	    // noti를 클릭할 경우 자동으로 제거
	    notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
	    
	    nm.notify(1234, notification);
	    //Toast.makeText(NotificationActivity.this, "notify", Toast.LENGTH_SHORT).show();
	    
	}

}
