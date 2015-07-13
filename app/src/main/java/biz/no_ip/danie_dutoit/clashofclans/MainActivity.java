package biz.no_ip.danie_dutoit.clashofclans;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    public static String name;
    public static String email;

    // Asyntask
    AsyncTask<Void, Void, Void> mRegisterTask;

    Button submitBtn;
    Button refreshNextAttackerListBtn;
    JSONArray data = null;
    Spinner nameSpinner;
    Spinner warSpinner;
    ArrayList<String> gameNamesList = new ArrayList<>();
    ArrayList<Integer> gameNameIDs = new ArrayList<>();
    ArrayList<Integer> playerRanks = new ArrayList<>();
    ArrayList<String> nextAttackerNames = new ArrayList<>();
    ArrayList<String> attackOrderList = new ArrayList<>();
    String selectedGameName;
    String selectedWar;
    Integer selectedWarID;
    GetActiveWars warsDownloader;
    ArrayAdapter<String> NamesDataAdapter;
    ArrayAdapter<String> NextAttackersAdapter;
    private ProgressDialog pDialogWar;
    private ProgressDialog pDialogNames;
    private ProgressDialog pIsPlayerAttacking;
    private int attackingRank = 0;
    private String attackingGameName = "";
    private Spinner recommendedNextAttackersSpinner;
    private String warDate = "";
    private GlobalState gs;
    private TextView lblMessage;
    private ScrollView svMessages;

    // Create a broadcast receiver to get message and show on screen
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String newMessage = intent.getExtras().getString(GCMConfig.EXTRA_MESSAGE);

            // Waking up mobile if it is sleeping
            gs.acquireWakeLock(getApplicationContext());

            // Display message on the screen
            lblMessage.append(newMessage + "\n");

            // Scroll to the bottom of the Scrollview
            svMessages.post(new Runnable() {
                @Override
                public void run() {
                    svMessages.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });

            Toast.makeText(getApplicationContext(), "Got Message: " + newMessage, Toast.LENGTH_LONG).show();

            // Releasing wake lock
            gs.releaseWakeLock();
        }
    };

    @Override
    protected void onRestart() {
        super.onRestart();
        new GetAttackOrders().execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblMessage = (TextView) findViewById(R.id.lblMessage);

        gs = (GlobalState) getApplication();

        // Check if Internet present
        if (!gs.isConnectingToInternet()) {

            // Internet Connection is not present
            gs.showAlertDialog(MainActivity.this,
                    "Internet Connection Error",
                    "Please connect to Internet connection", false);
            // stop executing code by return
            return;
        }

        // Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice(this);

        // Make sure the manifest permissions was properly set
        GCMRegistrar.checkManifest(this);

        // Register custom Broadcast receiver to show messages on activity
        registerReceiver(mHandleMessageReceiver, new IntentFilter(
                GCMConfig.DISPLAY_MESSAGE_ACTION));

        // Get GCM registration id
        final String regId = GCMRegistrar.getRegistrationId(this);

        // Check if regid already presents
        if (regId.equals("")) {

            // Register with GCM
            GCMRegistrar.register(GlobalState.getAppContext(), GCMConfig.GOOGLE_SENDER_ID);

        }

        String versionName = "";
        try {
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ActionBar ab = getActionBar();
        if (ab != null)
            ab.setTitle("COC War Guidance. Version: " + versionName);

        submitBtn = (Button) findViewById(R.id.submit);
        submitBtn.setOnClickListener(this);

        refreshNextAttackerListBtn = (Button) findViewById(R.id.btnRefreshAttackersList);
        refreshNextAttackerListBtn.setOnClickListener(this);
        svMessages = (ScrollView) findViewById(R.id.svPushNotifications);

//        getWarsUrl = GlobalState.getInternetURL() + "GetActiveWars.php";
        selectedWarID = 0;

        nameSpinner = (Spinner) findViewById(R.id.gameNameSpinner);
        NamesDataAdapter = new ArrayAdapter<String>
                (MainActivity.this, android.R.layout.simple_spinner_dropdown_item, gameNamesList);


        nameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int index = nameSpinner.getSelectedItemPosition();
                selectedGameName = gameNamesList.get(index);
                gs.setGameName(selectedGameName);
                gs.setOurParticipantID(gameNameIDs.get(index));
                gs.setRank(playerRanks.get(index));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGameName = gameNamesList.get(0);
                gs.setGameName(selectedGameName);
                gs.setOurParticipantID(gameNameIDs.get(0));
                gs.setRank(playerRanks.get(0));
            }
        });

        recommendedNextAttackersSpinner = (Spinner) findViewById(R.id.recommendedNextAttackersSpinner);
        recommendedNextAttackersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = recommendedNextAttackersSpinner.getSelectedItem().toString();
                nameSpinner.setSelection(NamesDataAdapter.getPosition(s));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        new GetActiveWars().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        CreateMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return MenuChoice(item);
    }

    private void CreateMenu(Menu menu) {
        MenuItem mnu0 = menu.add(0, 0, 1, "Setup GCM");
        {
            mnu0.setIcon(R.drawable.ic_launcher);
        }
        MenuItem mnu1 = menu.add(0, 1, 2, "War Progress");
        {
            mnu1.setIcon(R.drawable.ic_launcher);
        }
        MenuItem mnu2 = menu.add(0, 2, 3, "Us VS Them");
        {
            mnu2.setIcon(R.drawable.ic_launcher);
        }
        MenuItem mnu3 = menu.add(0, 3, 4, "Stars Left");
        {
            mnu3.setIcon(R.drawable.ic_launcher);
        }
    }

    private boolean MenuChoice(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                Intent setupGCMIntent = new Intent("android.intent.action.GCMRegisterActivity");
                startActivity(setupGCMIntent);
                return true;
            case 1:
                Intent warProgressIntent = new Intent("android.intent.action.WarProgressActivity");
                startActivity(warProgressIntent);
                return true;
            case 2:
                Intent usVsThemIntent = new Intent("android.intent.action.UsVsThemActivity");
                startActivity(usVsThemIntent);
                return true;
            case 3:
                Intent StarsLeftIntent = new Intent("android.intent.action.StarsLeftToBeWinActivity");
                startActivity(StarsLeftIntent);
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submit:
                new IsSomebodyAttacking().execute();
                break;
            case R.id.btnRefreshAttackersList:
                new GetAttackOrders().execute();
            default:
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onDestroy() {
        // Cancel AsyncTask
        if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
        try {
            // Unregister Broadcast Receiver
            unregisterReceiver(mHandleMessageReceiver);

            //Clear internal resources.
            GCMRegistrar.onDestroy(this);

        } catch (Exception e) {
            Log.e("UnRegister Error", "> " + e.getMessage());
        }
        super.onDestroy();
    }

    private class GetParticipantsForWar extends AsyncTask<Void, Void, Void> {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition tryAgain = lock.newCondition();
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();
        private volatile boolean finished = false;

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", selectedWarID.toString()));
            queryParams.add(new BasicNameValuePair("clanID", GlobalState.getClanID().toString()));
            String jsonStr = sh.makeServiceCall(GlobalState.getInternetURL() + "get_participantsForWarFoApp.php", ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            gameNamesList.clear();
            gameNameIDs.clear();
            playerRanks.clear();

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("warParticipants");
                    Log.e("JSONData", data.getString(0));

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        gameNamesList.add(c.getString("gamename") + "(" + String.valueOf(c.getInt("rank")) + ")");
                        gameNameIDs.add(c.getInt("ourparticipantid"));
                        playerRanks.add(c.getInt("rank"));
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
//            pDialogNames = new ProgressDialog(MainActivity.this);
//            pDialogNames.setMessage("Downloading Participants. Please wait...");
//            pDialogNames.setCancelable(true);
//            pDialogNames.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
//            if (pDialogNames.isShowing()) {
//                pDialogNames.dismiss();
//            }

            NamesDataAdapter = new ArrayAdapter<String>
                    (MainActivity.this, android.R.layout.simple_spinner_dropdown_item, gameNamesList);
            nameSpinner.setAdapter(NamesDataAdapter);
            new GetNumberOfParticipants().execute();
        }
    }

    private class GetAttackOrders extends AsyncTask<Void, Void, Void> {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition tryAgain = lock.newCondition();
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();
        private volatile boolean finished = false;

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", selectedWarID.toString()));
            String jsonStr = sh.makeServiceCall(GlobalState.getInternetURL() + "get_orderOfAttacks.php", ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            nextAttackerNames.clear();

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("orderOfAttacks");
                    Log.e("JSONData", data.getString(0));

                    // Gert the first rank to set the gameNane list to the same
                    JSONObject c = data.getJSONObject(0);
                    Integer selectedRank = c.getInt("OurRank");

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        c = data.getJSONObject(i);
                        nextAttackerNames.add(c.getString("GameName") + "(" + c.getString("OurRank") + ")");
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
            // Dismiss the progress dialog
            if (pDialogWar.isShowing()) {
                pDialogWar.dismiss();
            }
            pDialogNames = new ProgressDialog(MainActivity.this);
            pDialogNames.setMessage("Downloading Order of attacks. Please wait...");
            pDialogNames.setCancelable(true);
            pDialogNames.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pDialogNames.isShowing()) {
                pDialogNames.dismiss();
            }

            NextAttackersAdapter = new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_list_item_1, nextAttackerNames);
            recommendedNextAttackersSpinner.setAdapter(NextAttackersAdapter);
        }
    }

    private class GetActiveWars extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("clanID", GlobalState.getClanID().toString()));
            String jsonStr = sh.makeServiceCall(GlobalState.getInternetURL() + "GetActiveWars.php", ServiceHandler.POST, queryParams);
            //            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("wars");
                    Log.e("JSONData", data.getString(0));

                    // Data node is JSON Object
                    // Only one record is expected
                    JSONObject c = data.getJSONObject(0);
                    warDate = c.getString("Date"); // there should only be one
                    selectedWarID = c.getInt("WarID");
                    gs.setWarName(selectedWar);
                    gs.setWarID(selectedWarID);
                } catch (JSONException e) {
                    // Dismiss the progress dialog
                    if (pDialogWar.isShowing()) {
                        pDialogWar.dismiss();
                    }
                    warsDownloader.cancel(true);
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Result")
                    .setMessage("An unknown network error occured. The program will close.")
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialogWar = new ProgressDialog(MainActivity.this);
            pDialogWar.setMessage("Downloading Data. Please wait...");
            pDialogWar.setCancelable(true);
            pDialogWar.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            TextView tv = (TextView) findViewById(R.id.warText);
            tv.setText("War date: " + warDate);

            new GetParticipantsForWar().execute();
        }
    }

    private class GetNumberOfParticipants extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            String warID = gs.getWarID().toString();
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("WarID", (warID)));

            String jsonStr = sh.makeServiceCall(GlobalState.getInternetURL() + "get_NumberOfParticipants.php", ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("number");
                    Log.e("JSONData", data.getString(0));

                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        int t = c.getInt("counter");
                        gs.setNumberOfParticipants(t);
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
//            pDialogNumParticipants = new ProgressDialog(MainActivity.this);
//            pDialogNumParticipants.setMessage("Downloading Number Of Participants. Please wait...");
//            pDialogNumParticipants.setCancelable(true);
//            pDialogNumParticipants.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
//            if (pDialogNumParticipants.isShowing()) {
//                pDialogNumParticipants.dismiss();
//            }

            new GetAttackOrders().execute();
        }
    }

    private class IsSomebodyAttacking extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            String jsonStr = sh.makeServiceCall(GlobalState.getInternetURL() + "get_IsAttackingRank.php", ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("busyAttackingRank");
                    // Will only be one result
                    // Data node is JSON Object
                    JSONObject c = data.getJSONObject(0);
                    attackingRank = c.getInt("rank");
                    attackingGameName = c.getString("gameName");
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
            pIsPlayerAttacking = new ProgressDialog(MainActivity.this);
            pIsPlayerAttacking.setMessage("Checking if somebody else is attacking. Please wait...");
            pIsPlayerAttacking.setCancelable(true);
            pIsPlayerAttacking.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pIsPlayerAttacking.isShowing()) {
                pIsPlayerAttacking.dismiss();
            }
            if (attackingRank > 0) {
                if (attackingRank > 0) {
                    // Display a message to say that someone else is busy with an attack and that the player should try again later
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Attack in progress")
                            .setMessage("There is curently another attack in progress. Their Number " + attackingRank + " is being attacked by " + attackingGameName + ". Please try again in a few minutes time.")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }

            } else {
                Intent intent = new Intent("android.intent.action.SelectionActivity");
                startActivity(intent);
            }
        }
    }
}
