package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by dutoitd1 on 2015/06/08.
 */
public class SelectResultActivity extends Activity {
    JSONArray data = null;
    GlobalState gs;
    ProgressDialog pUnlockLockRank;
    GridView grid;
    int starstaken;
    String[] web = {"0", "1", "2", "3"};
    int[] imageId = {
            R.drawable.zerostars,
            R.drawable.onestar,
            R.drawable.twostar,
            R.drawable.threestars
    };
    private String unLockAttackUrl;
    private String setScoreUrl;
    private SelectResultCustomGrid adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_result);

        gs = (GlobalState) getApplication();

        unLockAttackUrl = gs.getInternetURL() + "removeAttackLock.php";
        setScoreUrl = gs.getInternetURL() + "update_ourAttackForApp.php";

        adapter = new SelectResultCustomGrid(SelectResultActivity.this, web, imageId);
        grid = (GridView) findViewById(R.id.selectResultGridView);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {
                new AlertDialog.Builder(SelectResultActivity.this)
                        .setTitle("Attack in progress")
                        .setMessage("You selected " + position + " stars. Correct?")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Set the stars for the attack
                                starstaken = position;
                                new SetScore().execute();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Do Nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Remove the lock
        new UnLockAttack().execute();//
//        Toast.makeText(SelectResultActivity.this, "You Clicked Back Button", Toast.LENGTH_SHORT).show();
    }

    private class UnLockAttack extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            String jsonStr = sh.makeServiceCall(unLockAttackUrl, ServiceHandler.POST, queryParams);
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
            pUnlockLockRank = new ProgressDialog(SelectResultActivity.this);
            pUnlockLockRank.setMessage("Unlocking Attacks. Please wait...");
            pUnlockLockRank.setCancelable(true);
            pUnlockLockRank.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pUnlockLockRank.isShowing()) {
                pUnlockLockRank.dismiss();
            }
            finish();
        }
    }

    private class SetScore extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        @Override
        protected Void doInBackground(Void... voids) {
            // Making a request to url and getting response
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();

            int selectedWarID = gs.getWarID();
            int st = starstaken;
            int theirRank = gs.getTheirRank();

            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            queryParams.add(new BasicNameValuePair("ourparticipantid", gs.getOurParticipantID().toString()));
            queryParams.add(new BasicNameValuePair("starstaken", String.valueOf(starstaken)));
            queryParams.add(new BasicNameValuePair("theirRank", gs.getTheirRank().toString()));
            String jsonStr = sh.makeServiceCall(setScoreUrl, ServiceHandler.POST, queryParams);

            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
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
            pUnlockLockRank = new ProgressDialog(SelectResultActivity.this);
            pUnlockLockRank.setMessage("Unlocking Attacks. Please wait...");
            pUnlockLockRank.setCancelable(true);
            pUnlockLockRank.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pUnlockLockRank.isShowing()) {
                pUnlockLockRank.dismiss();
            }
            new UnLockAttack().execute();
        }
    }
}
