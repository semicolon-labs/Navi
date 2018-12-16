package com.piedpiper.navi;

import android.annotation.SuppressLint;
import android.content.Context;

import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Wrapper for distance and places API
 */
class Navigator {

    private static final String TAG = "Navigator";
    private static final String ROUTES_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String PLACES_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";

    public static final int DRIVING_MODE = 0;
    public static final int WALKING_MODE = 1;

    public static final int DRIVING_MODE_THRESHOLD = 200;
    public static final int WALKING_MODE_THRESHOLD = 10;

    private static final int ROUTES_TASK = 0;
    private static final int PLACES_TASK = 1;
    public static final int MAX_PLACES_LENGTH = 4;

    private MainCallback listener;
    private Boolean updateRequired;
    private RouteParser routeParser;
    private Context context;

    /**
     * Navigation constructor
     *
     * @param context: Activity Context
     */
    Navigator(Context context) {
        listener = (MainCallback) context;
        this.context = context;
        routeParser = new RouteParser();
        updateRequired = true;
    }

    /**
     * Get the routes information from origin to destination
     *
     * @param origin : origin location
     */
    @SuppressLint("DefaultLocale")
    void getRoute(Location origin, String destinationId, int action) {
        if (updateRequired){
            getDirections(origin, destinationId);
            updateRequired = false;
            return;
        }
        // update is not required!
        Location nextPoint = routeParser.getNextEndPoint();
        if (nextPoint == null)
            return;
        if(origin.distanceTo(nextPoint) < Navigator.DRIVING_MODE_THRESHOLD
                && action == Navigator.DRIVING_MODE){
            updateRequired = true;
            getRoute(origin, destinationId, action);
        } else if(origin.distanceTo(nextPoint) < Navigator.WALKING_MODE_THRESHOLD
                && action == Navigator.WALKING_MODE){
            updateRequired = true;
            getRoute(origin, destinationId, action);
        } else {
            String totalDistanceString, distanceLeftString, timeLeftString;
            float distanceLeft = origin.distanceTo(nextPoint);
            int totalDistance = routeParser.getNextStepDistanceValue();
            int totalTime = routeParser.getNextStepDurationValue();
            int timeLeft = (int)(distanceLeft/totalDistance)*totalTime;
            if (distanceLeft > 500)
                distanceLeftString = String.format("%.1f", distanceLeft/1000) + " km";
            else
                distanceLeftString = Integer.toString((int)distanceLeft) + " m";
            if (timeLeft > 30)
                timeLeftString = String.format("%.1f", ((float)timeLeft)/60) + " hours";
            else
                timeLeftString = Integer.toString(timeLeft) + " mins";
            totalDistance = totalDistance - (int)distanceLeft;
            if (totalDistance > 500)
                totalDistanceString = String.format("%.1f", ((float)totalDistance)/1000) + " km";
            else
                totalDistanceString = Integer.toString((int)totalDistance) + " m";

            Navigator.this.listener.onRoute(totalDistanceString, timeLeftString, distanceLeftString, RendererRunnable.ARROW_STRAIGHT);

        }

    }

    private void transmitRoute(JSONObject jsonObject){
        routeParser.setLegs(jsonObject);
        Navigator.this.listener.onRoute(routeParser.getTotalDistance(), routeParser.getTotalTime(), routeParser.getCurrentDistance(), getArrowUri());
    }

    private Uri getArrowUri() {
        String maneuver = "";
        try {
            int meters = routeParser.getNextStepDistanceValue();
            if (meters < Navigator.WALKING_MODE_THRESHOLD) {
                maneuver = routeParser.getLegs().getJSONArray("steps").getJSONObject(1).getString("maneuver");
            } else {
                maneuver = "straight";
            }
            Log.d(TAG, maneuver);
            Toast.makeText(this.context, maneuver, Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            // ignore
        }

        maneuver = (maneuver == null) ? "straight" : maneuver;
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

    private void getDirections(Location origin, String destinationId) {
        // empty destination?
        // I trust origin since it comes from the GPS service
        if (destinationId.trim().equals(""))
            return;
        String requestUrl = String.format(Locale.getDefault(), "%s?origin=%f,%f&destination=place_id:%s&mode=%s",
                ROUTES_URL, origin.getLatitude(), origin.getLongitude(), destinationId, DRIVING_MODE);
        requestUrl = requestUrl.concat(String.format(Locale.getDefault(), "&key=%s", BuildConfig.API_KEY));
        HttpTask httpTask = new HttpTask(ROUTES_TASK);
        httpTask.execute(requestUrl);
    }

    /**
     * Get a prediction for autocompletion
     * Eg, Washing -> Washington DC
     *
     * @param query: query for autocompletion
     */
    void getPrediction(String query) {
        // empty query?
        if (query.trim().equals(""))
            return;
        query = query.replace(" ", "+");
        String requestUrl = String.format(Locale.getDefault(), "%s?input=%s", PLACES_URL, query);
        requestUrl = requestUrl.concat(String.format(Locale.getDefault(), "&key=%s", BuildConfig.API_KEY));
        HttpTask httpTask = new HttpTask(PLACES_TASK);
        httpTask.execute(requestUrl);
    }

    @SuppressLint("StaticFieldLeak")
    private class HttpTask extends AsyncTask<String, Integer, JSONObject> {

        private OkHttpClient client = new OkHttpClient();
        private int taskCode;

        HttpTask(int taskCode) {
            this.taskCode = taskCode;
        }

        @Override
        protected JSONObject doInBackground(String... urls) {
            JSONObject jsonObject = new JSONObject();
            try {
                String data = run(urls[0]);
                jsonObject = new JSONObject(data);
            } catch (IOException | JSONException e) {
                Log.d(TAG, e.toString());
            }
            Log.d(TAG, "completed background task - " + jsonObject.toString());
            return jsonObject;
        }

        private String run(String url) throws IOException {
            String data = null;
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                ResponseBody responseBody = response.body();
                data = responseBody != null ? responseBody.string() : null;
            } catch (NullPointerException e) {
                Log.d(TAG, e.toString());
            }
            return data;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {

            if (this.taskCode == Navigator.PLACES_TASK) {
                // get description, place_id
                List<String> places = new ArrayList<>();
                List<String> place_ids = new ArrayList<>();
                if (jsonObject == null)
                    return;
                try {
                    JSONArray prediction = jsonObject.getJSONArray("predictions");
                    for (int i = 0; i < prediction.length() && i < MAX_PLACES_LENGTH; i++) {
                        places.add(prediction.getJSONObject(i).getString("description"));
                        place_ids.add(prediction.getJSONObject(i).getString("place_id"));
                        Log.d(TAG, "got place: " + places.get(i));
                    }

                } catch (Exception e) {
                    Log.d(TAG, this.getClass().getName() + this.taskCode);
                }
                Navigator.this.listener.onPrediction(places, place_ids);
            } else {
                transmitRoute(jsonObject);
            }
        }
    }
}
