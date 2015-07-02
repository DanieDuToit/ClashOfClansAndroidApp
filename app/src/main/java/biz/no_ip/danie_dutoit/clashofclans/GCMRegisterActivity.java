package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by dutoitd1 on 2015/06/29.
 */
public class GCMRegisterActivity extends Activity {
    public String gameName;
    public String email;
    // UI elements
    EditText txtEmail;
    TextView txtViewRegistrationId;
    AsyncTask<Void, Void, Void> mRegisterTask;
    // Register buttons
    Button btnRegister;
    Button btnSaveRegistration;
    ArrayList<String> gameNamesList = new ArrayList<>();
    ArrayList<Integer> playerIDs = new ArrayList<>();
    JSONArray data = null;
    ProgressDialog pDialog;
    ArrayAdapter<String> NamesDataAdapter;
    Spinner gameNameSpinner;
    private GlobalState gs;
    private String registrationId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gcm_activity_register);

        gs = (GlobalState) getApplication();

        // Create a tempoarary button to handle the download of players
        final Button btn = new Button(GCMRegisterActivity.this);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final GetActivePlayers downloader = new GetActivePlayers();
                downloader.execute();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
                            downloader.cancel(true);
                            if (pDialog.isShowing()) {
                                pDialog.dismiss();
                            }
                            new AlertDialog.Builder(GCMRegisterActivity.this)
                                    .setTitle("Result")
                                    .setMessage("Internet connection timed out. Try again?")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            btn.performClick();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            android.os.Process.killProcess(android.os.Process.myPid());
                                            System.exit(1);
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    }
                }, 60000);
            }
        });
        // "Click" the button that will start the download of players
        btn.performClick();

        final Context context = GCMRegisterActivity.this;

        //Get Global Controller Class object (see application tag in AndroidManifest.xml)
        final GlobalState aGCMController = (GlobalState) getApplication();

        // Check if Internet Connection present
        if (!aGCMController.isConnectingToInternet()) {

            // Internet Connection is not present
            aGCMController.showAlertDialog(GCMRegisterActivity.this,
                    "Internet Connection Error",
                    "Please connect to working Internet connection", false);

            // stop executing code by return
            return;
        }

        // Check if GCM configuration is set
        if (GCMConfig.GOOGLE_SENDER_ID == null || GCMConfig.GOOGLE_SENDER_ID.length() == 0) {

            // GCM sernder id / server url is missing
            aGCMController.showAlertDialog(GCMRegisterActivity.this, "Configuration Error!",
                    "The Server URL and GCM Sender ID has not been set", false);

            // stop executing code by return
            return;
        }

//        txtName = (EditText) findViewById(R.id.txtName);
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnSaveRegistration = (Button) findViewById(R.id.btnSaveRegistration);
        txtViewRegistrationId = (TextView) findViewById(R.id.txtViewRegistrationId);
        gameNameSpinner = (Spinner) findViewById(R.id.gameNameSpinner);

        gameNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int index = gameNameSpinner.getSelectedItemPosition();
                gameName = gameNamesList.get(index);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                gameName = gameNamesList.get(0);
            }
        });

        // Click event on Register button
        btnRegister.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Get data from EditText
//                gameName = txtName.getText().toString();
//                email = txtEmail.getText().toString();

                // Check if user filled the form
                email = txtEmail.getText().toString();
                if (gameName.trim().length() > 0 && email.trim().length() > 0) {
                    // Get GCM registration id
                    registrationId = GCMRegistrar.getRegistrationId(GCMRegisterActivity.this);

                    // Check if regid already presents
                    if (registrationId.equals("")) {

                        // Register with GCM
                        GCMRegistrar.register(GCMRegisterActivity.this, GCMConfig.GOOGLE_SENDER_ID);

                    } else {

                        // Device is already registered on GCM Server
                        if (GCMRegistrar.isRegisteredOnServer(GCMRegisterActivity.this)) {

                            // Skips registration.
                            Toast.makeText(getApplicationContext(), "Already registered with GCM Server", Toast.LENGTH_LONG).show();

                        } else {

                            // Try to register again, but not in the UI thread.
                            // It's also necessary to cancel the thread onDestroy(),
                            // hence the use of AsyncTask instead of a raw thread.

                            mRegisterTask = new AsyncTask<Void, Void, Void>() {

                                @Override
                                protected Void doInBackground(Void... params) {

                                    // Register on our server
                                    // On server creates a new user
                                    aGCMController.register(context, gameName, email, registrationId);
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void result) {
                                    final String regId = GCMRegistrar.getRegistrationId(GCMRegisterActivity.this);
                                    txtViewRegistrationId.setText(regId);
                                    mRegisterTask = null;
                                }

                            };

                            // execute AsyncTask
                            mRegisterTask.execute(null, null, null);
                        }
                    }
                    txtViewRegistrationId.setText(registrationId);
                } else {

                    // user did not fill in all the data
                    aGCMController.showAlertDialog(GCMRegisterActivity.this,
                            "Registration Error!",
                            "Please enter your details",
                            false);
                }
            }
        });

        btnSaveRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SaveGCMInfo downloader = new SaveGCMInfo();
                downloader.execute();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
                            downloader.cancel(true);
                            if (pDialog.isShowing()) {
                                pDialog.dismiss();
                            }
                            new AlertDialog.Builder(GCMRegisterActivity.this)
                                    .setTitle("Result")
                                    .setMessage("Internet connection timed out. Try again?")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            btnSaveRegistration.performClick();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            android.os.Process.killProcess(android.os.Process.myPid());
                                            System.exit(1);
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    }
                }, 60000);
            }
        });
    }

    private class GetActivePlayers extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();
        private volatile boolean finished = false;

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            String jsonStr = sh.makeServiceCall(gs.getInternetURL() + "get_OurPlayers.php", ServiceHandler.POST, queryParams);

            Log.e("JSONString", jsonStr);

            gameNamesList.clear();
            playerIDs.clear();

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("Players");
                    Log.e("JSONData", data.getString(0));

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        gameNamesList.add(c.getString("gamename"));
                        playerIDs.add(c.getInt("playerid"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(GCMRegisterActivity.this);
            pDialog.setMessage("Downloading Players. Please wait...");
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            NamesDataAdapter = new ArrayAdapter<String>
                    (GCMRegisterActivity.this, android.R.layout.simple_spinner_dropdown_item, gameNamesList);
            gameNameSpinner.setAdapter(NamesDataAdapter);
        }
    }

    private class SaveGCMInfo extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("gameName", gameName));
            queryParams.add(new BasicNameValuePair("email", email));
            queryParams.add(new BasicNameValuePair("gcmRegistrationId", registrationId));
            String jsonStr = sh.makeServiceCall(gs.getInternetURL() + "save_GCMInfo.php", ServiceHandler.POST, queryParams);

            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    // Check if there was an error
                    boolean response = jsonObj.getBoolean("responseOK");
                    if (!response) {
                        // Handle error
                        String errorMsg = jsonObj.getString("responseMessage");
                        new AlertDialog.Builder(GCMRegisterActivity.this)
                                .setTitle("Error")
                                .setMessage("The following message occured while trying to retrieve the requisition detail: " + errorMsg)
                                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                        System.exit(1);
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        return null;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(GCMRegisterActivity.this);
            pDialog.setMessage("Downloading Players. Please wait...");
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
        }
    }
}