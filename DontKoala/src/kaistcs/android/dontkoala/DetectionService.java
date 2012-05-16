package kaistcs.android.dontkoala;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Toast;

abstract class AbstractSensor {
	public static class TimedEvent {
		public final long time;

		TimedEvent(long time) {
			this.time = time;
		}
	}
	
	public abstract String getName();
	
	public abstract LinkedList<? extends TimedEvent> getData();
}

// TODO: provide info to onDetectSituation
abstract class AbstractSituation {
	public abstract String getName();
	
	/** register sensors */
	public abstract void init(Map<String, AbstractSensor> sensors);
	
	/** detection start: use listener to notify */
	public abstract void start();
	
	/** detection stop */
	public abstract void stop();
	
	public interface DetectSituationListener {
		public void onDetectSituation();
	}
	
	DetectSituationListener listener;
	
	public void setOnDetectSituation(DetectSituationListener l) {
		listener = l;
	}
}

class SensorGPS extends AbstractSensor implements LocationListener {
	public static class LocationEvent extends TimedEvent{
		private Location loc;
		
		public LocationEvent(long time, Location loc) {
			super(time);
			
			this.loc = loc;
		}
		
		/** If it is null, it means GPS fix timeout was occurred at that time. */
		public Location getLocation() {
			return loc;
		}
	}
	
	public static final String NAME = "GPS";
	LinkedList<LocationEvent> data;
	LocationManager locM;
	
	/** Timeout for GPS fix */
	private static final int GPS_TIMEOUT = 15 * 1000;
	
	/** Required accuracy for GPS fix */
	private static final int ACCURACY_THRESHOLD = 30;
	
	private HandlerThread updateThread;
	private boolean updateResult;
	
	SensorGPS(LocationManager l) {
		locM = l;
		data = new LinkedList<LocationEvent>();
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	/** Update GPS data.
	 * @return Whether there's a GPS fix. */
	public boolean update() {
		if (locM.isProviderEnabled(LocationManager.GPS_PROVIDER) == false)
			return false;
		
		updateThread = new HandlerThread("GPS Update Thread");
		updateThread.start();
		
		updateResult = false;
		locM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this, updateThread.getLooper());
		
		try {
			updateThread.join(GPS_TIMEOUT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (updateResult == false) {
			data.addFirst(new LocationEvent(System.currentTimeMillis(), null));
		}
		
		if (updateThread.isAlive())
			updateThread.quit();

		return updateResult;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		LocationEvent e = new LocationEvent(System.currentTimeMillis(), location);
		data.addFirst(e);
		
		updateResult = true;
		
		// Try to achieve the given accuracy
		if (location.hasAccuracy() == false || location.getAccuracy() <= ACCURACY_THRESHOLD) {
			locM.removeUpdates(this);
			updateThread.quit();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		locM.removeUpdates(this);
		updateThread.quit();
	}

	@Override
	public void onProviderEnabled(String provider) {
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status == LocationProvider.OUT_OF_SERVICE) {
			locM.removeUpdates(this);
			updateThread.quit();
		}
	}

	@Override
	public LinkedList<LocationEvent> getData() {
		return data;
	}
}

class SensorTouch extends AbstractSensor implements OnTouchListener {
	public static final String NAME = "Touch";
	private static final int ICE_CREAM_SANDWICH = 14;
	private Context mContext;
	private View mTouchView;
	private LinkedList<TimedEvent> data;
	private OnTouchListener listener;
	
	public SensorTouch(Context context) {
		mContext = context;
		data = new LinkedList<TimedEvent>();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public LinkedList<TimedEvent> getData() {
		return data;
	}
	
	public void beginListenTouch(OnTouchListener l) {
		listener = l;
		
		mTouchView = new View(mContext);
        mTouchView.setOnTouchListener(this);
        
        WindowManager.LayoutParams params;
        
        // TODO: address issue: one touch ignore in ICS.
        // ICS Version
        if (Build.VERSION.SDK_INT >= ICE_CREAM_SANDWICH) {
        	params = new WindowManager.LayoutParams(
            		WindowManager.LayoutParams.WRAP_CONTENT,
            		WindowManager.LayoutParams.WRAP_CONTENT,
            		WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            		//WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            		//WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            		//WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
            		WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            		PixelFormat.TRANSLUCENT);
        // Lower Versions
        } else {
        	params = new WindowManager.LayoutParams(
        		WindowManager.LayoutParams.WRAP_CONTENT,
        		WindowManager.LayoutParams.WRAP_CONTENT,
        		WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
        		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
        		WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        		PixelFormat.TRANSLUCENT);
        }
        
        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).addView(mTouchView, params);
        
        // save the beginning time
        data.addFirst(new TimedEvent(System.currentTimeMillis()));
	}
	
    public void endListenTouch() {
    	try {
	        if (mTouchView != null)
	        	((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).removeView(mTouchView);
        } catch (Exception e) {
        	
        }
    	
    	mTouchView = null;
    }

	public boolean onTouch(View v, MotionEvent event) {
		data.addFirst(new TimedEvent(System.currentTimeMillis()));
		
		if (listener != null)
			listener.onTouch(v,  event);
        return false;
	}
}

class SensorBattery extends AbstractSensor {
	public static class BatteryEvent extends TimedEvent {
		public final int level;
		public final int scale;
		
		public BatteryEvent(long time, int level, int scale) {
			super(time);
			
			this.level = level;
			this.scale = scale;
		}
	}
	
	public static final String NAME = "Battery";
	private Context mContext;
	
	public SensorBattery(Context context) {
		mContext = context;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	/** No data logging for this sensor.
	 * @return null */
	@Override
	public LinkedList<BatteryEvent> getData() {
		return null;
	}
	
	public BatteryEvent get() {
		Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        
        return new BatteryEvent(System.currentTimeMillis(), level, scale);
	}
}

class SensorSMS extends AbstractSensor {
	public static final String NAME = "SMS";
	private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
	private Context mContext;
	private BroadcastReceiver smsReceiver;
	private OnReceiveSMSListener listener;
	
	public static interface OnReceiveSMSListener {
		public void onReceiveSMS(SmsMessage[] msgs);
	}
	
	public SensorSMS(Context context) {
		mContext = context;
		smsReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle bundle = intent.getExtras();        
				SmsMessage[] msgs = null;

				if (bundle != null)
				{
					Object[] pdus = (Object[]) bundle.get("pdus");
					msgs = new SmsMessage[pdus.length];            
					
					if (listener != null)
						listener.onReceiveSMS(msgs);
				}
			}
		};
	}
	
	public void beginListenSMS(OnReceiveSMSListener l) {
		listener = l;
		
		IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION);

        mContext.registerReceiver(smsReceiver, filter);        
	}
	
	public void endListenSMS() {
		mContext.unregisterReceiver(smsReceiver);
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	/** No data logging for this sensor.
	 * @return null */
	@Override
	public LinkedList<? extends TimedEvent> getData() {
		return null;
	}
}

// TODO: clean sensor data
class GoHomeSituation extends AbstractSituation {
	public static final String NAME = "Go Home";
	private Context mContext;
	private UserInfo mUserInfo;
	private SensorGPS mSensorGPS;
	private Handler handler;
	private Runnable updateTask;
	
	/** Use the latest location if it is not too old */
	private static final int INTERLEAVE_DELAY = 60 * 1000;
	private static final int UPDATE_INTERVAL_MAX = 10 * 60 * 1000;
	private static final int UPDATE_INTERVAL_MIN = 0 * 1000;
	private static final int HOMELOC_DIST_THRESHOLD = 10;
	
	public GoHomeSituation(Context context) {
		mContext = context;
		mUserInfo = new UserInfo(mContext);
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void init(Map<String, AbstractSensor> sensors) {
		mSensorGPS = (SensorGPS) sensors.get(SensorGPS.NAME);
		
		if (mSensorGPS == null) {
			mSensorGPS = new SensorGPS( (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE) );
			sensors.put(SensorGPS.NAME, mSensorGPS);
		}
	}

	@Override
	public void start() {
		updateTask = new Runnable() {
			@Override
			public void run() {
				synchronized (mSensorGPS) {
					LinkedList<SensorGPS.LocationEvent> l = mSensorGPS.getData();
					SensorGPS.LocationEvent e = null; 
					
					// Get the last known location
					if (l.isEmpty() == false)
						e = l.getFirst();
					
					// Update if it is not available or too old
					if (e == null || System.currentTimeMillis() - e.time >= INTERLEAVE_DELAY) {
						if (mSensorGPS.update() == true)
							e = mSensorGPS.getData().getFirst();
					}
					
					long delayMillis = UPDATE_INTERVAL_MAX;
					
					// If a GPS fix occurs...
					if (e != null) {
						Location loc = e.getLocation();
						float[] dist = new float[1];
						UserInfo.HomeLocationInfo home = mUserInfo.getHomeLocation();
						Location.distanceBetween(
								loc.getLatitude(), loc.getLongitude(),
								((double)home.getLatitudeE6()) / 1E6, ((double)home.getLongitudeE6()) / 1E6, dist);
						
						// Arrived home
						if (dist[0] <= HOMELOC_DIST_THRESHOLD) {
							if (listener != null)
								listener.onDetectSituation();
							return;
						}
						
						// Adjust delayMillis by the speed if home is getting closer
						if (loc.hasSpeed() == true) {
							float[] oldDist = new float[1];
							oldDist[0] = -1;	// If oldDist is not available, do not adjust
							
							Iterator<SensorGPS.LocationEvent> it = l.iterator();
							if (it.hasNext() == true) it.next();
							while (it.hasNext()) {
								SensorGPS.LocationEvent cur = it.next();
								if (cur.getLocation() != null) {
									Location.distanceBetween(
											cur.getLocation().getLatitude(), cur.getLocation().getLongitude(),
											((double)home.getLatitudeE6()) / 1E6, ((double)home.getLongitudeE6()) / 1E6, oldDist);
								}
							}
							
							float speed = loc.getSpeed();
							if (dist[0] < oldDist[0] && dist[0]/speed < UPDATE_INTERVAL_MAX/1000) {
								delayMillis = ((long) ( (dist[0]/2)/speed )) * 1000;
								if (delayMillis < UPDATE_INTERVAL_MIN/1000) {
									delayMillis = UPDATE_INTERVAL_MIN;
								}
							}
						}
					}
							
					if (handler != null)
						handler.postDelayed(this, delayMillis);
				}
			}
		};
		
		handler = new Handler();
		handler.postDelayed(updateTask, 0);
	}

	@Override
	public void stop() {
		if (updateTask != null) {
			handler.removeCallbacks(updateTask);
			updateTask = null;
			handler = null;
		}
	}
}

// TODO: clean sensor data 
class KoalaSituation extends AbstractSituation implements OnTouchListener {
	public static final String NAME = "Koala";
	private Context mContext;
	private SensorGPS mSensorGPS;
	private SensorTouch mSensorTouch;
	private Handler handler;
	private Runnable updateTask;
	private Runnable updateTouch;
	
	/** Use the latest location if it is not too old */
	private static final int INTERLEAVE_DELAY = 60 * 1000;
	private static final int UPDATE_INTERVAL = 10 * 60 * 1000;
	private static final int KOALA_INTERVAL = 40 * 60 * 1000;

	public KoalaSituation(Context context) {
		mContext = context;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void init(Map<String, AbstractSensor> sensors) {
		mSensorGPS = (SensorGPS) sensors.get(SensorGPS.NAME);
		
		if (mSensorGPS == null) {
			mSensorGPS = new SensorGPS( (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE) );
			sensors.put(SensorGPS.NAME, mSensorGPS);
		}
		
		mSensorTouch = (SensorTouch) sensors.get(SensorTouch.NAME);
		
		if (mSensorTouch == null) {
			mSensorTouch = new SensorTouch(mContext);
			sensors.put(SensorTouch.NAME, mSensorTouch);
		}
	}

	@Override
	public void start() {
		updateTask = new Runnable() {
			@Override
			public void run() {
				boolean bKoala = true;
				
				synchronized (mSensorTouch) {
					LinkedList<AbstractSensor.TimedEvent> l = mSensorTouch.getData();
					AbstractSensor.TimedEvent e = null;
					
					if (l.isEmpty() == false)
						e = l.getFirst();
					
					// Check for the last touch / detection beginning time
					if (e != null && System.currentTimeMillis() - e.time <= KOALA_INTERVAL) {
						bKoala = false;
					}
				}
				
				synchronized (mSensorGPS) {
					LinkedList<SensorGPS.LocationEvent> l = mSensorGPS.getData();
					SensorGPS.LocationEvent e = null;
					
					if (l.isEmpty() == false)
						e = l.getFirst();
					
					if (e == null || System.currentTimeMillis() - e.time >= INTERLEAVE_DELAY) {
						if (mSensorGPS.update() == true)
							e = mSensorGPS.getData().getFirst();
					}
					
					// Check GPS fix failure (not outside)
					for (SensorGPS.LocationEvent event : l) {
						if (System.currentTimeMillis() - event.time <= KOALA_INTERVAL) {
							if (event.getLocation() == null)
								bKoala = false;
						}
					}
					
					if (bKoala == true) {
						if (listener != null)
							listener.onDetectSituation();
					}
					
					long delayMillis = UPDATE_INTERVAL;
										
					if (handler != null)
						handler.postDelayed(this, delayMillis);
				}
			}
		};
		
		updateTouch = new Runnable(){
			@Override
			public void run() {
				mSensorTouch.beginListenTouch(KoalaSituation.this);
			}
		};
		
		handler = new Handler();
		handler.postDelayed(updateTask, 0);
		
		mSensorTouch.beginListenTouch(this);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mSensorTouch.endListenTouch();
		handler.postDelayed(updateTouch, KOALA_INTERVAL);
		return false;
	}

	@Override
	public void stop() {
		if (updateTask != null) {
			handler.removeCallbacks(updateTask);
			updateTask = null;
		}
		
		if (updateTouch != null) {
			handler.removeCallbacks(updateTouch);
			updateTouch = null;
			mSensorTouch.endListenTouch();
		}
		
		handler = null;
	}	
}

class LowBatterySituation extends AbstractSituation {
	public static final String NAME = "Low Battery";
	private Context mContext;
	private SensorBattery mSensorBattery;
	private Runnable updateTask;
	private Handler handler;
	private static final int UPDATE_INTERVAL = 10 * 60 * 1000;
	private static final int LOW_BATTERY_LEVEL = 5;
	
	public LowBatterySituation(Context context) {
		mContext = context;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void init(Map<String, AbstractSensor> sensors) {
		mSensorBattery = (SensorBattery) sensors.get(SensorBattery.NAME);
		
		if (mSensorBattery == null) {
			mSensorBattery = new SensorBattery(mContext);
			sensors.put(SensorBattery.NAME, mSensorBattery);
		}
	}

	@Override
	public void start() {
		updateTask = new Runnable() {
			@Override
			public void run() {
				SensorBattery.BatteryEvent e = mSensorBattery.get();
				
				if ((e.level * 100) / e.scale <= LOW_BATTERY_LEVEL) {
					if (listener != null)
					listener.onDetectSituation();
				}
				
				if (handler != null)
					handler.postDelayed(this, UPDATE_INTERVAL);
			}
		};
		
		handler = new Handler();
		handler.postDelayed(updateTask, 0);
	}

	@Override
	public void stop() {
		handler.removeCallbacks(updateTask);
		updateTask = null;
		handler = null;
	}
}

class LostPhoneSituation extends AbstractSituation implements SensorSMS.OnReceiveSMSListener {
	public static final String NAME = "Lost Phone";	
	private Context mContext;
	private SensorSMS mSensorSMS;
	private UserInfo userInfo;
	
	public LostPhoneSituation(Context context) {
		mContext = context;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void init(Map<String, AbstractSensor> sensors) {
		mSensorSMS = (SensorSMS) sensors.get(SensorSMS.NAME);
		
		if (mSensorSMS == null) {
			mSensorSMS = new SensorSMS(mContext);
			sensors.put(SensorBattery.NAME, mSensorSMS);
		}
		
		userInfo = new UserInfo(mContext);
	}

	@Override
	public void start() {
		mSensorSMS.beginListenSMS(this);
	}

	@Override
	public void stop() {
		mSensorSMS.endListenSMS();
	}

	@Override
	public void onReceiveSMS(SmsMessage[] msgs) {
		boolean sendSMS = userInfo.getSendLocation();
		String presetText = userInfo.getPresetText();
		
		if (sendSMS == true) {
			for (SmsMessage msg : msgs) {
				String addr = msg.getOriginatingAddress();
				String body = msg.getMessageBody();
				
				if (addr != null && body != null && body.contains(presetText)) {
					if (listener != null)
						listener.onDetectSituation();
				}
			}
		}
	}
}

// TODO: wake lock / alarm manager
// TODO: lostphonesituation / bind service
public class DetectionService extends Service {
	private HashMap<String, AbstractSensor> sensors;
	private GoHomeSituation goHome;
	private KoalaSituation koala;
	private LowBatterySituation low;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		sensors = new HashMap<String, AbstractSensor>();

		goHome = new GoHomeSituation(this);
		goHome.init(sensors);
		goHome.start();
		goHome.setOnDetectSituation(new AbstractSituation.DetectSituationListener() {
			@Override
			public void onDetectSituation() {
				Toast.makeText(DetectionService.this,"HOME", Toast.LENGTH_SHORT).show();
			}
		});
		
		koala = new KoalaSituation(this);
		koala.init(sensors);
		koala.start();
		
		low = new LowBatterySituation(this);
		low.init(sensors);
		low.start();
	}
	
	@Override
	public void onDestroy() {
		goHome.stop();
		koala.stop();
		low.stop();
	}
}
