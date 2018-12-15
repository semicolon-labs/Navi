package com.piedpiper.navi;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPEN_GL_VERSION = 3.0;

    private Handler handler;
    private RendererRunnable rendererRunnable;

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

        handler = new Handler();
        rendererRunnable = new RendererRunnable(this, this.handler, arFragment);

        startRendering();
    }

    private void startRendering() {
        handler.post(rendererRunnable);
    }

    private void stopRendering() {
        handler.removeCallbacks(rendererRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRendering();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRendering();
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
