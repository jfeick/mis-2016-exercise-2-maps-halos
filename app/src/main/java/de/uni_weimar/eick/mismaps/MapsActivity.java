package de.uni_weimar.eick.mismaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/*
    runtime permission requests from Google API Samples:
    https://github.com/googlemaps/android-samples/blob/master/ApiDemos/app/src/main/java/com/example/mapdemo/MyLocationDemoActivity.java

 */

public class MapsActivity extends AppCompatActivity
        implements
        GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnInfoWindowCloseListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener
{
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;
    private EditText mEditText;
    private Button mDeleteButton;
    private int mMarkerCount = 0;

    private Marker mLastSelectedMarker;
    private List<MarkerOptions> mMarkerOptions = new ArrayList<MarkerOptions>();
    private List<Circle> mCircles = new ArrayList<Circle>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mEditText = (EditText) findViewById(R.id.editText);
        mDeleteButton = (Button) findViewById(R.id.deleteButton);
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                mMarkerCount = 0;
                mMarkerOptions.clear();
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();
            }
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        enableMyLocation();
        mMap.setOnMyLocationButtonClickListener(this);


        //mMap.getUiSettings().setZoomControlsEnabled(true);



        // Set listeners for marker events.  See the bottom of this class for their behavior.
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnInfoWindowCloseListener(this);
        mMap.setOnInfoWindowLongClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);

        // Override the default content description on the view, for accessibility mode.
        // Ideally this string would be localised.
        mMap.setContentDescription("Halo map effect app");

        restoreMarkers();

        // Pan to see all markers in view.
        // Cannot zoom to bounds until the map has a size.
        final View mapView = getSupportFragmentManager().findFragmentById(R.id.map).getView();
        if (mapView.getViewTreeObserver().isAlive()) {
            mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    if (mMarkerOptions.isEmpty()) {
                        Location loc = mMap.getMyLocation();
                        if (loc != null) {
                            LatLng point = new LatLng(loc.getLatitude(), loc.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
                        }

                    } else {
                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                        for (MarkerOptions markerOptions : mMarkerOptions) {
                            LatLng latLng = markerOptions.getPosition();
                            boundsBuilder.include(latLng);
                        }
                        LatLngBounds bounds = boundsBuilder.build();
                        mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                    }
                }
            });
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //Toast.makeText(this, "Long click on " + latLng.toString(), Toast.LENGTH_SHORT).show();

        String title = mEditText.getText().toString();
        mEditText.setText("");

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .draggable(true);
        mMap.addMarker(markerOptions);
        mMarkerOptions.add(markerOptions);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("title" + Integer.toString((mMarkerCount)), title);
        editor.putString("latitude" + Integer.toString((mMarkerCount)), Double.toString(latLng.latitude));
        editor.putString("longitude" + Integer.toString((mMarkerCount)), Double.toString(latLng.longitude));
        mMarkerCount += 1;
        editor.putInt("markerCount", mMarkerCount);
        editor.commit();
    }

    private void restoreMarkers() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        mMarkerCount = sharedPref.getInt("markerCount", 0);
        if (mMarkerCount > 0) {
            for (int i = 0; i < mMarkerCount; i++) {
                String title = sharedPref.getString("title" + i, "");
                double lat = Double.valueOf(sharedPref.getString("latitude" + i, "0"));
                double lng = Double.valueOf(sharedPref.getString("longitude" + i, "0"));
                //Toast.makeText(this, lat + "," + lng, Toast.LENGTH_LONG).show();

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(title)
                        .draggable(true);
                mMap.addMarker(markerOptions);
                mMarkerOptions.add(markerOptions);
            }

        }
    }

    private boolean checkReady() {
        if (mMap == null) {
            Toast.makeText(this, "Map not ready", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /** Called when the Clear button is clicked. */
    public void onClearMap(View view) {
        if (!checkReady()) {
            return;
        }
        mMap.clear();
    }

    /** Called when the Reset button is clicked. */
    public void onResetMap(View view) {
        if (!checkReady()) {
            return;
        }
        // Clear the map because we don't want duplicates of the markers.
        mMap.clear();
    }

    //
    // Marker related listeners.
    //

    @Override
    public boolean onMarkerClick(final Marker marker) {


        mLastSelectedMarker = marker;
        mEditText.setText(marker.getTitle());
        // We return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //Toast.makeText(this, "Click Info Window", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        //Toast.makeText(this, "Close Info Window", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        //Toast.makeText(this, "Info Window long click", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        updateHalos(bounds);
    }

    private void updateHalos(LatLngBounds bounds) {
        for(Circle circle : mCircles) {
                circle.remove();
        }
        mCircles.clear();

        double left = bounds.southwest.longitude;
        double right = bounds.northeast.longitude;
        double bottom = bounds.southwest.latitude;
        double top = bounds.northeast.latitude;

        LatLng boundsPosition;

        for (MarkerOptions markerOptions : mMarkerOptions) {
            LatLng latLng = markerOptions.getPosition();

            if (latLng.longitude < left) { // left region
                if (latLng.latitude > top) // top left
                    boundsPosition = new LatLng(top, left);
                else if (latLng.latitude < bottom) // bottom left
                    boundsPosition = bounds.southwest;
                else
                    boundsPosition = new LatLng(latLng.latitude, left);
            } else if (latLng.longitude > right) { // right region
                if (latLng.latitude > top) // top right
                    boundsPosition = bounds.northeast;
                else if (latLng.latitude < bottom) // bottom right
                    boundsPosition = new LatLng(bottom, right);
                else
                    boundsPosition = new LatLng(latLng.latitude, right);
            } else if (latLng.latitude > top) // top middle
                boundsPosition = new LatLng(top, latLng.longitude);
            else if (latLng.latitude < bottom) // bottom middle
                boundsPosition = new LatLng(bottom, latLng.longitude);
            else
                continue;

            float distance[] = new float[3];
            Location.distanceBetween(latLng.latitude, latLng.longitude,
                    boundsPosition.latitude, boundsPosition.longitude, distance);
            Circle circle = mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(distance[0] + 10.0f)
                        .strokeWidth(30.0f)
                        .strokeColor(Color.argb(125, 255, 0, 0)));
            mCircles.add(circle);

            /*
            if (!(bounds.contains(latLng))) {
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(100)
                        .strokeWidth(20.0f)
                        .strokeColor(Color.argb(125, 255, 0, 0)));
                mCircles.add(circle);
            }
            */
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);

        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "Moving to current position", Toast.LENGTH_SHORT).show();
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
}
