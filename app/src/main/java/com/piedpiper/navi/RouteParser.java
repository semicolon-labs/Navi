package com.piedpiper.navi;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class RouteParser {

    private JSONObject legs;
    private Context context;

    RouteParser(Context context, JSONObject jsonObject){
        this.context = context;
        try {
            this.legs = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public String getTotalDistance(){
        try {
            return legs.getJSONObject("distance").getString("text");
        } catch (JSONException e) {
            return e.toString();
        }
    }

    public String getTotalTime(){
        try {
            return legs.getJSONObject("duration").getString("text");
        } catch (JSONException e) {
            return e.toString();
        }
    }

    public Uri getArrowUri(){
        String maneuver = "";
        try {
            Toast.makeText(this.context, legs.getJSONArray("steps").getJSONObject(0).getString("maneuver"), Toast.LENGTH_SHORT).show();
            maneuver = legs.getJSONArray("steps").getJSONObject(1).getString("maneuver");
        } catch (JSONException e){
            // ignore
        }
        maneuver = (maneuver==null)? "straight": maneuver;
        if (maneuver.equals("straight")) {
            return RendererRunnable.ARROW_STRAIGHT;
        } else if (maneuver.contains("left")) {
            return RendererRunnable.ARROW_LEFT;
        } else if (maneuver.contains("right")) {
            return RendererRunnable.ARROW_RIGHT;
        } else if (maneuver.contains("u-turn")) {
            return RendererRunnable.ARROW_UTURN;
        }
        return RendererRunnable.ARROW_STRAIGHT;
    }

}
