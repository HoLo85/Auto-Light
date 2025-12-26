package com.mine.autolight;

public class Constants {
    // Work Modes
    public static final int WORK_MODE_ALWAYS = 1;
    public static final int WORK_MODE_PORTRAIT = 2;
    public static final int WORK_MODE_UNLOCK = 3;
    public static final int WORK_MODE_LANDSCAPE = 4;

    // Intent Actions - Updated to unique package-based string
    public static final String SERVICE_INTENT_ACTION = "com.mine.autolight.ACTION_RECONFIGURE";
    
    // Intent Extras
    public static final String SERVICE_INTENT_EXTRA = "payload_extra";
    public static final String SERVICE_INTENT_EXTRA_TAP = "notification_tap";

    // Intent Payloads
    public static final int SERVICE_INTENT_PAYLOAD_PING = 0;
    public static final int SERVICE_INTENT_PAYLOAD_SET = 1;
}
