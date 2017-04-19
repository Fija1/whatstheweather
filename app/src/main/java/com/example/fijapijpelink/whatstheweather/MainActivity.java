package com.example.fijapijpelink.whatstheweather;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        LocationListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /*to shorten code along the wat make strings, kurl = key+url*/
    private static final String API_KURL = "https://api.darksky.net/forecast/e059a73af81e9747d69fa01f37082c46/";

    private static final String API_GGL = "https://maps.googleapis.com/maps/api/geocode/json?";
    private static final String GGL_KEY = "AIzaSyCv05ZlZuF_gh-YwR6JVPYWf4Kc6nuv4ng";


    private GoogleApiClient locationClient;

    /*make location request*/
    private LocationRequest locationRequest;

    /*UNO: CONSTANTEN ziet er uit als var maar is geen var*/
    public static final String LOGTAG = MainActivity.class.getSimpleName();

    private Location location;
    private String latitude;
    private String longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //toolbarfun
        Toolbar menuBar = (Toolbar)findViewById(R.id.menuBar);
        setSupportActionBar(menuBar);

        locationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create LocationRequest here
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                //in milliseconds
                .setInterval(10 * 1000)
                .setFastestInterval(1000);


    }

    /*maak hier je onresume voor als de licycle paused was*/
    @Override
    protected void onResume() {
        super.onResume();
        locationClient.connect();
    }

        /*we want to connect and disconnect whenever the app is paused  */

    @Override
    protected void onPause() {
        super.onPause();
        if (locationClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClient, this);
            locationClient.disconnect();
        }
    }

    /*als er dingen mis gaan*/
    /*DOS: DIT IS ONDERDEEL VAN DE GPS IMPLEMENTATIE*/
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOGTAG, "Location services are connected.");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, locationRequest, this);

            /*for only last know locations use:*/
          location = LocationServices.FusedLocationApi.getLastLocation(locationClient);
            if (location == null) {
             LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, locationRequest, this);
           } else {
           handleNewLocation(location);
            }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOGTAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*comments komen van github waar android map tutorial naar verwijst*/
            /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
                /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
            Log.d(LOGTAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location location) {
        Log.d(LOGTAG, location.toString());
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());

        new DownloadWeatherTask().execute();
        new DownloadLocationTask().execute();
    }

    /*TRES: INTERNET VOOR HET WEER IN EEN APARTE CLASS*/
    private class DownloadWeatherTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {

            try {
                /*build URL to get weather from specific location using long and lat */
                URL darkSky = new URL(API_KURL + latitude + "," + longitude);

                // open internet connection
                HttpURLConnection connection = (HttpURLConnection) darkSky.openConnection();

                //read input from connection and disconnect after
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;

                    // add all lines to builder

                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }

                    //close reader and return built string
                    reader.close();
                    return builder.toString();

                } finally {
                    connection.disconnect();
                }

            } catch (Exception e) {
                Log.d(LOGTAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null) {
                Log.d(LOGTAG, "there was an array in postExecute");
            } else {
                try {
                    // make jsonobject
                    JSONObject object = new JSONObject(response);

                    JSONObject weather = object.getJSONObject("currently");

                    TextView condition = (TextView) findViewById(R.id.ConditionTextView);
                    TextView temperature = (TextView) findViewById(R.id.TempTextView);

                    condition.setText(weather.getString("summary"));
                    String tempString = weather.getString("temperature");

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    String unit = sharedPref.getString(SettingsActivity.TEMPUNIT, "F");

                    double tempDouble = Double.parseDouble(tempString);

                    if(unit.equals("C")) {
                        tempDouble = (tempDouble-32)/1.8;
                        tempDouble = (double)(Math.round(tempDouble*100))/100;
                    }

                    tempString = String.valueOf(tempDouble)+unit;

                    temperature.setText(tempString);

                } catch (Exception e) {
                    Log.d(LOGTAG, e.getMessage());
                }
            }
        }
    }

        /*TRES DEEL 2: INTERNET VOOR HET CONVERTEN VAN LAT+LONG NAAR LOCATIENAAM IN EEN APARTE CLASS*/

    private class DownloadLocationTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {

            try {
                    /*build URL to get weather from specific location using long and lat */
                URL ggl = new URL(API_GGL + "latlng=" + latitude + "," + longitude + "&key=" + GGL_KEY + "&result_type=locality");

                // open internet connection
                HttpURLConnection connection = (HttpURLConnection) ggl.openConnection();

                //read input from connection and disconnect after
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;

                    // add all lines to builder

                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }

                    //close reader and return built string
                    reader.close();
                    return builder.toString();

                } finally {
                    connection.disconnect();
                }

            } catch (Exception e) {
                Log.d(LOGTAG, e.getMessage());
                return null;
            }
        }


        @Override
        protected void onPostExecute(String response) {
            String place;

            if (response == null) {
                Log.d(LOGTAG, "No response from server");
                place = "Location not found";
            } else {
                try {
                    // Make JSON object
                    JSONObject object = new JSONObject(response);
                    JSONArray results = object.getJSONArray("results");

                    if (results.length() > 0) {
                        JSONObject jsonPlace = results.getJSONObject(0);
                        place = jsonPlace.getString("formatted_address");
                    } else {
                        Log.d(LOGTAG, "No results");
                        place = "Location not found";
                    }
                } catch (Exception e) {
                    Log.d(LOGTAG, e.getMessage());
                    place = "Location not found";
                }

            }

            TextView locationTextView = (TextView) findViewById(R.id.locationTextView);
            locationTextView.setText(place);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Open the SettingsActivity if the settings item in the menu is clicked
        if (item.getItemId() == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return true;
    }

}
