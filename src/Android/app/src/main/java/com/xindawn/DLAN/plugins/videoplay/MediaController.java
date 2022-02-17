package com.xindawn.DLAN.plugins.videoplay;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.rockchip.mediacenter.dlna.dmr.SysUtils;
import com.rockchip.mediacenter.dlna.dmr.SysUtils.BrightnessUtil;
import com.xindawn.R;

import java.util.Formatter;
import java.util.Locale;

/**
 * MediaController will hide and
 * show the buttons according to these rules:
 * <ul>
 * <li> The "previous" and "next" buttons are hidden until setPrevNextListeners()
 *   has been called
 * <li> The "previous" and "next" buttons are visible but disabled if
 *   setPrevNextListeners() was called with null listeners
 * <li> The "rewind" and "fastforward" buttons are shown unless requested
 *   otherwise by using the MediaController(Context, boolean) constructor
 *   with the boolean set to false
 * </ul>
 */
public class MediaController extends FrameLayout {

	public static final boolean FULLSCREEN_MAINTIANXY = true;
	//屏幕大小模式
	public static final int SCREEN_MODE_ORIG = 0;
	public static final int SCREEN_MODE_169 = 1;
	public static final int SCREEN_MODE_43 = 2;
	public static final int SCREEN_MODE_FULL = 3;
	
    private MediaPlayerControl  mPlayer;
    private Context             mContext;
    private View                mControlView;
    private ProgressBar         mProgress;
    private TextView            mEndTime, mCurrentTime;
    private boolean             mShowing;
    private boolean             mDragging;
    private static final int    sDefaultTimeout = 3000;
    private static final int    FADE_OUT = 1;
    private static final int    SHOW_PROGRESS = 2;
    private static final int    SEEK_TO = 3;
    StringBuilder               mFormatBuilder;
    Formatter                   mFormatter;
    private ImageButton         mScreenModeButton;
    private ImageButton         mScreenBrightButton;
    private ImageButton         mPrevButton;
    private ImageButton         mPauseButton;
    private ImageButton         mNextButton;
    private ImageButton         mExitButton;
    private ImageButton         mMoreButton;
    private ImageButton         mVolumePlusButton;
    private ImageButton         mVolumeMinusButton;
    
    private int		 			mScreenSizeMode = -1;
    private int		 			mScreenBrightMode = 0;
    private long				mLastSeekTime;
    private int					mLastSeekPosition;
    private View 				mLastFocusView;

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }


    public MediaController(Context context) {
        super(context);
        mContext = context;
    }
    
    public void setControlView(View view){
    	mControlView = view;
    	initControllerView(mControlView);
    }
    
    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    private void initControllerView(View v) {
    	mScreenModeButton = (ImageButton) v.findViewById(R.id.menubar_btn_screenMode);
        if (mScreenModeButton != null) {
        	mScreenModeButton.requestFocus();
        	mScreenModeButton.setOnClickListener(mScreenModeListener);
        }
        
        mScreenBrightButton = (ImageButton) v.findViewById(R.id.menubar_btn_screenBright);
        if (mScreenBrightButton != null) {
        	mScreenBrightButton.requestFocus();
        	mScreenBrightButton.setOnClickListener(mScreenBrightListener);
        }

    	mPrevButton = (ImageButton) v.findViewById(R.id.menubar_btn_prev);
        if (mPrevButton != null) {
        	mPrevButton.requestFocus();
        	mPrevButton.setOnClickListener(mPrevListener);
        }
    	
        mPauseButton = (ImageButton) v.findViewById(R.id.menubar_btn_pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }
        
        mNextButton = (ImageButton) v.findViewById(R.id.menubar_btn_next);
        if (mNextButton != null) {
        	mNextButton.requestFocus();
        	mNextButton.setOnClickListener(mNextListener);
        }
        
        mExitButton = (ImageButton) v.findViewById(R.id.menubar_btn_exit);
        if (mExitButton != null) {
        	mExitButton.requestFocus();
        	mExitButton.setOnClickListener(mExitListener);
        }
        
        mMoreButton = (ImageButton) v.findViewById(R.id.menubar_btn_more);
        if (mMoreButton != null) {
        	mMoreButton.requestFocus();
        	mMoreButton.setOnClickListener(mMoreListener);
        }

        mProgress = (ProgressBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }
        
        mVolumePlusButton = (ImageButton) v.findViewById(R.id.menubar_btn_volumeplus);
        if (mVolumePlusButton != null) {
        	mVolumePlusButton.requestFocus();
        	mVolumePlusButton.setOnClickListener(mVolumePlusListener);
        }
        
        mVolumeMinusButton = (ImageButton) v.findViewById(R.id.menubar_btn_volumeminus);
        if (mVolumeMinusButton != null) {
        	mVolumeMinusButton.requestFocus();
        	mVolumeMinusButton.setOnClickListener(mVolumeMinusListener);
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
        	if(mPlayer==null) return;
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
            if (mPrevButton != null && !mPlayer.canSeekBackward()) {
            	mPrevButton.setEnabled(false);
            }
            if (mNextButton != null && !mPlayer.canSeekForward()) {
            	mNextButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }
    
    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout) {
    	if(mPlayer==null){
    		Log.e("MediaController", "MediaPlayer is null. ");
    		return;
    	}
        if (!mShowing) {
            setProgress();
            if(mLastFocusView!=null){
            	mLastFocusView.requestFocus();
            }else if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButtons();
            if(mControlView!=null){
            	mControlView.setVisibility(View.VISIBLE);
            }
            mShowing = true;
        }
        updatePausePlay();
        
        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }
    
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mControlView == null)
            return;

        if (mShowing) {
        	mLastFocusView = mControlView.findFocus();
            mHandler.removeMessages(SHOW_PROGRESS);
            if(mControlView!=null){
            	mControlView.setVisibility(View.GONE);
            }
            mShowing = false;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress();
                    if (!mDragging && mShowing && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case SEEK_TO:
                	mPlayer.seekTo(msg.arg1);
                	break;
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
//        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
//        } else {
//            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
//        }
    }

    public int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        if(mLastSeekPosition>0&&mHandler.hasMessages(SEEK_TO)){
        	position = mLastSeekPosition;
        }
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress( (int) pos);
            }
            //mPlayer.getBufferPercentage获取的不是整个内容的缓存百分比，暂时没有接口获取
            //int percent = mPlayer.getBufferPercentage();
            //mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(sDefaultTimeout);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
        	&& event.getAction() == KeyEvent.ACTION_DOWN;
        if (event.getRepeatCount() == 0 && event.getAction()==KeyEvent.ACTION_DOWN && (
                keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK ||
                keyCode ==  KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode ==  KeyEvent.KEYCODE_SPACE)) {
        	if(uniqueDown){
	            doPauseResume();
	            show(sDefaultTimeout);
	            if (mPauseButton != null) {
	                mPauseButton.requestFocus();
	            }
        	}
            return true;
        } else if (keyCode ==  KeyEvent.KEYCODE_MEDIA_STOP) {
            if (uniqueDown&&mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // don't show the controls for volume adjustment
        	if(uniqueDown){
        		SysUtils.volumeAdjust(mContext, mPlayer.getVolumeMode(), SysUtils.Def.VOLUMEMINUS);
        	}
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
        	if(uniqueDown){
        		SysUtils.volumeAdjust(mContext, mPlayer.getVolumeMode(), SysUtils.Def.VOLUMEPLUS);
        	}
       	 	return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
        	if(uniqueDown){
        		hide();
        	}
            //return true;
            return false;
        } else if(keyCode == KeyEvent.KEYCODE_HOME){
        	//DBUtils.setbackBacklight(mContext,mPlayer.getBrightMode());
        	if(uniqueDown){
        		SysUtils.setbackVolume(mContext, mPlayer.getVolumeMode());
        	}
  	   	 	return false;
        } else if(keyCode == com.xindawn.DLAN.plugins.videoplay.VideoPlayer.KEYCODE_VOLUME_MUTE){
        	return super.dispatchKeyEvent(event);
        } else {
            show(sDefaultTimeout);
        }
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private void updatePausePlay() {
        if (mControlView == null || mPauseButton == null)
            return;

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.plug_vp_pause);
        } else {
            mPauseButton.setImageResource(R.drawable.plug_vp_play);
        }
    }

    public void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
    	long duration;
        public void onStartTrackingTouch(SeekBar bar) {
        	setProgress();
        	//show(3600000);
        	show();
            duration = mPlayer.getDuration();
            mPlayer.pause();

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            // mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            //mDragging = true;
            show();//解决不断seek, Bar隐藏后，焦点移动到下方。
            duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            
            if(mPlayer.isSeeking()||mHandler.hasMessages(SEEK_TO)||(System.currentTimeMillis()-mLastSeekTime<180)){
            	mHandler.removeMessages(SEEK_TO);
            	Message seekMsg = mHandler.obtainMessage(SEEK_TO);
            	seekMsg.arg1 = (int) newposition;
            	mHandler.sendMessageDelayed(seekMsg, 180);
            }else{
            	mPlayer.seekTo( (int) newposition);
            }
            mLastSeekPosition = (int)newposition;
            mLastSeekTime = System.currentTimeMillis();
            
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime( (int) newposition));
            if(mEndTime != null){
            	mEndTime.setText(stringForTime( (int) duration));
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
        	mHandler.removeMessages(SEEK_TO);
        	mDragging = false;
            int progress = mProgress.getProgress();
            duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
        	if(newposition >= duration){                		
        		newposition = duration - 2000;
        	}
        	mPlayer.seekTo( (int) newposition);		
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime( (int) newposition));
            if(mEndTime != null){
            	mEndTime.setText(stringForTime( (int) duration));
            }
            setProgress();
        	mPlayer.start();
            updatePausePlay();
            show(sDefaultTimeout);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        if (mVolumePlusButton != null) {
        	mVolumePlusButton.setEnabled(enabled);
        }
        if (mVolumeMinusButton != null) {
        	mVolumeMinusButton.setEnabled(enabled);
        }
        if (mScreenModeButton != null) {
        	mScreenModeButton.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
        	doPrevious();
        }
    };
    public void doPrevious(){
    	if(!mPlayer.prev())//上一首不成功时 执行快退
    		doQuickPrevious();
    }
    public void doQuickPrevious(){
    	if(mPlayer==null) return;
    	int pos = mPlayer.getCurrentPosition();
        pos -= 5000; // milliseconds
        mPlayer.seekTo(pos);
        setProgress();

        show(sDefaultTimeout);
    }

    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
        	doNext();
        }
    };
    public void doNext(){
    	if(!mPlayer.next())//下一首不成功时 执行快进
    		doQuickNext();
    }
    public void doQuickNext(){
    	if(mPlayer==null) return;
    	int pos = mPlayer.getCurrentPosition();
        pos += 15000; // milliseconds
        mPlayer.seekTo(pos);
        setProgress();

        show(sDefaultTimeout);
    }
    
    private View.OnClickListener mExitListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		mPlayer.finish();
        	}
        }
    };
    
    private View.OnClickListener mMoreListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		mPlayer.selectPlayMode();
        	}
        }
    };
    
    private View.OnClickListener mVolumePlusListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		SysUtils.volumeAdjust(mContext, mPlayer.getVolumeMode(), SysUtils.Def.VOLUMEPLUS);
        		show();
        	}
        }
    };
    
    private View.OnClickListener mVolumeMinusListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		SysUtils.volumeAdjust(mContext, mPlayer.getVolumeMode(), SysUtils.Def.VOLUMEMINUS);
        		show();
        	}
        }
    };
    
    /**
     * 屏幕尺寸大小调整监听
     */
    private View.OnClickListener mScreenModeListener = new View.OnClickListener(){
        public void onClick(View v) 
        {
        	show(sDefaultTimeout);
        	//mPlayer.SetScreenSizeClickMode(true);
        	if(mScreenSizeMode == SCREEN_MODE_FULL){
        		mScreenSizeMode = SCREEN_MODE_ORIG;
        	}else{
        		mScreenSizeMode++;
        	}
        	setScreen(mScreenSizeMode);
        }
    };
    
    public void setCurrentScreen(){
    	if(mScreenSizeMode==-1){
    		mScreenSizeMode = SysUtils.getScreenValue(mContext);
    	}
    	setButtonSrcByMode(mScreenSizeMode);
    	setScreenSize(mScreenSizeMode);
    }
    
    public void setScreen(int ScreenSizeMode){
    	setButtonSrcByMode(ScreenSizeMode);
    	setScreenSize(ScreenSizeMode);
    }
    private void setScreenSize(int mode)
    {
    	DisplayMetrics dm = com.rockchip.mediacenter.plugins.videoplay.WindowUtils.getWindowMetrics((Activity) mContext);
	    int maxWidth = dm.widthPixels;
	    int maxHeight = dm.heightPixels;
    	switch(mode){
    	case SCREEN_MODE_ORIG:
    		if(FULLSCREEN_MAINTIANXY){
    			if(mPlayer.getDefaultWidth() > maxWidth || mPlayer.getDefaultHeight() > maxHeight){
	    			float degree = (float)mPlayer.getDefaultWidth() / (float)mPlayer.getDefaultHeight();
		    		int tmpWidth1 = maxWidth;
		    		int tmpHeight1 = (int)(tmpWidth1 / degree);
		    		
		    		int tmpHeight2 = maxHeight;
		    		int tmpWidth2 = (int)(tmpHeight2 * degree);
		    		
		    		if(tmpHeight1 > maxHeight && tmpWidth2 <= maxWidth){
		    			mPlayer.setScreenSize(tmpWidth2, tmpHeight2);
		    		}else if(tmpWidth2 > maxWidth && tmpHeight1 <= maxHeight){
		    			mPlayer.setScreenSize(tmpWidth1, tmpHeight1);
		    		}else if(tmpHeight1 <= maxHeight && tmpWidth2 <= maxWidth){
		    			if(tmpWidth1 * tmpHeight1 > tmpWidth2 * tmpHeight2){
		    				mPlayer.setScreenSize(tmpWidth1, tmpHeight1);
		    			}else{
		    				mPlayer.setScreenSize(tmpWidth2, tmpHeight2);
		    			}
		    		}
		    		else{
		    			mPlayer.setScreenSize(maxWidth,maxHeight);
		    		}
	    		}else{
	    			mPlayer.setScreenSize(mPlayer.getDefaultWidth(), mPlayer.getDefaultHeight());
	    		}
    		}else{
    			mPlayer.setScreenSize(mPlayer.getDefaultWidth(), mPlayer.getDefaultHeight());
    		}
    		SysUtils.setScreenValue(mContext, mode);
    		break;
    	case SCREEN_MODE_169:
    		mPlayer.setScreenSize(maxWidth,maxWidth/16*9);
    		SysUtils.setScreenValue(mContext, mode);
    		break;
    	case SCREEN_MODE_43:
    		mPlayer.setScreenSize(maxHeight/3*4,maxHeight);
    		SysUtils.setScreenValue(mContext, mode);
    		break;
    	case SCREEN_MODE_FULL:
    		if(mPlayer.getDefaultWidth() == 0 || mPlayer.getDefaultHeight() == 0){
    			mPlayer.setScreenSize(maxWidth,maxHeight);
    			SysUtils.setScreenValue(mContext, mode);
	    		break;
    		}
    		
    		if(FULLSCREEN_MAINTIANXY){
    			float degree = (float)mPlayer.getDefaultWidth() / (float)mPlayer.getDefaultHeight();
	    		int tmpWidth1 = maxWidth;
	    		int tmpHeight1 = (int)(tmpWidth1 / degree);
	    		
	    		int tmpHeight2 = maxHeight;
	    		int tmpWidth2 = (int)(tmpHeight2 * degree);
	    		
	    		if(tmpHeight1 > maxHeight && tmpWidth2 <= maxWidth){
	    			mPlayer.setScreenSize(tmpWidth2, tmpHeight2);
	    		}else if(tmpWidth2 > maxWidth && tmpHeight1 <= maxHeight){
	    			mPlayer.setScreenSize(tmpWidth1, tmpHeight1);
	    		}else if(tmpHeight1 <= maxHeight && tmpWidth2 <= maxWidth){
	    			if(tmpWidth1 * tmpHeight1 > tmpWidth2 * tmpHeight2){
	    				mPlayer.setScreenSize(tmpWidth1, tmpHeight1);
	    			}else{
	    				mPlayer.setScreenSize(tmpWidth2, tmpHeight2);
	    			}
	    		}
	    		else{
	    			mPlayer.setScreenSize(maxWidth,maxHeight);
	    		}
    		}else{
    			mPlayer.setScreenSize(maxWidth,maxHeight);
    		}
    		
    		SysUtils.setScreenValue(mContext, mode);
    		break;
    	}
    }
    
    public void setButtonSrcByMode(int mode)
    {
    	if(mScreenModeButton == null){
    		return;
    	}
    	Drawable drawable = mScreenModeButton.getDrawable();
    	drawable.setLevel(mode);
    	mScreenSizeMode = mode;
    }
    
    /**
     * 屏幕亮度监听
     */
    private final int MIN_BRIGHTNESS = 118;
    private final int INTEVAL_BRIGHTNESS = 32;
    private View.OnClickListener mScreenBrightListener = new View.OnClickListener() {
        public void onClick(View v) {
        	show();
        	if(mScreenBrightMode == 4)
        		mScreenBrightMode = 0;
        	else
        		mScreenBrightMode++;
        	setScreenMode(mScreenBrightMode);
        }
    };
    private void setScreenMode(int mode) {
    	if(mode>4) mode=4;
    	if(mode<0) mode=0;
    	BrightnessUtil.setUserBrightness(mContext, MIN_BRIGHTNESS+mode*INTEVAL_BRIGHTNESS);
    	Drawable drawable = mScreenBrightButton.getDrawable();
    	drawable.setLevel(mode);
    	mScreenBrightMode = mode;
    }
    public void setScreenBrightness(int brightness){
    	int screenBrightMode = 0;
    	int levelBrightness = brightness-MIN_BRIGHTNESS;
    	if(levelBrightness<=0){
    		screenBrightMode = 0;
    	}else{
    		screenBrightMode = levelBrightness/INTEVAL_BRIGHTNESS;
    	}
    	setScreenMode(screenBrightMode);
    }
    
    /**
     * Whether in media control view
     * @param x
     * @param y
     * @return
     */
    public boolean isInMediaControlLayout(float x, float y){
    	if(mControlView!=null){
    		int[] loc = new int[2];
    		ViewGroup vg = (ViewGroup)mControlView;
    		if(vg.getChildCount()>0){
    			vg.getChildAt(0).getLocationInWindow(loc);
	    		if((x>=loc[0]&&y>=loc[1])||(loc[0]==0&&loc[1]==0)){
	    			return true;
	    		}else{
	    			return false;
	    		}
    		}
    	}
    	return true;
    }

    public interface MediaPlayerControl {
        void    start();
	    boolean prev();
	    boolean next();
        void    pause();
        int     getDuration();
        int     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufferPercentage();
        boolean canPause();
        boolean canSeekBackward();
        boolean canSeekForward();
        void    finish();
	    int 	getVolumeMode();
	    int     getDefaultWidth();
	    int 	getDefaultHeight();
	    void 	setScreenSize(int width,int height);
	    void 	selectPlayMode();
	    boolean isSeeking();
    }
}
