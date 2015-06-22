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

class statUsVsThemRecord {
    statUsVsThemRecord() {
        gameName = "";
        ourRank = "";
        ourExperience = "";
        ourTownHall = "";
        theirRank = "";
        theirExperience = "";
        theirTownHall = "";
    }
    public String gameName;
    public String ourRank;
    public String ourExperience;
    public String ourTownHall;
    public String theirRank;
    public String theirExperience;
    public String theirTownHall;
}

/**
 * Created by Danie on 2015/06/15.
 */
public class UsVsThemActivity extends Activity {
    private ProgressDialog pStatsDialog;
    private GlobalState gs;
    JSONArray data = null;
    private List<statUsVsThemRecord> statUsVsThemRecords;
    private StatsUsVsThemGrid adapter;
    private GridView grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_us_vs_them);

        gs = (GlobalState) getApplication();
        statUsVsThemRecords = new ArrayList<>();

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
            String jsonStr = sh.makeServiceCall(gs.getInternetURL() + "get_UsVsThemStats.php", ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("stats");

                    statUsVsThemRecord sr = new statUsVsThemRecord();
                    sr.gameName = "Game Name";
                    sr.ourRank = "Our Rank";
                    sr.ourExperience = "Our Exp";
                    sr.ourTownHall = "Our Townhall";
                    sr.theirRank = "Their Rank";
                    sr.theirExperience = "Their Exp";
                    sr.theirTownHall = "Their Townhall";
                    statUsVsThemRecords.add(sr);

                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        sr = new statUsVsThemRecord();
                        sr.gameName = c.getString("GameName");
                        sr.ourRank = c.getString("OurRank");
                        sr.ourExperience = c.getString("OurExperience");
                        sr.ourTownHall = c.getString("OurTownhall");

                        sr.theirRank = c.getString("TheirRank");
                        sr.theirExperience = c.getString("TheirExperience");
                        sr.theirTownHall = c.getString("TheirTownhall");

                        statUsVsThemRecords.add(sr);
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
            pStatsDialog = new ProgressDialog(UsVsThemActivity.this);
            pStatsDialog.setMessage("Downloading Stats. Please wait...");
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
            adapter = new StatsUsVsThemGrid(UsVsThemActivity.this, statUsVsThemRecords);
            grid = (GridView) findViewById(R.id.UsVsThemGrid);
            grid.setAdapter(adapter);
//            grid.invalidateViews();
//            grid.setAdapter(adapter);
        }
    }
}
