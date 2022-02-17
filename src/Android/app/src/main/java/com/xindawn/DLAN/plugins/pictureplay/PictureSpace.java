package com.xindawn.DLAN.plugins.pictureplay;


import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Scroller;

import com.rockchip.mediacenter.common.logging.Log;
import com.rockchip.mediacenter.common.logging.LogFactory;
import com.rockchip.mediacenter.plugins.imageloader.ContentURLStreamHandlerFactory;
import com.rockchip.mediacenter.plugins.imageloader.ImageLoader;
import com.rockchip.mediacenter.plugins.imageloader.ImageLoader.BindResult;
import com.rockchip.mediacenter.plugins.imageloader.ImageLoader.Callback;

public class PictureSpace extends ViewGroup {
	
	private static Log logger = LogFactory.getLog(PictureSpace.class);
    private static final int SLIP_DISTANCE = 100;
    private static final int SNAP_VELOCITY = 1000;
    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;
    
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mScrollX = 0;
    private int mCurrentScreen;
    private float mLastMotionX;
    private float mLastDownX;
    private float mLastDownY;
    private int mTouchState = TOUCH_STATE_REST;
    private int mTouchSlop = 0;
    private ImageLoader imageLoader = null;
    private PictureViewer mPictureViewer = null;
	private ImageView mContentImage = null;
	private float mMoveXValue = 0.0f;
	private float mMoveYValue = 0.0f;
	private float mZoomValue = 1.0f;
	private float mOldZoomValue = 1.0f;
	private int mScreenWidth = -1;
	private int mScreenHeight = -1;
    
    private PictureController mPictureController;

    public PictureSpace(Context context) {
        this(context, null);
    }

    public PictureSpace(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		mScreenWidth = dm.widthPixels;
		mScreenHeight = dm.heightPixels;
    }
    
    public void prepare(PictureViewer pictureViewer, ImageView imgView) {
    	mPictureViewer = pictureViewer;
    	mContentImage = imgView;
    	if(imageLoader==null){
    		imageLoader = new ImageLoader(
    				new ContentURLStreamHandlerFactory(getContext().getContentResolver())
    		);
    		imageLoader.setContext(mPictureViewer);
    	}
    }
    
    public void setPictureController(PictureController pictureController){
    	mPictureController = pictureController;
    }
    
    /**
     * 绑定图片
     * @param url
     * @param callback
     * @return
     */
    public BindResult bindImage(String url, Callback callback){
    	return imageLoader.bind(mContentImage, url, callback);
    }
    /**
     * 预加载图片
     * @param url
     * @param callback
     */
    public void preloadImage(String url, Callback callback){
    	imageLoader.preload(url, callback);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE)
                && (mTouchState != TOUCH_STATE_REST)) {
            return true;
                }

        final float x = ev.getX();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                boolean xMoved = xDiff > mTouchSlop;
                if (xMoved) {
                    mTouchState = TOUCH_STATE_SCROLLING;
                }
                break;

            case MotionEvent.ACTION_DOWN:
                mLastMotionX = x;
                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mTouchState = TOUCH_STATE_REST;
                break;
        }
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastMotionX = x;
                mLastDownX = x;
                mLastDownY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                final int deltaX = (int) (mLastMotionX - x);
                mLastMotionX = x;
                if (deltaX < 0) {
                    if (mScrollX > 0) {
                        scrollBy(Math.max(-mScrollX, deltaX), 0);
                    }
                } else if (deltaX > 0) {
                    final int availableToScroll = getChildAt(getChildCount() - 1)
                        .getRight()
                        - mScrollX - getWidth();
                    if (availableToScroll > 0) {
                        scrollBy(Math.min(availableToScroll, deltaX), 0);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                
                logger.debug("velocityX:="+velocityX);

                if (velocityX > SNAP_VELOCITY) {
                	if(mCurrentScreen > 0){
	                    // Fling hard enough to move left
	                    snapToScreen(mCurrentScreen-1);
                	}
                } else if (velocityX < -SNAP_VELOCITY) {
                    if(mCurrentScreen < getChildCount() - 1) {
	                    // Fling hard enough to move right
	                    snapToScreen(mCurrentScreen+1);
                    }
                    
                } else {
                    snapToDestination();
                }
                float dis = x-mLastDownX;
                float moveXVal = (dis)/mScreenWidth;
                float moveYVal = (y-mLastDownY)/mScreenHeight;
                if(isZoomState()){
            		float newMoveXValue = getValidMoveValue(mMoveXValue+moveXVal);
            		float newMoveYValue = getValidMoveValue(mMoveYValue+moveYVal);
            		AnimationSet anim = createMoveAnimation(mMoveXValue, newMoveXValue, mMoveYValue, newMoveYValue);
            		mContentImage.startAnimation(anim);
            		mMoveXValue = newMoveXValue;
            		mMoveYValue = newMoveYValue;
            	}else{
	                if(dis>SLIP_DISTANCE){
	            		showPrevious();
	            		mPictureController.hide();
	                }else if(dis<-SLIP_DISTANCE){
                		showNext();
                		mPictureController.hide();
	                }else{
	                	mPictureController.toggleShow();
	                }
            	}

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mTouchState = TOUCH_STATE_REST;
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
        }
        mScrollX = this.getScrollX();
        return true;
    }

    private void snapToDestination() {
        final int screenWidth = getWidth();
        final int whichScreen = (mScrollX + (screenWidth / 2)) / screenWidth;
        snapToScreen(whichScreen);
    }

    public void snapToScreen(int whichScreen) {     
    	mCurrentScreen = whichScreen;   
    	final int newX = whichScreen * getWidth();   
    	final int delta = newX - mScrollX;   
    	mScroller.startScroll(mScrollX, 0, delta, 0, Math.abs(delta) * 2);
//        ImageView leftImage = (ImageView)getChildAt(0);
//        ImageView centerImage = (ImageView)getChildAt(1);
//        ImageView rightImage = (ImageView)getChildAt(2);
//        Drawable temp = leftImage.getDrawable();
//        leftImage.setImageDrawable(centerImage.getDrawable());
//        centerImage.setImageDrawable(rightImage.getDrawable());
//        rightImage.setImageDrawable(temp);
        //imageLoader.download(imageUrlList.get(mCurrentImage), (ImageView)this.getChildAt(0));
    }

    public void setToScreen(int whichScreen) {
    	mCurrentScreen = whichScreen;
        final int newX = whichScreen * getWidth();
        mScroller.startScroll(newX, 0, 0, 0, 10);            
        invalidate();
        
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child
                        .getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("error mode.");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("error mode.");
        }

        // The children are given the same width and height as the workspace
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        scrollTo(mCurrentScreen * width, 0);      
    }
    
    @Override  
    public void computeScroll() {   
    	if (mScroller.computeScrollOffset()) {   
    		mScrollX = mScroller.getCurrX();   
    		scrollTo(mScrollX, 0);   
    		postInvalidate();
        }   
    }   
	
	/**
	 * 显示上一张图片
	 */
	public void showPrevious(){
		mPictureViewer.showPrevious();
	}
	
	/**
	 * 显示下一张图片
	 */
	public void showNext(){
		mPictureViewer.showNext();
	}
	
	/**
	 * 图片加载完成动画
	 */
	public void startAnimationOnLoaded(){
		mZoomValue = 0.8f;
		if(mZoomValue!=1.0f){
			mOldZoomValue = mZoomValue;
			mZoomValue = 1.0f;
			AnimationSet set = createAnimation(mOldZoomValue, mZoomValue, true);
			mContentImage.startAnimation(set);
			mOldZoomValue = 1.0f;
		}
	}
	
	/**
	 * 放大图片
	 */
	public void zoomIn(){
		mZoomValue += 0.3f;
		if(mZoomValue>=4.0f)
			mZoomValue = 4.0f;
		AnimationSet set = createAnimation(mOldZoomValue, mZoomValue, false);
		mContentImage.startAnimation(set);
		mOldZoomValue = mZoomValue;
	}
	
	/**
	 * 缩小图片
	 */
	public void zoomOut(){
		mZoomValue -= 0.3f;
		if(mZoomValue <= 0.3f)
			mZoomValue = 0.3f;
		AnimationSet set = createAnimation(mOldZoomValue, mZoomValue, false);
		mContentImage.startAnimation(set);
		mOldZoomValue = mZoomValue;
	}
	
	/**
	 * 创建放大缩小动画
	 */
	private AnimationSet createAnimation(float fromZoomValue, float toZoomValue, boolean alpha){
		ScaleAnimation sa = new ScaleAnimation(fromZoomValue, toZoomValue, fromZoomValue, toZoomValue, 
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		sa.setDuration(500);
		sa.setFillAfter(true);
		
		AnimationSet set = new AnimationSet(false);
		set.addAnimation(sa);
		if(alpha){
			AlphaAnimation aa = new AlphaAnimation(0.3f, 1.0f);
			aa.setDuration(500);
			aa.setFillAfter(true);
			set.addAnimation(aa);
		}
		
		set.setFillAfter(true);
		return set;
	}
	
	/**
	 * 是否处于放大缩小状态
	 */
	private boolean isZoomState(){
		return mZoomValue!=1.0f;
	}
	
	/**
	 * 创建移动动画
	 */
	private AnimationSet createMoveAnimation(float fromX, float toX, float fromY, float toY){
		int type = Animation.RELATIVE_TO_SELF;
		TranslateAnimation ta = new TranslateAnimation(type, fromX, type, toX,
											type, fromY, type, toY);
		ta.setDuration(800);
		ta.setFillAfter(true);
		
		AnimationSet set = createAnimation(mZoomValue, mZoomValue, false);
		set.addAnimation(ta);
		set.setFillAfter(true);
		return set;
	}
	
	/**
	 * 获取合法的移动值
	 */
	private float getValidMoveValue(float value){
		float zoom = (mZoomValue/2<0.8f)?0.8f:mZoomValue/2;
		if(value>0.8f*zoom){
			value = 0.8f*zoom;
		}
		if(value<-0.8f*zoom){
			value = -0.8f*zoom;
		}
		return value;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT||keyCode==KeyEvent.KEYCODE_DPAD_UP){
			showPrevious();
		}else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT||keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
			showNext();
		}
		return super.onKeyUp(keyCode, event);
	}
	
	/**
	 * 当用户按下返回键
	 * @return
	 */
	public boolean onBackPressed(){
		if(isZoomState()){
			mZoomValue = 1.0f;
			AnimationSet set = createAnimation(mOldZoomValue, mZoomValue, false);
			mContentImage.startAnimation(set);
			mOldZoomValue = 1.0f;
			return true;
		}
		return false;
	}
	
	/**
     * 清理缓存
     */
    public void clearCache(){
    	this.imageLoader.clear();
    }
    
    /**
     * 取消请求
     */
    public void cancelRequest(){
    	this.imageLoader.cancelRequest();
    }
}