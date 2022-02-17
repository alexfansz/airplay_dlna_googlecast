package com.xindawn.DLAN.plugins.videoplay;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.xindawn.R;
import com.rockchip.mediacenter.common.util.ReflectionUtils;
import com.rockchip.mediacenter.dlna.dmr.SysUtils;
import com.rockchip.mediacenter.dlna.dmr.SysUtils.BrightnessUtil;
import com.xindawn.DLAN.plugins.DLNASeekHandler;
import com.xindawn.DLAN.plugins.videoplay.MediaController.MediaPlayerControl;
import com.xindawn.DLAN.plugins.widget.Alert;

import java.io.IOException;
import java.util.Map;

public class VideoView extends SurfaceView implements MediaPlayerControl {
    private String TAG = "VideoView";
    // settable by the client
    private Uri         mUri;
    @SuppressWarnings("unused")
	private Map<String, String> mHeaders;
    private int         mDuration;
    

	public static final int SCREEN_MODE_ORIG = 0;
	public static final int SCREEN_MODE_169 = 1;
	public static final int SCREEN_MODE_43 = 2;
	public static final int SCREEN_MODE_FULL = 3;

    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int STATE_SUSPEND            = 6;
    private static final int STATE_RESUME             = 7;
    private static final int STATE_SUSPEND_UNSUPPORTED = 8;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private int         mVideoWidth;
    private int         mVideoHeight;
    private int         mSurfaceWidth;
    private int         mSurfaceHeight;
    private MediaController mMediaController;
    private OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private int         mCurrentBufferPercentage;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private int         mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean     mCanPause;
    private boolean     mCanSeekBack;
    private boolean     mCanSeekForward;
    private int         mStateWhenSuspended;  //state before calling suspend()
    private Context     mContext;
    public static int sMaxWidth = -1;
    public static int sMaxHeight = -1;
    private VideoPlayer    mVideoPlayer;
    private DLNASeekHandler mSeekHandler;
    private boolean isSeeking;

    public VideoView(Context context) {
        super(context);
        mContext = context;
        initVideoView();
    }

    public VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initVideoView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.i("@@@@", "onMeasure");
//        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
//        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
//        if (mVideoWidth > 0 && mVideoHeight > 0) {
//            if ( mVideoWidth * height  > width * mVideoHeight ) {
//                //Log.i("@@@", "image too tall, correcting");
//                height = width * mVideoHeight / mVideoWidth;
//            } else if ( mVideoWidth * height  < width * mVideoHeight ) {
//                //Log.i("@@@", "image too wide, correcting");
//                width = height * mVideoWidth / mVideoHeight;
//            } else {
//                //Log.i("@@@", "aspect ratio is correct: " +
//                        //width+"/"+height+"="+
//                        //mVideoWidth+"/"+mVideoHeight);
//            }
//        }
        //Log.i("@@@@@@@@@@", "setting size: " + width + 'x' + height);
        setMeasuredDimension(mVideoWidth, mVideoHeight);
    }

    public int resolveAdjustedSize(int desiredSize, int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                /* Parent says we can be as big as we want. Just don't be larger
                 * than max size imposed on ourselves.
                 */
                result = desiredSize;
                break;

            case MeasureSpec.AT_MOST:
                /* Parent says we can be as big as we want, up to specSize.
                 * Don't be larger than specSize, and don't be larger than
                 * the max size imposed on ourselves.
                 */
                result = Math.min(desiredSize, specSize);
                break;

            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
}

    private void initVideoView() {
//        mVideoWidth = 0;
//        mVideoHeight = 0;
    	DisplayMetrics dm = com.rockchip.mediacenter.plugins.videoplay.WindowUtils.getWindowMetrics((Activity) mContext);
        mVideoHeight = dm.heightPixels;
	  	mVideoWidth = dm.widthPixels;
	  	sMaxWidth = dm.widthPixels;
	    sMaxHeight = dm.heightPixels;
        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        setFocusable(true);
//        setFocusableInTouchMode(true);
//        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
        //使用overlay来播放
//        ReflectionUtils.invokeMethod(this, "setTitle", "android.rk.RockVideoPlayer.VideoDisplayView");
    }
    
    public void setActivity(VideoPlayer activity){
    	mVideoPlayer = activity;
    }

    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * @hide
     */
    public void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
            if(mVideoPlayer!=null)
            	mVideoPlayer.doStop();
        }
    }

    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // Tell the music playback service to pause
        // TODO: these constants need to be published somewhere in the framework.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mDuration = -1;
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            attachMediaController();
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    public void setMediaController(MediaController controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            View anchorView = this.getParent() instanceof View ?
                    (View)this.getParent() : this;
            anchorView.setOnTouchListener(mTouchListener);
            //mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
        new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            	mMediaController.setCurrentScreen();
            }
    };

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mCurrentState = STATE_PREPARED;

            // Get the capabilities of the player for this stream
//            Metadata data = mp.getMetadata(MediaPlayer.METADATA_ALL,
//                                      MediaPlayer.BYPASS_METADATA_FILTER);
//
//            if (data != null) {
//                mCanPause = !data.has(Metadata.PAUSE_AVAILABLE)
//                        || data.getBoolean(Metadata.PAUSE_AVAILABLE);
//                mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
//                        || data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
//                mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
//                        || data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
//            } else {
//                mCanPause = mCanSeekBack = mCanSeekForward = true;
//            }
            mCanPause = mCanSeekBack = mCanSeekForward = true;

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            
            //初始化屏幕大小
            mMediaController.setScreen(SysUtils.getScreenValue(mContext));
            //初始化屏幕亮度
        	mMediaController.setScreenBrightness(BrightnessUtil.getUserBrightness(mContext));

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                //Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                    // We didn't actually change the size (it was already at the size
                    // we need), so we won't get a "surface changed" callback, so
                    // start the video here instead of in the callback.
                    if (mTargetState == STATE_PLAYING) {
                        start();
                        if (mMediaController != null) {
                            mMediaController.show();
                        }
                    } else if (!isPlaying() &&
                               (seekToPosition != 0 || getCurrentPosition() > 0)) {
                       if (mMediaController != null) {
                           // Show the media controls when we're paused into a video and make 'em stick.
                           mMediaController.show(0);
                       }
                   }
                }else{
                	if (mMediaController != null&&mMediaController.isShowing()) {//update duration 20141021
                        mMediaController.show();
                    }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
        new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mMediaController != null) {
                mMediaController.hide();
            }
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
        }
    };
    
    private MediaPlayer.OnInfoListener mInfoListener = 
    	new MediaPlayer.OnInfoListener() {
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				if(mOnInfoListener != null){
					return mOnInfoListener.onInfo(mp, what, extra);
				}
				return false;
			}
	};
	
	private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = 
		new MediaPlayer.OnSeekCompleteListener() {
			public void onSeekComplete(MediaPlayer mp) {
				isSeeking = false;
				if(mOnSeekCompleteListener!=null){
					mOnSeekCompleteListener.onSeekComplete(mp);
				}
			}
		};

    private MediaPlayer.OnErrorListener mErrorListener =
        new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            Log.d(TAG, "Error: " + framework_err + "," + impl_err);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            if (mMediaController != null) {
                mMediaController.hide();
            }

            /* If an error handler has been supplied, use it and finish. */
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                    return true;
                }
            }
            
            /* Otherwise, pop up an error dialog so the user knows that
             * something bad has happened. Only try and pop up the dialog
             * if we're attached to a window. When we're going away and no
             * longer have a window, don't bother showing the user an error.
             */
            if (getWindowToken() != null) {
                int messageId;

                if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                    messageId = R.string.plug_video_err_unsupport;
                } else {
                    messageId = R.string.plug_video_err_unknown;
                }

                new Alert.Builder(mContext)
                        .setTitle(R.string.plug_video_err_title)
                        .setMessage(messageId)
                        .setPositiveButton(R.string.dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        /* If we get here, there is no onError listener, so
                                         * at least inform them that the video is over.
                                         */
                                        if (mOnCompletionListener != null) {
                                            mOnCompletionListener.onCompletion(mMediaPlayer);
                                        }
                                    }
                                })
                        .setOnCancelListener(
                        		new DialogInterface.OnCancelListener() {
									public void onCancel(DialogInterface arg0) {
										finish();
									}
								})
                        //.setCancelable(false)
                        .show();
            }
            return true;
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
        new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
            if(mOnBufferingUpdateListener!=null){
            	mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
            }
        }
    };

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l)
    {
        mOnPreparedListener = l;
    }
    
    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l)
    {
        mOnSeekCompleteListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener l)
    {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(OnErrorListener l)
    {
        mOnErrorListener = l;
    }
    
    public void setOnInfoListener(OnInfoListener l)
    {
        mOnInfoListener = l;
    }
    
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener l)
    {
        mOnBufferingUpdateListener = l;
    }
    
    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
    {
        public void surfaceChanged(SurfaceHolder holder, int format,
                                    int w, int h)
        {
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState =  (mTargetState == STATE_PLAYING);
            boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
//                if (mMediaController != null) {
//                    if (mMediaController.isShowing()) {
//                        // ensure the controller will get repositioned later
//                        mMediaController.hide();
//                    }
//                    mMediaController.show();
//                }
            }
        }

        public void surfaceCreated(SurfaceHolder holder)
        {
            mSurfaceHolder = holder;
            if(!mVideoPlayer.isActivityVisible()){
            	//invisible, don't handle
            	return;
            }
            //resume() was called before surfaceCreated()
            if (mMediaPlayer != null && mCurrentState == STATE_SUSPEND
                   && mTargetState == STATE_RESUME) {
                mMediaPlayer.setDisplay(mSurfaceHolder);
                resume();
            } else {
                openVideo();
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder)
        {
        	if(!mVideoPlayer.isFinishing()){
        		//Sometimes pause activity make surface destroy, record position
        		mSeekWhenPrepared = getCurrentPosition();
        	}
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            if (mMediaController != null) mMediaController.hide();
            if (mCurrentState != STATE_SUSPEND) {
                release(true);
            }
//          if(mVideoPlayer != null ){
//				mVideoPlayer.finish();
//			}
        }
    };

    /*
     * release the media player in any state
     */
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState  = STATE_IDLE;
            }
            if(mVideoPlayer != null ){
				mVideoPlayer.doStop();
			}
        }
    }

    private OnTouchListener mTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				if (isInPlaybackState() && mMediaController != null) {
					if(!mMediaController.isInMediaControlLayout(event.getX(), event.getY())){
						toggleMediaControlsVisiblity();
					}
		        }
				return true;
			}
			return false;
		}
	};
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        if (isInPlaybackState() && mMediaController != null) {
//            toggleMediaControlsVisiblity();
//        }
//        return false;
//    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        boolean isKeyCodeSupported = 
//        	keyCode != KeyEvent.KEYCODE_BACK &&
//                                     keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
//                                     keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                                     keyCode != KeyEvent.KEYCODE_MENU &&
                                     keyCode != KeyEvent.KEYCODE_CALL &&
                                     keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    && mMediaPlayer.isPlaying()) {
                pause();
                mMediaController.show();
            } else if(keyCode == KeyEvent.KEYCODE_BACK){
           	 	finish();
            } else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
           	 	SysUtils.volumeAdjust(mContext, getVolumeMode(), SysUtils.Def.VOLUMEMINUS);
           	 	return true;
            } else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
           	 	SysUtils.volumeAdjust(mContext, getVolumeMode(), SysUtils.Def.VOLUMEPLUS);
           	 	return true;
            } else if(keyCode == VideoPlayer.KEYCODE_VOLUME_MUTE){
            	return super.onKeyDown(keyCode, event);
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            if(mVideoPlayer!=null) mVideoPlayer.doStart();
        }
        mTargetState = STATE_PLAYING;
    }

    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                System.gc();
                if(mVideoPlayer!=null) mVideoPlayer.doPause();
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        if (isInPlaybackState()) {
            //if (mMediaPlayer.suspend()) {
        	if ((Boolean)ReflectionUtils.invokeMethod(mMediaPlayer, "suspend", new Object[0])){
                mStateWhenSuspended = mCurrentState;
                mCurrentState = STATE_SUSPEND;
                mTargetState = STATE_SUSPEND;
            } else {
                release(false);
                mCurrentState = STATE_SUSPEND_UNSUPPORTED;
                Log.w(TAG, "Unable to suspend video. Release MediaPlayer.");
            }
        }
    }

    public void resume() {
        if (mSurfaceHolder == null && mCurrentState == STATE_SUSPEND){
            mTargetState = STATE_RESUME;
            return;
        }
        if (mMediaPlayer != null && mCurrentState == STATE_SUSPEND) {
            //if (mMediaPlayer.resume()) {
        	if ((Boolean)ReflectionUtils.invokeMethod(mMediaPlayer, "resume", new Object[0])) {
                mCurrentState = mStateWhenSuspended;
                mTargetState = mStateWhenSuspended;
            } else {
                Log.w(TAG, "Unable to resume video");
            }
            return;
        }
        if (mCurrentState == STATE_SUSPEND_UNSUPPORTED) {
            openVideo();
        }
    }

   // cache duration as mDuration for faster access
    public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0) {
                return mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int msec) {
        if (isInPlaybackState()) {
        	if (!mMediaPlayer.isPlaying()) {
        		mMediaPlayer.start();
        		mCurrentState = STATE_PLAYING;
        		mTargetState = STATE_PLAYING;
        	}
        	boolean seekable = true;
        	if(mSeekHandler!=null){
        		seekable = mSeekHandler.onSeekBefore(mUri, msec);
        	}
        	if(seekable){
	            isSeeking = true;
	            mMediaPlayer.seekTo(msec);
	            if(mVideoPlayer!=null) mVideoPlayer.doSeek();
        	}
        	if(mSeekHandler!=null){
        		mSeekHandler.onSeekAfter(mUri, msec);
        	}
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }
    
    public boolean isSeeking(){
    	return isSeeking;
    }

    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    public boolean canPause() {
        return mCanPause;
    }

    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        return mCanSeekForward;
    }

	@Override
	public boolean prev() {
		return mVideoPlayer.doPrevious();
	}

	@Override
	public boolean next() {
		return mVideoPlayer.doNext();
	}

	@Override
	public void finish() {
		if(mMediaPlayer != null){
            stopPlayback();
            if(mVideoPlayer!=null){
            	mVideoPlayer.finish();
            }
        }
	}

	@Override
	public int getVolumeMode() {
		return mVideoPlayer.getVolumeMode();
	}

	@Override
	public int getDefaultWidth() {
		if(mMediaPlayer == null){
    		return -1;
    	}
    	return mMediaPlayer.getVideoWidth();
	}

	@Override
	public int getDefaultHeight() {
		if(mMediaPlayer == null){
    		return -1;
    	}
    	return mMediaPlayer.getVideoHeight();
	}

	@Override
	public void setScreenSize(int width, int height) {
    	getHolder().setFixedSize(0x28062806, 0x28062806); //enable size changed 
    	getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	if(width > sMaxWidth){
    		width = sMaxWidth;
    	}
    	if(height > sMaxHeight){
    		height = sMaxHeight;
    	}
    	getHolder().setFixedSize(width, height);
    	mVideoWidth= width;
    	mVideoHeight = height;
	}

	public MediaController getMediaController() {
		return mMediaController;
	}

	/** 
	 * <p>Title: selectPlayMode</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.videoplay.MediaController.MediaPlayerControl#selectPlayMode() 
	 */
	@Override
	public void selectPlayMode() {
		mVideoPlayer.selectPlayMode();
	}
	
	public void setSeekHandler(DLNASeekHandler seekHandler){
		mSeekHandler = seekHandler;
	}
}
