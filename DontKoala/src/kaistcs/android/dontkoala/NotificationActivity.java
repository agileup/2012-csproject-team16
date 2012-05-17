package kaistcs.android.dontkoala;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class NotificationActivity extends Activity {
	
	private NotificationManager nm = null;
	CustomizeDialog customizeDialog = null;

    final Window win = getWindow();
    
    private String getData;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    	//PushWakeLock.acquireCpuWakeLock(getBaseContext());
	    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
	    
	    // 푸쉬 정보 받아오기
	    // KOALA|최진길|폰번호|상태|위도|경도
	    Intent push_info = getIntent();
	    getData = push_info.getStringExtra("sendData");
	    String[] token = getData.split("\\|");
	    for (int i=0; i<token.length; i++) {
	    	Log.i("DEBUG", token[i]);
	    }
	    
	    // 서버에 보내자~
	    ArrayList<NameValuePair> nameValue = new ArrayList<NameValuePair>();
		try {
			nameValue.add(new BasicNameValuePair("name", token[1]));
			nameValue.add(new BasicNameValuePair("phone", token[2]));
			nameValue.add(new BasicNameValuePair("description", "NONE"));
			nameValue.add(new BasicNameValuePair("status", token[3]));
			nameValue.add(new BasicNameValuePair("latitude", token[4]));
			nameValue.add(new BasicNameValuePair("longitude", token[5]));
			
			SimpleDateFormat formatter = new SimpleDateFormat("yy/MM/dd a hh:mm", Locale.KOREA);
			Date currentTime = new Date();
			String cTime = formatter.format(currentTime);
			nameValue.add(new BasicNameValuePair("time", cTime));
			
			HttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost("http://flclab.iptime.org/dontkoala/set_notification.php");
			request.setEntity(new UrlEncodedFormEntity(nameValue, HTTP.UTF_8));
			client.execute(request);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    // AlertDialog 띄우기
		customizeDialog = new CustomizeDialog(this);
		customizeDialog.setTitle("DON'T KOALA");
		String msgBody = token[1] + "님이 ";
		if (token[3].equals("0")) {
			msgBody += "KOALA가 됐습니다!\n집으로 갈 수 있게 도와주세요!";
		} else if (token[3].equals("1")) {
			msgBody += "안전하게 귀가했습니다";
		}
		customizeDialog.setMessage(msgBody);
		//customizeDialog.setMessage("최진길님이 KOALA가 됐습니다!\n집으로 갈 수 있게 도와주세요!");
		customizeDialog.show();

		Intent notiIntent = new Intent(this, DontKoalaActivity.class);
		notiIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notiIntent.putExtra("tag", "status");
		
		// Status bar에 알림 띄우기
	    nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    // PendingIntent를 등록하고, noti를 클릭시에 어떤 클래스를 호출 할 것인지 등록
	    PendingIntent intent = PendingIntent.getActivity(NotificationActivity.this, 0, notiIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
