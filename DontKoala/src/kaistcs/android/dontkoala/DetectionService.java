package kaistcs.android.dontkoala;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
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

// TODO: adjust accuracy, implement lock
class SensorGPS extends AbstractSensor implements LocationListener {
	public static class LocationEvent extends TimedEvent{
		private Location loc;
		
		public LocationEvent(long time, Location loc) {
			super(time);
			
			this.loc = loc;
		}
		
		public Location getLocation() {
			return loc;
		}
		
		/** flag for unnecesary events */
		private boolean lock;
		
		public boolean getLock() {
			return lock;
		}
		
		protected void clearLock() {
			lock = false;
		}
		
		public void acquireLock() {
			lock = true;
		}
	}
	
	public static final String NAME = "GPS";
	LinkedList<LocationEvent> data;
	LocationManager locM;
	private static final int GPS_TIMEOUT = 15 * 1000;
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
		
		if (updateThread.isAlive())
			updateThread.quit();

		return updateResult;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		LocationEvent e = new LocationEvent(System.currentTimeMillis(), location);
		data.addFirst(e);
		
		updateResult = true;
		
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
	
	public void cleanData() {
		LinkedList<LocationEvent> l = getData();
		Iterator<LocationEvent> it = l.iterator();
		
		while (it.hasNext()) {
			LocationEvent e = it.next();
			if (e.getLock() == false) {
				it.remove();
			}
		}
	}
}

class SensorTouch extends AbstractSensor implements OnTouchListener {
	public static final String NAME = "Touch";
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
		mTouchView = new View(mContext);
        mTouchView.setOnTouchListener(this);
        
        // TODO: address issue: one touch ignore in ICS.
        // ICS Version
        /* WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        		WindowManager.LayoutParams.WRAP_CONTENT,
        		WindowManager.LayoutParams.WRAP_CONTENT,
        		WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
        		//WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
        		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
        		//WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
        		//WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
        		WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        		PixelFormat.TRANSLUCENT); */
        
        // Lower Version
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        		WindowManager.LayoutParams.WRAP_CONTENT,
        		WindowManager.LayoutParams.WRAP_CONTENT,
        		WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
        		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
        		WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        		PixelFormat.TRANSLUCENT);
        
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        
        listener = l;
        wm.addView(mTouchView, params);
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
		data.add(new TimedEvent(System.currentTimeMillis()));
		
		if (listener != null)
			listener.onTouch(v,  event);
        return false;
	}
}

class GoHomeSituation extends AbstractSituation {
	public static final String NAME = "Go Home";
	private Context mContext;
	private UserInfo mUserInfo;
	private SensorGPS mSensorGPS;
	private Handler handler;
	private Runnable updateTask;
	private static final int INTERLEAVE_DELAY = 60 * 1000;
	private static final int UPDATE_DELAY_MAX = 10 * 60 * 1000;
	private static final int UPDATE_DELAY_MIN = 0 * 1000;
	private static final int HOMELOC_ERROR = 10;
	
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
					
					if (l.isEmpty() == false)
						e = l.getFirst();
					
					if (e == null || System.currentTimeMillis() - e.time >= INTERLEAVE_DELAY) {
						mSensorGPS.update();
						e = mSensorGPS.getData().getFirst();
					}
					
					Location loc = e.getLocation();
					long delayMillis = UPDATE_DELAY_MAX;
					float[] dist = new float[1];
					
					UserInfo.HomeLocationInfo home = mUserInfo.getHomeLocation();
					Location.distanceBetween(
							loc.getLatitude(), loc.getLongitude(),
							((double)home.getLatitudeE6()) / 1E6, ((double)home.getLongitudeE6()) / 1E6, dist);
					
					if (dist[0] <= HOMELOC_ERROR) {
						listener.onDetectSituation();
						return;
					}
					
					if (loc.hasSpeed() == true) {
						float speed = loc.getSpeed();
						
						if (dist[0]/speed < UPDATE_DELAY_MAX/1000) {
							delayMillis = ((long) ( (dist[0]/2)/speed )) * 1000;
							if (delayMillis < UPDATE_DELAY_MIN/1000) {
								delayMillis = UPDATE_DELAY_MIN;
							}
						}
					}
					
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

class KoalaSituation extends AbstractSituation {
	public static final String NAME = "Koala";
	private Context mContext;
	private SensorGPS mSensorGPS;
	private SensorTouch mSensorTouch;
	
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
}

public class DetectionService extends Service {
	private GoHomeSituation goHome;
	private HashMap<String, AbstractSensor> sensors;
	
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
	}
	
	@Override
	public void onDestroy() {
		goHome.stop();
	}
}
