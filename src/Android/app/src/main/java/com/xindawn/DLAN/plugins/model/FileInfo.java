/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    FileInfo.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-4-6 下午04:03:08  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-4-6      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins.model;

import android.graphics.drawable.Drawable;

import com.rockchip.mediacenter.dlna.dmp.model.ContainerItem;
import com.rockchip.mediacenter.dlna.dmp.model.ContentItem;
import com.rockchip.mediacenter.dlna.dmp.model.MediaItem;
import com.rockchip.mediacenter.dlna.model.DeviceItem;

import java.io.File;
import java.lang.ref.SoftReference;

/**
 *
 * @author fxw
 * @since 1.0
 */
public class FileInfo {

	private String path;
	private String title;
	private boolean isDir;
	private boolean isSelected;
	private Object item;
	private Drawable icon;
	private SoftReference<Drawable> softIcon;
	
	public FileInfo(ContentItem item){
		this.item = item;
		this.title = item.getTitle();
		isDir = !item.isMediaItem();
	}
	
	public FileInfo(DeviceItem item){
		this.item = item;
		this.title = item.getFriendlyName();
		isDir = true;
	}
	
	public FileInfo(File item){
		this.item = item;
		this.title = item.getName();
		this.path = item.getAbsolutePath();
		if(item.exists()){
			isDir = item.isDirectory();
		}
	}
	
	public FileInfo() {
	}

	public boolean isMediaItem(){
		if(item instanceof MediaItem){
			return true;
		}
		return false;
	}
	
	public boolean isContainerItem(){
		if(item instanceof ContainerItem){
			return true;
		}
		return false;
	}
	
	public boolean isDeviceItem(){
		if(item instanceof DeviceItem){
			return true;
		}
		return false;
	}
	
	public boolean isStorageDevice(){
		if(item instanceof StorageDevice){
			return true;
		}
		return false;
	}
	
	public boolean isFileItem(){
		if(item instanceof File){
			return true;
		}
		return false;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDir(boolean isDir) {
		this.isDir = isDir;
	}

	public boolean isDir() {
		return isDir;
	}
	
	public void setItem(Object item) {
		this.item = item;
	}

	public Object getItem() {
		return item;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public Drawable getIcon() {
		if(icon!=null){
			return icon;
		}
		if(softIcon!=null){
			return softIcon.get();
		}
		return null;
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	public void setSoftReferenceIcon(Drawable icon){
		this.softIcon = new SoftReference<Drawable>(icon);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileInfo other = (FileInfo) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
}
