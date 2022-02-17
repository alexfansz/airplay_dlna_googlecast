package com.xindawn.center;

import android.content.Context;
import android.content.Intent;

import com.xindawn.DLAN.DMR.DMRService;
import com.xindawn.RenderApplication;
import com.xindawn.ScreenCast.ScreenCastService;
import com.xindawn.service.MediaRenderService;
import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;



public class MediaRenderProxy implements IBaseEngine{

	private static final CommonLog log = LogFactory.createLog();
	
	private static  MediaRenderProxy mInstance;
	private Context mContext;
	
	private MediaRenderProxy(Context context) {
		mContext = context;
	}

	public static synchronized MediaRenderProxy getInstance() {
		if (mInstance == null){
			mInstance  = new MediaRenderProxy(RenderApplication.getInstance());
		}
		return mInstance;
	}

	@Override
	public boolean startEngine() {
		//mContext.startService(new Intent(MediaRenderService.START_RENDER_ENGINE));



		/*-------------------------------------------------------------------------*/
		/*-----attention:--getPackageName--is--acutually--applicationId----fuck!!!--------------------------------------------------------*/
		/*-------------------------------------------------------------------------*/
		/*-----so,-----mContext.getPackageName()+".service.MediaRenderService"-----IS----WRONG---!!!!!---------------------------------------------------*/
		/*-------------------------------------------------------------------------*/
		/*-------------------------------------------------------------------------*/
		/*-------------------------------------------------------------------------*/
		/*-------------------------------------------------------------------------*/

		Intent mIntent = new Intent();

		//ComponentName cn = new ComponentName(mContext.getPackageName(), mContext.getPackageName()+".service.MediaRenderService");
		//mIntent.setComponent(cn);
		mIntent.setClass(mContext,MediaRenderService.class);
		mIntent.setAction(MediaRenderService.START_RENDER_ENGINE); //你定义的service的action
		mContext.startService(mIntent);

		//Toast.makeText(RenderApplication.getInstance(), "MediaRenderProxy::startEngine+"+mContext.getPackageName(), Toast.LENGTH_SHORT).show();


		/*ComponentName cn = new ComponentName(mContext.getPackageName(), mContext.getPackageName()+".DLNA.DMR.DMRService");
		mIntent.setComponent(cn);*/
		//mIntent.setAction(DMRService.START_RENDER_ENGINE);

		Intent mIntentForDlnaDmr = new Intent();
		mIntentForDlnaDmr.setClass(mContext, DMRService.class);
		mIntentForDlnaDmr.setAction(DMRService.START_RENDER_ENGINE);
		mContext.startService(mIntentForDlnaDmr);

		Intent mIntentForScreenCast = new Intent();
		mIntentForScreenCast.setClass(mContext, ScreenCastService.class);
		mIntentForScreenCast.setAction(ScreenCastService.START_RENDER_ENGINE);
		mContext.startService(mIntentForScreenCast);

		return true;
	}

	@Override
	public boolean stopEngine() {
		mContext.stopService(new Intent(mContext, MediaRenderService.class));
		mContext.stopService(new Intent(mContext,DMRService.class));
		mContext.stopService(new Intent(mContext,ScreenCastService.class));

		return true;
	}

	@Override
	public boolean restartEngine() {
		//fix me, getPackageName is actually application id.
		//mContext.startService(new Intent(MediaRenderService.RESTART_RENDER_ENGINE));

		Intent mIntent = new Intent();
		mIntent.setAction(MediaRenderService.RESTART_RENDER_ENGINE);//你定义的service的action
		mIntent.setClass(mContext,MediaRenderService.class);
		//mIntent.setPackage(mContext.getPackageName());//这里你需要设置你应用的包名,actually ,it is applicationID
		mContext.startService(mIntent);

		Intent mIntentDmr = new Intent();
		mIntentDmr.setAction(DMRService.RESTART_RENDER_ENGINE);//你定义的service的action
		mIntentDmr.setClass(mContext,DMRService.class);
		//mIntent.setPackage(mContext.getPackageName());//这里你需要设置你应用的包名,actually ,it is applicationID
		mContext.startService(mIntentDmr);

		Intent mIntentForScreenCast = new Intent();
		mIntentForScreenCast.setClass(mContext, ScreenCastService.class);
		mIntentForScreenCast.setAction(ScreenCastService.RESTART_RENDER_ENGINE);
		mContext.startService(mIntentForScreenCast);

		return true;
	}

}
