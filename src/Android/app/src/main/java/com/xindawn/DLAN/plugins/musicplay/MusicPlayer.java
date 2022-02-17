package com.xindawn.DLAN.plugins.musicplay;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xindawn.R;
import com.rockchip.mediacenter.common.util.StringUtils;
import com.rockchip.mediacenter.core.dlna.model.ProtocolInfo;
import com.rockchip.mediacenter.dlna.dmp.model.MediaItem;
import com.rockchip.mediacenter.dlna.dmr.SysUtils;
import com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity;
import com.rockchip.mediacenter.plugins.renderplay.PlayMode;
import com.xindawn.DLAN.plugins.videoplay.CountdownTextView;
import com.xindawn.DLAN.plugins.widget.Alert;
import com.xindawn.DLAN.plugins.musicplay.MusicController;

import java.io.IOException;

public class MusicPlayer extends MediaRenderPlayerActivity implements OnPreparedListener, OnErrorListener, OnCompletionListener, MusicController.PlayControl {
	private static final int COUNT_DOWN_TIME = 3;
	private CountdownTextView mCountdownTextView;
    private PreviewPlayer mPlayer;
    private TextView mTextLine1;
    private TextView mTextLine2;
    private TextView mLoadingText;
    private AudioManager mAudioManager;
    private boolean mPausedByTransientLossOfFocus;
    private MusicController mMusicController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.plugin_music_play);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

		mCountdownTextView = (CountdownTextView) findViewById(R.id.exit_text);
		mCountdownTextView.setCount(COUNT_DOWN_TIME);
        mTextLine1 = (TextView) findViewById(R.id.line1);
        mTextLine2 = (TextView) findViewById(R.id.line2);
        mLoadingText = (TextView) findViewById(R.id.loading);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        mMusicController = (MusicController) findViewById(R.id.music_controller);
        mMusicController.setPlayControl(this);
        
        super.onCallHandleURL(getIntent());
    }
    
    /** 
	 * <p>Title: onNewIntent</p> 
	 * <p>Description: </p> 
	 * @param intent 
	 * @see android.app.Activity#onNewIntent(android.content.Intent) 
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		stopCountdownExit();
		mMusicController.stop();
		resetNames();
		super.onCallHandleURL(intent);
	}
	
	/** 
	 * <p>Title: onHandleURLCompletion</p> 
	 * <p>Description: </p>  
	 */
	@Override
	protected void onHandleURLCompletion(Uri uri) {
		showLoadPreparreUI();
        PreviewPlayer player = (PreviewPlayer) getLastNonConfigurationInstance();
        if (player == null) {
            mPlayer = new PreviewPlayer();
            mPlayer.setActivity(MusicPlayer.this);
            try {
                super.onCallURLChanged();
                mPlayer.setDataSourceAndPrepare(uri);
            } catch (IOException ex) {
				onError(null, -1, -1);
                return;
            }
        } else {
            mPlayer = player;
            mPlayer.setActivity(MusicPlayer.this);
            if (mPlayer.isPrepared()) {
                showPostPrepareUI();
                super.onCallMediaPrepared();
            }
        }
	}

    @Override
    public Object onRetainNonConfigurationInstance() {
        PreviewPlayer player = mPlayer;
        mPlayer = null;
        return player;
    }
    
    /** 
     * <p>Title: onStop</p> 
     * <p>Description: </p>  
     * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onStop() 
     */
    @Override
    protected void onStop() {
        stopPlayback();
    	super.onStop();
    	//mMusicController.pause();
    }
    
    
    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    private void stopPlayback() {
        if (mPlayer != null) {
        	mPlayer.stop();
        	mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }

    public void onPrepared(MediaPlayer mp) {
        if (isFinishing()) return;
        mPlayer = (PreviewPlayer) mp;
        setNames();
        start();
        showPostPrepareUI();
        mMusicController.show();
        super.onCallMediaPrepared();
    }

    private void showPostPrepareUI() {
    	mMainHandler.post(new Runnable(){
    		public void run() {
    			ProgressBar pb = (ProgressBar) findViewById(R.id.spinner);
    	        pb.setVisibility(View.GONE);
    	        mLoadingText.setVisibility(View.GONE);
    	        View v = findViewById(R.id.titleandbuttons);
    	        v.setVisibility(View.VISIBLE);
    	        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
    	                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    		}
    	});
    }
    
    /**
     * 加载
     */
    private void showLoadPreparreUI(){
    	mMainHandler.post(new Runnable(){
    		public void run() {
		    	ProgressBar pb = (ProgressBar) findViewById(R.id.spinner);
		        pb.setVisibility(View.VISIBLE);
		        String scheme = getCurrentURI().getScheme();
		        if ("http".equalsIgnoreCase(scheme)) {
		            mLoadingText.setVisibility(View.VISIBLE);
		            String msg = getString(R.string.plug_music_loading, getCurrentURI().getHost());
		            mLoadingText.setText(msg);
		        } else {
		            mLoadingText.setVisibility(View.GONE);
		        }
		        
		        View v = findViewById(R.id.titleandbuttons);
		        v.setVisibility(View.GONE);
		        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
		                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    		}
    	});
    }
    
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (mPlayer == null) {
                // this activity has handed its MediaPlayer off to the next activity
                // (e.g. portrait/landscape switch) and should abandon its focus
                mAudioManager.abandonAudioFocus(this);
                return;
            }
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    mPausedByTransientLossOfFocus = false;
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (isPlaying()) {
                        mPausedByTransientLossOfFocus = true;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        startPlay();
                    }
                    break;
            }
            mMusicController.updatePausePlay();
        }
    };
    
    private void startPlay() {
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        start();
    }
    
    /**
     * 重置名称
     */
    private void resetNames(){
    	mTextLine1.setText("");
    	mTextLine2.setText("");
    }
    
    public void setNames() {
    	String title = getCurrentMediaItem().getTitle();
    	if(!StringUtils.hasLength(title)){
	        if (TextUtils.isEmpty(mTextLine1.getText())) {
	            mTextLine1.setText(getCurrentURI().getLastPathSegment());
	        }
    	}else{
    		mTextLine1.setText(title);
    	}
        if (TextUtils.isEmpty(mTextLine2.getText())) {
            mTextLine2.setVisibility(View.GONE);
        } else {
            mTextLine2.setVisibility(View.VISIBLE);
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, R.string.plug_music_playback_failed, Toast.LENGTH_SHORT).show();
        finish();
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
        mMusicController.show();
        super.onCallMediaCompletion();
    }

    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        	case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
        		mMusicController.doQuickNext();
        		return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            	mMusicController.doQuickPrevious();
        		return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            	mMusicController.previous();
    			return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            	mMusicController.next();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            	mMusicController.doPauseResume();
    			return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
            	mMusicController.stop();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
     * Wrapper class to help with handing off the MediaPlayer to the next instance
     * of the activity in case of orientation change, without losing any state.
     */
    private static class PreviewPlayer extends MediaPlayer implements OnPreparedListener {
        MusicPlayer mActivity;
        boolean mIsPrepared = false;
        int mCurrentBufferPercentage = 0;

        public void setActivity(MusicPlayer activity) {
            mActivity = activity;
            setOnPreparedListener(this);
            setOnErrorListener(mActivity);
            setOnCompletionListener(mActivity);
            setOnBufferingUpdateListener(mBufferingUpdateListener);
            setOnSeekCompleteListener(mSeekCompleteListener);
        }

        public void setDataSourceAndPrepare(Uri uri) throws IllegalArgumentException,
                        SecurityException, IllegalStateException, IOException {
            setDataSource(mActivity,uri);
            mCurrentBufferPercentage = 0;
            prepareAsync();
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            mActivity.onPrepared(mp);
        }

        boolean isPrepared() {
            return mIsPrepared;
        }
        
        public int getBufferPercentage() {
            return mCurrentBufferPercentage;
        }
        
        /** 
         * <p>Title: reset</p> 
         * <p>Description: </p>  
         * @see android.media.MediaPlayer#reset() 
         */
        @Override
        public void reset() {
        	super.reset();
        	mIsPrepared = false;
        }
        
        /** 
         * <p>Title: release</p> 
         * <p>Description: </p>  
         * @see android.media.MediaPlayer#release() 
         */
        @Override
        public void release() {
        	super.release();
        	mIsPrepared = false;
        }
        
        private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mCurrentBufferPercentage = percent;
            }
        };
        
        private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener(){
			public void onSeekComplete(MediaPlayer arg0) {
				mActivity.doStart();
			}
        };
    }

	/** 
	 * <p>Title: prev</p> 
	 * <p>Description: </p> 
	 * @return 
	 */
	@Override
	public boolean prev() {
		return doPrevious();
	}

	/** 
	 * <p>Title: next</p> 
	 * <p>Description: </p> 
	 * @return 
	 */
	@Override
	public boolean next() {
		return doNext();
	}
	
	//初始化音乐 
	private boolean initMusic(){
		resetNames();
		try{
			stopPlayback();
			showLoadPreparreUI();
			mPlayer = new PreviewPlayer();
            mPlayer.setActivity(MusicPlayer.this);
			mPlayer.setDataSourceAndPrepare(getCurrentURI());
			super.onCallURLChanged();
			return true;
		}catch(Exception ex){
			onError(mPlayer, -1, -1);
			return false;
		}
	}

	/** 
	 * <p>Title: isPlaying</p> 
	 * <p>Description: </p> 
	 * @return 
	 */
	@Override
	public boolean isPlaying() {
		if(mPlayer==null||!mPlayer.mIsPrepared) return false;
		return mPlayer.isPlaying();
	}

	/** 
	 * <p>Title: start</p> 
	 * <p>Description: </p>  
	 */
	@Override
	public void start() {
		if(mPlayer!=null){
			if(mPlayer.mIsPrepared){
				mPlayer.start();
				super.doStart();
			}
		}else{
			initMusic();
		}
	}

	/** 
	 * <p>Title: pause</p> 
	 * <p>Description: </p>  
	 */
	@Override
	public void pause() {
		if(mPlayer==null||!mPlayer.mIsPrepared) return;
		mPlayer.pause();
		super.doPause();
		System.gc();
	}

	/** 
	 * <p>Title: stop</p> 
	 * <p>Description: </p>  
	 */
	@Override
	public void stop() {
//		if(isPlaying()){
//	    	mPlayer.seekTo(0);
//	    	mPlayer.pause();
//		}
		stopPlayback();
    	super.doStop();
	}

	/** 
	 * <p>Title: seekTo</p> 
	 * <p>Description: </p> 
	 * @param pos 
	 */
	@Override
	public void seekTo(int msec) {
		if(mPlayer==null||!mPlayer.mIsPrepared) return;
		if(!onSeekBefore()){
			Toast.makeText(this, R.string.plug_video_unsupport_seek, Toast.LENGTH_SHORT).show();
			return;
		}
		
		boolean isPlay = isPlaying();
		if(!isPlay) mPlayer.start();
		mPlayer.seekTo(msec);
		//if(!isPlay) mPlayer.pause();
		mMusicController.show();
		super.doSeek();
	}
	
	/**
	 * 在Seek前的处理
	 * @return
	 */
	private boolean onSeekBefore(){
		MediaItem item = getCurrentMediaItem();
		if(item==null) return false;
		ProtocolInfo proInfo = item.getParsedProtocolInfo();
		if(!proInfo.hasDlnaOrgPn()){
			return true;
		}

		if(proInfo.getFopBytes()){
			return true;
		}
		
		if(isRendererPlay()){//DMR
//			if(!proInfo.hasDlnaOrgFlags()){
//				return true;
//			}
//			if(!proInfo.getLopNpt()&&!proInfo.getLopBytes()){
//				return true;
//			}
		}
		return false;
	}

	/** 
	 * <p>Title: plusVolume</p> 
	 * <p>Description: </p>  
	 */
	@Override
	public void plusVolume() {
		SysUtils.volumeAdjust(this, SysUtils.Def.MODE_SYSTEM, SysUtils.Def.VOLUMEPLUS);
	}

	/** 
	 * <p>Title: minusVolume</p> 
	 * <p>Description: </p>  
	 */
	@Override
	public void minusVolume() {
		SysUtils.volumeAdjust(this, SysUtils.Def.MODE_SYSTEM, SysUtils.Def.VOLUMEMINUS);
	}

	/** 
	 * <p>Title: getBufferPercentage</p> 
	 * <p>Description: </p> 
	 * @return 
	 */
	@Override
	public int getBufferPercentage() {
		if(mPlayer==null||!mPlayer.mIsPrepared) return 0;
		return mPlayer.getBufferPercentage();
	}

	/** 
	 * <p>Title: getCurrentPosition</p> 
	 * <p>Description: </p> 
	 * @return 
	 */
	@Override
	public int getCurrentPosition() {
		if(mPlayer==null||!mPlayer.mIsPrepared) return 0;
		int postion = mPlayer.getCurrentPosition();
		int duration = getDuration();
		if(duration>0&&postion>duration){
			postion = duration;
			pause();
			mMusicController.updatePausePlay();
			onCompletion(mPlayer);
		}
		return postion;
	}

	/** 
	 * <p>Title: getDuration</p> 
	 * <p>Description: </p> 
	 * @return 
	 */
	@Override
	public int getDuration() {
		if(mPlayer==null||!mPlayer.mIsPrepared) return 0;
		return mPlayer.getDuration();
	}

	@Override
	public void finish() {
		stopPlayback();
		super.finish();
	}
	
	@Override
	public void doFinish() {
		finish();
	}
	
	/**
	 * 选择播放模式
	 * <p>Title: selectPlayMode</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.musicplay.MusicController.PlayControl#selectPlayMode()
	 */
	public void selectPlayMode() {
		Alert.Builder bulider = new Alert.Builder(this);
		bulider.setTitle(R.string.plug_video_playmode_title);
		bulider.setSingleChoiceItems(R.array.play_mode, getPlayMode().getId()-1, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				PlayMode mode = null;
				switch(item){
				case 0:{
					mode = PlayMode.Single; break;
				}
				case 1:{
					mode = PlayMode.Order; break;
				}
				case 2:{
					mode = PlayMode.RepeatOne; break;
				}
				case 3:{
					mode = PlayMode.RepeatAll; break;
				}
				}
				if(mode!=null){
					savePlayMode(mode);
				}
				dialog.dismiss();
			}
		});
		bulider.create().show();
	}
	
	//启动倒计时退出
	private void startCountdownExit(){
		mMainHandler.postDelayed(mStopRunnable, COUNT_DOWN_TIME*1000+500);//3s内未有新的任务, 则停掉播放器
		mCountdownTextView.start(1000);
	}
	//停止倒计时退出
	private void stopCountdownExit(){
		mMainHandler.removeCallbacks(mStopRunnable);
		mCountdownTextView.stop();
	}
	
	//停止音乐播放器
	private Runnable mStopRunnable = new Runnable(){
		public void run() {
			MusicPlayer.this.finish();
		}
	};

	/** 
	 * <p>Title: hasProgressUpdater</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#hasProgressUpdater() 
	 */
	@Override
	protected boolean hasProgressUpdater() {
		return true;
	}

	/** 
	 * <p>Title: onCommandHandleBefore</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandHandleBefore() 
	 */
	@Override
	protected boolean onCommandHandleBefore(String command) {
		if(mPlayer==null){
			logger.debug("Music player is null, can't exec command. ");
			return false;
		}
		stopCountdownExit();
		return true;
	}

	/** 
	 * <p>Title: onCommandStop</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandStop() 
	 */
	@Override
	protected void onCommandStop() {
		mMusicController.stop();
		startCountdownExit();
	}

	/** 
	 * <p>Title: onCommandPlay</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandPlay() 
	 */
	@Override
	protected void onCommandPlay() {
		mMusicController.start();
	}

	/** 
	 * <p>Title: onCommandPause</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandPause() 
	 */
	@Override
	protected void onCommandPause() {
		mMusicController.pause();
	}

	/** 
	 * <p>Title: onCommandSeek</p> 
	 * <p>Description: </p> 
	 * @param position 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandSeek(int) 
	 */
	@Override
	protected void onCommandSeek(int position) {
		seekTo(position);
	}

	/** 
	 * <p>Title: onCommandNewMedia</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandNewMedia() 
	 */
	@Override
	protected void onCommandNewMedia() {
		initMusic();
	}

	/** 
	 * <p>Title: onCommandPrevious</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandPrevious() 
	 */
	@Override
	protected void onCommandPrevious() {
		mMusicController.doQuickPrevious();
	}

	/** 
	 * <p>Title: onCommandNext</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandNext() 
	 */
	@Override
	protected void onCommandNext() {
		mMusicController.doQuickNext();
	}

	/** 
	 * <p>Title: onCommandHandleAfter</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandHandleAfter() 
	 */
	@Override
	protected void onCommandHandleAfter(String command) {
		//no action
	}

	/** 
	 * <p>Title: isMediaPlaying</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#isMediaPlaying() 
	 */
	@Override
	protected boolean isMediaPlaying() {
		return isPlaying();
	}

	/** 
	 * <p>Title: getMediaDuration</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#getMediaDuration() 
	 */
	@Override
	protected int getMediaDuration() {
		return getDuration();
	}

	/** 
	 * <p>Title: getMediaCurrentPosition</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#getMediaCurrentPosition() 
	 */
	@Override
	protected int getMediaCurrentPosition() {
		return getCurrentPosition();
	}

}
