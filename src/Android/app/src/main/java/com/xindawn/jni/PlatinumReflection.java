package com.xindawn.jni;

import com.xindawn.RenderApplication;
import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;



public class PlatinumReflection {
	
	private static final CommonLog log = LogFactory.createLog();
	private static final int DEVICE_CTL_MSG_BASE = 0x1000;
	/*---------------------------------------------------------------*/
	public static final int DEVICE_CTL_MSG_SET_AIRPALY_PORT = (DEVICE_CTL_MSG_BASE+1);
	public static final int DEVICE_CTL_MSG_SET_RAOP_PORT = (DEVICE_CTL_MSG_BASE+2);
	/*---------------------------------------------------------------*/
	
	private static final int MEDIA_RENDER_CTL_MSG_BASE = 0x100;
	/*----------------------------------------------------------------*/
	public static final int MEDIA_RENDER_CTL_MSG_SET_AV_URL = (MEDIA_RENDER_CTL_MSG_BASE+0);
	public static final int MEDIA_RENDER_CTL_MSG_STOP = (MEDIA_RENDER_CTL_MSG_BASE+1);
	public static final int MEDIA_RENDER_CTL_MSG_PLAY = (MEDIA_RENDER_CTL_MSG_BASE+2);
	public static final int MEDIA_RENDER_CTL_MSG_PAUSE = (MEDIA_RENDER_CTL_MSG_BASE+3);
	public static final int MEDIA_RENDER_CTL_MSG_SEEK = (MEDIA_RENDER_CTL_MSG_BASE+4);
	public static final int MEDIA_RENDER_CTL_MSG_SETVOLUME = (MEDIA_RENDER_CTL_MSG_BASE+5);
	public static final int MEDIA_RENDER_CTL_MSG_SETMUTE = (MEDIA_RENDER_CTL_MSG_BASE+6);
	public static final int MEDIA_RENDER_CTL_MSG_SETPLAYMODE = (MEDIA_RENDER_CTL_MSG_BASE+7);
	public static final int MEDIA_RENDER_CTL_MSG_PRE = (MEDIA_RENDER_CTL_MSG_BASE+8);
	public static final int MEDIA_RENDER_CTL_MSG_NEXT = (MEDIA_RENDER_CTL_MSG_BASE+9);
	public static final int MEDIA_RENDER_CTL_MSG_SETMETADATA = (MEDIA_RENDER_CTL_MSG_BASE+10);
	public static final int MEDIA_RENDER_CTL_MSG_SETIPADDR = (MEDIA_RENDER_CTL_MSG_BASE+11);
	
	/*----------------------------------------------------------------*/	
	
	
	
	/*----------------------------------------------------------------*/	
	/*
	 * 		
	 * 
	 * */
	public static final int MEDIA_RENDER_TOCONTRPOINT_SET_MEDIA_DURATION = (MEDIA_RENDER_CTL_MSG_BASE+0);
	public static final int MEDIA_RENDER_TOCONTRPOINT_SET_MEDIA_POSITION = (MEDIA_RENDER_CTL_MSG_BASE+1);
	public static final int MEDIA_RENDER_TOCONTRPOINT_SET_MEDIA_PLAYINGSTATE = (MEDIA_RENDER_CTL_MSG_BASE+2);
	/*----------------------------------------------------------------*/
	public static final String RENDERER_TOCONTRPOINT_CMD_INTENT_NAME= RenderApplication.getInstance().mDeviceInfo.applicationId+".platinum.tocontrolpointer.cmd.intent";
	public static final String GET_RENDERER_TOCONTRPOINT_CMD="get_dlna_renderer_tocontrolpointer.cmd";
	public static final String GET_PARAM_MEDIA_DURATION="get_param_media_duration";
	public static final String GET_PARAM_MEDIA_POSITION="get_param_media_position";
	public static final String GET_PARAM_MEDIA_PLAYINGSTATE="get_param_media_playingstate";
	/*----------------------------------------------------------------*/
	

    public static final String MEDIA_PLAYINGSTATE_STOP="STOPPED";
    public static final String MEDIA_PLAYINGSTATE_PAUSE="PAUSED_PLAYBACK";
    public static final String MEDIA_PLAYINGSTATE_PLAYING="PLAYING";
    public static final String MEDIA_PLAYINGSTATE_TRANSTION="TRANSITIONING";
    public static final String MEDIA_PLAYINGSTATE_NOMEDIA="NO_MEDIA_PRESENT";
    
    /*----------------------------------------------------------------*/
    public static final String MEDIA_SEEK_TIME_TYPE_REL_TIME="REL_TIME";	
    public static final String MEDIA_SEEK_TIME_TYPE_TRACK_NR="TRACK_NR";
    
	
	public static interface ActionReflectionListener{
		public void onActionInvoke(int cmd,String value,String data,String title);
		public void onActionInvoke(int cmd,String value,byte data[],String title);
		public void audio_init(int bits, int channels, int samplerate,int isaudio);
		public void audio_process(byte data[],double timestamp,int seqnum);
		public void audio_destroy();
		public void video_process(byte data[],double timestamp,int seqnum);
	}
	
	private static ActionReflectionListener mListener;
	
	
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
		if (mListener != null){
			mListener.audio_init(bits, channels, samplerate,isaudio);
		}
	}
	
	public static void audio_process(byte data[],double timestamp,int seqnum){
		if (mListener != null){
			mListener.audio_process(data,timestamp,seqnum);
		}
	}
	
	public static void audio_destroy(){
		if (mListener != null){
			mListener.audio_destroy();
		}
	}

	public static void video_process(byte data[],double timestamp,int seqnum){
		if (mListener != null){
			mListener.video_process(data,timestamp,seqnum);
		}
	}
	
	
	public static void setActionInvokeListener(ActionReflectionListener listener){
		mListener = listener;
	}
}
