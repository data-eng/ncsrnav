package com.example.ncsrparkingnavigation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

import java.util.ArrayList;
import java.util.Arrays;

public class MapActivity extends AppCompatActivity {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private LocationManager locationManager;
    private DirectedLocationOverlay myLocationOverlay;
    private GeoPoint startPoint;
    private GeoPoint endPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

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
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        //request permissions
        requestPermissionsIfNecessary(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        myLocationOverlay = new DirectedLocationOverlay(this);
        map.getOverlays().add(myLocationOverlay);
        map.invalidate();

        //get current location
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        Location location = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location == null)
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        //markers
        //starting point (ncsr gate for debugging, current location for prod)
        if(location == null) {
            startPoint = new GeoPoint(37.9990381, 23.8182062);
            myLocationOverlay.setEnabled(false);
        }
        else {
            startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            myLocationOverlay.setLocation(startPoint);
        }

        IMapController mapController = map.getController();
        mapController.setZoom(19.3);
        mapController.setCenter(startPoint);

        //found point to navigate to
        endPoint = new GeoPoint(foundLat, foundLon);
        Marker endMarker = new Marker(map);
        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(endMarker);
        map.invalidate();

        //road manager
        RoadManager roadManager = new OSRMRoadManager(this, Configuration.getInstance().getUserAgentValue());
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(startPoint);
        waypoints.add(endPoint);
        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        map.getOverlays().add(roadOverlay);
        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
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

//    @Override public void onLocationChanged(final Location pLoc) {
//        long currentTime = System.currentTimeMillis();
//        if (mIgnorer.shouldIgnore(pLoc.getProvider(), currentTime))
//            return;
//        double dT = currentTime - mLastTime;
//        if (dT < 100.0){
//            //Toast.makeText(this, pLoc.getProvider()+" dT="+dT, Toast.LENGTH_SHORT).show();
//            return;
//        }
//        mLastTime = currentTime;
//
//        GeoPoint newLocation = new GeoPoint(pLoc);
//        if (!myLocationOverlay.isEnabled()){
//            //we get the location for the first time:
//            myLocationOverlay.setEnabled(true);
//            map.getController().animateTo(newLocation);
//        }
//
//        GeoPoint prevLocation = myLocationOverlay.getLocation();
//        myLocationOverlay.setLocation(newLocation);
//        myLocationOverlay.setAccuracy((int)pLoc.getAccuracy());
//
//        if (prevLocation != null && pLoc.getProvider().equals(LocationManager.GPS_PROVIDER)){
//            mSpeed = pLoc.getSpeed() * 3.6;
//
//            //TODO: check if speed is not too small
//            if (mSpeed >= 0.1){
//                mAzimuthAngleSpeed = pLoc.getBearing();
//                myLocationOverlay.setBearing(mAzimuthAngleSpeed);
//            }
//        }
//
//        if (mTrackingMode){
//            //keep the map view centered on current location:
//            map.getController().animateTo(newLocation);
//            map.setMapOrientation(-mAzimuthAngleSpeed);
//        } else {
//            //just redraw the location overlay:
//            map.invalidate();
//        }
//    }
}