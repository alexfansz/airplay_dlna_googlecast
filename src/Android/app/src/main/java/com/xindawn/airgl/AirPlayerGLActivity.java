package com.xindawn.airgl;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.xindawn.R;
import com.xindawn.ScreenCast.ScreenCastService;
import com.xindawn.center.DlnaMediaModel;
import com.xindawn.center.DlnaMediaModelFactory;
import com.xindawn.center.MediaControlBrocastFactory;
import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;

import java.lang.reflect.Method;

public class AirPlayerGLActivity extends Activity implements MediaControlBrocastFactory.IMediaControlListener , SurfaceHolder.Callback
{
	private static final CommonLog log = LogFactory.createLog();
	private DlnaMediaModel mMediaInfo = new DlnaMediaModel();
	private MediaControlBrocastFactory mMediaControlBorcastFactor;
	public static Handler mHandler;

	private DisplayMetrics metric;
	WindowManager wm;
	private SurfaceView mSurfaceView;
	private int surfaceW,surfaceH;      //最初创建是对宽度和高度，中间实际宽高度会随setScreenSize变化，这里用来保留初始数据
	private float wOffset,hOffset;
	private VideoPlayer mVideoPlayer;
	//private AudioPlayer mAudioPlayer;
	
	private static final int REFRESH_SPEED= 0x0001;
	private static final int EXIT_ACTIVITY = 0x0002;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if(getIntent().hasExtra(DlnaMediaModelFactory.PARAM_GET_URL)){
			//Airplay
			//sprintf(url,"%s","http://127.0.0.1:55556/airmirror-760ba1810b647cb0.flv");

			//U2
			//sprintf(url,"%s","u2://127.0.0.1:40005/usbdisplay.h264");


			String url = getIntent().getStringExtra(DlnaMediaModelFactory.PARAM_GET_URL);
			log.d("onCreate 1->"+url);

			if(url.contains("airmirror")) {
				Intent intent = new Intent(ScreenCastService.MSG_filter);
				intent.putExtra("command", "TransState");
				intent.putExtra("status", "1");
				sendBroadcast(intent);
			}
		}

		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		getFilePath();
		
		
		mMediaControlBorcastFactor = new MediaControlBrocastFactory(this);
		mMediaControlBorcastFactor.register(this);
		
		mHandler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
				case EXIT_ACTIVITY:
					log.d("EXIT_ACTIVITY");
					mVideoPlayer.stopRUN();
					//if(null != mAudioPlayer) mAudioPlayer.stopRun();
					//mAudioPlayer = null;
					finish();
					break;
				}
			}
			
		};

	    Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        window.setAttributes(params);

		try {
			//Just for Realtek DMP Platform
			Class<?> cSystemProperties = Class.forName("android.os.SystemProperties");
			Method set = cSystemProperties.getDeclaredMethod("set", String.class, String.class);
			set.invoke(null, "persist.rtk.LowDelay", "1");

			set.invoke(null, "persist.rtk.searchI", "1");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			metric = new DisplayMetrics();
			wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
			wm.getDefaultDisplay().getRealMetrics(metric);
			initViews();
		}catch (NullPointerException e) {
			finish();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if(intent.hasExtra(DlnaMediaModelFactory.PARAM_GET_URL)){
			String url = intent.getStringExtra(DlnaMediaModelFactory.PARAM_GET_URL);
			log.d("onNewIntent 1->"+url);

			if(url.contains("airmirror")) {
				Intent intent_ = new Intent(ScreenCastService.MSG_filter);
				intent_.putExtra("command", "TransState");
				intent_.putExtra("status", "1");
				sendBroadcast(intent_);
			}
		}
	}

	@Override protected void onPause() {
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
    }
	
	
	private void removeExitMessage(){
		mHandler.removeMessages(EXIT_ACTIVITY);
	}
	
	private static final int EXIT_DELAY_TIME = 200;
	private void delayToExit(){
		removeExitMessage();
		//mHandler.sendEmptyMessageDelayed(EXIT_ACTIVITY, EXIT_DELAY_TIME);
		mHandler.sendEmptyMessage(EXIT_ACTIVITY);
	}
	

	private void getFilePath() {
		
		Intent intent = getIntent();
	    mMediaInfo = DlnaMediaModelFactory.createFromIntent(intent);
	}

	private void initViews() throws NullPointerException{
		setContentView(R.layout.player_activity);
		mSurfaceView = findViewById(R.id.surfaceView);

		mSurfaceView.getHolder().addCallback(this);
	}
//  保持屏幕常亮	
//	   @Override  
//	 protected void onResume() 
//	 {  
//	        super.onResume();  
//	        pManager = ((PowerManager) getSystemService(POWER_SERVICE));  
//	        mWakeLock = pManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK  
//	                | PowerManager.ON_AFTER_RELEASE, TAG);  
//	        mWakeLock.acquire();  
//	 }  
//	      
//	    @Override  
//	 protected void onPause() 
//	 {  
//	        super.onPause();  
//	          
//	        if(null != mWakeLock)
//	        {  
//	            mWakeLock.release();  
//	        }  
//	 }  
	
	@Override
	protected void onDestroy() {
		
		log.d("onDestroy 1");
		Intent intent_ = new Intent(ScreenCastService.MSG_filter);
		intent_.putExtra("command", "TransState");
		intent_.putExtra("status", "0");
		sendBroadcast(intent_);
		
		//mGlView.destroyDrawingCache();
		try {
			//Just for Realtek DMP Platform
			Class<?> cSystemProperties = Class.forName("android.os.SystemProperties");
			Method set = cSystemProperties.getDeclaredMethod("set", String.class, String.class);
			set.invoke(null, "persist.rtk.LowDelay", "0");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	
		//mGlView = null;
		mSurfaceView = null;
		
		mMediaControlBorcastFactor.unregister();
		super.onDestroy();
	}
	
	@Override
    public boolean onKeyUp(int i1, KeyEvent keyevent)
    {

        return true;
    }


	

	@Override
	public void finish() {
		// TODO Auto-generated method stub

		log.d("finish 1");
		super.finish();
		log.d("finish 2");

		log.d("finish 3");

	}

	
	@Override
	public void onPlayCommand() {
		// TODO Auto-generated method stub
		log.d("onPlayCommand ");
	}

	@Override
	public void onPauseCommand() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopCommand(int type) {
		if(1 == type)
		{
			log.d("excute onStopCommand" + "[" + type +"]");
			delayToExit();
		}
		else 
		{
			log.d("ignore onStopCommand" + "[" + type +"]");		
		}

	}

	@Override
	public void onSeekCommand(int time) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onCoverCommand(byte data[])
	{
		
	}
	
	@Override
	public void onMetaDataCommand(String data)
	{
		
	}
	
	@Override
	public void onIPAddrCommand(String data)
	{
		int width,height;

		String[] strData = data.split(":");
		width = Integer.parseInt(strData[0]);
		height= Integer.parseInt(strData[1]);


		log.d("onIPAddrCommand" + "[" + width +"]"+"["+height+"]");

		mSurfaceView.getHolder().setFixedSize(0x28062806, 0x28062806); //enable size changed
		mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        /*if(width > surfaceW){
            width = surfaceW;
        }
        if(height > surfaceH){
            height = surfaceH;
        }*/
		mSurfaceView.getHolder().setFixedSize(width, height);
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		//log.d("surfaceCreated" + "[" + surfaceW +"]"+"["+surfaceH+"]");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		log.d("surfaceChanged" + "[" + width +"]"+"["+height+"]" + "[" + metric.widthPixels +"]"+"["+metric.heightPixels+"]");

		wm.getDefaultDisplay().getRealMetrics(metric);

		surfaceH = height;
		surfaceW = width;

		wOffset = (metric.widthPixels - surfaceW) / 2;
		hOffset = (metric.heightPixels - surfaceH) / 2;

		if (mVideoPlayer == null) {
			mVideoPlayer = new VideoPlayer(holder.getSurface(),width,height);
			mVideoPlayer.start();
		}

		//MediaCodec 还有些问题，暂时用libfdk在jni中解出pcm
		//if(mAudioPlayer == null)
		//	mAudioPlayer = new AudioPlayer(MediaFormat.MIMETYPE_AUDIO_AAC);//adts
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}
//

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if(mVideoPlayer!=null) mVideoPlayer.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//-----------
		float x = event.getRawX() - wOffset;
		float y = event.getRawY() - hOffset;
		if(x>=0 && x<=surfaceW && y>=0 && y<=surfaceH) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				String parcle = String.format("ACTION_DOWN#%f#%f#%d#%d#", x, y, surfaceW, surfaceH);
				try {
					//log.d(parcle);
					Intent intent = new Intent(ScreenCastService.MSG_filter);
					intent.putExtra("WPOS", parcle);
					sendBroadcast(intent);
				} catch (Throwable t) {

				}
			} else if (event.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
				String parcle = String.format("ACTION_POINTER_DOWN#%f#%f#%d#%d#", x, y, surfaceW, surfaceH);
				try {
					//log.d(parcle);
					Intent intent = new Intent(ScreenCastService.MSG_filter);
					intent.putExtra("WPOS", parcle);
					sendBroadcast(intent);
				} catch (Throwable t) {

				}
			} else if (event.getAction() == MotionEvent.ACTION_POINTER_UP) {
				String parcle = String.format("ACTION_POINTER_UP#%f#%f#%d#%d#", x, y, surfaceW, surfaceH);
				try {
					//log.d(parcle);
					Intent intent = new Intent(ScreenCastService.MSG_filter);
					intent.putExtra("WPOS", parcle);
					sendBroadcast(intent);
				} catch (Throwable t) {

				}
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				String parcle = String.format("ACTION_UP#%f#%f#%d#%d#", x, y, surfaceW, surfaceH);
				try {
					//log.d(parcle);
					Intent intent = new Intent(ScreenCastService.MSG_filter);
					intent.putExtra("WPOS", parcle);
					sendBroadcast(intent);
				} catch (Throwable t) {

				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				String parcle = String.format("ACTION_MOVE#%f#%f#%d#%d#", x, y, surfaceW, surfaceH);
				try {
					//log.d(parcle);
					Intent intent = new Intent(ScreenCastService.MSG_filter);
					intent.putExtra("WPOS", parcle);
					sendBroadcast(intent);
				} catch (Throwable t) {

				}
			}
		}
		//++++++++++++
		return super.onTouchEvent(event);
	}
}
