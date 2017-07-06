package com.launchpod.myapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.common.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, LocationListener {

    HashMap<String, Integer> colorids = new HashMap<>();
    String weight_value, barcode_value, colour, app_id;
    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean isGPSEnabled = false;

    ConnectivityManager cm;
    NetworkInfo activeNetwork;

    ProgressDialog pd;
    private BluetoothSocket mSocket;
    private ZXingScannerView mScannerView;
    TextView weight;
    Button scan, send;
    ImageView bag;

    int color;

    LocationManager locationManager;
    private Location location = null;
    double latitude; // latitude
    double longitude; // longitude
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60; // 1 minute


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        SharedPreferences sharedPreferences;
        sharedPreferences = getSharedPreferences("login_status", MODE_PRIVATE);
        app_id = sharedPreferences.getString("app_id", null);

        colorids.put("yellow", 1);
        colorids.put("blue", 4);
        colorids.put("red", 2);
        colorids.put("black", 3);
        color = getIntent().getIntExtra("color", 0);
        colour = getIntent().getStringExtra("colour");
        pd = new ProgressDialog(ScanActivity.this);
        pd.setMessage("Processing");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        init();
    }

    @Override
    public void handleResult(final Result rawResult) {
        mScannerView.stopCamera();
        setContentView(R.layout.activity_scan);
        init();
        barcode_value = rawResult.toString();
        pd.show();
        getLocation();
        getDataFromAurdino();
    }

    public void getDataFromAurdino() {
        pd.dismiss();
        if (MainActivity.mSocket != null) {
            try {
                InputStream in = MainActivity.mSocket.getInputStream();
                int available = in.available();
                byte[] dat = new byte[40];
                if (available > 0) {
                    int bytes = in.read(dat);
                    MainActivity.mSocket.getOutputStream().write(2);
                    String readMessage = new String(dat, 0, bytes);
                    String[] value = readMessage.split("\r\n");
                    weight.setText(value[0] + " gms");
                    weight_value = value[0];
                } else
                    Toast.makeText(ScanActivity.this.getApplicationContext(), "please try again!", Toast.LENGTH_SHORT).show();
            } catch (ArrayIndexOutOfBoundsException e) {
                weight_value = null;
                Toast.makeText(ScanActivity.this.getApplicationContext(), "please try again!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                weight_value = null;
                Toast.makeText(ScanActivity.this.getApplicationContext(), "please try again!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public Location getLocation() {
        try {
            // getting GPS status
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {

                // First get location from Network Provider
                if (isNetworkEnabled) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, ScanActivity.this);

                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, ScanActivity.this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void init() {
        bag = (ImageView) findViewById(R.id.bag);
        bag.setBackgroundResource(color);
        scan = (Button) findViewById(R.id.scan);
        weight = (TextView) findViewById(R.id.weight);
        send = (Button) findViewById(R.id.send);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                activeNetwork = cm.getActiveNetworkInfo();
                isNetworkEnabled = activeNetwork != null && activeNetwork.isConnected();
                isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (!isGPSEnabled) {
                    Snackbar snack = Snackbar.make(send, "GPS is off!", Snackbar.LENGTH_LONG);
                    View v = snack.getView();
                    v.setBackgroundColor(ContextCompat.getColor(ScanActivity.this, R.color.blue));
                    TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    tv.setBackgroundColor(ContextCompat.getColor(ScanActivity.this, R.color.blue));
                    tv.setGravity(Gravity.CENTER_HORIZONTAL);
                    snack.show();
                } else if (!isNetworkEnabled) {
                    Snackbar snack = Snackbar.make(send, "Internet not connected!", Snackbar.LENGTH_LONG);
                    View v = snack.getView();
                    v.setBackgroundColor(ContextCompat.getColor(ScanActivity.this, R.color.blue));
                    TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    tv.setBackgroundColor(ContextCompat.getColor(ScanActivity.this, R.color.blue));
                    tv.setGravity(Gravity.CENTER_HORIZONTAL);
                    snack.show();
                } else if (!MainActivity.isMachineConnected) {
                    Snackbar snack = Snackbar.make(send, "Machine not connected!", Snackbar.LENGTH_LONG);
                    View v = snack.getView();
                    v.setBackgroundColor(ContextCompat.getColor(ScanActivity.this, R.color.blue));
                    TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    tv.setBackgroundColor(ContextCompat.getColor(ScanActivity.this, R.color.blue));
                    tv.setGravity(Gravity.CENTER_HORIZONTAL);
                    snack.show();
                } else {
                    //send data to server
                    new LongOperation().execute();
                    pd.show();
                }
            }
        });


        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    MainActivity.mSocket.getOutputStream().write(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getLocation();
                getDataFromAurdino();
                /*try {
                    MainActivity.mSocket.getOutputStream().write(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mScannerView = new ZXingScannerView(ScanActivity.this);
                setContentView(mScannerView);
                mScannerView.setResultHandler(ScanActivity.this);
                mScannerView.setAutoFocus(true);
                mScannerView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mScannerView.getFlash())
                            mScannerView.setFlash(false);
                        else
                            mScannerView.setFlash(true);

                    }
                });
                mScannerView.startCamera();*/
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mScannerView != null) {
            if (mScannerView.isShown()) {
                mScannerView.stopCamera();
                setContentView(R.layout.activity_scan);
                init();
            } else super.onBackPressed();
        } else
            super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder sb = new StringBuilder();
            try {
                String temp = "bag_weight=" + weight_value + "&" + "bag_color_id=" + colorids.get(colour) + "&" + "barcode_value=" + barcode_value + "&" + "driver_application_id=" + app_id +
                        "&" + "LAT=" + latitude + "&" + "LONGITUDE=" + longitude;
                URL url = new URL("http://www.gharvihar.in/write_data.php?" + temp);
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setRequestMethod("GET");

                urlConn.setDoOutput(true);
                urlConn.connect();

                InputStream is;
                if (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    is = urlConn.getInputStream();
                } else {
                    is = urlConn.getErrorStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        is, "UTF-8"), 8);
                sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.length() != 0) {
                if (result.contains("success")) {
                    float val, prevValue, currVal;
                    prevValue = MainActivity.weights.get(MainActivity.selectedColor);
                    currVal = Float.valueOf(weight_value) / 1000;
                    val = prevValue + currVal;
                    MainActivity.weights.put(MainActivity.selectedColor, val);
                    displayAlert("Scan success");
                }
                else if (result.contains("fail"))
                    displayAlert("Please try again!");

                else if (result.contains("already scanned"))
                    displayAlert("Already scanned!");

                else if (result.contains("zero scanned"))
                    displayAlert("Please come tomorrow");

                else if (result.contains("not exist"))
                    displayAlert("Invalid barcode!");

                else if (MainActivity.hospitalId == null)
                    MainActivity.hospitalId = result.split(",")[1];
                else if (!MainActivity.hospitalId.contentEquals(result.split(",")[1])) {
                    MainActivity.weights.put("yellow", 0f);
                    MainActivity.weights.put("red", 0f);
                    MainActivity.weights.put("black", 0f);
                    MainActivity.weights.put("blue", 0f);
                }
            } else
                displayAlert("Error please try after sometime");
            pd.dismiss();
        }
    }

    private void displayAlert(String msg) {
        new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();
            }
        }).show();
    }


}
