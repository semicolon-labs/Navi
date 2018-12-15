package com.piedpiper.navi;

import org.json.JSONObject;

import java.util.List;

public interface MainCallback {
    void onPrediction(List<String> places, List<String> places_id);
    void onRoute(JSONObject jsonObject);
    void onCalibrated();
}
