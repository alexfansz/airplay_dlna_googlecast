/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    SwitchPreference.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-4-18 下午05:46:04  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-4-18      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins.widget;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.xindawn.R;

/**
 *
 * @author fxw
 * @since 1.0
 */
public class SwitchPreference extends CheckBoxPreference {

	private ImageView mImageView;
	
	public SwitchPreference(Context context) {
		super(context);
	}

	public SwitchPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SwitchPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/** 
	 * <p>Title: onBindView</p> 
	 * <p>Description: </p> 
	 * @param view 
	 * @see android.preference.CheckBoxPreference#onBindView(android.view.View) 
	 */
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mImageView = (ImageView)view.findViewById(R.id.switch_image);
		syncImageState();
	}
	
	/** 
	 * <p>Title: setChecked</p> 
	 * <p>Description: </p> 
	 * @param checked 
	 * @see android.preference.CheckBoxPreference#setChecked(boolean) 
	 */
	@Override
	public void setChecked(boolean checked) {
		super.setChecked(checked);
		syncImageState();
	}
	
	private void syncImageState(){
		if(mImageView!=null){
			mImageView.setImageResource(isChecked()?R.drawable.settings_on:R.drawable.settings_of);
		}
	}

}
