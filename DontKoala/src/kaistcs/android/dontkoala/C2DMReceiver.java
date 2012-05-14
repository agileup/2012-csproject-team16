package kaistcs.android.dontkoala;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class C2DMReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
	    Log.e("DEBUG", "c2dm - onReceive");
	    
	    // 서버에 정상적으로 등록이 완료되었을 때
	    if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
	    	handleRegistration(context, intent);
	    }
	    else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
	    	handleMessage(context, intent);
	    }
	}
	
	private void handleRegistration(Context context, Intent intent) {
		Log.e("DEBUG", "c2dm - handleRegistration");
		// 서버에서 넘어오는 메세지의 내용에 key이름 "registration_id"에는 이 기기에만 사용하는 인증키값이 담겨서 넘어온다
		String registration = intent.getStringExtra("registration_id");
		if (intent.getStringExtra("error") != null) {
			Log.e("DEBUG", "c2dm - error");
		} else if (intent.getStringExtra("unregistered") != null) {
			Log.e("DEBUG", "c2dm - unregistered");
		} else if (registration != null) {
			Log.e("DEBUG", "c2dm - " + registration);
		}
	}
	
	private void handleMessage(Context context, Intent intent) {
		Log.e("DEBUG", "c2dm - handleMessage");
		Toast.makeText(context, intent.getStringExtra("msg"), 1000).show();
	}

}
