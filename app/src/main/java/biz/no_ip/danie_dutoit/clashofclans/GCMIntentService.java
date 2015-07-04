package biz.no_ip.danie_dutoit.clashofclans;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class GCMIntentService extends GCMBaseIntentService {
    GlobalState gs;

    private static final String TAG = "GCMIntentService";

    private GlobalState aGCMController = null;

    public GCMIntentService() {
        // Call extended class Constructor GCMBaseIntentService
        super(GCMConfig.GOOGLE_SENDER_ID);
    }

    /**
     * Method called on device registered
     **/
    @Override
    protected void onRegistered(Context context, String registrationId) {

        //Get Global Controller Class object (see application tag in AndroidManifest.xml)
        if(aGCMController == null)
            aGCMController = (GlobalState) getApplicationContext();

        Log.i(TAG, "Device registered: regId = " + registrationId);
        aGCMController.displayMessageOnScreen(context, "Your device registred with GCM");
        aGCMController.displayMessageOnScreen(context, "Please register GCM Id to the COC WEB Server");

        generateNotification(context, "attPlease register GCM Id to the COC WEB Server");
//        Log.d("NAME", MainActivity.name);
//        aGCMController.register(context, gs.getGameName(), MainActivity.email, registrationId);
    }

    /**
     * Method called on device unregistred
     * */
    @Override
    protected void onUnregistered(Context context, String registrationId) {
        if(aGCMController == null)
            aGCMController = (GlobalState) getApplicationContext();
        Log.i(TAG, "Device unregistered");
        aGCMController.displayMessageOnScreen(context, getString(R.string.gcm_unregistered));
        aGCMController.unregister(context, registrationId);
    }

    /**
     * Method called on Receiving a new message from GCM server
     * */
    @Override
    protected void onMessage(Context context, Intent intent) {
        if(aGCMController == null)
            aGCMController = (GlobalState) getApplicationContext();

        Log.i(TAG, "Received message");
        String message = intent.getExtras().getString("data");

        String actualMessage = message;

        // check if the message is an [ann]ouncement or an [att]ention message or a [cal]l for action
        if (message.startsWith("att") || message.startsWith("ann") || message.startsWith("cal")) {
            actualMessage = message.substring(3);
        }
        aGCMController.displayMessageOnScreen(context, actualMessage);
        // notifies user
        generateNotification(context, message);
    }

    /**
     * Method called on receiving a deleted message
     * */
    @Override
    protected void onDeletedMessages(Context context, int total) {

        if(aGCMController == null)
            aGCMController = (GlobalState) getApplicationContext();

        Log.i(TAG, "Received deleted messages notification");
        String message = getString(R.string.gcm_deleted, total);
        aGCMController.displayMessageOnScreen(context, message);
        // notifies user
        generateNotification(context, message);
    }

    /**
     * Method called on Error
     * */
    @Override
    public void onError(Context context, String errorId) {

        if(aGCMController == null)
            aGCMController = (GlobalState) getApplicationContext();

        Log.i(TAG, "Received error: " + errorId);
        aGCMController.displayMessageOnScreen(context, getString(R.string.gcm_error, errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {

        if(aGCMController == null)
            aGCMController = (GlobalState) getApplicationContext();

        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        aGCMController.displayMessageOnScreen(context, getString(R.string.gcm_recoverable_error,
                errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Create a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context, String message) {
        int icon = R.drawable.ic_launcher;
        long when = System.currentTimeMillis();
        String actualMessage = message;

        // check if the message is an [ann]ouncement or an [att]ention message or a [cal]l for action
        if (message.startsWith("att") || message.startsWith("ann") || message.startsWith("cal")) {
            actualMessage = message.substring(3);
        }

        // Get the first 3 characters of the message
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, actualMessage, when);

        String title = context.getString(R.string.app_name);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, actualMessage, intent);

        notification.defaults = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Play specific notification sound
        // "ann" - announcement message; "att" - attention message; "cal" - call to attack
        if (message.startsWith("att")) { // this message is for attention
            notification.sound = Uri.parse("android.resource://" +
                    context.getPackageName() + "/" + R.raw.attention);
        } else if (message.startsWith("ann")) { // This is an announcement
            notification.sound = Uri.parse("android.resource://" +
                    context.getPackageName() + "/" + R.raw.announcement);
        } else if (message.startsWith("cal")) { // This is an call for attack
            notification.sound = Uri.parse("android.resource://" +
                    context.getPackageName() + "/" + R.raw.call_to_attack);
        } else {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }


        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notificationManager.notify(0, notification);
    }

}
