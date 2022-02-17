package com.xindawn.ScreenCast;

import android.util.Log;

import com.xindawn.jni.PlatinumReflection;

public final class ScreenCastClass {
    private static String TAG = "ScreenCastClass";
    // Define the Notify ID
    public static final int NOTIFY_ID_QUERYFORCONNECTION = 0;
    public static final int NOTIFY_ID_SETAVTRANSPORTURI = 1;
    public static final int NOTIFY_ID_LOADMEDIA = 2;
    public static final int NOTIFY_ID_PLAY = 3;
    public static final int NOTIFY_ID_PAUSE = 4;
    public static final int NOTIFY_ID_STOP = 5;
    public static final int NOTIFY_ID_SEEK = 6;
    public static final int NOTIFY_ID_SETRATE = 7;
    public static final int NOTIFY_ID_FACTORY_DEFAULT = 8;
    public static final int NOTIFY_ID_SETVOLUME = 9;
    public static final int NOTIFY_ID_SETMUTE = 10;
    public static final int NOTIFY_ID_SETCONTRAST = 11;
    public static final int NOTIFY_ID_SETBRIGHTNESS = 12;
    public static final int NOTIFY_ID_RESTART_DMR = 13;

    // Define the play back status
    public static final int PLAYBACKSTATUS_CURRENTPLAYMODE = 0;
    public static final int PLAYBACKSTATUS_CONTRAST = 1;
    public static final int PLAYBACKSTATUS_BRIGHTNESS = 2;
    public static final int PLAYBACKSTATUS_VOLUME = 3;
    public static final int PLAYBACKSTATUS_MUTE = 4;
    public static final int PLAYBACKSTATUS_TRANSPORTSTATE = 5;
    public static final int PLAYBACKSTATUS_DURATION = 6;
    public static final int PLAYBACKSTATUS_TIMEPOSITION = 7;
    public static final int PLAYBACKSTATUS_SEEK = 8;

    // Define value for PLAYBACKSTATUS_TRANSPORTSTATE
    public static final int TRANSPORTSTATE_NoMedia = 0;
    public static final int TRANSPORTSTATE_Stopped = 1;
    public static final int TRANSPORTSTATE_Paused = 2;
    public static final int TRANSPORTSTATE_Playing = 3;
    public static final int TRANSPORTSTATE_Transitioning = 4;

    // Define value for MedialInfoType
    public static final int MedialInfoType_Filename 	= 0;
    public static final int MedialInfoType_Filesize		= 1;
    public static final int MedialInfoType_Duration		= 2;
    public static final int MedialInfoType_ResolutinoX	= 3;
    public static final int MedialInfoType_ResolutinoY	= 4;
    public static final int MedialInfoType_ColorDepth	= 5;

    // Native method
    public native final void Start(String friendlyName, String deviceUUID);

    public native final void Stop();

    public native final String GetAVTransportURI();

    public native final String GetMIMEType();

    public native final String GetMediaProtocolInfo();

    public native final void UpdatePlaybackStatus(int type, int value);

    public native final String GetMediaInfo(int type/*MedialInfoType*/);

    public native final String GetExtraPlaybackInfo();
    // Native callback method
    public static void onActionReflection(int cmd,String value,String data,String title){
        if (mListener != null){
            mListener.onActionInvoke(cmd, value, data,title);
        }
    }
    public static void onActionReflection(int cmd,String value,byte data[],String title){
        if (mListener != null){
            mListener.onActionInvoke(cmd, value, data,title);
        }
    }


    public static void audio_init(int bits, int channels, int samplerate,int isaudio){
        Log.v(TAG, "audio_init" + " bits "+ bits + " ch " + channels + " sam " + samplerate);

        if (mListener != null) mListener.audio_init(bits, channels, samplerate,isaudio);


        //放到audioplayer onOutputFormatChanged
        /*
        //这里做一些初始话处理
            DMRCenter.audBuf.initPacker();

            Intent intent = new Intent(ScreenCastService.MSG_filter);
            intent.putExtra("command", "audio_init");
            intent.putExtra("bit",bits);
            intent.putExtra("samprate",samplerate);
            intent.putExtra("channel",channels);

            RenderApplication.getInstance().sendBroadcast(intent);*/
    }

    public static void audio_process(byte data[],double timestamp,int seqnum){
        if (mListener != null){
            mListener.audio_process(data,timestamp,seqnum);
        }

        /*
        未启用MediaCode
        NALPacket nalPacket = new NALPacket();
        nalPacket.nalData = data;
        nalPacket.nalType = 0;
        nalPacket.pts = (long)timestamp;

        DMRCenter.audBuf.addPacker(nalPacket);

        //MediaControlBrocastFactory.sendPlayBrocast(RenderApplication.getInstance().getApplicationContext());
         */
    }

    public static void audio_destroy(){
        Log.v(TAG, "audio_destroy");
        if (mListener != null){
            mListener.audio_destroy();
        }
    }

    public static void video_process(byte data[],double timestamp,int seqnum){
        if (mListener != null){
            mListener.video_process(data,timestamp,seqnum);
        }
    }

    private static PlatinumReflection.ActionReflectionListener mListener;

    public void SetOnEventListener(PlatinumReflection.ActionReflectionListener cb) {
        mListener = cb;
    }

    static {
        System.loadLibrary("ScreenCastCtrl");
    }
}
