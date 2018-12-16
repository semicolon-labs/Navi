package com.piedpiper.navi;

import android.net.Uri;

import org.json.JSONObject;

import java.util.List;

public interface MainCallback {
    void onPrediction(List<String> places, List<String> places_id);
    void onRoute(String totalDistance, String totalTime, String partialDistance, Uri arrow);
    void onCalibrated();
}
