package com.xindawn.util;



import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Environment;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

public class CommonUtil {

	private static final CommonLog log = LogFactory.createLog();
	
	public static boolean hasSDCard() {
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			return false;
		} 
		return true;
	}
	
	public static String getRootFilePath() {
		if (hasSDCard()) {
			return Environment.getExternalStorageDirectory().getAbsolutePath() + "/";// filePath:/sdcard/
		} else {
			return Environment.getDataDirectory().getAbsolutePath() + "/data/"; // filePath: /data/data/
		}
	}
	
	public static boolean checkNetworkState(Context context){
    	boolean netstate = false;
		ConnectivityManager connectivity = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(connectivity != null)
		{
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++)
				{
					if (info[i].getState() == NetworkInfo.State.CONNECTED) 
					{
						netstate = true;
						break;
					}
				}
			}
		}
		return netstate;
    }

	public static String getLocalMacAddress(Context mc) {
		String defmac = "02:00:00:00:00:00";

		try {


		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface iF = interfaces.nextElement();

			byte[] addr = iF.getHardwareAddress();
			if (addr == null || addr.length == 0) {
				continue;
			}

			StringBuilder buf = new StringBuilder();
			for (byte b : addr) {
				buf.append(String.format("%02X:", b));
			}
			if (buf.length() > 0) {
				buf.deleteCharAt(buf.length() - 1);
			}
			defmac = buf.toString();
			break;
		}
	}catch (SocketException e) {
			e.printStackTrace();
		}

			return defmac;
	}
	
	public static MulticastLock openWifiBrocast(Context context){
		WifiManager wifiManager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		MulticastLock  multicastLock=wifiManager.createMulticastLock("MediaRender");
		if (multicastLock != null){
			multicastLock.acquire();
		}
		return multicastLock;
	}
	
	
	public static void setCurrentVolume(int percent,Context mc){
		AudioManager am=(AudioManager)mc.getSystemService(Context.AUDIO_SERVICE);
		int maxvolume=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxvolume*percent)/100, 
				AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
		am.setMode(AudioManager.MODE_INVALID);
	}
	
	public static void setVolumeMute(Context mc){
		AudioManager am=(AudioManager)mc.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamMute(AudioManager.STREAM_MUSIC, true);
	}
	public static void setVolumeUnmute(Context mc){
		AudioManager am=(AudioManager)mc.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamMute(AudioManager.STREAM_MUSIC, false);
	}
	
	public static void showToask(Context context, String tip){
		Toast.makeText(context, tip, Toast.LENGTH_SHORT).show();
	}

	public static int getScreenWidth(Context context) {
		WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		return display.getWidth();
	}
	
	public static int getScreenHeight(Context context) {
		WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		return display.getHeight();
	}
	
	public static ViewSize getFitSize(Context context, MediaPlayer mediaPlayer)
	{
		int videoWidth = mediaPlayer.getVideoWidth();  
	    int videoHeight = mediaPlayer.getVideoHeight();  	    
	    double fit1 = videoWidth * 1.0 / videoHeight;	 
	    
	    int width2 = getScreenWidth(context);
	    int height2 = getScreenHeight(context);      
	    double fit2 = width2 * 1.0 / height2;  
	    
	    double fit = 1;
	    if (fit1 > fit2)
	    {
	    	fit = width2 * 1.0 / videoWidth;
	    }else{
	    	fit = height2 * 1.0 / videoHeight;
	    }

	    ViewSize viewSize = new ViewSize();
	    viewSize.width = (int) (fit * videoWidth);
	    viewSize.height = (int) (fit * videoHeight);

	    return viewSize;
	}

	public static class ViewSize
	{
		public int width = 0;
		public int height = 0;
	}
    
	  public static boolean getWifiState(Context context){
		  ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);  
		  State wifistate = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
		  if (wifistate != State.CONNECTED){
			  return false;
		  }
		  
		  State mobileState = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
		  boolean ret = State.CONNECTED != mobileState;
		  return ret;
	  }
	  
	  
	  public static boolean getMobileState(Context context){
		  ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);  
		  State wifistate = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
		  if (wifistate != State.CONNECTED){
			  return false;
		  }
		  
		  State mobileState = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
		  boolean ret = State.CONNECTED == mobileState;
		  return ret;
	  }
	  
	  
	  
	  private static long m_lSysNetworkSpeedLastTs = 0;
		private static long m_lSystNetworkLastBytes = 0;
		private static float m_fSysNetowrkLastSpeed = 0.0f;
		public static float getSysNetworkDownloadSpeed() {
			long nowMS = System.currentTimeMillis();
			long nowBytes = TrafficStats.getTotalRxBytes();

			long timeinterval = nowMS - m_lSysNetworkSpeedLastTs;
			long bytes = nowBytes - m_lSystNetworkLastBytes;

			if(timeinterval > 0) m_fSysNetowrkLastSpeed = (float)bytes * 1.0f / (float)timeinterval;

			m_lSysNetworkSpeedLastTs = nowMS;
			m_lSystNetworkLastBytes = nowBytes;

			return m_fSysNetowrkLastSpeed;
		}
}


