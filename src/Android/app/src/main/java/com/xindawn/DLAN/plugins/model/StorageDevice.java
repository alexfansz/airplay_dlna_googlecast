/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    StorageDevice.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2011-9-5 上午10:31:04  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2011-9-5      xwf         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.IBinder;

import com.rockchip.mediacenter.common.util.ReflectionUtils;

import java.io.File;

/**
 * 存储设备
 * 1.SD CARD
 * 2.INTENEL FLASH
 * 3.USB
 */
public abstract class StorageDevice {
	
	//设备名称
	private String name;
	private Drawable icon; 
	private Object mMountManager;
	
	public StorageDevice(){
	}
	
	public StorageDevice(String name){
		this.name = name;
	}
	
	/**
	 * 设备是否在线
	 */
	public abstract boolean isLiving();
	
	/**
	 * 获取设备路径
	 * @return
	 */
	public abstract String getPath();
	
	/**
	 * 转换为FileInfo对象
	 * @return
	 */
	public com.xindawn.DLAN.plugins.model.FileInfo convert(){
		com.xindawn.DLAN.plugins.model.FileInfo fileInfo = new com.xindawn.DLAN.plugins.model.FileInfo(new File(getPath()));
		fileInfo.setIcon(icon);
		fileInfo.setDir(true);
		return fileInfo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	public Drawable getIcon() {
		return icon;
	}
	
	public void setIcon(Context context, int resId) {
		this.icon = context.getResources().getDrawable(resId);
	}
	
	public boolean isMounted(String path){
		if(mMountManager==null){
			Object mountService = ReflectionUtils.invokeStaticMethod("android.os.ServiceManager", "getService", "mount");
			mMountManager = ReflectionUtils.invokeStaticMethod("android.os.storage.IMountService$Stub", "asInterface", new Class[]{IBinder.class}, mountService);
		}
		String mountState = (String)ReflectionUtils.invokeMethod(mMountManager, "getVolumeState", path);
		return Environment.MEDIA_MOUNTED.equals(mountState);
	}
	
}
