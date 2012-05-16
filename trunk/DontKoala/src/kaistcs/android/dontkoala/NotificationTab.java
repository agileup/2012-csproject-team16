package kaistcs.android.dontkoala;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class NotificationTab extends Activity implements OnClickListener {
	
	// C2DM variable initialize
	//private String mailAddr = "akamk87@gmail.com";
	private static String authToken = null;
	
	// result variable initialize
	private ListView mResultView = null;
	
	CustomizeDialog customizeDialog = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
        setContentView(R.layout.notification);
        
        Button btnRegist = (Button)findViewById(R.id.btn_c2dm_regist);
        Button btnUnregist = (Button)findViewById(R.id.btn_c2dm_unregist);
        btnRegist.setOnClickListener(this);
        btnUnregist.setOnClickListener(this);
        
        Button btnPush = (Button)findViewById(R.id.btn_push);
        btnPush.setOnClickListener(this);
        
        /** CustomizeAlertDialog */
        View v = getWindow().getDecorView();
        v.setBackgroundResource(android.R.color.transparent);
        
        mResultView = (ListView)findViewById(R.id.noti_listview);
        
        final ResultAdapter adapter = new ResultAdapter(this);
        Resources res = getResources();
        adapter.addItem(new ResultItem(res.getDrawable(R.drawable.warning), "최우혁님이 KOALA!", "12/05/10 AM 03:01"));
        adapter.addItem(new ResultItem(res.getDrawable(R.drawable.safe), "코알라님이 안전하게 귀가했습니다", "12/05/09 PM 10:23"));
		adapter.addItem(new ResultItem(res.getDrawable(R.drawable.safe), "최우혁님이 안전하게 귀가했습니다", "12/05/09 PM 10:03"));		
		
		mResultView.setAdapter(adapter);
		mResultView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				//IconTextItem  Item = (IconTextItem)adapter.getItem(arg2);
				//String[] data = Item.getData();						
				//Toast.makeText(getApplicationContext(),"국가 : " + data[0] + "\n" + "Nation : "+ data[1] +"\n"+ "피파순위 : "+ data[2] , 1).show();
				Toast.makeText(getApplicationContext(), "상세보기로 넘어갑니다 (준비중)", 1000).show();
			}
		});	
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_c2dm_regist:	
			/**
			 * Android C2DM에 push 메세지를 받겠다는 메세지를 보내는 Intent
			 * 정상적으로 등록이되면 Android C2DM Server 쪽에서 인증키를 보내줌
			 * 받아온 인증키는 해당 어플리케이션과 해당 기기를 대표하는 인증키로 서버에서 메세지를 보낼때 사용
			 * 서버에 등록을 할 때마다 인증키는 달라진다
			 */
/*			Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
			registrationIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));
			registrationIntent.putExtra("sender", mailAddr);
			startService(registrationIntent);
*/			
			try {
				requestRegistrationId();
				authToken = getAuthToken();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
			
		case R.id.btn_c2dm_unregist:
			/** Android C2DM에 push 메세지를 그만 받겠다는 메세지를 보내는 Intent */
/*			Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
			unregIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));
			startService(unregIntent); */
			break;
			
		case R.id.btn_push:
			//startActivity(new Intent(this, NotificationActivity.class));
			try {
				//requestPush(REG_ID, "푸쉬테스트", "과연 정말로 가는거니?");
				sender(C2DMReceiver.registration_id, authToken, "ALL THE SAME");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Status bar에 알람 등록
			NotificationManager nm;
			nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		    /** PendingIntent를 등록하고, noti를 클릭시에 어떤 클래스를 호출 할 것인지 등록 */
		    PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(), 0);
		    /** status bar에 등록될 메세지 (Ticker, icon, time) */
		    Notification notification = new Notification(android.R.drawable.btn_star, "<돈꽐라> 알림입니다", System.currentTimeMillis());
		    /** List에 표시될 항목 */
		    notification.setLatestEventInfo(NotificationTab.this, "DON'T KOALA", "새로운 알림이 있습니다", intent);
		    /** noti를 클릭할 경우 자동으로 제거 */
		    notification.flags = notification.flags | notification.FLAG_AUTO_CANCEL;
		    nm.notify(1234, notification);
		    
		    // AlertDialog 띄우기
			customizeDialog = new CustomizeDialog(this);
			customizeDialog.setTitle("DON'T KOALA");
			customizeDialog.setMessage("최우혁님이 안전하게 귀가했습니다");
			customizeDialog.show();
			
			break;
			
		default:
			break;
		}
	}

	/** C2DM으로 메세지를 보내는 메소드 */
    public void sender(String registration_id, String authToken, String msg) throws Exception {
    	/**
         * collapse_key는 C2DM에서 사용자가 SEND 버튼을 실수로 여러번 눌렀을때
         * 이전 메세지 내용과 비교해서 반복전송되는 것을 막기 위해서 사용된다.
         * 여기서는 반복전송도 허용되게끔 매번 collapse_key를 랜덤함수로 뽑는다.
         */
        String collaspe_key = String.valueOf(Math.random() % 100 + 1);
 
        // 보낼 메세지 조립
        StringBuffer postDataBuilder = new StringBuffer();
 
        postDataBuilder.append("registration_id=" + registration_id);
        postDataBuilder.append("&collapse_key=" + collaspe_key); // 중복방지 필터
        postDataBuilder.append("&delay_while_idle=1");
        postDataBuilder.append("&data.msg=" + URLEncoder.encode(msg, "UTF-8")); // 메세지                                                                          // 내용
 
        // 조립된 메세지를 Byte배열로 인코딩
        byte[] postData = postDataBuilder.toString().getBytes("UTF-8");
 
        // HTTP 프로토콜로 통신한다.
        // 먼저 해당 url 커넥션을 선언하고 연다.
        URL url = new URL("https://android.apis.google.com/c2dm/send");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 
        conn.setDoOutput(true); // 출력설정
        conn.setUseCaches(false);
        conn.setRequestMethod("POST"); // POST 방식
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
        conn.setRequestProperty("Authorization", "GoogleLogin auth=" + authToken);
 
        // 출력스트림을 생성하여 postData를 기록.
        OutputStream out = conn.getOutputStream();
 
        // 출력(송신)후 출력스트림 종료
        out.write(postData);
        out.close();
 
        // 소켓의 입력스트림을 반환
        conn.getInputStream();
    }
	
	/**
     * Request for RegistrationID to C2DM Activity 시작시 구글 C2DM으로 Registration ID
     * 발급을 요청한다. Registration ID를 발급받기 위해서는 Application ID, Sender ID가 필요.
     * Registration ID는 Device를 대표하는 ID로써 한번만 받아서 저장하면 되기 때문에 매번 실행시 체크.
     */
    public void requestRegistrationId() throws Exception{
 
        SharedPreferences shrdPref = PreferenceManager.getDefaultSharedPreferences(this);
        String registration_id = shrdPref.getString("registration_id", null);
        shrdPref = null;
 
        if (registration_id == null) {
            Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
            // Application ID(Package Name)
            registrationIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));
            // Developer ID
            registrationIntent.putExtra("sender", "akamk87@gmail.com");
            // Start request.
            startService(registrationIntent);
        } else {
            C2DMReceiver.registration_id = registration_id;
            Log.v("C2DM", "Registration ID is Exist!");
            Log.v("C2DM", "Registration ID : " + C2DMReceiver.registration_id);
        }
    }
     
    /**
     * C2DM을 이용하기 위해서는 보안상 authToken(인증키)이 필요하다. 
     * authToken도 역시 한 번만 받아놓고 저장한다음 쓰면 된다.
     */
    public String getAuthToken() throws Exception {
 
        SharedPreferences shrdPref = PreferenceManager.getDefaultSharedPreferences(this);
        String authToken = shrdPref.getString("authToken", null);
 
        Log.v("C2DM", "AuthToken : " + authToken);
 
        if (authToken == null) {
            StringBuffer postDataBuilder = new StringBuffer();
 
            postDataBuilder.append("accountType=HOSTED_OR_GOOGLE");
            postDataBuilder.append("&Email=akamk87@gmail.com");
            postDataBuilder.append("&Passwd=je8f37mk"); 
            postDataBuilder.append("&service=ac2dm");
            postDataBuilder.append("&source=androidpush-test. htcsensation-2.3");
 
            byte[] postData = postDataBuilder.toString().getBytes("UTF-8");
 
            URL url = new URL("https://www.google.com/accounts/ClientLogin");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
 
            // 출력스트림을 생성하여 서버로 송신
            OutputStream out = conn.getOutputStream();
            out.write(postData);
            out.close();
 
            // 서버로부터 수신받은 스트림 객체를 버퍼에 넣어 읽는다.
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
 
            String sIdLine = br.readLine();
            String lsIdLine = br.readLine();
            String authLine = br.readLine();
 
            Log.v("C2DM", sIdLine);
            Log.v("C2DM", lsIdLine);
            Log.v("C2DM", authLine);
 
            authToken = authLine.substring(5, authLine.length());
 
            SharedPreferences.Editor editor = shrdPref.edit();
            editor.putString("authToken", authToken);
            editor.commit();
        }
 
        shrdPref = null;
        return authToken;
    }
	
/*	
	// ListView 안에 Item을 클릭시에 호출되는 Listener
	private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
			//Toast.makeText(getApplicationContext(), ""+(position+1), Toast.LENGTH_SHORT).show();
			
			mCustomAdapter.setChecked(position);
			// Data 변경시 호출 Adapter에 Data 변경 사실을 알려줘서 Update 함.
			mCustomAdapter.notifyDataSetChanged();
		}
	};
*/
	// Custom Adapter
	class ResultAdapter extends BaseAdapter {

		private Context mContext;

		private List<ResultItem> mItems = new ArrayList<ResultItem>();

		public ResultAdapter(Context context) {
			mContext = context;
		}

		public void addItem(ResultItem ri) {
			mItems.add(ri);
		}
		public void setListItems(List<ResultItem> lri) {
			mItems = lri;
		}
		public int getCount() {
			return mItems.size();
		}
		public Object getItem(int position) {
			return mItems.get(position);
		}
		public boolean areAllItemsSelectable() {
			return false;
		}
		public boolean isSelectable(int position) {
			try {
				return mItems.get(position).isSelectable();
			} catch (IndexOutOfBoundsException ex) {
				return false;
			}
		}
		public long getItemId(int position) {
			return position;
		}
		public View getView(int position, View convertView, ViewGroup parent) {
			ResultItemView itemView;
			
			//convertView가 한번만 생성됨으로 성능상으로 좋음
			if (convertView == null) {
				itemView = new ResultItemView(mContext, mItems.get(position));
			} else {
				itemView = (ResultItemView) convertView;
				
				itemView.setIcon(mItems.get(position).getIcon());
				itemView.setMessage(mItems.get(position).getMessage());
				itemView.setTime(mItems.get(position).getTime());
			}

			return itemView;
		}

	}

	class ResultItemView extends LinearLayout {
		private ImageView mIcon;
		private TextView mMessage;
		private TextView mTime;
		
		ResultItem aItem;
		
		public ResultItemView(Context context, ResultItem aItem) {
			super(context);
			// Layout Inflation
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
			inflater.inflate(R.layout.noti_result_item, this, true);
			
			// Set Icon
			mIcon = (ImageView) findViewById(R.id.resultIcon);
			mIcon.setImageDrawable(aItem.getIcon());
			// Set Text 01
			mMessage = (TextView) findViewById(R.id.resultMessage);
			mMessage.setText(aItem.getMessage());
			// Set Text 02
			mTime = (TextView) findViewById(R.id.resultTime);
			mTime.setText(aItem.getTime());
			
			this.aItem = aItem;
		}
		
		public void setIcon(Drawable icon) {
			mIcon.setImageDrawable(icon);
		}
		public void setMessage(String msg) {
			mMessage.setText(msg);
		}
		public void setTime(String time) {
			mTime.setText(time);
		}
		
		public ResultItem getResultItem() {
			return aItem;
		}
		
	}
	
	class ResultItem {
		private Drawable mIcon = null;
		private String mMessage = null;
		private String mTime = null;
		private boolean mSelectable = true;

		public ResultItem(Drawable icon, String msg, String time) {
			mIcon = icon;
			mMessage = msg;
			mTime = time;
		}
		
		public boolean isSelectable() {
			return mSelectable;
		}
		public void setSelectable(boolean selectable) {
			mSelectable = selectable;
		}
		
		public Drawable getIcon() {
			return mIcon;
		}
		public void setIcon(Drawable mIcon) {
			this.mIcon = mIcon;
		}
		public String getMessage() {
			return mMessage;
		}
		public void setMessage(String mMessage) {
			this.mMessage = mMessage;
		}
		public String getTime() {
			return mTime;
		}
		public void setTime(String mTime) {
			this.mTime = mTime;
		}
	}
}
