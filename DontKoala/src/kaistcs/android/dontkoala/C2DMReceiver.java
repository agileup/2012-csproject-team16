package kaistcs.android.dontkoala;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class C2DMReceiver extends BroadcastReceiver {
	
	static String registration_id = null;
	static String s2dm_msg = "";

	@Override
	public void onReceive(Context context, Intent intent) {
	    Log.v("C2DM", "onReceive");
	    
	    // 서버에 정상적으로 등록이 완료되었을 때
	    if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
	    	handleRegistration(context, intent);
	    }
	    else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
	    	handleMessage(context, intent);
/*	    	Toast toast = Toast.makeText(context, intent.getStringExtra("msg"), Toast.LENGTH_SHORT);
	    	toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 150);
	    	toast.show();
*/	    }
	}
	
	private void handleRegistration(Context context, Intent intent) {
		
		registration_id = intent.getStringExtra("registration_id");
		
		Log.v("C2DM", "Get the Registration ID from C2DM");
		Log.v("C2DM", "Registration ID : " + registration_id);

		// 서버에서 넘어오는 메세지의 내용에 key이름 "registration_id"에는 이 기기에만 사용하는 인증키값이 담겨서 넘어온다
		if (intent.getStringExtra("error") != null) {
			Log.v("C2DM", "C2DM REGISTRATION : Registration failed, should try again later");
		} else if (intent.getStringExtra("unregistered") != null) {
			Log.v("C2DM", "C2DM REGISTRATION : unregistration done, new messages from the authorized sender will be rejected");
		} else if (registration_id != null) {
			Log.v("C2DM", "Registration ID complete!");
			
			SharedPreferences shrdPref = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor editor = shrdPref.edit();
			editor.putString("registration_id", registration_id);
			editor.commit();
		}
	}
	
	private void handleMessage(Context context, Intent intent) {
		Log.v("C2DM", "Get the Message from C2DM");
//		Toast.makeText(context, intent.getStringExtra("msg"), 1000).show();
/*		
		String title = intent.getStringExtra("title");
		String msg = intent.getStringExtra("msg");
		NotificationManager notiManager = (NotificationManager)context.getSystemService(Activity.NOTIFICATION_SERVICE);
		String text = "<"+title+"> "+msg;
		
		Notification notification = new Notification(R.drawable.warning, text, System.currentTimeMillis());
		notification.defaults = Notification.DEFAULT_VIBRATE;
		
		notiManager.notify(0, notification);
*/	}

}
