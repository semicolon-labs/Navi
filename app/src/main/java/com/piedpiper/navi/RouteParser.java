package com.piedpiper.navi;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

public class RouteParser {

    private static final String TAG = RouteParser.class.getSimpleName();

    private JSONObject legs;

    RouteParser() {

    }

    public void setLegs(JSONObject jsonObject) {
        try {
            this.legs = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getLegs() {
        return legs;
    }

    public String getTotalDistance() {
        try {
            return legs.getJSONObject("distance").getString("text");
        } catch (JSONException e) {
            return e.toString();
        }
    }

    public int getTotalDistanceValue() {
        try {
            return legs.getJSONObject("distance").getInt("value");
        } catch (JSONException e) {
            return -1;
        }
    }

    public String getTotalTime() {
        try {
            return legs.getJSONObject("duration").getString("text");
        } catch (JSONException e) {
            return e.toString();
        }
    }

    public int getTotalTimeValue() {
        try {
            return legs.getJSONObject("duration").getInt("value");
        } catch (JSONException e) {
            return -1;
        }
    }

    public String getCurrentDistance() {
        try {
            return legs.getJSONArray("steps").getJSONObject(0).getJSONObject("distance").getString("text");
        } catch (JSONException e) {
            return e.toString();
        }
    }

    public Location getNextEndPoint() {
        Location location = null;
        try {
            JSONObject end_location = legs.getJSONArray("steps").getJSONObject(0).getJSONObject("end_location");
            location = new Location("");
            location.setLatitude(end_location.getDouble("lat"));
            location.setLongitude(end_location.getDouble("lng"));
            return location;
        } catch (JSONException e) {
            return location;
        }
    }

    public int getNextStepDistanceValue() {
        try {
            return legs.getJSONArray("steps").getJSONObject(0).getJSONObject("distance").getInt("value");
        } catch (JSONException e) {
            return -1;
        }
    }

    public int getNextStepDurationValue() {
        try {
            return legs.getJSONArray("steps").getJSONObject(0).getJSONObject("duration").getInt("value");
        } catch (JSONException e) {
            return -1;

        }
    }

}
