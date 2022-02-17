package com.xindawn.DLAN.plugins.videoplay;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.rockchip.mediacenter.core.constants.MediaPlayConsts;
import com.rockchip.mediacenter.core.dlna.model.ProtocolInfo;
import com.rockchip.mediacenter.dlna.dmp.model.MediaItem;
import com.rockchip.mediacenter.dlna.dmr.SysUtils.PowerManagerUtil;
import com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity;
import com.rockchip.mediacenter.plugins.renderplay.PlayMode;
import com.xindawn.DLAN.plugins.DLNAMediaSupport;
import com.xindawn.DLAN.plugins.DLNASeekHandler;
import com.xindawn.DLAN.plugins.widget.Alert;
import com.xindawn.R;

public class VideoPlayer extends MediaRenderPlayerActivity implements
		MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
			MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener{
	
	public static final int KEYCODE_VOLUME_MUTE = 164;
	private static final int DIALOG_PLAY_MODE = 1;
	private static final int COUNT_DOWN_TIME = 3;
	
	private CountdownTextView mCountdownTextView;
	private VideoView mVideoView;

	private MediaController mMediaController;
	private PowerManagerUtil mPowerManagerUtil;
	private View mProgressZone;
	private TextView mProgressText;
	
	private boolean isPlayingWhenPopup = false;
	private int mStoppedCnt = 0;
	private boolean hasBufferStarted;
	private boolean isActivityVisible = false;
	
	private Dialog mErrorDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isActivityVisible = true;
		setFullScreen();
		setContentView(R.layout.plugin_video_view);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mCountdownTextView = (CountdownTextView) findViewById(R.id.exit_text);
		mCountdownTextView.setCount(COUNT_DOWN_TIME);
		mVideoView = (VideoView) findViewById(R.id.videoview);
		mProgressZone = findViewById(R.id.progress_indicator);
		mProgressText = (TextView)findViewById(R.id.progress_text);
		View controlView = findViewById(R.id.layout_controller);
		mMediaController = new MediaController(this);
		mMediaController.setControlView(controlView);
		mVideoView.setMediaController(mMediaController);
		mVideoView.setActivity(this);
		mPowerManagerUtil = new PowerManagerUtil(VideoPlayer.this);
		mPowerManagerUtil.acquireWakeLock();
		
		//获取URL相关信息
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
		mVideoView.stopPlayback();
		super.onCallHandleURL(intent);
	}
	
	/**
	 * 获取URL完成时回调
	 */
	public void onHandleURLCompletion(Uri uri) {
		initVideo();
	}
	
	protected void onStart() {
		super.onStart();
		isActivityVisible = true;
	}
	
	public void onResume() {
		super.onResume();
		isActivityVisible = true;
	}
	
	private void initVideo() {
		if(mErrorDialog!=null&&mErrorDialog.isShowing()){
			mErrorDialog.dismiss();
		}
		mProgressZone.setVisibility(View.VISIBLE);
		mVideoView.stopPlayback();
		mVideoView.setSeekHandler(mSeekHandler);
		mVideoView.setOnErrorListener(VideoPlayer.this);
		mVideoView.setOnCompletionListener(VideoPlayer.this);
		mVideoView.setOnSeekCompleteListener(VideoPlayer.this);
		mVideoView.setOnInfoListener(VideoPlayer.this);
		mVideoView.setOnBufferingUpdateListener(VideoPlayer.this);
		mVideoView.requestFocus();
		mVideoView.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				mVideoView.setBackgroundColor(Color.argb(0, 0, 255, 0));
				if(!hasBufferStarted){
					mProgressZone.setVisibility(View.INVISIBLE);
				}
				mVideoView.start();
				VideoPlayer.this.onCallMediaPrepared();
			}
		});
		super.onCallURLChanged();
		if(DLNAMediaSupport.DLNA_CERT_ENABLED&&getCurrentMediaItem().hasIfoFileUri()){
			DLNAMediaSupport.getIFOFileURI(getCurrentURI());
		}
		hasBufferStarted = false;
		mVideoView.setVideoURI(handleDLNAVideoURI(getCurrentURI()));

		VideoPlayer.this.onCallMediaPrepared();
	}
	
	//处理DLNA视频URI，在URI末尾追加dlna=1
	private Uri handleDLNAVideoURI(Uri uri){
		if(uri==null||isAndroid4Plus()) return uri;
		//if(uri==null) return uri;
		String uriStr = uri.toString();
		if("http".equalsIgnoreCase(uri.getScheme())&&uriStr.indexOf("dlna=1")==-1){
			if(uriStr.indexOf("?")>0){
				uriStr += "&dlna=1";
			}else{
				uriStr += "?dlna=1";
			}
			return Uri.parse(uriStr);
		}
		return uri;
	}
	@SuppressWarnings("unused")
	private Uri handleDLNAVideoURI2(Uri uri){
		//if(uri==null||isAndroid4Plus()) return uri;
		if(uri==null) return uri;
		int sdkInt = Build.VERSION.SDK_INT;//Android 4.0/14  4.0.3/15  4.1.2/16  4.2/17
		if(sdkInt!=16){//4.0以上版本只有4.1的SF有对dlna=2做处理
			return handleDLNAVideoURI(uri);
		}
		String uriStr = uri.toString();
		if("http".equalsIgnoreCase(uri.getScheme())&&uriStr.indexOf("dlna=")==-1){
			uriStr += (uriStr.indexOf("?")>0)?"&":"?";
			MediaItem item = getCurrentMediaItem();
			ProtocolInfo proInfo = item.getParsedProtocolInfo();
			if(!proInfo.hasDlnaOrgPn()||proInfo.getFopBytes()){
				uriStr += "dlna=1";
			}else{
				uriStr += "dlna=2";
			}
			return Uri.parse(uriStr);
		}
		return uri;
	}
	
	public Uri handleDLNAVideoURI3(Uri uri){
		if(uri==null) return uri;
		String uriStr = uri.toString();
		if("http".equalsIgnoreCase(uri.getScheme())&&uriStr.indexOf("dlna=")==-1){
			MediaItem item = getCurrentMediaItem();
			ProtocolInfo proInfo = item.getParsedProtocolInfo();
			String mimeType = proInfo.getContentFormat();
			if(mimeType==null||(!mimeType.contains("video/quicktime")
								&&!mimeType.contains("video/mov")
								&&!mimeType.contains("video/mp4")
								&&!mimeType.contains("video/3gp")
								&&!mimeType.contains("video/3gpp"))){
				return uri;
			}
			uriStr += (uriStr.indexOf("?")>0)?"&":"?";
			uriStr += "dlna=mov";
			return Uri.parse(uriStr);
		}
		return uri;
	}
	
	public void onPause() {
		isActivityVisible = false;
		//mVideoView.pause();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		mVideoView.stopPlayback();
		mPowerManagerUtil.releaseWakeLock();
		super.onDestroy();
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		super.doStop();
		/*if(mVideoView.getWindowToken()==null){
			showError();
			logger.debug("the windowToken of VideoView is null. ");
			return true;
		}*/
		showPlayError(mp, what, extra);
		return true;
	}
	
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		if(what==MediaPlayer.MEDIA_INFO_BUFFERING_START){
			hasBufferStarted = true;
			mProgressZone.setVisibility(View.VISIBLE);
			mProgressText.setText("0%");
			return true;
		}else if(what==MediaPlayer.MEDIA_INFO_BUFFERING_END){
			hasBufferStarted = false;
			mProgressText.setText("100%");
			mProgressZone.setVisibility(View.INVISIBLE);
			return true;
		}
		return false;
	}
	
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		if(mProgressZone.getVisibility()==View.VISIBLE){
			mProgressText.setText(percent+"%");
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		super.onCallMediaCompletion();
	}
	
	/** 
	 * <p>Title: onSeekComplete</p> 
	 */
	@Override
	public void onSeekComplete(MediaPlayer mediaplayer) {
		super.doStart();
	}
	
	/** 
	 * <p>Title: onCreateDialog</p> 
	 * <p>Description: </p> 
	 * @param id
	 * @return 
	 * @see android.app.Activity#onCreateDialog(int) 
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id==DIALOG_PLAY_MODE){
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
					if(isPlayingWhenPopup) mVideoView.start();
				}
			});
			bulider.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialoginterface) {
					dialoginterface.dismiss();
					if(isPlayingWhenPopup) mVideoView.start();
				}
			});
			return bulider.create();
		}
		return super.onCreateDialog(id);
	}
	
	/**
	 * 选择播放模式
	 */
	public void selectPlayMode(){
		//showDialog(DIALOG_PLAY_MODE);
		onCreateDialog(DIALOG_PLAY_MODE).show();
		isPlayingWhenPopup = mVideoView.isPlaying();
		if(isPlayingWhenPopup) mVideoView.pause();
	}
	
	/**
	 * 显示错误信息
	 * <p>Title: showError</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#showError()
	 */
	protected void showError(){
		mErrorDialog = new Alert.Builder(this).setTitle(R.string.plug_video_err_title)
			.setMessage(R.string.plug_video_err_unknown)
			.setPositiveButton(R.string.dialog_ok,	new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					dialog.dismiss();
					VideoPlayer.this.finish();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					VideoPlayer.this.finish();
				}
			})
			.create();
		mErrorDialog.show();
	}
	private void showPlayError(final MediaPlayer mp, int framework_err, int impl_err){
		int messageId;
        if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
            messageId = R.string.plug_video_err_unsupport;
        } else {
            messageId = R.string.plug_video_err_unknown;
        }
        
        //to void window leak
        if (mErrorDialog != null && mErrorDialog.isShowing())
			return;
        
        mErrorDialog = new Alert.Builder(this).setTitle(R.string.plug_video_err_title)
                .setMessage(messageId)
                .setPositiveButton(R.string.dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            	onCompletion(mp);
                            }
                        })
                .setOnCancelListener(
                		new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface arg0) {
								VideoPlayer.this.finish();
							}
						})
                //.setCancelable(false)
                .create();
        mErrorDialog.show();
	}
	
	/** 
	 * <p>Title: onKeyDown</p> 
	 * <p>Description: </p> 
	 * @param keyCode
	 * @param event
	 * @return 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent) 
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		MediaController controller = mVideoView.getMediaController();
		switch(keyCode){
        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
        	mVideoView.getMediaController().doQuickNext();
        	return true;
        case KeyEvent.KEYCODE_MEDIA_REWIND:
        	mVideoView.getMediaController().doQuickPrevious();
        	return true;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        	mVideoView.getMediaController().doPrevious();
			return true;
        case KeyEvent.KEYCODE_MEDIA_NEXT:
        	mVideoView.getMediaController().doNext();
			return true;
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        	mVideoView.getMediaController().doPauseResume();
			return true;
        case KeyEvent.KEYCODE_MEDIA_STOP:
    		mVideoView.stopPlayback();
			return true;
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_HOME:
        case KEYCODE_VOLUME_MUTE:
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        	break;
    	default:
    		boolean consume = false;
    		if(!controller.isShowing()&&(keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN
    									||keyCode==KeyEvent.KEYCODE_DPAD_LEFT||keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)){
    			consume = true;//For save last focus in controller view
    		}
			controller.show();
    		if(consume)
    			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean isActivityVisible(){
		return isActivityVisible;
	}
	
	//--------------------------------------处理远端控制命令------------------------------------------
	protected void onHandleURLCompleted() {
		initVideo();
	}
	
	protected boolean hasProgressUpdater() {
		return true;
	}
	
	protected boolean onCommandHandleBefore(String command) {
		stopCountdownExit();
		if(MediaPlayConsts.CMD_STOP.equals(command)){
			mStoppedCnt++;
		}else{
			mStoppedCnt=0;
		}
		return true;
	}
	
	protected void onCommandStop() {
		mVideoView.stopPlayback();
		mProgressZone.setVisibility(View.GONE);
		if(mStoppedCnt>=2){//连续收到两次STOP 直接退出
			mStopRunnable.run();
		}else{
			startCountdownExit();
		}

		logger.debug("VideoPlayer execute stop command finished. ");
	}
	
	protected void onCommandPlay() {
		mVideoView.start();
		mVideoView.getMediaController().show();
	}
	
	protected void onCommandPause() {
		mVideoView.pause();
		mVideoView.getMediaController().show();
	}
	
	protected void onCommandSeek(int position) {
//		boolean isPlay = mVideoView.isPlaying();
		mVideoView.seekTo(position);
//		if(!isPlay) mVideoView.pause();
		mVideoView.getMediaController().show();
	}
	
	protected void onCommandNewMedia() {
		initVideo();
		mVideoView.getMediaController().show();
	}
	
	protected void onCommandPrevious() {
		mVideoView.getMediaController().doQuickPrevious();
	}
	
	protected void onCommandNext() {
		mVideoView.getMediaController().doQuickNext();
	}
	
	protected void onCommandHandleAfter(String command) {
		//no action
	}

	protected int getMediaDuration() {
		return mVideoView.getDuration();
	}

	protected int getMediaCurrentPosition() {
		//alex,hack for duration.. ,fix me
		//??
		//if(getMediaDuration()>0) VideoPlayer.this.onCallMediaPrepared();

		return mVideoView.getCurrentPosition();
	}
	
	protected boolean isMediaPlaying() {
		return mVideoView.isPlaying();
	}
	
	//启动倒计时退出
	private void startCountdownExit(){
		mMainHandler.removeCallbacks(mStopRunnable);
		mMainHandler.postDelayed(mStopRunnable, COUNT_DOWN_TIME*1000+500);//3s内未有新的任务, 则停掉播放器
		mCountdownTextView.start(1000);
	}
	//停止倒计时退出
	private void stopCountdownExit(){
		mMainHandler.removeCallbacks(mStopRunnable);
		mCountdownTextView.stop();
	}
	
	//停止视频播放器
	private Runnable mStopRunnable = new Runnable(){
		public void run() {
			VideoPlayer.this.finish();
		}
	};
	
	
	private DLNASeekHandler mSeekHandler = new DLNASeekHandler(){
		public boolean onSeekBefore(Uri uri, int msec) {
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
//				if(!proInfo.hasDlnaOrgFlags()){
//					return true;
//				}
//				if(!proInfo.getLopNpt()&&!proInfo.getLopBytes()){
//					return true;
//				}
			}

			Toast.makeText(VideoPlayer.this, R.string.plug_video_unsupport_seek, Toast.LENGTH_SHORT).show();
			mProgressZone.setVisibility(View.INVISIBLE);
			return false;
			
			
//			long[] range = DLNAMediaSupport.getAvailableSeekRange(getCurrentURI());
//			double startPos = (range[0]*1.00/range[2])*mVideoView.getDuration();
//			double endPos = (range[1]*1.00/range[2])*mVideoView.getDuration();
//			if(msec<startPos||msec>endPos){
//				System.out.println("can not seek......................startPos: "+startPos+", endPos: "+endPos+", msec:"+msec );
//				return false;
//			}else{
//				System.out.println("seek......................startPos: "+startPos+", endPos: "+endPos+", msec:"+msec );
//				return true;
//			}
		}

		public void onSeekAfter(Uri uri, int msec) {
		}
	};

}