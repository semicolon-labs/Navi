package com.piedpiper.navi;

import android.annotation.SuppressLint;
import android.content.Context;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;


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

    private static final int ROUTES_TASK = 0;
    private static final int PLACES_TASK = 1;
    public static final int MAX_PLACES_LENGTH = 4;

    private MainCallback listener;

    /**
     * Navigation constructor
     *
     * @param context: Activity Context
     */
    Navigator(Context context) {
        listener = (MainCallback) context;
    }

    /**
     * Get the routes information from origin to destination
     *
     * @param origin : origin location
     */
    void getRoute(Location origin, String destinationId) {
        // empty destination?
        // I trust origin since it comes from the GPS service
        if (destinationId.trim().equals(""))
            return;
        String requestUrl = String.format(Locale.getDefault(), "%s?origin=%f,%f&destination=place_id:%s",
                ROUTES_URL, origin.getLatitude(), origin.getLongitude(), destinationId);
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
                Navigator.this.listener.onRoute(jsonObject);
            }
        }
    }
}
