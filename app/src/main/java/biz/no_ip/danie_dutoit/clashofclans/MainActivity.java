package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
    final ArrayList<String> warList = new ArrayList<>();
    final ArrayList<Integer> warIDList = new ArrayList<>();
    Button submitBtn;
    JSONArray data = null;
    Spinner nameSpinner;
    Spinner warSpinner;
    ArrayList<String> gameNamesList = new ArrayList<>();
    ArrayList<Integer> gameNameIDs = new ArrayList<>();
    ArrayList<Integer> playerRanks = new ArrayList<>();
    ArrayList<String> nextAttackerNames = new ArrayList<>();
    ArrayList<Integer> nextAttacker = new ArrayList<>();
    String selectedGameName;
    String selectedWar;
    Integer selectedWarID;
    ArrayAdapter<String> warSelectorAdapter;
    GetParticipantsForWar participantsDownloader;
    GetNumberOfParticipants numberOfparticipantsDownloader;
    GetActiveWars warsDownloader;
    ArrayAdapter<String> WarsDataAdapter;
    ArrayAdapter<String> NamesDataAdapter;
    ArrayAdapter<String> NextAttackersAdapter;
    private String getWarsUrl = "";
    private String getNumberOfParticipantsUrl = "";
    private String getParticipantNamesUrl = "";
    private String isSomebodyAttackingUrl = "";
    ProgressDialog pDialogNumParticipants;
    private ProgressDialog pDialogWar;
    private ProgressDialog pDialogNames;
    private ProgressDialog pIsPlayerAttacking;
    private GlobalState gs;
    private IsSomebodyAttacking somebodyAttackingDownloader;
    private int prevWarID = 0;
    private int attackingRank = 0;
    private String attackingGameName = "";
    private ProgressBar pb;
    private ListView lvAttackers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        submitBtn = (Button) findViewById(R.id.submit);
        submitBtn.setOnClickListener(this);
        gs = (GlobalState) getApplication();
        getWarsUrl = gs.getInternetURL() + "GetActiveWars.php";
        getParticipantNamesUrl = gs.getInternetURL() + "get_participantsForWarFoApp.php";
        getNumberOfParticipantsUrl = gs.getInternetURL() + "get_NumberOfParticipants.php";
        isSomebodyAttackingUrl = gs.getInternetURL() + "get_IsAttackingRank.php";
        selectedWarID = 0;

        // Spinner element
        warsDownloader = new GetActiveWars();
        warsDownloader.execute();

//        participantsDownloader = new GetParticipantsForWar();
//        participantsDownloader.execute();
//
//        numberOfparticipantsDownloader = new GetNumberOfParticipants();
//        numberOfparticipantsDownloader.execute();

//        somebodyAttackingDownloader = new IsSomebodyAttacking();

        lvAttackers = (ListView) findViewById(R.id.nextAttackersListView);

        warSpinner = (Spinner) findViewById(R.id.selectWarSpinner);
        nameSpinner = (Spinner) findViewById(R.id.gameNameSpinner);

        NamesDataAdapter = new ArrayAdapter<String>
                (MainActivity.this, android.R.layout.simple_spinner_dropdown_item, gameNamesList);
        WarsDataAdapter = new ArrayAdapter<String>
                (MainActivity.this, android.R.layout.simple_spinner_dropdown_item, warList);

        warSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int index = warSpinner.getSelectedItemPosition();
                selectedWar = warList.get(index);
                selectedWarID = warIDList.get(index);
                gs.setWarName(selectedWar);
                gs.setWarID(selectedWarID);
                // did this to skip it for only the first time
                if (prevWarID != 0) {
                    prevWarID = selectedWarID;
                    NamesDataAdapter.clear();
                    participantsDownloader.execute();
                }
                prevWarID = selectedWarID;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedWar = warList.get(0);
                gs.setWarName(selectedWar);
                gs.setWarID(warIDList.get(0));
            }
        });

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
        MenuItem mnu1 = menu.add(0, 0, 0, "War Progress");
        {
//            mnu1.setAlphabeticShortcut('p');
            mnu1.setIcon(R.drawable.ic_launcher);
        }
        MenuItem mnu2 = menu.add(0, 1, 1, "Us VS Them");
        {
//            mnu2.setAlphabeticShortcut('v');
            mnu2.setIcon(R.drawable.ic_launcher);
        }
        MenuItem mnu3 = menu.add(0, 2, 2, "Stars Left");
        {
//            mnu3.setAlphabeticShortcut('c');
            mnu3.setIcon(R.drawable.ic_launcher);
        }
    }

    private boolean MenuChoice(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                Intent warProgressIntent = new Intent("android.intent.action.WarProgressActivity");
                startActivity(warProgressIntent);
                return true;
            case 1:
                Intent usVsThemIntent = new Intent("android.intent.action.UsVsThemActivity");
                startActivity(usVsThemIntent);
                return true;
            case 2:
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
            default:
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedWar = warList.get(position);
        selectedWarID = warIDList.get(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
            String jsonStr = sh.makeServiceCall(getParticipantNamesUrl, ServiceHandler.POST, queryParams);
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
                        gameNamesList.add(c.getString("gamename"));
                        gameNameIDs.add(c.getInt("ourparticipantid"));
                        playerRanks.add(c.getInt("rank"));
                        nextAttacker.add(c.getInt("nextattacker"));
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
            pDialogNames = new ProgressDialog(MainActivity.this);
            pDialogNames.setMessage("Downloading Participants. Please wait...");
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

            nextAttackerNames.clear();
            for (int i = 0; i < nextAttacker.size(); i++) {
                if (nextAttacker.get(i) == 1) {
                    nextAttackerNames.add(gameNamesList.get(i));
                }
            }

            NextAttackersAdapter = new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_list_item_1, nextAttackerNames);
            lvAttackers.setAdapter(NextAttackersAdapter);

            nameSpinner.setAdapter(NamesDataAdapter);
            new GetNumberOfParticipants().execute();
        }
    }

    private class GetActiveWars extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("dummy", ""));
            String jsonStr = sh.makeServiceCall(getWarsUrl, ServiceHandler.POST, queryParams);
//            Log.e("JSONString", jsonStr);

            warList.clear();
            warIDList.clear();

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("wars");
                    Log.e("JSONData", data.getString(0));

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        publishProgress();
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        warList.add(c.getString("Date"));
                        warIDList.add(c.getInt("WarID"));
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
            pDialogWar = new ProgressDialog(MainActivity.this);
            pDialogWar.setMessage("Downloading Wars. Please wait...");
            pDialogWar.setCancelable(true);
            pDialogWar.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pDialogWar.isShowing()) {
                pDialogWar.dismiss();
            }
            warSpinner.setAdapter(WarsDataAdapter);
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

            String jsonStr = sh.makeServiceCall(getNumberOfParticipantsUrl, ServiceHandler.POST, queryParams);
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
            pDialogNumParticipants = new ProgressDialog(MainActivity.this);
            pDialogNumParticipants.setMessage("Downloading Number Of Participants. Please wait...");
            pDialogNumParticipants.setCancelable(true);
            pDialogNumParticipants.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pDialogNumParticipants.isShowing()) {
                pDialogNumParticipants.dismiss();
            }
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
            String jsonStr = sh.makeServiceCall(isSomebodyAttackingUrl, ServiceHandler.POST, queryParams);
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
