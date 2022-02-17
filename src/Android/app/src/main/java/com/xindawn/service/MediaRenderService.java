package com.xindawn.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.xindawn.DNSNotify;
import com.xindawn.DeviceUpdateBrocastFactory;
import com.xindawn.RenderApplication;
import com.xindawn.center.DLNAGenaEventBrocastFactory;
import com.xindawn.center.DMRCenter;
import com.xindawn.center.DMRWorkThread;
import com.xindawn.center.IBaseEngine;
import com.xindawn.jni.PlatinumReflection;
import com.xindawn.jni.PlatinumReflection.ActionReflectionListener;
import com.xindawn.util.CommonLog;
import com.xindawn.util.CommonUtil;
import com.xindawn.util.LogFactory;

public class MediaRenderService extends Service implements IBaseEngine, DeviceUpdateBrocastFactory.IDevUpdateListener{

	private static final CommonLog log = LogFactory.createLog();
	
	public static final String START_RENDER_ENGINE = "com.xindawn.start.engine";
	public static final String RESTART_RENDER_ENGINE = "com.xindawn.restart.engine";


	private DMRWorkThread mWorkThread;
	
	private ActionReflectionListener mListener;
	private DLNAGenaEventBrocastFactory mMediaGenaBrocastFactory;
	
	private Handler mHandler;
	private static final int START_ENGINE_MSG_ID = 0x0001;
	private static final int RESTART_ENGINE_MSG_ID = 0x0002;
	
	private static final int DELAY_TIME = 1000;
	
	private MulticastLock mMulticastLock;

	private DNSNotify mDNSNotify;
	private DeviceUpdateBrocastFactory mBrocastFactory;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();		
		initRenderService();	
		log.e("MediaRenderService onCreate");
	}

	@Override
	public void onDestroy() {
		unInitRenderService();	
		log.e("MediaRenderService onDestroy");
		super.onDestroy();
	
	}

	@Override
	public void onUpdate(Intent intent) {
		String playCommond = intent.getStringExtra("command");

		if (null != playCommond) {
			if (playCommond.equalsIgnoreCase("startdns")) {
				startDnsServer();
			}
		}else {
			log.e("ignore ui message");
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null){
			String actionString = intent.getAction();
			if (actionString != null){		
				if (actionString.equalsIgnoreCase(START_RENDER_ENGINE)){
					delayToSendStartMsg();
				}else if (actionString.equalsIgnoreCase(RESTART_RENDER_ENGINE)){
					delayToSendRestartMsg();
				}
			}
		}	
	
		return super.onStartCommand(intent, flags, startId);
		
	}
	
	
	private void initRenderService(){

		getSystemService(Context.NSD_SERVICE);

		mListener = DMRCenter.getInstance();
		PlatinumReflection.setActionInvokeListener(mListener);
		mMediaGenaBrocastFactory = new DLNAGenaEventBrocastFactory(this);
		mMediaGenaBrocastFactory.registerBrocast();
		mWorkThread = new DMRWorkThread(this);

		mDNSNotify = DNSNotify.getInstance();
		mBrocastFactory = new DeviceUpdateBrocastFactory(this);
		mBrocastFactory.register(this);

		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
				case START_ENGINE_MSG_ID:
					startEngine();
					break;
				case RESTART_ENGINE_MSG_ID:
					restartEngine();
					break;
				}
			}
			
		};
		
		mMulticastLock = CommonUtil.openWifiBrocast(this);
		log.e("openWifiBrocast = "  +  mMulticastLock != null ? true : false);
	}

	
	private void unInitRenderService(){
		stopDnsServer();
		stopEngine();
		removeStartMsg();
		removeRestartMsg();
		mMediaGenaBrocastFactory.unRegisterBrocast();
		mBrocastFactory.unregister();
		if (mMulticastLock != null){
			mMulticastLock.release();
			mMulticastLock = null;
			log.e("closeWifiBrocast");
		}
	}

	private void delayToSendStartMsg(){
		removeStartMsg();
		mHandler.sendEmptyMessageDelayed(START_ENGINE_MSG_ID, DELAY_TIME);
	}
	
	private void delayToSendRestartMsg(){
		removeStartMsg();
		removeRestartMsg();
		mHandler.sendEmptyMessageDelayed(RESTART_ENGINE_MSG_ID, DELAY_TIME);
	}
	
	private void removeStartMsg(){
		mHandler.removeMessages(START_ENGINE_MSG_ID);
	}
	
	private void removeRestartMsg(){
		mHandler.removeMessages(RESTART_ENGINE_MSG_ID);	
	}
	
	
	@Override
	public boolean startEngine() {
		awakeWorkThread();

		return true;
	}

	@Override
	public boolean stopEngine() {
		exitWorkThread();
		return true;
	}

	@Override
	public boolean restartEngine() {
		if (mWorkThread.isAlive()){
			mWorkThread.restartEngine();
		}else{
			mWorkThread.start();
		}
		return true;
	}

	private void awakeWorkThread(){
		if (mWorkThread.isAlive()){
			mWorkThread.awakeThread();
		}else{
			mWorkThread.start();
		}
	}
	
	private void exitWorkThread(){
		if (mWorkThread != null && mWorkThread.isAlive()){
			mWorkThread.exit();
			long time1 = System.currentTimeMillis();
			while(mWorkThread.isAlive()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			long time2 = System.currentTimeMillis();
			log.e("exitWorkThread cost time:" + (time2 - time1));
			mWorkThread = null;
		}
	}

	private void startDnsServer() {
		mDNSNotify.changeDeviceName();

		mDNSNotify.registerAirplay(RenderApplication.getInstance().mDeviceInfo.airplay_port);
		mDNSNotify.registerRaop(RenderApplication.getInstance().mDeviceInfo.airtunes_port);
	}


	private void stopDnsServer() {
		mDNSNotify.stop();
	}
}
