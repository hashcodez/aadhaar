package com.launchpod.myapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, LocationListener {
    TextView blue_weight, black_weight, red_weight, yellow_weight, tv_internet, tv_gps, tv_machine;
    TextView tot_value_red, tot_value_yellow, tot_value_blue, tot_value_black;
    Button connect_machine, scan_barcode;

    private ConnectivityManager cm;
    public static String hospitalId;
    private LocationManager locationManager;
    private Location location = null;
    private ZXingScannerView mScannerView;

    public static boolean isMachineConnected = false;
    boolean isNetworkEnabled = false;
    boolean isGPSEnabled = false;
    public static String selectedColor;

    public static HashMap<String, Float> weights;             // weights for each bag
    private Set<String> devicesList;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60; // 1 minute

    //private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static BluetoothSocket mSocket;
    private BluetoothDevice btDevice;
    private BluetoothAdapter btAdapter;

    private ProgressDialog pd;
    private ProgressDialog mProgressDlg;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alertDialog;

    private IntentFilter filter;
    double latitude; // latitude
    double longitude; // longitude


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        weights = new HashMap<>();
        weights.put("yellow", 0f);
        weights.put("red", 0f);
        weights.put("black", 0f);
        weights.put("blue", 0f);
        devicesList = new TreeSet<>();

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Connecting...");

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);

        MainActivity.this.registerReceiver(mReceiver, filter);

        // progress dialog for scanning btt devices
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mProgressDlg = new ProgressDialog(MainActivity.this);
        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                btAdapter.cancelDiscovery();
            }
        });

    }

    private void checkcondition(int color, String colour) {

        if (!isNetworkEnabled) {
            Snackbar snack = Snackbar.make(yellow_weight, "please turn on internet", Snackbar.LENGTH_LONG);
            View v = snack.getView();
            v.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            snack.show();
        } else if (!isGPSEnabled) {
            Snackbar snack = Snackbar.make(yellow_weight, "please turn on GPS", Snackbar.LENGTH_LONG);
            View v = snack.getView();
            v.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            snack.show();
        } else if (!isMachineConnected) {
            Snackbar snack = Snackbar.make(yellow_weight, "please connect to machine", Snackbar.LENGTH_LONG);
            View v = snack.getView();
            v.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            snack.show();
        } else {
            selectedColor = colour;
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            intent.putExtra("color", color);
            intent.putExtra("colour", colour);
            startActivity(intent);
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        mScannerView.stopCamera();
        setContentView(R.layout.activity_main);
        init();
        controls(false);
        pd.setMessage("processing...");
        pd.show();
        getLocation();
        EditText aadhaarText = (EditText) findViewById(R.id.bt_aadhaar);
        aadhaarText.setText(rawResult.toString());
        pd.dismiss();
        controls(true);
        //new LongOperation().execute(rawResult.toString());
    }

    private void init() {
        //control button
        connect_machine = (Button) findViewById(R.id.bt_connect);
        scan_barcode = (Button) findViewById(R.id.bt_hospital_barcode);

        // to update internet, gps and machine status
        tv_internet = (TextView) findViewById(R.id.internet);
        tv_gps = (TextView) findViewById(R.id.gps);
        tv_machine = (TextView) findViewById(R.id.machine);

        // when clicked on respective colors for scan
        blue_weight = (TextView) findViewById(R.id.blue_weight);
        black_weight = (TextView) findViewById(R.id.black_weight);
        red_weight = (TextView) findViewById(R.id.red_weight);
        yellow_weight = (TextView) findViewById(R.id.yellow_weight);

        tot_value_red = (TextView) findViewById(R.id.tot_value_red);
        tot_value_yellow = (TextView) findViewById(R.id.tot_value_yellow);
        tot_value_blue = (TextView) findViewById(R.id.tot_value_blue);
        tot_value_black = (TextView) findViewById(R.id.tot_value_black);

        // getting GPS status
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isNetworkEnabled)
            tv_internet.setText("Connected");
        if (isGPSEnabled)
            tv_gps.setText("On");
        if (isMachineConnected)
            tv_machine.setText("Connected");

        setClickListener(black_weight);
        setClickListener(blue_weight);
        setClickListener(red_weight);
        setClickListener(yellow_weight);
        setClickListener(connect_machine);
        setClickListener(scan_barcode);
    }

    private void controls(boolean value) {
        connect_machine.setEnabled(value);
        scan_barcode.setEnabled(value);
        blue_weight.setEnabled(value);
        black_weight.setEnabled(value);
        red_weight.setEnabled(value);
        yellow_weight.setEnabled(value);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (isGPSEnabled) {
                    tv_gps.setText("On");
                    isGPSEnabled = true;
                } else {
                    tv_gps.setText("Off");
                    isGPSEnabled = false;
                }
            }
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (null != activeNetwork) {
                    tv_internet.setText("Connected");
                    isNetworkEnabled = true;
                } else {
                    isNetworkEnabled = false;
                    tv_internet.setText("Disconnected");
                }
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName().contains("HC-"))
                    devicesList.add("Name: " + device.getName() + " Add: " + device.getAddress());

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mProgressDlg.dismiss();

                final CharSequence[] devicesSequence = devicesList.toArray(new String[devicesList.size()]);
                dialogBuilder.setTitle("devices found");
                dialogBuilder.setItems(devicesSequence, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        String selectedText = devicesSequence[item].toString();  //Selected item in listview
                        String address = selectedText.substring(selectedText.length() - 17); // 17 is the length of a MAC address in form 00:00:00:00:00:00
                        BluetoothDevice btDev = btAdapter.getRemoteDevice(address);
                        //alertDialog.dismiss();
                        //pd.show();
                        //pd.setCancelable(false);
                        pairDevice(btDev);
                        connect(btDev);
                    }
                });
                dialogBuilder.setCancelable(false);
                alertDialog = dialogBuilder.create();
                alertDialog.show();

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mProgressDlg.show();

            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF) {
                    isMachineConnected = false;
                    tv_machine.setText("Disconnected");
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                alertDialog.dismiss();
                //Toast.makeText(MainActivity.this.getApplicationContext(), "connected", Toast.LENGTH_SHORT).show();
                isMachineConnected = true;
                tv_machine.setText("Connected");
                //pd.dismiss();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                alertDialog.dismiss();
                displayAlert("Bluetooth disconnected!");
                //Toast.makeText(MainActivity.this.getApplicationContext(), "disconnected", Toast.LENGTH_SHORT).show();
                isMachineConnected = true;
                tv_machine.setText("Disconnected");
            }
        }
    };

    public void pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this.getApplicationContext(), "wrong pairing",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void connect(BluetoothDevice device) {
        btDevice = device;
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            //Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            //mSocket = (BluetoothSocket) m.invoke(device, 1);
        }
//        catch (NoSuchMethodException e){
//            e.printStackTrace();
//        }
//        catch (IllegalAccessException e){
//            e.printStackTrace();
//        }
//        catch (InvocationTargetException e){
//            e.printStackTrace();
//        }
        catch (Exception e) {
            displayAlert("Bluetooth failed!");
            //Toast.makeText(MainActivity.this.getApplicationContext(), "wrong_socket", Toast.LENGTH_SHORT).show();
        }
        startConnection();
    }

    private void startConnection() {
        // Cancel discovery because it will slow down the connection
        btAdapter.cancelDiscovery();

        try {
            mSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            displayAlert("Failed to connect");
            //Toast.makeText(MainActivity.this.getApplicationContext(), "unable to connect", Toast.LENGTH_SHORT).show();
            try {
                mSocket.close();
            } catch (IOException closeException) {
            }
            return;
        }
    }


    public void startScan() {
        btAdapter.startDiscovery();
        // Listing paired devices
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();

        for (BluetoothDevice device : devices) {
            devicesList.add("Name: " + device.getName() + " Add: " + device.getAddress());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK)
                    startScan();
                else
                    displayAlert("Sorry bluetooth failed!");
                //Toast.makeText(MainActivity.this, "Sorry bluetooth failed!", Toast.LENGTH_SHORT).show();
        }
    }


    public void updateUI() {
        tot_value_blue.setText(String.valueOf(weights.get("blue")));
        tot_value_black.setText(String.valueOf(weights.get("black")));
        tot_value_red.setText(String.valueOf(weights.get("red")));
        tot_value_yellow.setText(String.valueOf(weights.get("yellow")));
    }

    public void setClickListener(View v) {
        final int id = v.getId();
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (id) {
                    case R.id.bt_connect:
                        if (!isMachineConnected) {
                            if (btAdapter.isEnabled())
                                startScan();
                            else
                                Connections.checkStatus(MainActivity.this);
                        }
                        break;
                    // scan zero bar code
                    case R.id.bt_hospital_barcode:
                        if (!isNetworkEnabled) {
                            Snackbar snack = Snackbar.make(yellow_weight, "please turn on internet", Snackbar.LENGTH_LONG);
                            View v = snack.getView();
                            v.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
                            TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                            tv.setTextColor(Color.WHITE);
                            tv.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
                            tv.setGravity(Gravity.CENTER_HORIZONTAL);
                            snack.show();

                        } else if (!isGPSEnabled) {
                            Snackbar snack = Snackbar.make(yellow_weight, "please turn on GPS", Snackbar.LENGTH_LONG);
                            View v = snack.getView();
                            v.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
                            TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                            tv.setTextColor(Color.WHITE);
                            tv.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
                            tv.setGravity(Gravity.CENTER_HORIZONTAL);
                            snack.show();
                        } else {
                            mScannerView = new ZXingScannerView(MainActivity.this);
                            setContentView(mScannerView);
                            mScannerView.setResultHandler(MainActivity.this);
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
                            mScannerView.startCamera();
                        }
                        break;
                    case R.id.black_weight:
                        checkcondition(R.drawable.garbage_black_48x48, "black");
                        break;
                    case R.id.blue_weight:
                        checkcondition(R.drawable.garblue, "blue");
                        break;
                    case R.id.red_weight:
                        checkcondition(R.drawable.garred, "red");
                        break;
                    case R.id.yellow_weight:
                        checkcondition(R.drawable.garyellow, "yellow");
                        break;
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onBackPressed() {
        if (mScannerView != null) {
            if (mScannerView.isShown()) {
                mScannerView.stopCamera();
                setContentView(R.layout.activity_main);
                init();
            } else
                DisplayyAlertForLogOut("Are you sure you want to logout?");
        } else
            DisplayyAlertForLogOut("Are you sure you want to logout?");
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder sb = new StringBuilder();
            try {
                String args = "zero_barcode_value=" + params[0] + "&" +
                        "driver_application_id=" + "1" + "&" +
                        "LAT=" + latitude + "&" + "LONGITUDE=" + longitude;
                URL url = new URL("http://www.gharvihar.in/write_zero_barcode.php?" + args);
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
            pd.dismiss();
            if (result.contains("success"))
                displayAlert("Scan success");
                //Toast.makeText(MainActivity.this, "success", Toast.LENGTH_SHORT).show();
            else if (result.contains("not exist"))
                displayAlert("Invalid barcode!");
                //Toast.makeText(MainActivity.this, "Invalid Barcode", Toast.LENGTH_SHORT).show();
            else if (result.contains("already scanned"))
                displayAlert("Already scanned");
                //Toast.makeText(MainActivity.this, "Already Scanned", Toast.LENGTH_SHORT).show();
            else if (result.contains("fail"))
                displayAlert("Error please try after sometime");
            //Toast.makeText(MainActivity.this, "failed", Toast.LENGTH_SHORT).show();
            controls(true);
        }
    }

    //------------------------------GPS------------------------------------------

    public Location getLocation() {
        try {
            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {

                // First get location from Network Provider
                if (isNetworkEnabled) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, MainActivity.this);


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
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, MainActivity.this);

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
        longitude = location.getLatitude();
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

    private void DisplayyAlertForLogOut(String msg) {
        new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                MainActivity.this.finish();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            }
        }).show();
    }

    private void displayAlert(String msg) {
        new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();
            }
        }).show();
    }
}




/*
// show the list of paired bt devices if bt is already On
        if (btAdapter.isEnabled()) {
            Set<BluetoothDevice> set = btAdapter.getBondedDevices();
            Iterator<BluetoothDevice> iterator = set.iterator();
            Set<String> pairedDevicesAddress = new TreeSet<>();

            while (iterator.hasNext())
                pairedDevicesAddress.add(iterator.next().getAddress());

            final CharSequence[] pairedDevicesAdd = pairedDevicesAddress.toArray(new String[btAdapter.getBondedDevices().size()]);
            dialogBuilder.setTitle("Paired Devices");
            dialogBuilder.setItems(pairedDevicesAdd, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    String selectedText = pairedDevicesAdd[item].toString();  //Selected item in listview
                    String address = selectedText.substring(selectedText.length() - 17); // 17 is the length of a MAC address in form 00:00:00:00:00:00

                    BluetoothDevice btDev = btAdapter.getRemoteDevice(address);
                    dialog.dismiss();
                    pd.show();
                    //pairDevice(btDev);
                    connect(btDev);
                }
            });
            dialogBuilder.setCancelable(false);
            alertDialog = dialogBuilder.create();
            alertDialog.show();
        }
        init();
* */
