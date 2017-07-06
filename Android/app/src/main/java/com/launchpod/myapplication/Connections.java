package com.launchpod.myapplication;

/**
 * Created by Mahesh on 08-03-2017.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;


public class Connections {

    private static BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    private static AlertDialog alert;

    /**
     * Check bluetooth status on current device.
     * If bluetooth is off, ask to turn it on by an AlertDialog
     */
    public static void checkStatus(final Activity ac) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ac);
        if (isSupported()) {
            if (!bluetooth.isEnabled()) {
                // Bluetooth is not enable
//                builder.setTitle("Warning");
//                builder.setMessage("bt off");
//                builder.setPositiveButton("bt on", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                        ac.startActivityForResult(enableBtIntent, 1);
//                    }
//                });
//                builder.setNegativeButton("close app", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        ac.finish();
//                    }
//                });
//                builder.setCancelable(false);
//                alert = builder.create();
//                alert.show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                ac.startActivityForResult(enableBtIntent, 1);
            }
        } else {
            builder.setTitle("fatal error");
            builder.setMessage("bt not found");
            builder.setCancelable(false);
            builder.setPositiveButton("close app", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ac.finish();
                }
            });
            alert = builder.create();
            alert.show();
        }
    }

    /**
     * Check if bluetooth is supported by current device.
     *
     * @return true if device supports bluetooth, else false
     */
    public static boolean isSupported() {
        boolean isSupported;
        isSupported = bluetooth != null;
        return isSupported;
    }

}
