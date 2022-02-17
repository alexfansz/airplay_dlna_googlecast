package com.xindawn.center;

import android.content.Context;

import com.xindawn.RenderApplication;
import com.xindawn.jni.PlatinumJniProxy;
import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;

public class DMRWorkThread extends Thread implements IBaseEngine{


	private static final CommonLog log = LogFactory.createLog();
	
	private static final int CHECK_INTERVAL = 30 * 1000; 
	
	private Context mContext = null;
	private boolean mStartSuccess = false;
	private boolean mExitFlag = false;
	
	private String mFriendName = "";
	private String mUUID = "";	
	private RenderApplication mApplication;
	
	public DMRWorkThread(Context context){
		mContext = context;
		mApplication = RenderApplication.getInstance();
	}
	
	public void  setFlag(boolean flag){
		mStartSuccess = flag;
	}
	
	public void awakeThread(){
		synchronized (this) {
			notifyAll();
		}
	}
	
	public void exit(){
		mExitFlag = true;
		awakeThread();
	}

	@Override
	public void run() {

		log.e("DMRWorkThread run...");
		/*Bugly.init(RenderApplication.getInstance(), "a2efc4c0e5", false);*/

		while(true)
		{
			if (mExitFlag){
				stopEngine();
				break;
			}
			refreshNotify();
			synchronized(this)
			{				
				try
				{
					wait(CHECK_INTERVAL);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}								
			}
			if (mExitFlag){
				stopEngine();
				break;
			}
		}
		
		log.e("DMRWorkThread over...");
		
	}
	
	public void refreshNotify(){
		//if (!CommonUtil.checkNetworkState(mContext)){
		//	return ;
		//}
		
		if (!mStartSuccess){
			stopEngine();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean ret = startEngine();
			if (ret){
				mStartSuccess = true;
			}
		}

	}
	
	@Override
	public boolean startEngine() {
		mFriendName = RenderApplication.getInstance().mDeviceInfo.dev_name;

		int ret = PlatinumJniProxy.startMediaRender_Java(mFriendName);
		boolean result = (ret == 0 ? true : false);
		mApplication.setDevStatus(result);
		return result;
	}

	@Override
	public boolean stopEngine() {
		PlatinumJniProxy.stopMediaRender();
		mApplication.setDevStatus(false);
		return true;
	}

	@Override
	public boolean restartEngine() {
		setFlag(false);
		awakeThread();
		return true;
	}

}
