package com.launchpod.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

public class RegistrationActivity extends AppCompatActivity {
    Button btRegister;
    EditText phone, pwd;
    ConnectivityManager cm;
    NetworkInfo activeNetwork;
    public static RegistrationActivity ref;
    ProgressDialog pd;
    boolean NisConnected;
    String userid = null;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_signup);

        sharedPreferences = getSharedPreferences("login_status", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        btRegister = (Button) findViewById(R.id.register_button);

        phone = (EditText) findViewById(R.id.et_phone);
        pwd = (EditText) findViewById(R.id.et_password);

        setAddTextChangedListener(phone);
        setAddTextChangedListener(pwd);

        pd = new ProgressDialog(RegistrationActivity.this);
        pd.setMessage("Processing..");

        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register_button:
                activeNetwork = cm.getActiveNetworkInfo();
                NisConnected = activeNetwork != null && activeNetwork.isConnected();
                if (!NisConnected) {
                    Snackbar snack = Snackbar.make(btRegister, "please turn on internet", Snackbar.LENGTH_LONG);
                    View v = snack.getView();
                    v.setBackgroundColor(ContextCompat.getColor(RegistrationActivity.this, R.color.red));
                    TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setBackgroundColor(ContextCompat.getColor(RegistrationActivity.this, R.color.red));
                    tv.setTextColor(Color.WHITE);
                    tv.setGravity(Gravity.CENTER_HORIZONTAL);
                    snack.show();

                } else if (phone.getText().length() < 10) {
                    phone.setError("10 digits!");
                    phone.requestFocus();

                } else if (pwd.getText().length() < 8) {
                    pwd.setError("8 characters min!");
                    pwd.requestFocus();

                } else {
                    new LongOperation().execute(phone.getText().toString(), pwd.getText().toString());
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
                URL url = new URL("http://192.168.0.13:8082/regis");
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setRequestMethod("POST");
                urlConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                //urlConn.setRequestProperty("Accept", "application/json");
                urlConn.setDoOutput(true);
                urlConn.connect();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConn.getOutputStream()));
                JSONObject json = new JSONObject();

                json.put("phone", params[0]);
                json.put("pwd", params[1]);
                json.put("mac", "");

                writer.write(json.toString());
                writer.flush();
                writer.close();
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
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            pd.dismiss();
            if (result == null) {
                Snackbar snack = Snackbar.make(btRegister, "Registration failed!", Snackbar.LENGTH_LONG);
                View v = snack.getView();
                v.setBackgroundColor(ContextCompat.getColor(RegistrationActivity.this, R.color.red));
                TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                snack.show();

            } else if (result.contains("registered")) {
                Snackbar snack = Snackbar.make(btRegister, "Registered already!", Snackbar.LENGTH_LONG);
                View v = snack.getView();
                v.setBackgroundColor(ContextCompat.getColor(RegistrationActivity.this, R.color.red));
                TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                snack.show();
            } else {
                editor.putString("userid", result);
                editor.commit();
                displayAlert("Registration Complete");
            }
        }
    }

    private void displayAlert(String msg) {
        new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();
                RegistrationActivity.this.finish();
            }
        }).show();
    }


}