package com.launchpod.myapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {
    Button btRegister, btLogin;
    EditText phone, pwd;
    ConnectivityManager cm;
    NetworkInfo activeNetwork;
    public static LoginActivity ref;
    ProgressDialog pd;
    boolean NisConnected;
    String app_id = null;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_login);

        // check for registered status or data clear status
        // step 1 check whether data is cleared
        sharedPreferences = getSharedPreferences("login_status", MODE_PRIVATE);
        app_id = sharedPreferences.getString("app_id", null);
        editor = sharedPreferences.edit();
        if(app_id != null){
            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
            startActivity(intent);
            LoginActivity.this.finish();
        }

        btRegister = (Button) findViewById(R.id.register_button);
        btLogin = (Button) findViewById(R.id.login_button);

        phone = (EditText) findViewById(R.id.et_phone);
        pwd = (EditText) findViewById(R.id.et_password);

        setAddTextChangedListener(phone);
        setAddTextChangedListener(pwd);

        pd = new ProgressDialog(LoginActivity.this);
        pd.setMessage("Processing...");

        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_button:
                activeNetwork = cm.getActiveNetworkInfo();
                NisConnected = activeNetwork != null && activeNetwork.isConnected();
                if (!NisConnected) {
                    Snackbar snack = Snackbar.make(btLogin, "please turn on internet", Snackbar.LENGTH_LONG);
                    View v = snack.getView();
                    v.setBackgroundColor(ContextCompat.getColor(LoginActivity.this, R.color.red));
                    TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setBackgroundColor(ContextCompat.getColor(LoginActivity.this, R.color.red));
                    tv.setTextColor(Color.WHITE);
                    tv.setGravity(Gravity.CENTER_HORIZONTAL);
                    snack.show();

                } else if (phone.getText().length() < 10) {
                    phone.setError("10 digits");
                    phone.requestFocus();

                } else if (pwd.getText().length() < 8) {
                    pwd.setError("8 chars at least");
                    pwd.requestFocus();

                } else {
                    //new LongOperation().execute(phone.getText().toString(),pwd.getText().toString());
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    LoginActivity.this.finish();
                    pd.show();
                }
                break;
        }
    }

    private void setAddTextChangedListener(final EditText v) {
        v.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                v.setError(null);
            }
        });
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                StringBuilder sb;
                URL url = new URL("http://www.gharvihar.in/login.php?phone_number=" + params[0] + "&password=" + params[1]);
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setRequestMethod("GET");
//                urlConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
//                urlConn.setRequestProperty("Accept", "plain/text");
                urlConn.setDoInput(true);
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
                    sb.append(line + "\n");
                }
                is.close();

                if (urlConn.getResponseCode() == 200)
                    return sb.toString();
                else
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            pd.dismiss();
            if (result == null) {
                Snackbar snack = Snackbar.make(btLogin, "Server error", Snackbar.LENGTH_LONG);
                View v = snack.getView();
                v.setBackgroundColor(ContextCompat.getColor(LoginActivity.this, R.color.red));
                TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                snack.show();

            } else if(result.contains("not exist")){
                Snackbar snack = Snackbar.make(btLogin, "Login failed", Snackbar.LENGTH_LONG);
                View v = snack.getView();
                v.setBackgroundColor(ContextCompat.getColor(LoginActivity.this, R.color.red));
                TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                snack.show();
            }
            else if(result.contains("success")){
                int len = result.split(",")[1].length();
                String appid = result.split(",")[1].substring(0,len-1);
                editor.putString("app_id",appid);
                editor.commit();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                LoginActivity.this.finish();
            }
        }
    }

}