package biz.no_ip.danie_dutoit.clashofclans;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import android.provider.Settings.Secure;

public class GlobalState extends Application {

    private final Random random = new Random();
    private Integer ourParticipantID;
    private Integer warID;
    private Integer rank;
    private Integer theirRank;
    private String warName;
    private String gameName;
    private Integer numberOfParticipants = 0;
    private PowerManager.WakeLock wakeLock;
    private static Context context;
    private static String android_id;

    public static Integer getClanID() {
        // The ClanID will be hardcoded for each Clan
        return 1; // DragonHeart's Clan ID
    }

    public static String getInternetURL() {
		return "http://172.24.0.239/ClashOfClans/";
//        return "http://172.24.0.239:9001/ClashOfClans/";
//        return "http://daniedutoit.no-ip.biz/ClashOfClans/";
//        return "http://10.0.0.6:9001/ClashOfClans/";
//        return "http://10.0.0.6/ClashOfClans/";
    }

    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    public static Context getAppContext() {
        return GlobalState.context;
    }

    // Issue a POST request to the server.
    private static void post(String endpoint, Map<String, String> params)
            throws IOException {

        URL url;
        try {

            url = new URL(endpoint);

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }

        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();

        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }

        String body = bodyBuilder.toString();

        Log.v(GCMConfig.TAG, "Posting '" + body + "' to " + url);

        byte[] bytes = body.getBytes();

        HttpURLConnection conn = null;
        try {

            Log.e("URL", "> " + url);

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();

            // handle the response
            int status = conn.getResponseCode();

            // If response is not success
            if (status != 200) {

                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public String getAndroid_ID() {
        return android_id;
    }

    public Integer getOurParticipantID() {
        return ourParticipantID;
    }

    public void setOurParticipantID(Integer participantID) {
        this.ourParticipantID = participantID;
    }

    public Integer getNumberOfParticipants() {
        return numberOfParticipants;
    }

    public void setNumberOfParticipants(Integer numberOfParticipants) {
        this.numberOfParticipants = numberOfParticipants;
    }

    public Integer getRank() {
        return rank;
    }
    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getTheirRank() {
        return theirRank;
    }
    public void setTheirRank(Integer theirRank) {
        this.theirRank = theirRank;
    }

    public Integer getWarID() {
        return warID;
    }
    public void setWarID(Integer warid) {
        this.warID = warid;
    }

    public String getGameName() {
        return gameName;
    }
    public void setGameName(String gamename) {
        this.gameName = gamename;
    }

    public String getWarName() {
        return warName;
    }
    public void setWarName(String warname) {
        this.warName = warname;
    }

    public void sendGlobalNotification(String message) {
        String serverUrl = getInternetURL() + "GCM_sendGlobalNotification.php";
        Map<String, String> params = new HashMap<String, String>();
        params.put("message", message);
        params.put("clanID", this.getClanID().toString());
        int BACKOFF_MILLI_SECONDS = 2000;
        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        int MAX_ATTEMPTS = 5;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Log.d(GCMConfig.TAG, "Attempt #" + i + " to register");
            try {
                //Send Broadcast to Show message on screen
                displayMessageOnScreen(context, context.getString(
                                R.string.sending_push_notification, gameName)
                );
                // Post registration values to web server
                post(serverUrl, params);
                return;
            } catch (IOException e) {
                Log.e(GCMConfig.TAG, "Failed to send notification on attempt " + i + ":" + e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Log.d(GCMConfig.TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    Log.d(GCMConfig.TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
    }

    // Register this account with the server.
//    void register(final Context context, String name, String email, final String regId) {
//
//        Log.i(GCMConfig.TAG, "registering device (regId = " + regId + ")");
//
//        String serverUrl = getInternetURL() + "GCM_Register.php";
//
//        Map<String, String> params = new HashMap<String, String>();
//        params.put("regId", regId);
//        params.put("name", name);
//        params.put("email", email);
//
//        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
//
//        // Once GCM returns a registration id, we need to register on our server
//        // As the server might be down, we will retry it a couple
//        // times.
//        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
//
//            Log.d(GCMConfig.TAG, "Attempt #" + i + " to register");
//
//            try {
//                //Send Broadcast to Show message on screen
//                displayMessageOnScreen(context, context.getString(
//                        R.string.server_registering, i, MAX_ATTEMPTS));
//
//                // Post registration values to web server
//                post(serverUrl, params);
//
//                GCMRegistrar.setRegisteredOnServer(context, true);
//
//                //Send Broadcast to Show message on screen
//                String message = context.getString(R.string.server_registered);
//                displayMessageOnScreen(context, message);
//
//                return;
//            } catch (IOException e) {
//
//                // Here we are simplifying and retrying on any error; in a real
//                // application, it should retry only on unrecoverable errors
//                // (like HTTP error code 503).
//
//                Log.e(GCMConfig.TAG, "Failed to register on attempt " + i + ":" + e);
//
//                if (i == MAX_ATTEMPTS) {
//                    break;
//                }
//                try {
//
//                    Log.d(GCMConfig.TAG, "Sleeping for " + backoff + " ms before retry");
//                    Thread.sleep(backoff);
//
//                } catch (InterruptedException e1) {
//                    // Activity finished before we complete - exit.
//                    Log.d(GCMConfig.TAG, "Thread interrupted: abort remaining retries!");
//                    Thread.currentThread().interrupt();
//                    return;
//                }
//
//                // increase backoff exponentially
//                backoff *= 2;
//            }
//        }
//
//        String message = context.getString(R.string.server_register_error,
//                MAX_ATTEMPTS);
//
//        //Send Broadcast to Show message on screen
//        displayMessageOnScreen(context, message);
//    }

    // Unregister this account/device pair within the server.
    void unregister(final Context context, final String regId) {

        Log.i(GCMConfig.TAG, "unregistering device (regId = " + regId + ")");

        String serverUrl = getInternetURL() + "GCM_Register.php/unregister";
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);

        try {
            post(serverUrl, params);
            GCMRegistrar.setRegisteredOnServer(context, false);
            String message = context.getString(R.string.server_unregistered);
            displayMessageOnScreen(context, message);
        } catch (IOException e) {

            // At this point the device is unregistered from GCM, but still
            // registered in the our server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.

            String message = context.getString(R.string.server_unregister_error,
                    e.getMessage());
            displayMessageOnScreen(context, message);
        }
    }

    // Checking for all possible internet providers
    public boolean isConnectingToInternet() {

        ConnectivityManager connectivity =
                (ConnectivityManager) getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    // Notifies UI to display a message.
    void displayMessageOnScreen(Context context, String message) {

        Intent intent = new Intent(GCMConfig.DISPLAY_MESSAGE_ACTION);
        intent.putExtra(GCMConfig.EXTRA_MESSAGE, message);

        // Send Broadcast to Broadcast receiver with message
        context.sendBroadcast(intent);

    }

    //Function to display simple Alert Dialog
    public void showAlertDialog(Context context, String title, String message,
                                Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();

        // Set Dialog Title
        alertDialog.setTitle(title);

        // Set Dialog Message
        alertDialog.setMessage(message);

        if (status != null)
            // Set alert dialog icon
            alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);

        // Set OK Button
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        // Show Alert Message
        alertDialog.show();
    }

    public void acquireWakeLock(Context context) {
        if (wakeLock != null) wakeLock.release();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "WakeLock");

        wakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (wakeLock != null) wakeLock.release();
        wakeLock = null;
    }
}
