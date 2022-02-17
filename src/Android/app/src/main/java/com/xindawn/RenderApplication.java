package com.xindawn;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.xindawn.util.CommonLog;
import com.xindawn.util.CommonUtil;
import com.xindawn.util.DlnaUtils;
import com.xindawn.util.LogFactory;

import java.util.HashMap;


public class RenderApplication  extends Application implements ItatisticsEvent{

	private static final CommonLog log = LogFactory.createLog();

	private static RenderApplication mInstance;

	//private DeviceInfo mDeviceInfo;
	public DeviceInfo mDeviceInfo;
	
	public synchronized static RenderApplication getInstance(){
		return mInstance;
	}
	@Override
	public void onCreate() {
			super.onCreate();
		log.e("RenderApplication onCreate");
		
		mInstance = this;
		mDeviceInfo = new DeviceInfo();

		String friendName = Build.BRAND +" "+ Build.MODEL+":";//DlnaUtils.getDevName(this);
		String uuid = DlnaUtils.creat12BitUUID(this);
		updateDevInfo(friendName,uuid,getPackageName());

		//crash !!??
		//BetaBugly();
	}


	/**
	 * get App versionCode
	 * @param
	 * @return
	 */
	public String getVersionCode(){
		PackageManager packageManager=getPackageManager();
		PackageInfo packageInfo;
		String versionCode="";
		try {
			packageInfo=packageManager.getPackageInfo(getPackageName(),0);
			versionCode=packageInfo.versionCode+"";
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	/**
	 * get App versionName
	 * @param
	 * @return
	 */
	public String getVersionName() {
		PackageManager packageManager = getPackageManager();
		PackageInfo packageInfo;
		String versionName = "";
		try {
			packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
			versionName = packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionName;
	}

	private void updateDevInfo(String name, String uuid,String appId){
		mDeviceInfo.uuid = uuid;
		mDeviceInfo.applicationId = appId;
		mDeviceInfo.mMacAddress = CommonUtil.getLocalMacAddress(this);

		mDeviceInfo.dev_name = name + mDeviceInfo.mMacAddress.substring(mDeviceInfo.mMacAddress.length()-2,mDeviceInfo.mMacAddress.length());
	}
	
	public void setDevStatus(boolean flag){
		mDeviceInfo.status = flag;
		DeviceUpdateBrocastFactory.sendDevUpdateBrocast(this);
	}

	public void setDevAirplayPort(int airplayPort){
		mDeviceInfo.airplay_port = airplayPort;
	}

	public void setDevRaopPort(int raopPort){
		mDeviceInfo.airtunes_port = raopPort;
	}
	
	public DeviceInfo getDevInfo(){
		return mDeviceInfo;
	}
	
	@Override
	public void onEvent(String eventID) {
		log.e("eventID = " + eventID);	

	}

	@Override
	public void onEvent(String eventID, HashMap<String, String> map) {
		log.e("eventID = " + eventID);	

	}
	
	public static void onPause(Activity context){

	}
	
	public static void onResume(Activity context){

	}
	
	public static void onCatchError(Context context){
	}
}
