package com.example.ncsrparkingnavigation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.View;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

import java.util.ArrayList;
import java.util.Arrays;

public class MapActivity extends AppCompatActivity implements LocationListener, MapView.OnFirstLayoutListener, SensorEventListener {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    protected MapView map = null;
    protected LocationManager locationManager;
    protected DirectedLocationOverlay myLocationOverlay;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    protected boolean mTrackingMode;
    float mAzimuthAngleSpeed = 0.0f;
    private final NetworkLocationIgnorer mIgnorer = new NetworkLocationIgnorer();
    long mLastTime = 0; // milliseconds
    double mSpeed = 0.0; // km/h

    BoundingBox mInitialBoundingBox = null;

    public MapActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }

        //get found lat and lon
        double foundLat, foundLon;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            foundLat = Double.parseDouble(extras.getString("lat"));
            foundLon = Double.parseDouble(extras.getString("lon"));
        } else {
            foundLat = 0.0;
            foundLon = 0.0;
        }

        //create context
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue("MyOwnUserAgent/1.0");
        setContentView(R.layout.activity_map);

        //create map
        map = (MapView) findViewById(R.id.map);
        map.setTilesScaledToDpi(true);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMinZoomLevel(1.0);
        map.setMaxZoomLevel(21.0);
        map.setMultiTouchControls(true);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //request permissions
        requestPermissionsIfNecessary(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        myLocationOverlay = new DirectedLocationOverlay(this);
        map.getOverlays().add(myLocationOverlay);

        //get current location
        if(savedInstanceState == null) {
            Location location = null;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null)
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location == null) {
                startPoint = new GeoPoint(37.9990381, 23.8182062);
                myLocationOverlay.setEnabled(false);
            } else {
                startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                onLocationChanged(location);
            }
        }
        else{
            myLocationOverlay.setLocation(savedInstanceState.getParcelable("location"));
            startPoint = savedInstanceState.getParcelable("start");
            endPoint = savedInstanceState.getParcelable("destination");
        }

        if (savedInstanceState != null){
            mTrackingMode = savedInstanceState.getBoolean("tracking_mode");
            updateUIWithTrackingMode();
        } else
            mTrackingMode = false;

        IMapController mapController = map.getController();
        mapController.setZoom(10.3);
        mapController.setCenter(startPoint);

        //found point to navigate to
//        endPoint = new GeoPoint(foundLat, foundLon);
//        Marker endMarker = new Marker(map);
//        endMarker.setPosition(endPoint);
//        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        map.getOverlays().add(endMarker);
//        map.invalidate();
//
//        //road manager
//        RoadManager roadManager = new OSRMRoadManager(this, Configuration.getInstance().getUserAgentValue());
//        ArrayList<GeoPoint> waypoints = new ArrayList<>();
//        waypoints.add(startPoint);
//        waypoints.add(endPoint);
//        Road road = roadManager.getRoad(waypoints);
//        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
//        map.getOverlays().add(roadOverlay);
//        map.invalidate();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("location", myLocationOverlay.getLocation());
        outState.putBoolean("tracking_mode", mTrackingMode);
        outState.putParcelable("start", startPoint);
        outState.putParcelable("destination", endPoint);
    }

    void updateUIWithTrackingMode(){
        if (mTrackingMode){
            if (myLocationOverlay.isEnabled()&& myLocationOverlay.getLocation() != null){
                map.getController().animateTo(myLocationOverlay.getLocation());
            }
            map.setMapOrientation(-mAzimuthAngleSpeed);
        } else {
            map.setMapOrientation(0.0f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        boolean isOneProvider = false;
        for(final String provider : locationManager.getProviders(true)){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(provider, 2 * 1000, 0.0f, this);
                isOneProvider = true;
            }
        }
        myLocationOverlay.setEnabled(isOneProvider);
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager.removeUpdates(this);
        }
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>(Arrays.asList(permissions).subList(0, grantResults.length));
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onLocationChanged(@NonNull final Location location) {
        long currentTime = System.currentTimeMillis();
        if(mIgnorer.shouldIgnore(location.getProvider(), currentTime))
            return;

        double dT = currentTime - mLastTime;
        if(dT < 100.0)
            return;
        mLastTime = currentTime;

        GeoPoint newLocation = new GeoPoint(location);
        if(!myLocationOverlay.isEnabled()){
            myLocationOverlay.setEnabled(true);
            map.getController().animateTo(newLocation);
        }

        GeoPoint prevLocation = myLocationOverlay.getLocation();
        myLocationOverlay.setLocation(newLocation);
        myLocationOverlay.setAccuracy((int)location.getAccuracy());

        if(prevLocation != null && location.getProvider().equals(LocationManager.GPS_PROVIDER)){
            mSpeed = location.getSpeed() * 3.6;
            if(mSpeed >= 0.1){
                mAzimuthAngleSpeed = location.getBearing();
                myLocationOverlay.setBearing(mAzimuthAngleSpeed);
            }
        }

        if (mTrackingMode){
            //keep the map view centered on current location:
            map.getController().animateTo(newLocation);
            map.setMapOrientation(-mAzimuthAngleSpeed);
        } else {
            //just redraw the location overlay:
            map.invalidate();
        }
    }

    @Override
    public void onFirstLayout(View v, int left, int top, int right, int bottom) {
        if (mInitialBoundingBox != null)
            map.zoomToBoundingBox(mInitialBoundingBox, false);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_ORIENTATION:
                if (mSpeed < 0.1){
					/* TODO Filter to implement...
					float azimuth = event.values[0];
					if (Math.abs(azimuth-mAzimuthOrientation)>2.0f){
						mAzimuthOrientation = azimuth;
						myLocationOverlay.setBearing(mAzimuthOrientation);
						if (mTrackingMode)
							map.setMapOrientation(-mAzimuthOrientation);
						else
							map.invalidate();
					}
					*/
                }
                //at higher speed, we use speed vector, not phone orientation.
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        myLocationOverlay.setAccuracy(accuracy);
        map.invalidate();
    }
}