package com.xindawn.DLAN.plugins.musicplay;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.xindawn.R;

import java.util.Formatter;
import java.util.Locale;

public class MusicController extends LinearLayout {

    public MusicController(Context context) {
        super(context);
        mRoot = this;
    }
	
	public MusicController(Context context, AttributeSet attrs) {
		super(context, attrs);
		mRoot = this;
	}

	private PlayControl  		mPlayer;
    private View                mRoot;
    private TextView            mEndTime, mCurrentTime;
    private boolean             mDragging;
    private static final int    SHOW_PROGRESS = 1;
    private static final int    SEEK_TO = 3;
    StringBuilder               mFormatBuilder;
    Formatter                   mFormatter;

    private SeekBar             mSeekBar;
    private ImageButton         mStopButton;
    private ImageButton         mPrevButton;
    private ImageButton         mPauseButton;
    private ImageButton         mNextButton;
    private ImageButton         mExitButton;
    private ImageButton         mMoreButton;
    private ImageButton         mVolumePlusButton;
    private ImageButton         mVolumeMinusButton;
    
    private int					DEFAULT_CYCLE = 1000;
    private int					mUpdateCycle = DEFAULT_CYCLE;
    private long				mLastSeekTime;
    private int					mLastSeekPosition;
    
    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
    }
    
    public void setPlayControl(PlayControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    private void initControllerView(View v) {
    	mStopButton = (ImageButton) findViewById(R.id.playbar_btn_stop);
        if (mStopButton != null) {
        	mStopButton.setOnClickListener(mStopListener);
        }
    	
    	mPrevButton = (ImageButton) v.findViewById(R.id.playbar_btn_prev);
        if (mPrevButton != null) {
        	mPrevButton.setOnClickListener(mPrevListener);
        }
    	
        mPauseButton = (ImageButton) v.findViewById(R.id.playbar_btn_pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }
        
        mNextButton = (ImageButton) v.findViewById(R.id.playbar_btn_next);
        if (mNextButton != null) {
        	mNextButton.setOnClickListener(mNextListener);
        }
        
        mExitButton = (ImageButton) v.findViewById(R.id.playbar_btn_exit);
        if (mExitButton != null) {
        	mExitButton.setOnClickListener(mExitListener);
        }
        
        mMoreButton = (ImageButton) v.findViewById(R.id.playbar_btn_more);
        if (mMoreButton != null) {
        	//mMoreButton.requestFocus();
        	mMoreButton.setOnClickListener(mMoreListener);
        }

        mSeekBar = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
        if (mSeekBar != null) {
        	mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        	mSeekBar.setMax(1000);
        }
        
        mVolumePlusButton = (ImageButton) v.findViewById(R.id.playbar_btn_volumeplus);
        if (mVolumePlusButton != null) {
        	mVolumePlusButton.setOnClickListener(mVolumePlusListener);
        }
        
        mVolumeMinusButton = (ImageButton) v.findViewById(R.id.playbar_btn_volumeminus);
        if (mVolumeMinusButton != null) {
        	mVolumeMinusButton.setOnClickListener(mVolumeMinusListener);
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }


    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
        } catch (IncompatibleClassChangeError ex) {
        }
    }
    
    public void setDefaultFoucs(){
    	if(mPauseButton!=null){
    		mPauseButton.requestFocus();
    	}
    }
    
    /**
     * 启动更新进度
     * cycle millSec
     */
    public void show(){
    	updatePausePlay();
    	mHandler.removeMessages(SHOW_PROGRESS);
    	mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }
    
    /**
     * 设置进度更新周期 默认为1000ms
     * @param msec
     */
    public void setUpdateCycle(int msec){
    	mUpdateCycle = msec;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS:
                    setProgress();
                    if (!mDragging && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, mUpdateCycle);
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
        return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
    }

    /**
     * 修改滚动条
     */
    public int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        if(mLastSeekPosition>0&&mHandler.hasMessages(SEEK_TO)){
        	position = mLastSeekPosition;
        }
        int duration = mPlayer.getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mSeekBar.setProgress( (int) pos);
            }else{
            	mSeekBar.setProgress(0);
            }
            //mPlayer.getBufferPercentage获取的不是整个内容的缓存百分比，暂时没有接口获取
            //int percent = mPlayer.getBufferPercentage();
            //mSeekBar.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }


    /**
     * 开始/暂停
     */
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    public void updatePausePlay() {
        if (mRoot == null || mPauseButton == null)
            return;

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.plug_vp_pause);
        } else {
            mPauseButton.setImageResource(R.drawable.plug_vp_play);
        }
        //mPauseButton.requestFocus();//解决遥控器操作Seekbar 无法连续快进/快退
    }

    public void doPauseResume() {
        if (mPlayer.isPlaying()) {
            pause();
        } else {
            start();
        }
        updatePausePlay();
    }

    /**
     * 拖动SeekBar
     */
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
    	long duration;
    	boolean isPlaying = false;
        public void onStartTrackingTouch(SeekBar bar) {
        	setProgress();
        	show();
            duration = mPlayer.getDuration();
            if(isPlaying=mPlayer.isPlaying()){
            	pause();
            	//updatePausePlay();
            }
            mDragging = true;
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            //mDragging = true;
            duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            
            if(mHandler.hasMessages(SEEK_TO)||(System.currentTimeMillis()-mLastSeekTime<180)){
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
            int progress = mSeekBar.getProgress();
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
        	show();
            if(isPlaying){
            	start();
            	//updatePausePlay();
            }
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
    	if (mStopButton != null) {
    		mStopButton.setEnabled(enabled);
        }
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled);
        }
        if (mSeekBar != null) {
        	mSeekBar.setEnabled(enabled);
        }
        if (mVolumePlusButton != null) {
        	mVolumePlusButton.setEnabled(enabled);
        }
        if (mVolumeMinusButton != null) {
        	mVolumeMinusButton.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }
    
    /**
     * 停止播放
     */
    private View.OnClickListener mStopListener = new View.OnClickListener() {
        public void onClick(View v) {
        	stop();
        }
    };

    /**
     * 上一首
     */
    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
        	previous();
        }
    };
    public void previous(){
    	if(!mPlayer.prev())//上一首不成功时 执行快退
    		doQuickPrevious();
    }
    public void doQuickPrevious(){
    	int pos = mPlayer.getCurrentPosition();
        pos -= 5000; // milliseconds
        mPlayer.seekTo(pos);
        setProgress();
    }

    /**
     * 下一首
     */
    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
        	next();
        }
    };
    public void next(){
    	if(!mPlayer.next())//下一首不成功时 执行快进
    		doQuickNext();
    }
    public void doQuickNext(){
    	int pos = mPlayer.getCurrentPosition();
        pos += 15000; // milliseconds
        mPlayer.seekTo(pos);
        setProgress();
    }
    
    /**
     * 退出
     */
    private View.OnClickListener mExitListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		mPlayer.doFinish();
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
    
    /**
     * 音量加
     */
    private View.OnClickListener mVolumePlusListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		mPlayer.plusVolume();
        	}
        }
    };
    
    /**
     * 音量减
     */
    private View.OnClickListener mVolumeMinusListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		mPlayer.minusVolume();
        	}
        }
    };
    
    public void start(){
    	if(mPlayer!=null){
    		mPlayer.start();
            updatePausePlay();
            show();
    	}
    }
    
    public void stop(){
    	if(mPlayer!=null){
    		mPlayer.stop();
            updatePausePlay();
    		mHandler.removeMessages(SHOW_PROGRESS);
    		setProgress();
    	}
    }
    
    public void pause(){
    	if(mPlayer!=null){
    		mPlayer.pause();
            updatePausePlay();
    		mHandler.removeMessages(SHOW_PROGRESS);
    		setProgress();
    	}
    }
    

    public interface PlayControl {
	    boolean prev();
		boolean next();
        boolean isPlaying();
        void    start();
        void    pause();
        void    stop();
        void    seekTo(int pos);
        void    plusVolume();
        void    minusVolume();
        void    doFinish();
        int     getBufferPercentage();
        int     getCurrentPosition();
        int     getDuration();
		void 	selectPlayMode();
    }
}
