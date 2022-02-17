package com.xindawn;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class DeviceUpdateBrocastFactory {

	public static final String PARAM_DEV_UPDATE=RenderApplication.getInstance().mDeviceInfo.applicationId+".PARAM_DEV_UPDATE";
	
	public static interface IDevUpdateListener {
		public void onUpdate(Intent intent);
	}
	
	
	private DeviceUpdateBrocastReceiver mReceiver;
	private Context mContext;
	
	public DeviceUpdateBrocastFactory(Context context){
		mContext = context;
	}
	
	
	public void register(IDevUpdateListener listener){
		if (mReceiver == null){
			mReceiver = new DeviceUpdateBrocastReceiver();
			mReceiver.setListener(listener);	
			mContext.registerReceiver(mReceiver, new IntentFilter(PARAM_DEV_UPDATE));
		}
	}

	public void unregister()
	{
		if (mReceiver != null){
			mContext.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}
	
	public static void sendDevUpdateBrocast(Context context){
		Intent intent = new Intent();
		intent.setAction(PARAM_DEV_UPDATE);
		context.sendBroadcast(intent);
	}

	public static void sendDevUpdateBrocast(Intent intent){
		RenderApplication.getInstance().sendBroadcast(intent);
	}
}
