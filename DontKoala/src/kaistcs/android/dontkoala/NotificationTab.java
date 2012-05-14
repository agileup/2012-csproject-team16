package kaistcs.android.dontkoala;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
	private String mailAddr = "akamk87@gmail.com";
	
	// result variable initialize
	private ListView mResultView = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // TODO Auto-generated method stub
        setContentView(R.layout.notification);
        
        Button btnRegist = (Button)findViewById(R.id.btn_c2dm_regist);
        Button btnUnregist = (Button)findViewById(R.id.btn_c2dm_unregist);
        btnRegist.setOnClickListener(this);
        btnUnregist.setOnClickListener(this);
        
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
			// Android C2DM에 push 메세지를 받겠다는 메세지를 보내는 Intent
			// 정상적으로 등록이되면 Android C2DM Server 쪽에서 인증키를 보내줌
			// 받아온 인증키는 해당 어플리케이션과 해당 기기를 대표하는 인증키로 서버에서 메세지를 보낼때 사용
			// 서버에 등록을 할 때마다 인증키는 달라진다
			Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
			registrationIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));
			registrationIntent.putExtra("sender", mailAddr);
			startService(registrationIntent);
			break;
		case R.id.btn_c2dm_unregist:
			// Android C2DM에 push 메세지를 그만 받겠다는 메세지를 보내는 Intent
			Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
			unregIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));
			startService(unregIntent);
			break;
		default:
			break;
		}
	}
	
	private static final String HOST_PUSH = "http://android.apis.google.com/c2dm/send";
	private static final String AUTH = "DQAAAL4AAACX4LV_JVS2vQKGocgWJr5JiIYOTCyM-LLLouh_D90CA9GMvV-7nxNP3A6W0PoM7UdskeMDD6DtYIJlUHA_pUqLsgrQI37-_0cqXvfq3zeiJwi3kj_gXr7_l3P9kSUreYOznAvUEW7VArWDWCB3l-e7S_oVckDz8_4SefQxLK021U5M0OF2nd1SKZQeM0uRb-W99kMLEotrtrXY9yhjC467-G2ry7s5gZJaG-qjy53DUmAABAi_Q3lzydLhunPp3Qs";
	
	public void requestPush(String regId, String title, String msg) {
		try {
			StringBuffer postDataBuilder = new StringBuffer();
			
			// device 인증 키
			postDataBuilder.append("registration_id=" + regId);
			postDataBuilder.append("&collapse_key=1");
			postDataBuilder.append("&delay_while_idle=1");
			
			// message
			postDataBuilder.append("&data.msg=" + URLEncoder.encode(msg, "UTF-8"));
			byte[] postData = postDataBuilder.toString().getBytes("UTF8");
			URL url = new URL(HOST_PUSH);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencode");
			conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
			
			// AUTH 키
			conn.setRequestProperty("Authorization", "GoogleLogin auth="+AUTH);
			OutputStream out = conn.getOutputStream();
			out.write(postData);
			out.close();
			conn.getInputStream();
		} catch (Exception e) {
			// TODO: handle exception
		}
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

/*		
		// 뷰를 새로 만들기 위한 Inflater
		private LayoutInflater inflater = null;
		private ArrayList<ResultItem> sResultList = null;
		
		public ResultAdapter (Context c , ArrayList<ResultItem> mList){
			//inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater = LayoutInflater.from(c);
			this.sResultList = mList;
		}
		@Override
		public int getCount() { 
			return sResultList.size();
		}
		@Override
		public Object getItem(int arg0) {
			return sResultList.get(arg0);
		}
		@Override
		public long getItemId(int arg0) {
			return arg0;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			// ConvertView가 null 일 경우
			if( v == null ){
				// View를 inflater 시켜준다.
				v = inflater.inflate(R.id.noti_listview, null);
			} else {
				ImageView inflaterIcon = (ImageView)v.findViewById(R.id.resultIcon);
			}
			return v;
		}
		*/
	}

	class ResultItemView extends LinearLayout {
		private ImageView mIcon;
		private TextView mMessage;
		private TextView mTime;
		
		ResultItem aItem;
		
		public ResultItemView(Context context, ResultItem aItem) {
			super(context);
			// TODO Auto-generated constructor stub
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
