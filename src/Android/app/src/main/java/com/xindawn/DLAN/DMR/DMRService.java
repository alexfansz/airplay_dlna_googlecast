package com.xindawn.DLAN.DMR;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.rockchip.mediacenter.core.util.TimeUtils;
import com.xindawn.DLAN.DMR.util.PlayerUtil;
import com.xindawn.DLAN.DMRBridge.DMRClass;
import com.xindawn.RenderApplication;
import com.xindawn.ScreenCast.ScreenCastClass;

import java.lang.ref.WeakReference;

public class DMRService extends Service implements VolumeControl.VolumeChangeListener {
    private static DMRClass mDMR = null;
    private static ScreenCastClass mScreenCast = null;
    private Boolean isPlaying = false;
    private static String TAG = "DMRService";
    private static String SharedPreferencesName = "DMRService";
    private BroadcastReceiver playbackReceiver = null;
    private String mPlayType = null;
    private String mSourceFrom = null;
    private VolumeControl volCtrl;
    private int seekingTime = -1;
    private int curElapsedTime = -1;

    private static final int CAN_PAUSE_FLAG     = 0x01;
    private static final int CAN_SEEK_FWD_FLAG  = 0x02;
    private static final int CAN_SEEK_BWD_FLAG  = 0x04;
    private static final int CAN_SEEK           = 0x08;

    public static final String START_RENDER_ENGINE = "com.xindawn.start.dmr";
    public static final String RESTART_RENDER_ENGINE = "com.xindawn.restart.dmr";

    private static final String MEDIA_BROWSER_USE_RT_MEDIA_PLAYER = "MEDIA_BROWSER_USE_RT_MEDIA_PLAYER";

    private BroadcastReceiver pbBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (mDMR == null)
                    return;

                String playCommond = intent.getStringExtra("command");

                if (null != playCommond) {
                    if (playCommond.equalsIgnoreCase("Play")) {
                        int duration = TimeUtils.convertRealTimeToInt(intent.getStringExtra("CurrentDuration"));

                        //Log.v(TAG, "CurrentDuration:--->" + intent.getStringExtra("CurrentDuration") + "-->"+duration);
                        if (duration > 0) {
                            mDMR.UpdatePlaybackStatus(DMRClass.PLAYBACKSTATUS_DURATION,
                                    duration);

                            mDMR.UpdatePlaybackStatus(DMRClass.PLAYBACKSTATUS_SEEK,
                                    1);
                        }
                        mDMR.UpdatePlaybackStatus(
                                DMRClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                                DMRClass.TRANSPORTSTATE_Playing);
                        isPlaying = true;
                    }else if (playCommond.equalsIgnoreCase("Stop")) {
                        mDMR.UpdatePlaybackStatus(
                                DMRClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                                DMRClass.TRANSPORTSTATE_Stopped);
                        isPlaying = false;
                    }else if (playCommond.equalsIgnoreCase("Pause")) {
                        mDMR.UpdatePlaybackStatus(
                                DMRClass.PLAYBACKSTATUS_TRANSPORTSTATE,
                                DMRClass.TRANSPORTSTATE_Paused);
                    }else if (playCommond.equalsIgnoreCase("Seek")) {
                        int timePosition = TimeUtils.convertRealTimeToInt(intent.getStringExtra("realtime"));

                        //Log.v(TAG, "timePosition:--->" +intent.getStringExtra("realtime")+ "-->"+timePosition);
                        if (timePosition > 0) {
                            if (!isInSeekingMode(timePosition) && isPlaying) {
                                mDMR.UpdatePlaybackStatus(
                                        DMRClass.PLAYBACKSTATUS_TIMEPOSITION,
                                        timePosition);
                                curElapsedTime = timePosition;
                            }
                            else
                            {
                                Log.v(TAG, String.format("TimePosition is not updated. isPlaying:[%b]", isPlaying));
                            }
                        }
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

    private DMRClass.OnEventListener DMREventListener = new DMRClass.OnEventListener() {

        @Override
        public int onEvent(int notifyID, String info) {
            if (mDMR == null)
                return -1;
            String idString = null;

            switch (notifyID) {
                case DMRClass.NOTIFY_ID_QUERYFORCONNECTION:
                    idString = "QueryForConnection";
                    break;
                case DMRClass.NOTIFY_ID_SETAVTRANSPORTURI:
                    idString = "SetAVTransportURI";
                    mPlayType = null;
                    mSourceFrom = null;
                    break;
                case DMRClass.NOTIFY_ID_LOADMEDIA:
                    idString = "LoadMedia";
                    isPlaying = false;
                    curElapsedTime = -1;
                    seekingTime = -1;
                    break;
                case DMRClass.NOTIFY_ID_PLAY:
                    if (!isPlaying) {
                        String uriStr = mDMR.GetAVTransportURI();
                        String mimeType = mDMR.GetMIMEType();
                        String protocolInfo = mDMR.GetMediaProtocolInfo();
                        idString = "Play for URI: "
                                + uriStr
                                + " , MimeType: "
                                + mimeType
                                + " , Protocol Info: "
                                + protocolInfo
                        ;

                        if (mimeType != null) {

                            if (mimeType.startsWith("video/")) {
                                mPlayType = "Video";
                            } else if (mimeType.startsWith("audio/")) {
                                mPlayType = "Audio";
                            }else if (mimeType.startsWith("image/")) {
                                mPlayType = "Image";
                            }
                        }

                        Log.v(TAG, "The Title " + mDMR.GetMediaInfo(mDMR.MedialInfoType_Filename));

                        PlayerUtil.startPlayer(RenderApplication.getInstance(),mDMR.GetMediaInfo(mDMR.MedialInfoType_Filename),uriStr,mimeType);
                        isPlaying = true;
                        hookPlaybackStatus(true);
                    } else {
                        idString = "Play";
                        ExecuteCommand("Play");
                    }
                    break;
                case DMRClass.NOTIFY_ID_PAUSE: {
                    idString = "Pause";
                    ExecuteCommand("Pause");
                }
                    break;
                case DMRClass.NOTIFY_ID_STOP: {

                    if(isPlaying) {
                        ExecuteCommand("Stop");
                        mPlayType = null;
                        mSourceFrom = null;
                        isPlaying = false;
                    }
                }
                    break;
                case DMRClass.NOTIFY_ID_SEEK:
                    idString = "Seek";
                    if (info != null) {
                        idString += " (" + info + ") ";

                        Intent i = createCommand("Seek");
                        if (i != null) {
                            seekingTime = Integer.parseInt(info)*1000;
                            i.putExtra("position",
                                    "REL_TIME");
                            i.putExtra("realtime",
                                    TimeUtils.convertToRealTime(seekingTime));
                            sendBroadcast(i);

                            Log.v(TAG, "NOTIFY_ID_SEEK " + seekingTime);
                        }
                    }
                    break;
                case DMRClass.NOTIFY_ID_SETRATE:
                    idString = "SetRate";
                    if (info != null) {
                        idString += " (" + info + ") ";

                        Intent i = createCommand("rate");
                        if (i != null) {
                            int rateValue = Integer.parseInt(info);
                            i.putExtra("rate", rateValue);
                            sendBroadcast(i);
                        }
                    }
                    break;
                case DMRClass.NOTIFY_ID_FACTORY_DEFAULT:
                    idString = "Factory Default";
                    if (info != null)
                        idString += " (" + info + ") ";
                    break;
                case DMRClass.NOTIFY_ID_SETVOLUME:
                    idString = "Set Volume";
                    if (info != null)
                    {
                        idString += " (" + info + ") ";

                        if (volCtrl != null)
                        {
                            volCtrl.setVolume(Integer.parseInt(info));
                        }
                    }
                    break;
                case DMRClass.NOTIFY_ID_SETMUTE:
                    idString = "Set Mute";
                    if (info != null)
                    {
                        idString += " (" + info + ") ";
                        if (volCtrl != null)
                        {
                            volCtrl.setMute(Integer.parseInt(info) != 0);
                        }
                    }
                    break;
                case DMRClass.NOTIFY_ID_SETCONTRAST:
                    idString = "Set Contrast";
                    if (info != null)
                        idString += " (" + info + ") ";
                    break;
                case DMRClass.NOTIFY_ID_SETBRIGHTNESS:
                    idString = "Set Brightness";
                    if (info != null)
                        idString += " (" + info + ") ";
                    break;
                case DMRClass.NOTIFY_ID_RESTART_DMR:
                    idString = "The DMR restart is needed";
                    if (info != null)
                        idString += " (" + info + ") ";

                    if (mHandler != null)
                    {
                        Log.v(TAG, "The MESSAGE_RESTART_DMR is send");
                        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_RESTART_DMR));
                    }
                    break;
            }
            if (idString == null) {
                idString = Integer.toString(notifyID);
            }

            Log.v(TAG, "The activity " + idString);
            return 0;
        }
    };

    private static final int MESSAGE_RESTART_DMR = 0;

    private static class InternalHandler extends Handler {
        private final WeakReference<DMRService> mDMRService;

        public InternalHandler(DMRService service) {
            mDMRService = new WeakReference<DMRService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            DMRService service = mDMRService.get();
            if (service == null)
                return;
            Log.v(TAG, "mHandler : Get the message " + msg.what);
            switch (msg.what)
            {
                case MESSAGE_RESTART_DMR:
                    service.StopDMR();
                    service.StartDMR();
                    break;
                default:
                    break;
            }
        }
    }

    private final InternalHandler mHandler = new InternalHandler(this);

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "The onCreate is called.");
        //StartDMR();
        volCtrl = new VolumeControl(this);
        if (volCtrl != null)
        {
            volCtrl.setVolumeChangeListener(this);
            volCtrl.startMonitor();
        }
    }

    private Intent createCommand(String strCMD) {
        String mimeType = mDMR.GetMIMEType();
        Intent i = null;
        if (mimeType != null) {
            if (mimeType.startsWith("video/")) {
                i = new Intent("com.android.rockchip.mediashare.player.command");
            } else if (mimeType.startsWith("audio/")) {
                i = new Intent("com.android.rockchip.mediashare.player.command");
            } else if (mimeType.startsWith("image/")) {
                i = new Intent("com.android.rockchip.mediashare.player.command");
            }
            if (i != null) {
                i.putExtra("command", strCMD);
            }
        }

        return i;
    }

    private void ExecuteCommand(String strCMD) {
        Intent i = createCommand(strCMD);
        if (i != null) {
            sendBroadcast(i);
        }
    }

    private void hookPlaybackStatus(boolean bEnable) {
        Log.v(TAG, "hookPlaybackStatus " + bEnable);
        if (bEnable) {
            if (playbackReceiver == null)
            {
                IntentFilter intentFilter = new IntentFilter(
                        "com.rockchip.mediacenter.player.STATE");
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

    @Override
    public void onDestroy() {
        Log.v(TAG, "The onDestroy is called.");
        StopDMR();
        if (volCtrl != null)
        {
            volCtrl.stopMonitor();
            volCtrl = null;
        }
    }

    @Override
    //public void onStart(Intent intent, int startId) {
    //    Log.v(TAG, "The onStart is called");
    //}
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String actionString = intent.getAction();
            if (actionString != null){
                if (actionString.equalsIgnoreCase(START_RENDER_ENGINE)){
                    StartDMR();
                }else if (actionString.equalsIgnoreCase(RESTART_RENDER_ENGINE)){
                    //delayToSendRestartMsg();
                    StopDMR();
                    StartDMR();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);

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
        String strDevice = RenderApplication.getInstance().mDeviceInfo.dev_name;
        Log.v(TAG, "The device name is " + strDevice);
        return strDevice;
    }

    private String getUUID() {
        /*SharedPreferences dmrData = getSharedPreferences(SharedPreferencesName, 0);
        String strUUID = dmrData.getString("UUID", null);
        if (strUUID == null)
        {
            Log.v(TAG, "The UUID is gengerated");
            strUUID = UUID.randomUUID().toString();
            dmrData.edit().putString("UUID", strUUID).commit();
        }*/
        String strUUID = RenderApplication.getInstance().mDeviceInfo.mMacAddress+"@"+RenderApplication.getInstance().mDeviceInfo.APP_CHANNEL;
        Log.v(TAG, "The UUID is " + strUUID);
        return strUUID;
    }

    private void StartDMR() {
        Log.v(TAG, "StartDMR is called.");
        mDMR = new DMRClass();

        if (mDMR != null)
        {
            mDMR.Start(getDeviceName(), getUUID());
            mDMR.SetOnEventListener(DMREventListener);
        }

        /*mScreenCast = new ScreenCastClass();
        if(mScreenCast != null){
            mScreenCast.Start(getDeviceName(), getUUID());
        }*/
    }

    private void StopDMR() {
        Log.v(TAG, "The StopDMR is called");
        hookPlaybackStatus(false);

        if (mDMR != null)
        {
            mDMR.SetOnEventListener(null);
            mDMR.Stop();
            mDMR = null;
        }
        Log.v(TAG, "The StopDMR is called - done");
    }

    @Override
    public void volumeChanged(int vol) {
        Log.v(TAG, String.format("The Volume Change is received. [%d]", vol));
        if (mDMR != null && vol > 0)
        {
            mDMR.UpdatePlaybackStatus(DMRClass.PLAYBACKSTATUS_VOLUME, vol);
        }
    }

    @Override
    public void muteChanged(boolean bMute) {
        Log.v(TAG, String.format("The Mute Change is received. [%b]", bMute));
        if (mDMR != null)
        {
            mDMR.UpdatePlaybackStatus(DMRClass.PLAYBACKSTATUS_MUTE, bMute? 1 : 0);
        }
    }

    private Boolean isInSeekingMode(int curTime) {
        final int SEEK_TIME_GAP = 2;
        final int SEEK_TIME_GAP_RANGE = 5;
        if (seekingTime != -1)
        {
            //Log.v(TAG, String.format("It is in isInSeekingMode.. curTime[%d] prevTime:%d, seekingTime:%d", curTime, curElapsedTime, seekingTime));
            int diffToTime = curTime - seekingTime;
            int diffFromTime = Math.abs(curElapsedTime - curTime);
            if ((SEEK_TIME_GAP < diffFromTime && diffFromTime < SEEK_TIME_GAP_RANGE) ||
                (0 < diffToTime))
            {
                seekingTime = -1;
                curElapsedTime = -1;
                Log.v(TAG, String.format("It is InSeekingMode reset. [%d]", curTime));
            }
            else {
                Log.v(TAG, String.format("It is InSeekingMode. [%d]", curTime));
                return true;
            }
        }

        return false;
    }

    private void processPlaybackInfo(Intent intent, String strPlaybackInfo) {
        if (intent == null || strPlaybackInfo == null)
            return;

        Log.v(TAG, String.format("PlaybackInfo is [%s]", strPlaybackInfo));
        intent.putExtra("RTMediaPlayer.Config", strPlaybackInfo);
    }
}
