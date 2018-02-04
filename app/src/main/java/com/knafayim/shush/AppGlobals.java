package com.knafayim.shush;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by s9iper1 on 11/9/17.
 */

public class AppGlobals extends Application {

    private static Context sContext;
    private static final String KEY_PREVIOUS_RINGER_STATE = "previous_ringer_state";
    private static final String KEY_FENCE_RADIUS = "fence_radius";
    private static final String KEY_START_AFTER_BOOT = "start_after_boot";
    private static final String KEY_SERVICE_ACTIVE = "is_service_active";
    public static int LOCATION_ENABLE = 12;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }

    public static Context getContext() {
        return sContext;
    }

    public static void setPreviousRingerState(int previousRingerState) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putInt(KEY_PREVIOUS_RINGER_STATE, previousRingerState).apply();
    }

    public static int getPreviousRingerState() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getInt(KEY_PREVIOUS_RINGER_STATE, -1);
    }

    public static SharedPreferences getPreferenceManager() {
        return getContext().getSharedPreferences("com.knafayim.sharedpreferences", MODE_PRIVATE);
    }

    public static void setFenceRadius(int radius) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putInt(KEY_FENCE_RADIUS, radius).apply();
    }

    public static int getFenceRadius() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getInt(KEY_FENCE_RADIUS, 150);
    }

    public static int getEnterMode() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getInt("entry", 1);
    }

    public static int getExitMode() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getInt("exit", 0);
    }

    public static void setEnterMode(int value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putInt("entry", value).apply();
    }

    public static void setExitMode(int value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putInt("exit", value).apply();
    }

    public static boolean isModeChangedByUs() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean("ringer_mode", false);
    }

    public static void ringerModeChangedByUs(boolean value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean("ringer_mode", value).apply();
    }

    public static void setServiceState(boolean serviceState) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(KEY_SERVICE_ACTIVE, serviceState).apply();
    }

    public static boolean isServiceActive() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(KEY_SERVICE_ACTIVE, false);
    }

    public static void startAfterBoot(boolean startAfterBoot) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(KEY_START_AFTER_BOOT, startAfterBoot).apply();
    }

    public static boolean shouldStartAfterBoot() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(KEY_START_AFTER_BOOT, false);
    }
}
