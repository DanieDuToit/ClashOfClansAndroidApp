package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Application;

public interface GCMConfig {
    // CONSTANTS
    // Google project id
    static final String GOOGLE_SENDER_ID = "773833774703";  // Place here your Google project id

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM Android Example";

    static final String DISPLAY_MESSAGE_ACTION =
            "biz.no_ip.danie_dutoit.clashofclans.DISPLAY_MESSAGE";

    static final String EXTRA_MESSAGE = "message";
}
