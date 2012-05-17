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

import android.content.Context;
import android.content.Intent;
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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class HomeLocation extends MapActivity  {
	private MapView mMapView;
	private EditText mMapAddr;
	private Button mBtnSearch;
	
	private Geocoder geoCoder;
	
	private class MapMarker extends Overlay {
		public GeoPoint p;
		Bitmap markerBmp;
		
		MapMarker(GeoPoint _p) {
			p = _p;
			markerBmp = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
		}
		
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			super.draw(canvas, mapView, shadow);
		 
			Point screenPts = new Point();
			mapView.getProjection().toPixels(p, screenPts);
			
			canvas.drawBitmap(markerBmp, screenPts.x-markerBmp.getWidth()/2, screenPts.y-markerBmp.getHeight(), null);         
			return true;
		}
	}
	
	private class LongTouchDetector extends Overlay {
		private GeoPoint lastMapCenter;
		private Timer longpressTimer = new Timer();
		static final int LONGPRESS_THRESHOLD = 500;
	    
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
	
	private MapMarker mapMarker;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.homelocation);
		
		mMapView = (MapView)findViewById(R.id.mapview);	
		mMapAddr = (EditText)findViewById(R.id.mapAddress);
		mBtnSearch = (Button)findViewById(R.id.mapSearch);
		
		geoCoder = new Geocoder(this, Locale.getDefault());
		
		mMapView.setBuiltInZoomControls(true);

		List<Overlay> mapOverlays = mMapView.getOverlays();
		mapOverlays.clear();
		mapOverlays.add(new LongTouchDetector());
		
		// Animate to...
		GeoPoint curLoc = null;
		UserInfo.HomeLocationInfo homeLocIn = getIntent().getParcelableExtra("HomeLocationIn");
		
		if (homeLocIn == null) {
			// ...the last known location
			LocationManager locM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			String bestProvider = locM.getBestProvider(new Criteria(), true);
			if (bestProvider != null) {
				Location l = locM.getLastKnownLocation(bestProvider);
				curLoc = new GeoPoint( (int)(l.getLatitude() * 1E6), (int)(l.getLongitude() * 1E6) );
			}
		} else {
			// ...the home location
			curLoc = new GeoPoint(homeLocIn.getLatitudeE6(), homeLocIn.getLongitudeE6());
			mapMarker = new MapMarker(curLoc);
			mapOverlays.add(mapMarker);
		}
		
		if (curLoc != null) {
			MapController ctrl = mMapView.getController();
			
			ctrl.animateTo(curLoc);
			ctrl.setZoom(18);
		}
		
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
		
		mMapAddr.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || 
						(event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					mBtnSearch.performClick();
				}
				return true;
			}
		});
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public void onLongTouchMap(MotionEvent event, MapView mapView) {
	    View contentView = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
		
		GeoPoint point = mapView.getProjection().fromPixels(
				(int)(event.getRawX()-contentView.getLeft()-mapView.getLeft()),
				(int)(event.getRawY()-contentView.getTop()-mapView.getTop()) );
		
		Intent intent = new Intent();
		List<Address> addresses = null;
		String address = "";
		
		try {
			addresses = geoCoder.getFromLocation( ((double)point.getLatitudeE6())/1E6, ((double)point.getLongitudeE6())/1E6, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (addresses != null && addresses.size() > 0) {
			address = addresses.get(0).getAddressLine(0);
		}
		
		UserInfo.HomeLocationInfo homeLocOut = new UserInfo.HomeLocationInfo(point.getLatitudeE6(), point.getLongitudeE6(), address);
		intent.putExtra("HomeLocationOut", homeLocOut);
		setResult(RESULT_OK, intent);

		finish();
	}
}
