package com.xindawn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;

public class DeviceUpdateBrocastReceiver extends BroadcastReceiver{

	private static final CommonLog log = LogFactory.createLog();
	private DeviceUpdateBrocastFactory.IDevUpdateListener mListener;
	
	public void setListener(DeviceUpdateBrocastFactory.IDevUpdateListener listener){
		mListener = listener;
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action == null){
			return ;
		}
		
		if (DeviceUpdateBrocastFactory.PARAM_DEV_UPDATE.equalsIgnoreCase(action)){
			if (mListener != null){
				mListener.onUpdate(intent);
			}
		}
	}
	
	
}
