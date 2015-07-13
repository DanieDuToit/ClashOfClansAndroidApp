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

class starsLeftToBeWinRecord {
    starsLeftToBeWinRecord() {
        theirRank = "";
        starsLeftImage = 0;
    }
    public String theirRank;
    public Integer starsLeftImage;
}


/**
 * Created by Danie on 2015/06/15.
 */
public class StarsLeftToBeWinActivity extends Activity {
    private ProgressDialog pStatsDialog;
    private GlobalState gs;
    JSONArray data = null;
    private List<starsLeftToBeWinRecord> starsLeftToBeWinRecords;
    private StarsLeftToBeWinGrid adapter;
    private GridView grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stars_left_to_win);

        gs = (GlobalState) getApplication();
        starsLeftToBeWinRecords = new ArrayList<>();

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
            String jsonStr = sh.makeServiceCall(GlobalState.getInternetURL() + "get_StarsToBeWin.php", ServiceHandler.POST, queryParams);
            Log.e("JSONString", jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    data = jsonObj.getJSONArray("starsLeftToWin");

                    for (int i = 0; i < data.length(); i++) {
                        // Data node is JSON Object
                        JSONObject c = data.getJSONObject(i);
                        starsLeftToBeWinRecord sr = new starsLeftToBeWinRecord();
                        sr.theirRank = c.getString("TheirRank");
                        switch(c.getInt("StarsToBeWin")) {
                            case 0:
                                sr.starsLeftImage = R.drawable.flat_no_stars;
                                break;
                            case 1:
                                sr.starsLeftImage = R.drawable.flat_one_star;
                                break;
                            case 2:
                                sr.starsLeftImage = R.drawable.flat_two_stars;
                                break;
                            case 3:
                                sr.starsLeftImage = R.drawable.flat_three_stars;
                                break;
                        }
                        starsLeftToBeWinRecords.add(sr);
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
            pStatsDialog = new ProgressDialog(StarsLeftToBeWinActivity.this);
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
            adapter = new StarsLeftToBeWinGrid(StarsLeftToBeWinActivity.this, starsLeftToBeWinRecords);
            grid = (GridView) findViewById(R.id.StarsLeftToWinGrid);
            grid.setAdapter(adapter);
//            grid.invalidateViews();
//            grid.setAdapter(adapter);
        }
    }
}
