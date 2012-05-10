package kaistcs.android.dontkoala;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GroupTab extends Activity implements SensorEventListener {
	
	//private Handler mHandler;
	private ProgressDialog mProgress;

	private TextView mResult;
	private doGrouping mTask;
	
	// bump variable initialize
	private long lastTime;
	private float speed;
	private float x, y, z;
	private float last_x, last_y, last_z;
	private static final int SHAKE_THRESHOLD = 1000;
/*	private static final int DATA_X = SensorManager.DATA_X;
	private static final int DATA_Y = SensorManager.DATA_Y;
	private static final int DATA_Z = SensorManager.DATA_Z;
*/	private SensorManager sensorMgr;
	private Sensor accelerometerSensor;
	int test_idx = 1;
	private boolean isShaking = false;
	
	// list variable initialize
	private CustomAdapter mCustomAdapter = null;	// Data를 관리해주는 Adapter
	private ArrayList<String> mArrayList = new ArrayList<String>();	// 제네릭(String)을 사용한 ArrayList
	private ListView mListView = null;
	private Button mSetGroupBtn = null;
	private Button mCheckAllBtn = null;
	private Button mCheckNoneBtn = null;
	
	
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // TODO Auto-generated method stub
        setContentView(R.layout.group);
        
        mResult = (TextView)findViewById(R.id.group_result);
        mResult.setFocusable(false);
        
		mListView = (ListView)findViewById(R.id.main_listview);
		
		mTask = new doGrouping();
		
		mProgress = new ProgressDialog(GroupTab.this);
		mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgress.setTitle("Loading...");
		mProgress.setIndeterminate(false);
		mProgress.setCancelable(true);
		mProgress.setButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				mTask.cancel(false);
			}
		});
		        
        mSetGroupBtn = (Button)findViewById(R.id.btn_make_group);
        mSetGroupBtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				
				new doGrouping().execute("홍길동", "01012345678");
				
			}
        });
        
        // start motion detection
        sensorMgr = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        //LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //GPSProvider gps = new GPSProvider(locManager);
        
        //double longitude = gps.getLongitude();
	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		
		if (accelerometerSensor != null) {
			sensorMgr.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
		}
	}
	
	@Override
	protected void onStop() {
		
		super.onStop();
		
		if (sensorMgr != null)
			sensorMgr.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			long currTime = System.currentTimeMillis();
			long diffTime = (currTime - lastTime);
			
			// only allow one update every 100ms
			if (diffTime > 100) {
				lastTime = currTime;
				
				x = event.values[SensorManager.DATA_X];
				y = event.values[SensorManager.DATA_Y];
				z = event.values[SensorManager.DATA_Z];
				
				speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
				
				if (speed > SHAKE_THRESHOLD) {
					// 쉐이킹이 감지된 경우 플래그를 참으로 변경
					//mResult.setText("SHAKING count : " + test_idx);
					Log.d("DEBUG", "count : "+test_idx);
				    test_idx++;
				    
				    isShaking = true;
				} else {
					isShaking = false;
					test_idx = 0;
				}
				
				last_x = event.values[SensorManager.DATA_X];
				last_y = event.values[SensorManager.DATA_Y];
				last_z = event.values[SensorManager.DATA_Z];
			}
		}
	}
	
	// AsyncTask
	private class doGrouping extends AsyncTask<String, String, String> {
		// AsyncTask가 execute 되자마자 UI스레드에서 실행됨
		@Override
		protected void onPreExecute() {
			// Background 작업 시작
			mProgress.setMessage("휴대폰을 흔들어주세요!!");
			mProgress.show();
			super.onPreExecute();
		}
		@Override
		protected String doInBackground(String... params) {
			// execute(...)로 실행되는 콜백
			// params[] : Name, Phone
			String group_list = null;
			for (int i=0; i<50 && !isShaking; i++) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (isShaking) {
				publishProgress("그룹 확인 중입니다. 잠시만 기다려주세요.");
				try {
					// 서버에 접속해서 세션 설정
					setGroup(params);
					// 다른 애들 조금 기다렸다가
					Thread.sleep(4000);
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				return "fail";
			}
			
			// 살아있는 세션만 긁어오기
			try {
				group_list = getGroup(params[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return group_list;
		}
		@Override
		protected void onProgressUpdate(String... progress) {
			// publishProgress(...)의 콜백
			mProgress.setMessage(progress[0].toString());
		}
		@Override
		protected void onPostExecute(String result) {
			// result[] : 결과로 받아온 그룹핑된 유저 목록 배열
			isShaking = false;
			mProgress.dismiss();
			//mResult.setText(result);
			// 그룹 생성 실패한 경우 (흔들지 않았거나 / 서버에 아무도 없거나)
			if (result == "fail" || result == null) {
				Toast.makeText(getBaseContext(), "다시 시도해주세요", Toast.LENGTH_SHORT).show();
			} else {
				// 그룹 생성에 성공한 경우 make 버튼을 disable 시키고
				mSetGroupBtn.setEnabled(false);
//				mArrayList.add(result);
				
				// arraylist에 하나씩 끊어서 추가해주고
				Log.d("DEBUG", result);
				String[] group = result.split("\\|");
		    	for (int i=0; i<group.length; i++) {
		    		mArrayList.add(group[i]);
		    	}
				
		    	// 리스트뷰에 뿌려준다
				mCustomAdapter = new CustomAdapter(GroupTab.this , mArrayList);
				mListView.setAdapter(mCustomAdapter);
				mListView.setOnItemClickListener(mItemClickListener);
				
				// 전체선택/해제 버튼 리스너 등록
				mCheckAllBtn = (Button)findViewById(R.id.btn_select_all);
				mCheckAllBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mCustomAdapter.setAllChecked(true);
						mCustomAdapter.notifyDataSetChanged();
						Toast.makeText(v.getContext(), "all", Toast.LENGTH_SHORT).show();
					}
				});
				mCheckNoneBtn = (Button)findViewById(R.id.btn_select_none);
				mCheckNoneBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mCustomAdapter.setAllChecked(false);
						mCustomAdapter.notifyDataSetChanged();
					}
				});
			}
			super.onPostExecute(result);
		}
		@Override
		protected void onCancelled() {
			isShaking = false;
			mProgress.dismiss();
			super.onCancelled();
		}
	}
	
	public void setGroup(String... params) throws IOException {
		
		ArrayList<NameValuePair> nameValue = new ArrayList<NameValuePair>();
		
		try {
			
			nameValue.add(new BasicNameValuePair("name", params[0]));
			nameValue.add(new BasicNameValuePair("phone", params[1]));
			
			HttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost("http://pemako.iptime.org/dontkoala/set_group.php");
			request.setEntity(new UrlEncodedFormEntity(nameValue, HTTP.UTF_8));
			client.execute(request);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getGroup(String param) throws IOException {
		
		ArrayList<NameValuePair> nameValue = new ArrayList<NameValuePair>();
		String response_result = null;
		try {	
			nameValue.add(new BasicNameValuePair("me", param));
			
			HttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost("http://pemako.iptime.org/dontkoala/get_group.php");
			request.setEntity(new UrlEncodedFormEntity(nameValue, HTTP.UTF_8));
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			InputStream is = entity.getContent();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(is, HTTP.UTF_8));
			
			String line = null;
			while ((line=br.readLine()) != null) {
				response_result = line;
				Log.d("DEBUG", line);
			}
			
			is.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return response_result;
	}
	
	// ListView 안에 Item을 클릭시에 호출되는 Listener
	private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
			Toast.makeText(getApplicationContext(), ""+(position+1), Toast.LENGTH_SHORT).show();
			
			mCustomAdapter.setChecked(position);
			// Data 변경시 호출 Adapter에 Data 변경 사실을 알려줘서 Update 함.
			mCustomAdapter.notifyDataSetChanged();
		}
	};

	// Custom Adapter
	class CustomAdapter extends BaseAdapter {

		private ViewHolder viewHolder = null;
		// 뷰를 새로 만들기 위한 Inflater
		private LayoutInflater inflater = null;
		private ArrayList<String> sArrayList = new ArrayList<String>();
		private boolean[] isCheckedConfrim;

		public CustomAdapter (Context c , ArrayList<String> mList){
			inflater = LayoutInflater.from(c);
			this.sArrayList = mList;
			// ArrayList Size 만큼의 boolean 배열을 만든다.
			// CheckBox의 true/false를 구별 하기 위해
			this.isCheckedConfrim = new boolean[sArrayList.size()];
		}

		// CheckBox를 모두 선택하는 메서드
		public void setAllChecked(boolean ischeked) {
			int tempSize = isCheckedConfrim.length;
			for(int a=0 ; a<tempSize ; a++){
				isCheckedConfrim[a] = ischeked;
			}
		}

		public void setChecked(int position) {
			isCheckedConfrim[position] = !isCheckedConfrim[position];
		}

		public ArrayList<Integer> getChecked(){
			int tempSize = isCheckedConfrim.length;
			ArrayList<Integer> mArrayList = new ArrayList<Integer>();
			for(int b=0 ; b<tempSize ; b++){
				if(isCheckedConfrim[b]){
					mArrayList.add(b);
				}
			}
			return mArrayList;
		}

		@Override
		public int getCount() { 
			return sArrayList.size();
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			// ConvertView가 null 일 경우
			View v = convertView;

			if( v == null ){
				viewHolder = new ViewHolder();
				// View를 inflater 시켜준다.
				v = inflater.inflate(R.layout.row, null);
				viewHolder.cBox = (CheckBox) v.findViewById(R.id.main_check_box);
				v.setTag(viewHolder);
			}

			else {
				viewHolder = (ViewHolder)v.getTag();
			}

			// CheckBox는 기본적으로 이벤트를 가지고 있기 때문에 ListView의 아이템
			// 클릭리즈너를 사용하기 위해서는 CheckBox의 이벤트를 없애 주어야 한다.
			viewHolder.cBox.setClickable(false);
			viewHolder.cBox.setFocusable(false);

			viewHolder.cBox.setText(sArrayList.get(position));
			// isCheckedConfrim 배열은 초기화시 모두 false로 초기화 되기때문에
			// 기본적으로 false로 초기화 시킬 수 있다.
			viewHolder.cBox.setChecked(isCheckedConfrim[position]);

			return v;
		}
	}

	class ViewHolder {
		// 새로운 Row에 들어갈 CheckBox
		private CheckBox cBox = null;
	}
	
}
