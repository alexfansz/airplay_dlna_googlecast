/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    CountdownTextView.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-5-31 下午02:26:10  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-5-31      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins.videoplay;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;

import com.xindawn.R;

/**
 *
 * @author fxw
 * @since 1.0
 */
public class CountdownTextView extends TextView implements Runnable {
	
	private static final int INTERVAL = 1000;
	private int count;//Seconds
	private int cnt;

	public CountdownTextView(Context context) {
		this(context, null);
	}
	
	public CountdownTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public CountdownTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = 
            context.obtainStyledAttributes(attrs, R.styleable.CountdownTextView);
		count = a.getInt(R.styleable.CountdownTextView_count, 0);
        a.recycle();
	}
	
	public void start(){
		start(1);
	}
	
	public void start(int afterTime){
		this.cnt = count;
		Handler handler = getHandler();
		if(handler!=null)
			handler.postDelayed(this, afterTime);
	}
	
	public void stop(){
		Handler handler = getHandler();
		if(handler!=null)
			handler.removeCallbacks(this);
		reset();
		setText("");
	}
	

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public void reset(){
		this.cnt = 0;
	}

	public void run() {
		if(cnt==0){
			setText("");
		}else{
			setText(cnt+"");
			cnt--;
			Handler handler = getHandler();
			if(handler!=null)
				handler.postDelayed(this, INTERVAL);
		}
	}
}
