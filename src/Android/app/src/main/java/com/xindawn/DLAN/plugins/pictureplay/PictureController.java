/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    PictureController.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-5-17 上午09:49:37  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-5-17      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins.pictureplay;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.xindawn.R;

/**
 *
 * @author fxw
 * @since 1.0
 */
public class PictureController extends FrameLayout implements View.OnClickListener {

	private com.xindawn.DLAN.plugins.pictureplay.PictureViewer mPictureViewer;
	private View mControllerView;
	private static final int DEFAULT_TIMEOUT = 0;
	protected static final int FADE_OUT = 0;
	private ImageView mPreviousImage;
	private ImageView mNextImage;
	private ImageView mBackImage;
	private TextView mSlideText;
	private boolean mShowing;
	
	public PictureController(Context context) {
		super(context);
	}
	
	public PictureController(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public PictureController(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/** 
	 * <p>Title: onFinishInflate</p> 
	 * <p>Description: </p>  
	 * @see android.view.View#onFinishInflate() 
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		initView();
	}
	
	private void initView(){
		mControllerView = this;//findViewById(R.id.pictureControllerZoon);
		mPreviousImage = (ImageView)findViewById(R.id.pre_image);
		mNextImage = (ImageView)findViewById(R.id.next_image);
		mBackImage = (ImageView)findViewById(R.id.back_image);
		mSlideText = (TextView)findViewById(R.id.slide_text);
		mPreviousImage.setOnClickListener(this);
		mNextImage.setOnClickListener(this);
		mBackImage.setOnClickListener(this);
		mSlideText.setOnClickListener(this);
		findViewById(R.id.zoomOut_image).setOnClickListener(this);
		findViewById(R.id.zoomIn_image).setOnClickListener(this);
	}
	
	public void setPictureViewer(com.xindawn.DLAN.plugins.pictureplay.PictureViewer pictureViewer){
		mPictureViewer = pictureViewer;
	}
	
	
	/**
	 * 修改播放状态
	 * @param slideShow
	 */
	public void updateSlideShowState(boolean slideShow){
		if(slideShow){
			mSlideText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.plug_pic_stop, 0, 0, 0);
			mSlideText.setText(R.string.plug_image_stop_slide);
		}else{
			mSlideText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.plug_pic_play, 0, 0, 0);
			mSlideText.setText(R.string.plug_image_slide);
		}
	}
	
	public void toggleShow(){
		if(mShowing){
			hide();
		}else{
			show();
		}
	}
	
	/**
	 * 显示控制区
	 */
	public void show(){
		show(DEFAULT_TIMEOUT);
	}
	public void show(int timeout){
		mShowing = (mControllerView.getVisibility() == View.VISIBLE);
		if(!mShowing){
			Animation animation = new AlphaAnimation(0.2f, 1.0f);
			animation.setDuration(500);
			mControllerView.startAnimation(animation);
			mControllerView.setVisibility(View.VISIBLE);
			mShowing = true;
		}
		Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout > 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }else{
        	mHandler.removeMessages(FADE_OUT);
        }
	}
	
	
	/**
	 * 隐藏控制区
	 */
	public void hide(){
		mShowing = (mControllerView.getVisibility() == View.VISIBLE);
		if (mShowing) {
			Animation animation = new AlphaAnimation(1.0f, 0f);
			animation.setDuration(500);
			mControllerView.startAnimation(animation);
			mControllerView.setVisibility(View.INVISIBLE);
			mShowing = false;
		}
	}
	
	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
            }
        }
    };
	

	/** 
	 * <p>Title: onClick</p> 
	 * <p>Description: </p> 
	 * @param arg0 
	 * @see android.view.View.OnClickListener#onClick(android.view.View) 
	 */
	public void onClick(View view) {
		int viewId = view.getId();
		if(viewId == R.id.pre_image){
			mPictureViewer.showPrevious();
		}else if(viewId == R.id.next_image){
			mPictureViewer.showNext();
		}else if(viewId == R.id.slide_text){
			mPictureViewer.slideShow();
		}else if(viewId == R.id.zoomOut_image){
			mPictureViewer.zoomOut();
		}else if(viewId == R.id.zoomIn_image){
			mPictureViewer.zoomIn();
		}else if(viewId == R.id.back_image){
			mPictureViewer.finish();
		}
	}
	
	/** 
	 * <p>Title: onTouchEvent</p> 
	 * <p>Description: </p> 
	 * @param event
	 * @return 
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent) 
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		show();
		return false;
	}
	
	public boolean onTrackballEvent(MotionEvent event) {
		show();
		return false;
	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            hide();
            return false;
        }else {
            show();
        }
		return super.dispatchKeyEvent(event);
	}
}
