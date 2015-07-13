package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class statWarProgressRecord {
    statWarProgressRecord() {
        gameName = "";
        attack = "";
        attack = "";
        starsTaken = 0;
        theirRank = 0;
        ourRank = 0;
    }
    public String gameName;
    public String attack;
    public int starsTaken;
    public int ourRank;
    public int theirRank;
}

/**
 * Created by dutoitd1 on 2015/06/12.
 */
public class WarProgressActivity extends Activity {
    private ProgressDialog pStatsDialog;
    private GlobalState gs;
    JSONArray data = null;
    private List<statWarProgressRecord> statWarProgressRecords;
    private StatsWarProgressGrid adapter;
    private GridView grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_war_progress);

        gs = (GlobalState) getApplication();
        statWarProgressRecords = new ArrayList<>();

        new GetWarStats().execute();
    }

    private class GetWarStats extends AsyncTask<Void, Void, Void> {
        // Creating service handler class instance
        ServiceHandler sh = new ServiceHandler();

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected Void doInBackground(Void... params) {
            List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("selectedWarID", gs.getWarID().toString()));
            String jsonStr = sh.makeServiceCall(GlobalState.getInternetURL() + "get_WarStats.php", ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("stats");
                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        statWarProgressRecord sr = new statWarProgressRecord();
                        sr.gameName = c.getString("GameName");
                        sr.attack= c.getString("Attack");
                        try {
                            sr.starsTaken = c.getInt("StarsTaken");
                        } catch (Exception e) {
                            sr.starsTaken = - 1;
                        }
                        sr.ourRank = c.getInt("OurRank");
                        try {
                            sr.theirRank = c.getInt("TheirRank");
                        } catch (Exception e) {
                            sr.theirRank = -1;
                        }
                        statWarProgressRecords.add(sr);
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
            pStatsDialog = new ProgressDialog(WarProgressActivity.this);
            pStatsDialog.setMessage("Downloading Participants. Please wait...");
            pStatsDialog.setCancelable(true);
            pStatsDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Dismiss the progress dialog
            if (pStatsDialog.isShowing()) {
                pStatsDialog.dismiss();
            }
            adapter = new StatsWarProgressGrid(WarProgressActivity.this, statWarProgressRecords);
            grid = (GridView) findViewById(R.id.warProgressGrid);
            grid.setAdapter(adapter);
//            grid.invalidateViews();
//            grid.setAdapter(adapter);
        }
    }
}
