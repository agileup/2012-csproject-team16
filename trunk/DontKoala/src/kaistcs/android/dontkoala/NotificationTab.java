package kaistcs.android.dontkoala;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
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
	
	private ResultAdapter adapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
        setContentView(R.layout.notification);
/*        
        Button btnRegist = (Button)findViewById(R.id.btn_c2dm_regist);
        Button btnUnregist = (Button)findViewById(R.id.btn_sms_send);
        btnRegist.setOnClickListener(this);
        btnUnregist.setOnClickListener(this);
        Button btnPush = (Button)findViewById(R.id.btn_push);
        btnPush.setOnClickListener(this);
*/        
        mResultView = (ListView)findViewById(R.id.noti_listview);
        
        //adapter = new ResultAdapter(this);
        //Resources res = getResources();
        //adapter.addItem(new ResultItem(res.getDrawable(R.drawable.safe), "최민기님이 안전하게 귀가했습니다", "12/05/17 PM 11:31"));
        //adapter.addItem(new ResultItem(res.getDrawable(R.drawable.warning), "최진길님이 KOALA가 됐습니다!", "12/05/18 AM 01:19"));
		//adapter.addItem(new ResultItem(res.getDrawable(R.drawable.safe), "최우혁님이 안전하게 귀가했습니다", "12/05/09 PM 10:03"));		
		//mResultView.setAdapter(adapter);
		mResultView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				//IconTextItem  Item = (IconTextItem)adapter.getItem(arg2);
				//String[] data = Item.getData();						
				//Toast.makeText(getApplicationContext(),"국가 : " + data[0] + "\n" + "Nation : "+ data[1] +"\n"+ "피파순위 : "+ data[2] , 1).show();
				//Toast.makeText(getApplicationContext(), "상세보기로 넘어갑니다 (준비중)", 1000).show();
				Intent detail = new Intent(getApplicationContext(), NotificationDetail.class);
				detail.putExtra(NotificationDetail.PERSON_NAME, "최진길");
				detail.putExtra(NotificationDetail.DESCRIPTION, "대한민국 대전광역시 온천2동");
				detail.putExtra(NotificationDetail.PHONE_NUMBER, "01043888128");
				detail.putExtra(NotificationDetail.LATITUDEE6, 36366642);
				detail.putExtra(NotificationDetail.LONGITUDEE6, 127357326);
				startActivity(detail);
			}
		});	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		String response_result = DownloadHtml("http://flclab.iptime.org/dontkoala/get_notification.php");
		//Toast.makeText(getApplicationContext(), response_result, Toast.LENGTH_SHORT).show();
		
		JSONArray jArray;
		
		adapter = new ResultAdapter(this);
		
		// parsing data
		String json_name;
		int json_status;	// 꽐라:0, 귀가:1, 
		String json_time;	// 12/05/17 PM 11:31
		int json_lat;
		int json_lon;
		try {
			jArray = new JSONArray(response_result);
			JSONObject json_data = null;
			for (int i=0; i<jArray.length(); i++) {
				
				json_data = jArray.getJSONObject(i);
				json_name = json_data.getString("name");
				json_status = json_data.getInt("status");
				json_time = json_data.getString("create_time");
				//json_lat = json_data.getInt("latitude");
				//json_lon = json_data.getInt("longitude");
				
				//Toast.makeText(getApplicationContext(), json_name + " / " + json_time, Toast.LENGTH_SHORT).show();
				
				if (json_status == 0) {
					String txt = json_name+"님이 KOALA가 됐습니다!";
					adapter.addItem(new ResultItem(getResources().getDrawable(R.drawable.warning), txt, json_time));
				}
				else if (json_status == 1) {
					String txt = json_name+"님이 안전하게 귀가했습니다";
					adapter.addItem(new ResultItem(getResources().getDrawable(R.drawable.safe), txt, json_time));
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		mResultView.setAdapter(adapter);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		NotificationManager nm;
		nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(1234);
		//Toast.makeText(getApplicationContext(), "NotificationTab::onNewIntent()", Toast.LENGTH_SHORT).show();
		
		//String sendNum = intent.getStringExtra("sendNum");
		//Toast.makeText(getApplicationContext(), "<"+sendNum+">", Toast.LENGTH_SHORT).show();
	}
	
	String DownloadHtml(String addr)
	{
		StringBuilder jsonHtml = new StringBuilder();
		try
		{
			// 연결 url 설정
			URL url = new URL(addr);
			// 커넥션 객체 생성
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			// 연결되었으면.
			if(conn != null){
				conn.setConnectTimeout(10000);
				conn.setUseCaches(false);
				// 연결되었음 코드가 리턴되면.
				if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
					for(;;){
						// 웹상에 보여지는 텍스트를 라인단위로 읽어 저장.  
						String line = br.readLine();
						if(line == null) break;
						// 저장된 텍스트 라인을 jsonHtml에 붙여넣음
						jsonHtml.append(line + "\n");
					}
					br.close();
				}
				conn.disconnect();
			}
		} catch(Exception ex) {
			Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		return jsonHtml.toString();
	}

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

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
}
