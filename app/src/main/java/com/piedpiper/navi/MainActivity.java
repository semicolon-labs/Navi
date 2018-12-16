package com.piedpiper.navi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.ar.sceneform.ux.ArFragment;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MainCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPEN_GL_VERSION = 3.0;
    private static final int MIN_UPDATE_DISTANCE = 1;

    private int autoCompleteTextViewLocation[];
    private Boolean requestingLocationUpdates;
    private Handler handler;
    private RendererRunnable rendererRunnable;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private AutoCompleteTextView autoCompleteTextView;
    private ImageView startButton;
    private ArrayAdapter<String> PlaceAdapter;

    private String destinationPlaceId;
    private Navigator navigator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // check if device is supported or not
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        // initialize fragment
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
        autoCompleteTextViewLocation = new int[2];
        requestingLocationUpdates = false;
        createLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        destinationPlaceId = "";
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(TAG, "result: " + locationResult.toString());
                navigator.getRoute(locationResult.getLastLocation(), destinationPlaceId, Navigator.WALKING_MODE);
            }
        };
        handler = new Handler();
        rendererRunnable = new RendererRunnable(this, handler, arFragment);
        navigator = new Navigator(this);
        startRendering();

        startButton = findViewById(R.id.start_button);

        /*
         * Get Predictions for autocomplete menu
         */
        autoCompleteTextView = findViewById(R.id.autoCompleteMaps);
        autoCompleteTextView.getLocationOnScreen(autoCompleteTextViewLocation);
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                navigator.getPrediction(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        autoCompleteTextView.setThreshold(1);   // will start working from first character
        PlaceAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, new String[0]);
        PlaceAdapter.setNotifyOnChange(true);
        autoCompleteTextView.setAdapter(PlaceAdapter);
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

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void startRendering() {
        handler.post(rendererRunnable);
    }

    private void stopRendering() {
        handler.removeCallbacks(rendererRunnable);
    }

    /**
     * Check if device is AR ready!
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
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


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onPrediction(List<String> places, List<String> places_id) {
        PlaceAdapter.clear();
        PlaceAdapter.addAll(places);
        PlaceAdapter.notifyDataSetChanged();

        List<String> PlaceIdAdapter = new ArrayList<String>();
        PlaceIdAdapter.clear();
        PlaceIdAdapter.addAll(places_id);

        /*
         * Get the index of the selected place from the list. (Starts with 0)
         */
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, Integer.toString(position));
            Log.d(TAG, PlaceIdAdapter.get(position));
            destinationPlaceId = PlaceIdAdapter.get(position);
            startButton.setVisibility(View.VISIBLE); //Show start button
            this.hideKeyboard();

        });

    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    public void onRoute(String totalDistance, String totalTime, String partialDistance, Uri arrow) {
        ((TextView)findViewById(R.id.text_distance)).setText(totalDistance);
        ((TextView)findViewById(R.id.text_duration)).setText(totalTime);
        ((TextView)findViewById(R.id.text_curr_distance)).setText(partialDistance);
        rendererRunnable.addObject(arrow);
    }

    @Override
    public void onCalibrated() {
        findViewById(R.id.splash_view).setVisibility(View.GONE);
        findViewById(R.id.autoCompleteMaps).setVisibility(View.VISIBLE);
    }

    private void startNavigationMode(){
        Animation buttonAnimation = AnimationUtils.loadAnimation(this, R.anim.button_animation);
        findViewById(R.id.start_button).startAnimation(buttonAnimation);
        findViewById(R.id.start_button).setVisibility(View.GONE);
        // AutoCompleteTextView
        autoCompleteTextView.animate().translationYBy(-autoCompleteTextViewLocation[1]).translationXBy(autoCompleteTextViewLocation[0]).alpha(0.5f).setDuration(700).withEndAction(new Runnable() {
            @Override
            public void run() {
                String str_temp = autoCompleteTextView.getText().toString();
                int commaCount = 0, i;
                for (i = 0; i < str_temp.length(); i++) {
                    if (str_temp.charAt(i) == ',')
                        commaCount++;
                    if (commaCount == 3)
                        break;

                }
                ((TextView) findViewById(R.id.destination_text_view)).setText(str_temp.substring(0, i));
                autoCompleteTextView.setVisibility(View.GONE);
                findViewById(R.id.linear_layout_top).setVisibility(View.VISIBLE);
                findViewById(R.id.linear_layout_bottom).setVisibility(View.VISIBLE);
            }
        });
    }

    private void stopNavigationMode(){
        // return things to where they were
        autoCompleteTextView.animate().translationYBy(autoCompleteTextViewLocation[1]).translationXBy(-autoCompleteTextViewLocation[0]).alpha(1f);
        // draw stuff
        findViewById(R.id.linear_layout_top).setVisibility(View.GONE);
        findViewById(R.id.linear_layout_bottom).setVisibility(View.GONE);
        autoCompleteTextView.setVisibility(View.VISIBLE);
        autoCompleteTextView.getParent().requestChildFocus(autoCompleteTextView, autoCompleteTextView);
        findViewById(R.id.start_button).setVisibility(View.VISIBLE);
    }

    public void startButtonOnClick(View view) {
        startNavigationMode();
        requestingLocationUpdates = true;
        startLocationUpdates();
        stopRendering();
    }

    public void backButtonOnClick(View view) {
        stopNavigationMode();
        requestingLocationUpdates = false;
        stopLocationUpdates();
        startRendering();
    }

    @Override
    public void onBackPressed() {}
}
