package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by Danie on 2015/06/03.
 */
public class SelectionActivity extends Activity {
    String attacksForPlayerUrl;
    String starsWonUrl;
    String bestAttackForPlayerUrl;
    GridView grid;
    JSONArray data = null;
    private ProgressDialog pAttacksForPlayer;
    private ProgressDialog pBestAttackForPlayer;
    private ProgressDialog pIsPlayerAttacking;
    private ProgressDialog pStarsWon;
    private ProgressDialog pLockRank;
    private HashMap<Integer, Integer> attacks;
    private HashMap<Integer, Integer> attacksDone;
    private HashMap<Integer, Integer> starsWonTable;
    private GetAttacksForPlayer attacksForPlayerDownLoader;
    private GetNextBestAttack nextBestAttackDownloader;
    private IsSomebodyAttacking isSomebodyAttackingDownLoader;
    private LockAttack LockAttackDownloader;
    private CustomGrid adapter;
    private int numberOfAttacks = 0;
    private int starsWon = 0;
    private int nextBestAttack = 0;
    private String isSomebodyAttackingUrl = "";
    private String lockAttackUrl = "";
    private int attackingRank = 0;
    private String attackingGameName = "";
    private int selectedRank = 0;
    private GlobalState gs;
    private TextView gameName;
    private List<String> web;
    private List<Integer> imageId;
    private Button btnStatsView;

//    String[] web = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", // 0 is just a position holder
//            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
//            "21", "22", "23", "24", "25"
//    };

//    int[] imageId = {
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//            R.drawable.no_attack,
//
//    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection);
        web = new ArrayList<>();
        imageId = new ArrayList<>();

        gs = (GlobalState) getApplication();

        for (int i = 0; i < gs.getNumberOfParticipants(); i++) {
            imageId.add(i, R.drawable.no_attack);
        }

        for (int i = 1; i <= gs.getNumberOfParticipants(); i++) {
            web.add(i-1, String.valueOf(i));
        }

        int numberOfParticipants = gs.getNumberOfParticipants();
        attacksForPlayerUrl = gs.getInternetURL() + "get_AttacksForPlayer.php";
        bestAttackForPlayerUrl = gs.getInternetURL() + "get_NextBestAttack.php";
        isSomebodyAttackingUrl = gs.getInternetURL() + "get_IsAttackingRank.php";
        starsWonUrl = gs.getInternetURL() + "get_StarsWon.php";
        lockAttackUrl = gs.getInternetURL() + "set_RankAttacking.php";
        attacks = new HashMap<>();
        attacksDone = new HashMap<>();
        starsWonTable = new HashMap<>();

        gameName = (TextView) findViewById(R.id.gameName);
        gameName.setText(gs.getGameName());

        btnStatsView = (Button) findViewById(R.id.buttonViewWarStats);
        btnStatsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.StatsActivity");
                startActivity(intent);
            }
        });

        new GetNextBestAttack().execute();

        adapter = new CustomGrid(SelectionActivity.this, web, imageId, numberOfParticipants, attacks);
        grid = (GridView) findViewById(R.id.gridView);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (starsWon < 2) { //Still have attack/s left
                    gs.setTheirRank(position + 1);
                    Set<Integer> keys = attacksDone.keySet();
                    for (int key : keys) {
                        if (key == position) {
                            // do nothing
                        } else {
                            selectedRank = position + 1;
                            break;
                        }
                    }
                    new IsSomebodyAttacking().execute();
                } // else Game Over
            }
        });
        grid.setAdapter(adapter);
    }

    private class GetNextBestAttack extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            queryParams.add(new BasicNameValuePair("rank", gs.getRank().toString()));
            String jsonStr = sh.makeServiceCall(bestAttackForPlayerUrl, ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("rankToAttack");
//                    Log.e("JSONData", data.getString(0));

                    // Will only be one result
                    // Data node is JSON Object
                    JSONObject c = data.getJSONObject(0);
                    nextBestAttack = c.getInt("rank");
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
            pBestAttackForPlayer = new ProgressDialog(SelectionActivity.this);
            pBestAttackForPlayer.setMessage("Downloading Best Attack for Player. Please wait...");
            pBestAttackForPlayer.setCancelable(true);
            pBestAttackForPlayer.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pBestAttackForPlayer.isShowing()) {
                pBestAttackForPlayer.dismiss();
            }
            new GetAttacksForPlayer().execute();
        }
    }

    private class GetAttacksForPlayer extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            queryParams.add(new BasicNameValuePair("OurParticipantID", gs.getOurParticipantID().toString()));
            String jsonStr = sh.makeServiceCall(attacksForPlayerUrl, ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            attacks.clear();

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("attacks");
//                    Log.e("JSONData", data.getString(0));

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        attacks.put(c.getInt("TheirRank"), c.getInt("StarsTaken"));
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
            pAttacksForPlayer = new ProgressDialog(SelectionActivity.this);
            pAttacksForPlayer.setMessage("Downloading Attacks for Player. Please wait...");
            pAttacksForPlayer.setCancelable(true);
            pAttacksForPlayer.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pAttacksForPlayer.isShowing()) {
                pAttacksForPlayer.dismiss();
            }
            Set<Integer> keys = attacks.keySet();
            for (int key : keys) {
                if (key > 0) {
                    starsWon++;
                    switch (attacks.get(key)) {
                        case 0:
                            starsWonTable.put(key - 1, 0);
                            imageId.set(key - 1, R.drawable.zerostars);
                            break;
                        case 1:
                            starsWonTable.put(key - 1, 1);
                            imageId.set(key - 1, R.drawable.onestar);
                            break;
                        case 2:
                            starsWonTable.put(key - 1, 2);
                            imageId.set(key - 1, R.drawable.twostar);
                            break;
                        case 3:
                            starsWonTable.put(key - 1, 3);
                            imageId.set(key - 1, R.drawable.threestars);
                            break;
                    }
                }
            }
            new GetStarsWon().execute();
        }
    }

    private class GetStarsWon extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            String jsonStr = sh.makeServiceCall(starsWonUrl, ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            starsWonTable.clear();

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("starsWon");
//                    Log.e("JSONData", data.getString(0));

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        starsWonTable.put(c.getInt("TheirRank"), c.getInt("StarsTaken"));
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
            pStarsWon = new ProgressDialog(SelectionActivity.this);
            pStarsWon.setMessage("Downloading the stars we won. Please wait...");
            pStarsWon.setCancelable(true);
            pStarsWon.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pStarsWon.isShowing()) {
                pStarsWon.dismiss();
            }
            Set<Integer> keys = starsWonTable.keySet();
            for (int i = 1; i <= gs.getNumberOfParticipants(); i++) {
                for (int key : keys) {
                    if (key == i) { //This rank took some stars
                        switch (starsWonTable.get(key)) {
                            case 0:
                                imageId.set(key - 1, R.drawable.zerostars);
                                break;
                            case 1:
                                imageId.set(key - 1, R.drawable.onestar);
                                break;
                            case 2:
                                imageId.set(key - 1, R.drawable.twostar);
                                break;
                            case 3:
                                imageId.set(key - 1, R.drawable.threestars);
                                break;
                        }
                    }
                }
            }
            if (nextBestAttack > 0) {
                web.set(nextBestAttack - 1, "X");
//                textView.setText(web[nextBestAttack - 1]);
//                imageId[nextBestAttack - 1] = R.drawable.suggestedbutton;
            }

            grid.invalidateViews();
            grid.setAdapter(adapter);
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
            pIsPlayerAttacking = new ProgressDialog(SelectionActivity.this);
            pIsPlayerAttacking.setMessage("Check if somebody else is attacking. Please wait...");
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
                    new AlertDialog.Builder(SelectionActivity.this)
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
                // Take the player to a page where he can input his score (stars taken)
                // First "lock" the attacks
                Toast.makeText(SelectionActivity.this, "You Clicked at " + web.get(selectedRank), Toast.LENGTH_SHORT).show();
                new LockAttack().execute();
            }
        }
    }

    private class LockAttack extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            queryParams.add(new BasicNameValuePair("ourParticipantID", gs.getOurParticipantID().toString()));
            queryParams.add(new BasicNameValuePair("rank", String.valueOf(selectedRank)));
            String jsonStr = sh.makeServiceCall(lockAttackUrl, ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("result");
                    // Will only be one result
                    // Data node is JSON Object
                    JSONObject c = data.getJSONObject(0);
                    int success = c.getInt("success");
                    String error = c.getString("error");
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
            pLockRank = new ProgressDialog(SelectionActivity.this);
            pLockRank.setMessage("Locking Rank " + selectedRank + ". Please wait...");
            pLockRank.setCancelable(true);
            pLockRank.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pLockRank.isShowing()) {
                pLockRank.dismiss();
            }
//            Toast.makeText(SelectionActivity.this, "You Clicked at " + web[selectedRank], Toast.LENGTH_SHORT).show();
            Intent intent = new Intent("android.intent.action.SelectResultActivity");
            startActivity(intent);
            finish();
        }
    }
}