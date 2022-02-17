package com.xindawn.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.xindawn.RenderApplication;
import com.xindawn.center.MediaRenderProxy;
import com.xindawn.datastore.LocalConfigSharePreference;

public class BootManager extends BroadcastReceiver {
    private static String TAG = "BootManager";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "The boot completed event is received.");

        if(LocalConfigSharePreference.getSettingsVal(RenderApplication.getInstance(),"autoBoot").equals("true"))
        {
            /*Intent intent1 = context.getPackageManager().getLaunchIntentForPackage("com.xindawn");
            intent1.setClass(context, MainActivity.class);
            context.startActivity(intent1);*/

            MediaRenderProxy.getInstance().startEngine();
        }
    }
}
