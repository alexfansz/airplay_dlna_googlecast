/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    IndicatorGridView.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-4-19 上午11:05:38  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-4-19      fxw         1.0         create
*******************************************************************/   


package com.rockchip.mediacenter.plugins.widget;

import static android.view.View.MeasureSpec.EXACTLY;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * 带指示器的GridView
 * @author fxw
 * @since 1.0
 */
public class IndicatorGridView extends GridView implements AdapterView.OnItemSelectedListener {

	private OnItemSelectedListener mOnItemSelectedListener;
	private Drawable cursorDrawable;
	private Drawable iconBackground;
	private Rect cursorRect;
	private int cursorCenter;
	private int cursorWidth;
	private int cursorHeight;
	private int destCenterPosition;//目标项的中间位置
	private int moveStep = 25;//移动步长
	private boolean cursorVisible;
	private boolean isStopped;
	
	public IndicatorGridView(Context context) {
		this(context, null);
	}
	public IndicatorGridView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public IndicatorGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		super.setOnItemSelectedListener(this);
	}
	
	/** 
	 * <p>Title: onMeasure</p> 
	 * <p>Description: </p> 
	 * @param widthMeasureSpec
	 * @param heightMeasureSpec 
	 * @see android.widget.GridView#onMeasure(int, int) 
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		//增加光标高度
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        height += cursorHeight;
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	/** 
	 * <p>Title: setOnItemSelectedListener</p> 
	 * <p>Description: </p> 
	 * @param listener 
	 * @see android.widget.AdapterView#setOnItemSelectedListener(android.widget.AdapterView.OnItemSelectedListener) 
	 */
	@Override
	public void setOnItemSelectedListener(
			android.widget.AdapterView.OnItemSelectedListener listener) {
		mOnItemSelectedListener = listener;
	}
	
	/**
	 * 设置光标图片
	 * @param drawable
	 */
	public void setCursorResource(int resID){
		setCursorDrawable(getContext().getResources().getDrawable(resID));
	}
	public void setCursorDrawable(Drawable drawable){
		this.cursorDrawable = drawable;
		this.cursorWidth = cursorDrawable.getIntrinsicWidth();
		this.cursorHeight = cursorDrawable.getIntrinsicHeight();
	}
	
	/**
	 * 设置ICON背景
	 * @param drawable
	 */
	public void setIconBackground(int resID){
		setIconBackground(getContext().getResources().getDrawable(resID));
	}
	public void setIconBackground(Drawable drawable){
		this.iconBackground = drawable;
	}
	
	/**
	 * 设置光标移动步长
	 */
	public void setMoveStep(int step){
		this.moveStep = step;
	}
	
	public boolean onTouchEvent(MotionEvent ev) {
		cursorVisible = false;
		postInvalidate();
		return super.onTouchEvent(ev);
	}
	
	/** 
	 * <p>Title: onDraw</p> 
	 * <p>Description: </p> 
	 * @param canvas 
	 * @see android.view.View#onDraw(android.graphics.Canvas) 
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(iconBackground!=null){
			Rect rect = new Rect(0, 0, getMeasuredWidth(), iconBackground.getIntrinsicHeight());
			iconBackground.setBounds(rect);
			iconBackground.draw(canvas);
		}
		if(cursorVisible){
			cursorDrawable.draw(canvas);
		}
	}
	
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		//初始化光标位置
		if(cursorRect==null&&getChildCount()>0){
			cursorRect = new Rect();
			getChildAt(0).getHitRect(cursorRect);
			cursorCenter = (cursorRect.left+cursorRect.right)/2;
			cursorRect.top = cursorRect.bottom;//光标位置在Child的下方
			cursorRect.bottom = cursorRect.top+cursorHeight;
			cursorRect.left = cursorCenter-cursorWidth/2;
			cursorRect.right = cursorCenter+cursorWidth/2;
		}
	}
	
	/** 
	 * <p>Title: onItemSelected</p> 
	 * <p>Description: </p> 
	 */
	public void onItemSelected(AdapterView<?> parent, View view, int postion, long id) {
		if(view==null){
			return;
		}
		Rect selectedRect = new Rect();
		view.getHitRect(selectedRect);
		destCenterPosition = (selectedRect.left + selectedRect.right)/2;
		Handler handler = getHandler();
		if(handler!=null){
			cursorVisible = true;
			isStopped = false;
			handler.post(mCursorRunnable);
		}
		if(mOnItemSelectedListener!=null){
			mOnItemSelectedListener.onItemSelected(parent, view, postion, id);
		}
	}
	
	/** 
	 * <p>Title: onNothingSelected</p> 
	 * <p>Description: </p> 
	 * @see android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android.widget.AdapterView) 
	 */
	public void onNothingSelected(AdapterView<?> parent) {
		cursorVisible = false;
		isStopped = true;
		postInvalidate();
		if(mOnItemSelectedListener!=null){
			mOnItemSelectedListener.onNothingSelected(parent);
		}
	}
	
	private Runnable mCursorRunnable = new Runnable(){
		public void run() {
			int position = getSelectedItemPosition();
			if(position<0||getSelectedView()==null||!isFocused()||cursorDrawable==null||cursorRect==null){
				Handler handler = getHandler();
				if(handler!=null){
					handler.removeCallbacks(this);
				}
				isStopped = true;
				postInvalidate();
				return;
			}
			if(cursorCenter>destCenterPosition){//向左移动
				int step = Math.min(moveStep, cursorCenter-destCenterPosition);
				cursorRect.left -= step;
				cursorRect.right -= step;
				cursorCenter -= step;
			}else if(cursorCenter<destCenterPosition){//向右移动
				int step = Math.min(moveStep, destCenterPosition-cursorCenter);
				cursorRect.left += step;
				cursorRect.right += step;
				cursorCenter += step;
			}else{//停止移动
				isStopped = true;
			}
			cursorDrawable.setBounds(cursorRect);
			postInvalidate();
			if(!isStopped||!cursorVisible){
				//getHandler().post(this);
				Handler handler = getHandler();
				if(handler!=null){
					handler.postDelayed(this, 8);
				}
			}
		}
	};

}
