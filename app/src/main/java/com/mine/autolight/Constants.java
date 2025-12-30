package com.mine.autolight;

public class Constants {
    public static final int WORK_MODE_ALWAYS = 1;
    public static final int WORK_MODE_PORTRAIT = 2;
    public static final int WORK_MODE_UNLOCK = 3;
    public static final int WORK_MODE_LANDSCAPE = 4;

    // Best practice: fully-qualified action names (avoid collisions with other apps)
    public static final String SERVICE_INTENT_ACTION = "com.mine.autolight.ACTION_LIGHT_COMMAND";

    // Best practice: fully-qualified extra keys (avoid collisions)
    public static final String SERVICE_INTENT_EXTRA = "com.mine.autolight.EXTRA_COMMAND";

    // Payloads
    public static final int SERVICE_INTENT_PAYLOAD_PING = 0;
    public static final int SERVICE_INTENT_PAYLOAD_SET = 1;

    // Optional legacy constant
    public static final String PREF_SERVICE_ENABLED = "pref_service_enabled";
}
