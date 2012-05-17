package kaistcs.android.dontkoala;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class NotificationDetail extends Activity {
	public static final String PERSON_NAME = "PERSON_NAME";
	public static final String DESCRIPTION = "DESCRIPTION";
	public static final String PHONE_NUMBER = "PHONE_NUMBER";
	public static final String LATITUDEE6 = "LATITUDEE6";
	public static final String LONGITUDEE6 = "LONGITUDEE6";
	
	private String personName;
	private String description;
	private String phoneNumber;
	private int latitudeE6;
	private int longitudeE6;
	
	private TextView mTvPerson;
	private TextView mTvDesc;
	private ImageButton mBtnCall;
	private MapView mMapView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notificationdetail);
		
		Intent i = getIntent();
		personName = i.getStringExtra(PERSON_NAME);
		description = i.getStringExtra(DESCRIPTION);
		phoneNumber = i.getStringExtra(PHONE_NUMBER);
		latitudeE6 = i.getIntExtra(LATITUDEE6, 0);
		longitudeE6 = i.getIntExtra(LONGITUDEE6, 0);
		
		mTvPerson = (TextView) findViewById(R.id.tvPerson);
		mTvDesc = (TextView) findViewById(R.id.tvDesc);
		mBtnCall = (ImageButton) findViewById(R.id.btnCall);
		mMapView = (MapView) findViewById(R.id.notificationMap);
		
		mTvPerson.setText(personName);
		mTvDesc.setText(description);
		
		MapController mapCtrl = mMapView.getController();
		List<Overlay> mapOverlays = mMapView.getOverlays();
		GeoPoint p = new GeoPoint(latitudeE6, longitudeE6);
		
		mapOverlays.clear();
		mapOverlays.add(new MapMarker(p));
		mapCtrl.animateTo(p);
		mapCtrl.setZoom(18);
		mMapView.postInvalidate();
		
		mBtnCall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
				startActivity(i);
			}
		});
	}
	
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
}
