package com.xindawn.DLAN.plugins.pictureplay;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xindawn.R;
import com.rockchip.mediacenter.common.util.StringUtils;
import com.rockchip.mediacenter.dlna.dmp.model.MediaItem;
import com.rockchip.mediacenter.plugins.imageloader.ImageLoader.BindResult;
import com.rockchip.mediacenter.plugins.imageloader.ImageLoader.Callback;
import com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity;
import com.xindawn.DLAN.plugins.videoplay.CountdownTextView;

public class PictureViewer extends MediaRenderPlayerActivity implements Callback {

	//控制命令
	private static final int SLIDE_CYCLE = 3000;
	private static final int COUNT_DOWN_TIME = 3;
	
	private CountdownTextView mCountdownTextView;
	private View mProgressBar = null;
	private com.xindawn.DLAN.plugins.pictureplay.PictureSpace mPictureSpace = null;
	private ImageView mContentImage;
	private TextView mTitleText;
	private int mSlideErrorCnt = 0;
	private boolean mSlideShowing = false;
	private PictureController mPictureController;
    private boolean isLastViewPreImage = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		setFullScreen();
		setContentView(R.layout.plugin_picture_play);
		mCountdownTextView = (CountdownTextView) findViewById(R.id.exit_text);
		mCountdownTextView.setCount(COUNT_DOWN_TIME);
		mTitleText = (TextView)findViewById(R.id.pic_title_text);
		mProgressBar = findViewById(R.id.progressBar);
		mPictureSpace = (com.xindawn.DLAN.plugins.pictureplay.PictureSpace)findViewById(R.id.pictureSpace);
		mContentImage = (ImageView)findViewById(R.id.contentImage);
		mPictureController = (PictureController)findViewById(R.id.pictureController);
		mPictureController.setPictureViewer(this);
		mPictureController.show(SLIDE_CYCLE);
		mPictureSpace.setPictureController(mPictureController);
		mPictureSpace.prepare(this, mContentImage);
		super.onCallHandleURL(getIntent());
		mSlideShowing = false;
	}
	
	/** 
	 * <p>Title: onHandleURLCompletion</p> 
	 * <p>Description: </p> 
	 * @param uri 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onHandleURLCompletion(android.net.Uri) 
	 */
	@Override
	protected void onHandleURLCompletion(Uri uri) {
		loadImage();
	}
	
	/** 
	 * FLAG_ACTIVITY_SINGLE_TOP
	 * <p>Title: onNewIntent</p> 
	 * <p>Description: </p> 
	 * @param intent 
	 * @see android.app.Activity#onNewIntent(android.content.Intent) 
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		stopCountdownExit();
		super.onCallHandleURL(intent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		stopSlideShow();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPictureSpace.clearCache();
	}
	
	/**
	 * 显示加载进度条
	 */
	public void showProgressBar(){
		mProgressBar.setVisibility(View.VISIBLE);
	}
	
	/**
	 * 关闭加载进度条
	 */
	public void closeProgressBar(){
		mProgressBar.setVisibility(View.INVISIBLE);
	}
	
	
	/**
     * 启动加载图片
     */
    public void loadImage() {
    	MediaItem mediaItem = getCurrentMediaItem();
    	if(mediaItem!=null){
			super.onCallURLChanged();
    		showProgressBar();
    		updateTitle(mediaItem.getTitle());
    		mPictureSpace.cancelRequest();
			BindResult bindResult = mPictureSpace.bindImage(mediaItem.getResourceURL(), this);
			if(bindResult != BindResult.LOADING){
				closeProgressBar();
				if(bindResult == BindResult.ERROR){
					Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
				}
			}
    	}
    }

	/** 
	 * <p>Title: onImageLoaded</p> 
	 * <p>Description: 图片加载完成时</p> 
	 */
	@Override
	public void onImageLoaded(ImageView imageview, String url, Bitmap bitmap,
			boolean flag) {
		super.onCallMediaPrepared();
		mPictureSpace.startAnimationOnLoaded();
		closeProgressBar();
		
		//Preload Image handle
		//When lasttime view previous images, then preload previous Image
		//Otherwise preload next image
		MediaItem mediaitem = null;
		if(isLastViewPreImage){
			mediaitem = getLastMediaItem();
		}else{
			mediaitem = getNextMediaItem();
		}
		if(mediaitem!=null){
			String preloadURL = mediaitem.getResourceURL();
			if(!url.equals(preloadURL)){
				mPictureSpace.preloadImage(preloadURL, this);
				logger.debug("PreLoad next/previous Image. ");
			}
		}
		logger.debug("Load Image completed. ");
		
		if(mSlideShowing){
			doSlideShow();
		}
		mSlideErrorCnt = 0;
	}
	
	/** 
	 * <p>Title: onImageError</p> 
	 * <p>Description: 图片加载出错时回调</p> 
	 */
	@Override
	public void onImageError(ImageView imageview, String s, Throwable throwable) {
		Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
		super.doStop();
		closeProgressBar();
		mSlideErrorCnt++;
		if(mSlideShowing){
			if(mSlideErrorCnt<=3){
				doSlideShow();
			}else{//连续加载错误超过三次,停止加载
				stopSlideShow();
			}
		}
	}
	
	/**
	 * 修改标题
	 * @param title
	 */
	public void updateTitle(String title){
		if(StringUtils.isEmptyObj(title)){
			mTitleText.setText("");
		}else{
			mTitleText.setText(title);
		}
	}
	
	
	/**
	 * 上一张图片
	 */
	public void showPrevious(){
		if(!doPrevious()){
			Toast.makeText(this, getString(R.string.plug_image_first_img), Toast.LENGTH_SHORT).show();
			return;
		}
		isLastViewPreImage = true;
		logger.debug("show Previous Image");
	}
	
	/**
	 * 下一张图片
	 */
	public void showNext(){
		if(!doNext()){
			Toast.makeText(this, getString(R.string.plug_image_last_img), Toast.LENGTH_SHORT).show();
			return;
		}
		isLastViewPreImage = false;
		logger.debug("show Next Image");
	}
	
	/**
	 * 放大
	 */
	public void zoomOut(){
		mPictureSpace.zoomOut();
	}
	
	/**
	 * 缩小
	 */
	public void zoomIn(){
		mPictureSpace.zoomIn();
	}
	
	/**
	 * 是否正在播放
	 * @return
	 */
	public boolean isSlideShowing(){
		return mSlideShowing;
	}
	
	/**
	 * 顺序播放 
	 */
	public void slideShow(){
		mSlideShowing = !mSlideShowing;
		mPictureController.updateSlideShowState(mSlideShowing);
		if(mSlideShowing){
			mPictureController.hide();
			doSlideShow();
		}
	}
	
	/**
	 * 停止播放
	 */
	public void stopSlideShow(){
		mSlideShowing = false;
		mPictureController.updateSlideShowState(mSlideShowing);
	}
	
	private void doSlideShow(){
		mMainHandler.postDelayed(new Runnable(){
			public void run() {
				if(!mSlideShowing) return;
				if(!doNext()){
					mSlideShowing = false;
					mPictureController.updateSlideShowState(mSlideShowing);
					Toast.makeText(PictureViewer.this, PictureViewer.this.getString(R.string.plug_image_last_img), Toast.LENGTH_SHORT).show();
				}
			}
		}, SLIDE_CYCLE);
	}
	
	
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_HEADSETHOOK:
		case KeyEvent.KEYCODE_MEDIA_STOP:
			stopSlideShow();
			mPictureController.show();
			return true;
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			if(isSlideShowing()){
				stopSlideShow();
			}else{
				slideShow();
			}
			mPictureController.show();
			return true;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        	showPrevious();
        	mPictureController.show();
        	return true;
        case KeyEvent.KEYCODE_MEDIA_NEXT:
        	showNext();
        	mPictureController.show();
        	return true;
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_HOME:
        case KeyEvent.KEYCODE_MENU:
        	break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
    		mPictureController.show();
    		break;
    	default:
    		mPictureController.toggleShow();
    		return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * 返回键
	 * <p>Title: onBackPressed</p> 
	 * <p>Description: </p>  
	 * @see android.app.Activity#onBackPressed()
	 */
	public void onBackPressed() {
		boolean isHandle = mPictureSpace.onBackPressed();
		if(!isHandle) finish();
	}
	
	//启动倒计时退出
	private void startCountdownExit(){
		mMainHandler.postDelayed(mStopRunnable, (COUNT_DOWN_TIME+1)*1000);//5s内未有新的任务, 则停掉播放器
		mCountdownTextView.start(1000);
	}
	//停止倒计时退出
	private void stopCountdownExit(){
		mMainHandler.removeCallbacks(mStopRunnable);
		mCountdownTextView.stop();
	}
	
	//停止图片查看器
	private Runnable mStopRunnable = new Runnable(){
		public void run() {
			PictureViewer.this.finish();
		}
	};
	
	/**
	 * <p>Title: hasProgressUpdater</p> 
	 * <p>Description: 是否有进度条更新</p> 
	 * @return 
	 */
	protected boolean hasProgressUpdater() {
		return false;
	}

	/** 
	 * <p>Title: onCommandHandleBefore</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandHandleBefore() 
	 */
	@Override
	protected boolean onCommandHandleBefore(String command) {
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
		mPictureSpace.cancelRequest();
		super.doStop();
		startCountdownExit();
	}

	/** 
	 * <p>Title: onCommandPlay</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandPlay() 
	 */
	@Override
	protected void onCommandPlay() {
		//No Action
	}

	/** 
	 * <p>Title: onCommandPause</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandPause() 
	 */
	@Override
	protected void onCommandPause() {
		super.doPause();
		//No Action
	}

	/** 
	 * <p>Title: onCommandSeek</p> 
	 * <p>Description: </p> 
	 * @param position 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandSeek(int) 
	 */
	@Override
	protected void onCommandSeek(int position) {
		//No Action
	}

	/** 
	 * <p>Title: onCommandNewMedia</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandNewMedia() 
	 */
	@Override
	protected void onCommandNewMedia() {
		loadImage();
	}

	/** 
	 * <p>Title: onCommandPrevious</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandPrevious() 
	 */
	@Override
	protected void onCommandPrevious() {
		//No Action
	}

	/** 
	 * <p>Title: onCommandNext</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandNext() 
	 */
	@Override
	protected void onCommandNext() {
		//No Action
	}

	/** 
	 * <p>Title: onCommandHandleAfter</p> 
	 * <p>Description: </p>  
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#onCommandHandleAfter() 
	 */
	@Override
	protected void onCommandHandleAfter(String command) {
		//No Action
	}

	/** 
	 * <p>Title: isMediaPlaying</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#isMediaPlaying() 
	 */
	@Override
	protected boolean isMediaPlaying() {
		return STATE_PLAYING.equals(getCurrentState());
	}

	/** 
	 * <p>Title: getMediaDuration</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#getMediaDuration() 
	 */
	@Override
	protected int getMediaDuration() {
		return 0;
	}

	/** 
	 * <p>Title: getMediaCurrentPosition</p> 
	 * <p>Description: </p> 
	 * @return 
	 * @see com.rockchip.mediacenter.plugins.renderplay.MediaRenderPlayerActivity#getMediaCurrentPosition() 
	 */
	@Override
	protected int getMediaCurrentPosition() {
		return 0;
	}}
