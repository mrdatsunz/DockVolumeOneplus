package com.ordis.volumedock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (intent==null) return;
        String action=intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        if (!c.getSharedPreferences("dock",0).getBoolean("enabled",false)) return;
        if (!Settings.canDrawOverlays(c)) return;
        try { c.startForegroundService(new Intent(c,FloatingService.class)); }
        catch (RuntimeException e) { Log.w("VolumeDock","Automatic restart rejected",e); }
    }
}
