package com.piedpiper.navi;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPEN_GL_VERSION = 3.0;
    private static final int MIN_UPDATE_DISTANCE = 1;

    private Boolean requestingLocationUpdates;
    private RendererRunnable rendererRunnable;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // check if device is supported or not
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_main);
        // initialize fragment
        ArFragment arFragment = (ArFragment)
                getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        requestingLocationUpdates = false;
        createLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(TAG, "result: " + locationResult.toString());
                Toast.makeText(MainActivity.this, locationResult.toString(), Toast.LENGTH_SHORT).show();
            }
        };
        startLocationUpdates();
        rendererRunnable = new RendererRunnable(this, arFragment);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates)
            startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2500);
        locationRequest.setSmallestDisplacement(MIN_UPDATE_DISTANCE);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Parmisan lena chahiye tha
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    /**
     * Check if device is AR ready!
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        ActivityManager activityManager = ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE));
        if (activityManager != null) {
            String openGlVersionString = activityManager.getDeviceConfigurationInfo().getGlEsVersion();
            if (Double.parseDouble(openGlVersionString) < MIN_OPEN_GL_VERSION) {
                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
                Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                        .show();
                activity.finish();
                return false;
            }
            return true;
        }
        return false;
    }


}
