package kaistcs.android.dontkoala;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.Overlay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;

public class HomeLocation extends MapActivity  {
	private MapView mMapView;
	private EditText mMapAddr;
	private Button mBtnSearch;
	
	private Geocoder geoCoder;
	private SharedPreferences sharedPrefs;
	
	class MapMarker extends Overlay {
		private GeoPoint p;
		int k;
		
		MapMarker(GeoPoint _p) {
			p = _p;
		}
		
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			super.draw(canvas, mapView, shadow);                   
		 
			Point screenPts = new Point();
			mapView.getProjection().toPixels(p, screenPts);
			
			if (k % 60 == 0) {
				Log.d("draw", screenPts.x + "," + screenPts.y + "," + p);
			}
			k++;
			
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker);
			canvas.drawBitmap(bmp, screenPts.x-11, screenPts.y-55, null);         
			return true;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.homelocation);
		
		mMapView = (MapView)findViewById(R.id.mapview);	
		mMapAddr = (EditText)findViewById(R.id.mapAddress);
		mBtnSearch = (Button)findViewById(R.id.mapSearch);
		
		geoCoder = new Geocoder(this, Locale.getDefault());
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		mMapView.setBuiltInZoomControls(true);
		
		// Animate to...
		List<Overlay> mapOverlays = mMapView.getOverlays();
		mapOverlays.clear();
		
		GeoPoint curLoc = null;
		String homeLoc = sharedPrefs.getString("profile_home_location", "");
		
		if (homeLoc.isEmpty()) {
			// ...the last known location
			LocationManager locM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			String bestProvider = locM.getBestProvider(new Criteria(), true);
			Location l = locM.getLastKnownLocation(bestProvider);
			curLoc = new GeoPoint( (int)(l.getLatitude() * 1E6), (int)(l.getLongitude() * 1E6) );
		} else {
			// ...the home location
			String[] ar = homeLoc.split(",");
			curLoc = new GeoPoint(Integer.parseInt(ar[0]), Integer.parseInt(ar[1]));
			mapOverlays.add(new MapMarker(curLoc));
		}
		
		if (curLoc != null) {
			MapController ctrl = mMapView.getController();
			
			ctrl.animateTo(curLoc);
			ctrl.setZoom(15);
		}
		
		mapOverlays.add(new LongTouchDetector());
		mMapView.postInvalidate();

		mBtnSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					List<Address> addresses = geoCoder.getFromLocationName(mMapAddr.getText().toString(), 1);
					
					if (addresses.size() > 0) {
						mMapView.getController().animateTo (
								new GeoPoint(	(int)(addresses.get(0).getLatitude() * 1E6),
												(int)(addresses.get(0).getLongitude() * 1E6) ));
						mMapView.postInvalidate();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	class LongTouchDetector extends Overlay {
		private GeoPoint lastMapCenter;
		private Timer longpressTimer = new Timer();
		static final int LONGPRESS_THRESHOLD = 1000;
	    
		@Override
		public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				// Finger has touched screen.
				longpressTimer = new Timer();
				longpressTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						onLongTouchMap(event, mapView);
	                }
	            }, LONGPRESS_THRESHOLD);

				lastMapCenter = mapView.getMapCenter();
			}
		 
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (!mapView.getMapCenter().equals(lastMapCenter)) {
					// User is panning the map, this is no longpress
					longpressTimer.cancel();
				}
				lastMapCenter = mapView.getMapCenter();
			}
		 
			if (event.getAction() == MotionEvent.ACTION_UP) {
				// User has removed finger from map.
				longpressTimer.cancel();
			}
		 
			if (event.getPointerCount() > 1) {
				// This is a multitouch event, probably zooming.
				longpressTimer.cancel();
			}

			return false;
		}
	}
	
	public void onLongTouchMap(MotionEvent event, MapView mapView) {
		GeoPoint point = mapView.getProjection().fromPixels( (int)event.getX(), (int)event.getY() );
		sharedPrefs.edit().putString("profile_home_location", point.getLatitudeE6() + "," + point.getLongitudeE6()).apply();
		Log.d("onLongTouchMap", event.getX() + "," + event.getY() + "," + point);
		
		//List<Overlay> mapOverlays = mapView.getOverlays();
		//mapOverlays.clear();
		//mapOverlays.add(new LongTouchDetector());
		//mapOverlays.add(new MapMarker(point));
		//mapView.postInvalidate();
		
		finish();
	}
}
