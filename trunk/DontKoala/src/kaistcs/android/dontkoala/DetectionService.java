package kaistcs.android.dontkoala;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

abstract class AbstractSensor {
	public static class TimedEvent {
		/** wall time based on System.currentTimeMillis() */
		public final long wallTime;
		/** real time based on SystemClock.elpasedRealTime() */
		public final long realTime;
		
		/** set to current time */
		public TimedEvent() {
			wallTime = System.currentTimeMillis();
			realTime = SystemClock.elapsedRealtime();
		}

		public TimedEvent(long wallTime, long realTime) {
			this.wallTime = wallTime;
			this.realTime = realTime;
		}
	}
	
	public abstract String getName();
	
	public abstract LinkedList<? extends TimedEvent> getData();
}

abstract class AbstractSituation {
	public abstract String getName();
	
	/** register sensors */
	public abstract void init(Map<String, AbstractSensor> sensors);
	/** detection start: use listener to notify */
	public abstract void start();
	/** detection stop */
	public abstract void stop();
	
	/** detection callback.
	 * @param location: null if it is not available.*/
	public interface OnDetectSituationListener {
		public void onDetectSituation(AbstractSituation s, long wallTime, Location location);
	}
	
	OnDetectSituationListener listener;
	
	public void setOnDetectSituation(OnDetectSituationListener l) {
		listener = l;
	}
}

class SensorGPS extends AbstractSensor implements LocationListener {
	public static class LocationEvent extends TimedEvent{
		private Location loc;
		
		public LocationEvent(Location loc) {
			super();
			this.loc = loc;
		}
		
		public LocationEvent(long wallTime, long realTime, Location loc) {
			super(wallTime, realTime);
			this.loc = loc;
		}
		
		/** If it is null, it means GPS fix didn't occur at that time. */
		public Location getLocation() {
			return loc;
		}
	}
	
	public static final String NAME = "GPS";
	LinkedList<LocationEvent> data;
	LinkedList<LocationEvent> temp;
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
		temp = new LinkedList<LocationEvent>();
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
		
		Log.i(getName(), "update()");
		
		updateResult = false;
		temp.clear();
		locM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this, updateThread.getLooper());
		
		try {
			updateThread.join(GPS_TIMEOUT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (updateResult == false) {
			Log.i(getName(), "No GPS Signal");
			data.addFirst(new LocationEvent(null));
		} else {
			// add location with minimum accuracy
			float minAcc = 100000;
			LocationEvent minEvent = null;
			
			for (LocationEvent tempEvent : temp) {
				if (tempEvent.getLocation().hasAccuracy() == true) {
					float curAcc = tempEvent.getLocation().getAccuracy();
					if (curAcc < minAcc) {
						minEvent = tempEvent;
						minAcc = curAcc;
					}
				}
			}
			
			// if accuracy is not available
			if (minEvent == null)
				minEvent = temp.getFirst();
			
			Location location = minEvent.getLocation();
			Log.i(getName(), 
					"lat: " + location.getLatitude() + ", " + "long: " + location.getLongitude() + ", " +
					"acc: " + location.hasAccuracy() + ", " + "acc_value: " + location.getAccuracy() +
					"spd: " + location.hasSpeed() + ", " + "spd_value: " + location.getSpeed());
			
			data.add(minEvent);
		}
		
		if (updateThread.isAlive())
			updateThread.quit();

		return updateResult;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		LocationEvent e = new LocationEvent(location);
		temp.addFirst(e);
		
		updateResult = true;
		
		// Try to achieve the given accuracy
		if (location.hasAccuracy() == false || location.getAccuracy() <= ACCURACY_THRESHOLD) {
			locM.removeUpdates(this);
			updateThread.quit();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		data.addFirst(new LocationEvent(null));
		locM.removeUpdates(this);
		updateThread.quit();
	}

	@Override
	public void onProviderEnabled(String provider) {
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status == LocationProvider.OUT_OF_SERVICE) {
			data.addFirst(new LocationEvent(null));
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
        Log.i(getName(), "beginListenTouch()");
        
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
        data.addFirst(new TimedEvent());
	}
	
    public void endListenTouch() {
    	Log.i(getName(), "endListenTouch()");
    	
    	try {
	        if (mTouchView != null)
	        	((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).removeView(mTouchView);
        } catch (Exception e) {
        	
        }
    	
    	mTouchView = null;
    }

	public boolean onTouch(View v, MotionEvent event) {
		Log.i(getName(), "onTouch()");
		data.addFirst(new TimedEvent());
		
		if (listener != null)
			listener.onTouch(v,  event);
        return false;
	}
}

class SensorBattery extends AbstractSensor {
	public static class BatteryEvent extends TimedEvent {
		public final int level;
		public final int scale;
		
		public BatteryEvent(int level, int scale) {
			super();
			this.level = level;
			this.scale = scale;
		}
		
		public BatteryEvent(long wallTime, long realTime, int level, int scale) {
			super(wallTime, realTime);
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
		
		Log.i(getName(), "level: " + level + ", " + "scale: " + scale);
        
        return new BatteryEvent(level, scale);
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
				Log.i(getName(), "onReceive()");
				Bundle bundle = intent.getExtras();        
				SmsMessage[] msgs = null;

				if (bundle != null)
				{
					Object[] pdus = (Object[]) bundle.get("pdus");
					msgs = new SmsMessage[pdus.length];            
					
					for (int i=0; i<msgs.length; i++)
						msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					
					if (listener != null)
						listener.onReceiveSMS(msgs);
				}
			}
		};
	}
	
	public void beginListenSMS(OnReceiveSMSListener l) {
		Log.i(getName(), "beginListenSMS()");
		listener = l;
		
		IntentFilter filter = new IntentFilter(ACTION);
		filter.setPriority(999);
        mContext.registerReceiver(smsReceiver, filter);        
	}
	
	public void endListenSMS() {
		Log.i(getName(), "endListenSMS()");
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

class GoHomeSituation extends AbstractSituation {
	public static final String NAME = "Go Home";
	private Context mContext;
	private UserInfo mUserInfo;
	private SensorGPS mSensorGPS;
	private AlarmManager mAlarmManager;
	private PendingIntent pendingTask;
	private BroadcastReceiver updateTask;
	
	private static final int REQUEST_CODE = 0x100;
	private static final String UPDATE_ACTION = "kaistcs.android.dontkoala.GoHome_UPDATE";
	
	/** Use the latest location if it is not too old */
	private static final int INTERLEAVE_DELAY = 60 * 1000;
	private static final int UPDATE_INTERVAL_MAX = 10 * 60 * 1000;
	private static final int UPDATE_INTERVAL_MIN = 0 * 1000;
	private static final int HOMELOC_DIST_THRESHOLD = 30;
	
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
	
	private class UpdateTask extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(getName(), "onReceive()");
			
			synchronized (mSensorGPS) {
				UserInfo.HomeLocationInfo home = mUserInfo.getHomeLocation();
				long delayMillis = UPDATE_INTERVAL_MAX;
				
				if (home != null) {
					LinkedList<SensorGPS.LocationEvent> l = mSensorGPS.getData();
					SensorGPS.LocationEvent e = null; 
					
					// Get the last known location
					if (l.isEmpty() == false)
						e = l.getFirst();
					
					// Update if it is not available or too old
					if (e == null || SystemClock.elapsedRealtime() - e.realTime >= INTERLEAVE_DELAY) {
						if (mSensorGPS.update() == true)
							e = mSensorGPS.getData().getFirst();
					}
					
					// If a GPS fix occurs...
					if (e != null) {
						Location loc = e.getLocation();
						float[] dist = new float[1];
						Location.distanceBetween(
								loc.getLatitude(), loc.getLongitude(),
								((double)home.getLatitudeE6()) / 1E6, ((double)home.getLongitudeE6()) / 1E6, dist);
						
						Log.i(GoHomeSituation.this.getName(), "dist to home: " + dist[0]);
						
						// Arrived home
						if (dist[0] <= HOMELOC_DIST_THRESHOLD) {
							Log.i(GoHomeSituation.this.getName(), "arrived home!" + dist[0]);
							if (listener != null)
								listener.onDetectSituation(GoHomeSituation.this, System.currentTimeMillis(), null);
							// No more update
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
								Log.i(GoHomeSituation.this.getName(), "adjust update time by speed: " + speed + "m/s");
								delayMillis = ((long) ( (dist[0]/2)/speed )) * 1000;
								if (delayMillis < UPDATE_INTERVAL_MIN/1000) {
									delayMillis = UPDATE_INTERVAL_MIN;
								}
							}
						}
					}
				}
				
				Log.i(GoHomeSituation.this.getName(), "next update in: " + delayMillis + "ms");
						
				if (mAlarmManager != null)
					mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, pendingTask);
			}
		}
	}

	@Override
	public void start() {
		Log.i(getName(), "start()");
		
		if (updateTask == null) {
			updateTask = new UpdateTask();
			mContext.registerReceiver(updateTask, new IntentFilter(UPDATE_ACTION));
			
			Intent intent = new Intent(UPDATE_ACTION);
			pendingTask = PendingIntent.getBroadcast(mContext, REQUEST_CODE, intent, 0);
			
			mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingTask);
		}
	}

	@Override
	public void stop() {
		Log.i(getName(), "stop()");
		
		if (updateTask != null) {
			mContext.unregisterReceiver(updateTask);
			updateTask = null;
			
			mAlarmManager.cancel(pendingTask);
			mAlarmManager = null;
			
			pendingTask.cancel();
			pendingTask = null;	
		}
	}
}
 
class KoalaSituation extends AbstractSituation implements OnTouchListener {
	public static final String NAME = "Koala";
	private Context mContext;
	private SensorGPS mSensorGPS;
	private SensorTouch mSensorTouch;
	private AlarmManager mAlarmManager;
	private PendingIntent pendingTask;
	private PendingIntent pendingTouch;
	private BroadcastReceiver updateTask;
	private BroadcastReceiver updateTouch;
	
	private static final int REQUEST_CODE_TASK = 0x101;
	private static final int REQUEST_CODE_TOUCH = 0x102;
	private static final String UPDATE_ACTION = "kaistcs.android.dontkoala.Koala_UPDATE";
	private static final String UPDATE_TOUCH = "kaistcs.android.dontkoala.Koala_UPDATE_TOUCH";
	
	/** Use the latest location if it is not too old */
	private static final int INTERLEAVE_DELAY = 60 * 1000;
	private static final int UPDATE_INTERVAL = 10 * 60 * 1000;
	private static final int KOALA_INTERVAL = 1 * 60 * 1000;
	private static final int MOVE_THRESHOLD = 80;

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
	
	private class UpdateTask extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(getName(), "UpdateTask.onReceive()");
			
			boolean bKoala = true;
			
			synchronized (mSensorTouch) {
				LinkedList<AbstractSensor.TimedEvent> l = mSensorTouch.getData();
				AbstractSensor.TimedEvent e = null;
				
				if (l.isEmpty() == false)
					e = l.getFirst();
				
				// Check for the last touch / detection beginning time
				if (e != null && SystemClock.elapsedRealtime() - e.realTime <= KOALA_INTERVAL) {
					Log.i(getName(), "No Koala, Reason: Touch");
					bKoala = false;
				}
			}
			
			synchronized (mSensorGPS) {
				LinkedList<SensorGPS.LocationEvent> l = mSensorGPS.getData();
				SensorGPS.LocationEvent e = null;
				
				if (l.isEmpty() == false)
					e = l.getFirst();
				
				if (e == null || SystemClock.elapsedRealtime() - e.realTime >= INTERLEAVE_DELAY) {
					if (mSensorGPS.update() == true)
						e = mSensorGPS.getData().getFirst();
				}
				
				// Check GPS fix failure (not outside) / Check if there's a move
				if (e.getLocation() == null) {
					bKoala = false;
					Log.i(getName(), "No Koala, Reason: No GPS Signal, Inside");
				} else {
					Iterator<SensorGPS.LocationEvent> it = l.iterator();
					
					while (it.hasNext()) {
						SensorGPS.LocationEvent event = it.next();
						
						if (SystemClock.elapsedRealtime() - event.realTime <= KOALA_INTERVAL) {
							if (event.getLocation() == null) {
								bKoala = false;
								break;
							} else {
								float[] dist = new float[1];
								Location.distanceBetween(
										e.getLocation().getLatitude(), e.getLocation().getLongitude(),
										event.getLocation().getLatitude(), event.getLocation().getLongitude(), dist);
								if (dist[0] > MOVE_THRESHOLD) {
									Log.i(getName(), "No Koala, Reason: Move for " + dist[0] + "m");
									bKoala = false;
								}
							}
						// clean location events
						} else {
							it.remove();
						}
					}
				}
				
				if (bKoala == true) {
					Log.i(getName(), "Koala Detected");
					if (listener != null)
						listener.onDetectSituation(KoalaSituation.this, System.currentTimeMillis(), e.getLocation());
				}
				
				long delayMillis = UPDATE_INTERVAL;
				
				Log.i(getName(), "next update in: " + delayMillis);
				if (mAlarmManager != null)
					mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, pendingTask);
			}
		}
	}
	
	private class UpdateTouch extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(getName(), "UpdateTouch.onReceive()");
			mSensorTouch.beginListenTouch(KoalaSituation.this);
		}
	}

	@Override
	public void start() {
		Log.i(getName(), "start()");
		
		if (updateTask == null) {
			updateTask = new UpdateTask();
			updateTouch = new UpdateTouch();
			mContext.registerReceiver(updateTask, new IntentFilter(UPDATE_ACTION));
			mContext.registerReceiver(updateTouch, new IntentFilter(UPDATE_TOUCH));
			
			Intent intent = new Intent(UPDATE_ACTION);
			pendingTask = PendingIntent.getBroadcast(mContext, REQUEST_CODE_TASK, intent, 0);
			
			Intent intent2 = new Intent(UPDATE_TOUCH);
			pendingTouch = PendingIntent.getBroadcast(mContext, REQUEST_CODE_TOUCH, intent2, 0);
			
			mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingTask);
			
			mSensorTouch.beginListenTouch(this);
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.i(getName(), "onTouch(), next touch in: " + KOALA_INTERVAL + "ms");
		mSensorTouch.endListenTouch();
		mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + KOALA_INTERVAL, pendingTouch);
		return false;
	}

	@Override
	public void stop() {
		Log.i(getName(), "stop()");
		if (updateTask != null) {
			mSensorTouch.endListenTouch();
			
			mContext.unregisterReceiver(updateTask);
			mContext.unregisterReceiver(updateTouch);
			updateTask = null;
			updateTouch = null;
			
			mAlarmManager.cancel(pendingTask);
			mAlarmManager.cancel(pendingTouch);
			mAlarmManager = null;
			
			pendingTask.cancel();
			pendingTouch.cancel();
			pendingTask = null;
			pendingTouch = null;
		}
	}	
}

class LowBatterySituation extends AbstractSituation {
	public static final String NAME = "Low Battery";
	private Context mContext;
	private SensorBattery mSensorBattery;
	private AlarmManager mAlarmManager;
	private PendingIntent pendingTask;
	private BroadcastReceiver updateTask;
	
	private static final int REQUEST_CODE = 0x103;
	private static final String UPDATE_ACTION = "kaistcs.android.dontkoala.LowBattery_UPDATE";
	
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
	
	private class UpdateTask extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(getName(), "onReceive()");
			SensorBattery.BatteryEvent e = mSensorBattery.get();
			
			if ((e.level * 100) / e.scale <= LOW_BATTERY_LEVEL) {
				Log.i(getName(), "5%");
				if (listener != null)
					listener.onDetectSituation(LowBatterySituation.this, System.currentTimeMillis(), null);
			}
			
			if (mAlarmManager != null)
				mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + UPDATE_INTERVAL, pendingTask);
		}
	}

	@Override
	public void start() {
		Log.i(getName(), "start()");
		
		if (updateTask == null) {
			updateTask = new UpdateTask();
			mContext.registerReceiver(updateTask, new IntentFilter(UPDATE_ACTION));
			
			Intent intent = new Intent(UPDATE_ACTION);
			pendingTask = PendingIntent.getBroadcast(mContext, REQUEST_CODE, intent, 0);
			
			mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingTask);
		}
	}

	@Override
	public void stop() {
		Log.i(getName(), "stop()");
		if (updateTask != null) {
			mContext.unregisterReceiver(updateTask);
			updateTask = null;
			
			mAlarmManager.cancel(pendingTask);
			mAlarmManager = null;
			
			pendingTask.cancel();
			pendingTask = null;
		}
	}
}

class LostPhoneSituation extends AbstractSituation implements SensorSMS.OnReceiveSMSListener {
	public static final String NAME = "Lost Phone";	
	private Context mContext;
	private SensorSMS mSensorSMS;
	private UserInfo userInfo;
	
	/** Use this instead of AbstractSituation.onDetectSituation */
	public interface OnLostPhoneListener {
		public void onLostPhone(long wallTime, String sender);
	}
	
	OnLostPhoneListener listener2;
	
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
			sensors.put(SensorSMS.NAME, mSensorSMS);
		}
		
		userInfo = new UserInfo(mContext);
	}

	@Override
	public void start() {
		Log.i(getName(), "start()");
		mSensorSMS.beginListenSMS(this);
	}

	@Override
	public void stop() {
		Log.i(getName(), "stop()");
		mSensorSMS.endListenSMS();
	}
	
	public void setOnLostPhoneListener(OnLostPhoneListener l) {
		listener2 = l;
	}

	@Override
	public void onReceiveSMS(SmsMessage[] msgs) {
		Log.i(getName(), "onReceiveSMS()");
		boolean sendSMS = userInfo.getSendLocation();
		String presetText = userInfo.getPresetText();
		
		if (sendSMS == true) {
			for (SmsMessage msg : msgs) {
				String addr = msg.getOriginatingAddress();
				String body = msg.getMessageBody();
				
				if (addr != null && body != null && body.contains(presetText)) {
					Log.i(getName(), "Lost phone detected");
					if (listener2 != null)
						listener2.onLostPhone(System.currentTimeMillis(), addr);
				}
			}
		}
	}
}

// TODO: resume service when restart
public class DetectionService extends Service implements AbstractSituation.OnDetectSituationListener, LostPhoneSituation.OnLostPhoneListener {
	private HashMap<String, AbstractSensor> sensors;
	private GoHomeSituation goHome;
	private KoalaSituation koala;
	private LowBatterySituation lowBattery;
	private LostPhoneSituation lostPhone;
	
	public static final String ACTION_START_DETECTION = "kaistcs.android.dontkoala.START_DETECTION";
	public static final String ACTION_STOP_DETECTION = "kaistcs.android.dontkoala.STOP_DETECTION";
	public static final String ACTION_START_LOST_PHONE = "kaistcs.android.dontkoala.START_LOST_PHONE";
	public static final String ACTION_STOP_LOST_PHONE = "kaistcs.android.dontkoala.STOP_LOST_PHONE";
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		sensors = new HashMap<String, AbstractSensor>();
		
		goHome = new GoHomeSituation(this);
		goHome.init(sensors);
		
		koala = new KoalaSituation(this);
		koala.init(sensors);
		
		lowBattery = new LowBatterySituation(this);
		lowBattery.init(sensors);
		
		lostPhone = new LostPhoneSituation(this);
		lostPhone.init(sensors);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = null;
		if (intent != null)
			action = intent.getAction();
		
		if (action != null) {
			if (action.equals(ACTION_START_DETECTION)) {
				goHome.setOnDetectSituation(this);
				koala.setOnDetectSituation(this);
				lowBattery.setOnDetectSituation(this);
				
				goHome.start();
				koala.start();
				lowBattery.start();
			} else if (action.equals(ACTION_STOP_DETECTION)) {
				goHome.stop();
				koala.stop();
				lowBattery.stop();
			} else if (action.equals(ACTION_START_LOST_PHONE)) {
				lostPhone.setOnLostPhoneListener(this);
				
				lostPhone.start();
			} else if (action.equals(ACTION_STOP_LOST_PHONE)) {
				lostPhone.stop();
			}
		}
		
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		goHome.stop();
		koala.stop();
		lowBattery.stop();
		lostPhone.stop();
	}

	@Override
	public void onDetectSituation(AbstractSituation s, long wallTime, Location location) {
		
	}

	@Override
	public void onLostPhone(long wallTime, String sender) {
		// FIXME: Just send GPS location
		SensorGPS sensorGPS = (SensorGPS) sensors.get(SensorGPS.NAME);
		if (sensorGPS.update() == true) {
			Location l = sensorGPS.getData().getFirst().getLocation();
			
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(sender, null, "Latitude: " + l.getLatitude() + ", Longitude: " + l.getLongitude(), null, null);
		}
	}
}