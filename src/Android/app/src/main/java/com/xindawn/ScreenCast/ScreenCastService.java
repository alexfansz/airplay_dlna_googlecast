package com.xindawn.ScreenCast;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.xindawn.center.DMRCenter;
import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;

import java.util.UUID;

public class ScreenCastService extends Service {
    public static final String START_RENDER_ENGINE = "net.basicgo.start.engine";
    public static final String RESTART_RENDER_ENGINE = "net.basicgo.restart.engine";
    public static final String MSG_filter = "com.basicgo.sreencast.player.STATE";
    private static ScreenCastClass mScreenCast = null;
    private BroadcastReceiver playbackReceiver = null;
    private static String SharedPreferencesName = "ScreenCastService";
    private static String TAG = "ScreenCastService";


    private WifiManager wifiManager;
    private WifiManager.MulticastLock multicastLock;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "The onCreate is called.");
        CommonLog log = LogFactory.createLog();
        log.e("ScreenCastService onCreate");

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock(START_RENDER_ENGINE);
        multicastLock.acquire();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String actionString = intent.getAction();
            if (actionString != null){
                if (actionString.equalsIgnoreCase(START_RENDER_ENGINE)){
                    StartScreenCast();
                }else if (actionString.equalsIgnoreCase(RESTART_RENDER_ENGINE)){
                    StopScreenCast();
                    StartScreenCast();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "The onDestroy is called.");
        StopScreenCast();

        multicastLock.release();
    }

    private BroadcastReceiver pbBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mScreenCast == null)
                return;

            String playCommond = intent.getStringExtra("command");

            if (null != playCommond) {
                if (playCommond.equalsIgnoreCase("Play")) {

                    mScreenCast.UpdatePlaybackStatus(
                            ScreenCastClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                            ScreenCastClass.TRANSPORTSTATE_Playing);
                }else if (playCommond.equalsIgnoreCase("Stop")) {
                    mScreenCast.UpdatePlaybackStatus(
                            ScreenCastClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                            ScreenCastClass.TRANSPORTSTATE_Stopped);
                }else if (playCommond.equalsIgnoreCase("Pause")) {
                    mScreenCast.UpdatePlaybackStatus(
                            ScreenCastClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                            ScreenCastClass.TRANSPORTSTATE_Paused);
                }else if (playCommond.equalsIgnoreCase("Seek")) {

                }
                else if(playCommond.equalsIgnoreCase("TransState")){
                    int status = Integer.parseInt(intent.getStringExtra("status"));
                    mScreenCast.UpdatePlaybackStatus(ScreenCastClass.TRANSPORTSTATE_Transitioning,status);
                }
                else if (playCommond.equalsIgnoreCase("audio_init")){
                    int bitRate = intent.getIntExtra("bit",16);
                    int sampleRate = intent.getIntExtra("samprate",44100);
                    int channelCount = intent.getIntExtra("channel",2);
                    Log.v(TAG, "audio_init" + " bits "+ bitRate + " ch " + channelCount + " sam " + sampleRate);

                    DMRCenter.getInstance().audio_init(bitRate,channelCount,sampleRate,1);
                }
            }else{
                playCommond = intent.getStringExtra("WPOS");
                if(null != playCommond){

                   // Log.v(TAG, playCommond);
                    mScreenCast.Start("WPOS",playCommond);
                }
            }
        }
            /*
            String playMode = intent.getStringExtra("PlayMode");
            if (playMode != null) {
                String playType = intent.getStringExtra("PlayType");
                if (!(playType != null && mPlayType != null && playType.matches(mPlayType)))
                {
                    Log.v(TAG, "PlayType is not match. Cur:" + mPlayType + ", Received:" + playType);
                    return;
                }
                String SourceFrom = intent.getStringExtra("SourceFrom");
                if (!(SourceFrom != null && mSourceFrom != null && SourceFrom.matches(mSourceFrom)))
                {
                    Log.v(TAG, "SourceFrom is not match. Cur:" + mSourceFrom+ ", Received:" + SourceFrom);
                    return;
                }
                Log.v(TAG, String.format("PlayMode is received. - %s, curPlayType: %s, recvPlayType: %s", playMode, mPlayType, playType));
                if (playMode.equalsIgnoreCase("Play")) {
                    mDMR.UpdatePlaybackStatus(
                            DMRClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                            DMRClass.TRANSPORTSTATE_Playing);
                } else if (playMode.equalsIgnoreCase("Stop")) {
                    mDMR.UpdatePlaybackStatus(
                            DMRClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                            DMRClass.TRANSPORTSTATE_Stopped);
                    isPlaying = false;
                } else if (playMode.equalsIgnoreCase("Pause")) {
                    mDMR.UpdatePlaybackStatus(
                            DMRClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                            DMRClass.TRANSPORTSTATE_Paused);
                }
            }

            int duration = intent.getIntExtra("Duration", -1);
            if (duration != -1) {
                mDMR.UpdatePlaybackStatus(DMRClass.PLAYBACKSTATUS_DURATION,
                        duration * 1000);
            }

            int timePosition = intent.getIntExtra("TimePosition", -1);
            if (timePosition > 0) {
                if (!isInSeekingMode(timePosition) && isPlaying) {
                    mDMR.UpdatePlaybackStatus(
                            DMRClass.PLAYBACKSTATUS_TIMEPOSITION,
                            timePosition * 1000);
                    curElapsedTime = timePosition;
                }
                else
                {
                    Log.v(TAG, String.format("TimePosition is not updated. isPlaying:[%b]", isPlaying));
                }
            }

            int seekSupported = intent.getIntExtra("SeekSupported", -1);
            if (seekSupported != -1) {
                boolean canSeek = (seekSupported&CAN_SEEK)==CAN_SEEK;
                Log.v(TAG, String.format("SeekSupported[%d]: %s", seekSupported, canSeek));
                mDMR.UpdatePlaybackStatus(DMRClass.PLAYBACKSTATUS_SEEK,
                        canSeek ? 1 : 0);
            }
        }*/
    };

    private void hookPlaybackStatus(boolean bEnable) {
        Log.v(TAG, "hookPlaybackStatus " + bEnable);
        if (bEnable) {
            if (playbackReceiver == null)
            {
                IntentFilter intentFilter = new IntentFilter(
                        MSG_filter);
                playbackReceiver = pbBroadcastReceiver;
                registerReceiver(playbackReceiver, intentFilter);
                Log.v(TAG, "hookPlaybackStatus register");
            }
        } else {
            if (playbackReceiver != null)
            {
                unregisterReceiver(playbackReceiver);
                playbackReceiver = null;
                Log.v(TAG, "hookPlaybackStatus unregister");
            }
        }
    }

    private void StartScreenCast() {
        Log.v(TAG, "StartScreenCast is called.");

        mScreenCast = new ScreenCastClass();
        if(mScreenCast != null){
            //mScreenCast.Start(getDeviceName(), getUUID());
            mScreenCast.Start("BOOT", getUUID());
            mScreenCast.SetOnEventListener(DMRCenter.getInstance());

            hookPlaybackStatus(true);
        }
    }

    private void StopScreenCast() {
        Log.v(TAG, "The StopScreenCast is called");
        hookPlaybackStatus(false);

        if (mScreenCast != null)
        {
            mScreenCast.SetOnEventListener(null);
            mScreenCast.Stop();
            mScreenCast = null;
        }
        Log.v(TAG, "The StopScreenCast is called - done");
    }

    private String getDeviceName() {
        /*
        String setupDeviceName = "device_name"; // Settings.Global.DEVICE_NAME;
        String strDevice = Settings.Global.getString(getContentResolver(), setupDeviceName);

        if (strDevice == null)
        {
            SharedPreferences dmrData = getSharedPreferences(SharedPreferencesName, 0);
            strDevice = dmrData.getString("DeviceName", null);
            if (strDevice == null)
            {
                Log.v(TAG, "The device name is gengerated");
                strDevice = "DMR for android - "
                        + Integer.toOctalString((int) (Math.random() * 100 + 1));
                dmrData.edit().putString("DeviceName", strDevice).commit();
            }
        }*/
        String strDevice = "JPing_DlnaDmr";
        Log.v(TAG, "The device name is " + strDevice);
        return strDevice;
    }

    private String getUUID() {
        SharedPreferences dmrData = getSharedPreferences(SharedPreferencesName, 0);
        String strUUID = dmrData.getString("UUID", null);
        if (strUUID == null)
        {
            Log.v(TAG, "The UUID is gengerated");
            strUUID = UUID.randomUUID().toString();
            dmrData.edit().putString("UUID", strUUID).commit();
        }
        Log.v(TAG, "The UUID is " + strUUID);
        return strUUID;
    }
}
