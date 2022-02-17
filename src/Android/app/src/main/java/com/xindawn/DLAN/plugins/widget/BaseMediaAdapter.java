/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    MediaListAdapter.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-4-6 下午12:04:29  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-4-6      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rockchip.mediacenter.dlna.dmp.model.MediaItem;
import com.xindawn.DLAN.plugins.model.FileInfo;
import com.xindawn.R;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fxw
 * @since 1.0
 */
public class BaseMediaAdapter extends ArrayAdapter<FileInfo> {

	private List<FileInfo> mDataList;
	protected Drawable mFolderIcon;
	protected Drawable mImageIcon;
	protected Drawable mVideoIcon;
	protected Drawable mAudioIcon;
	
	public BaseMediaAdapter(Context context, List<FileInfo> dataList) {
		super(context, 0, dataList);
		mDataList = dataList==null?new ArrayList<FileInfo>():dataList;
		mFolderIcon = getDrawable(R.drawable.dmp_folder_item);
		mImageIcon = getDrawable(R.drawable.dmp_image_item);
		mVideoIcon = getDrawable(R.drawable.dmp_video_item);
		mAudioIcon = getDrawable(R.drawable.dmp_audio_item);
	}
	
	public List<FileInfo> getDataList(){
		return mDataList;
	}
	
	/**
	 * 获得所有选择项
	 * @return
	 */
	public List<FileInfo> getSelectedItems(){
		List<FileInfo> fileInfoList = new ArrayList<FileInfo>();
		int cnt = getCount();
		for(int i=0; i<cnt; i++){
			FileInfo item = getItem(i);
			if(item.isSelected()){
				fileInfoList.add(item);
			}
		}
		return fileInfoList;
	}
	
	
	public void setMediaItemState(MediaItem mediaItem, ViewHolder holder, boolean isSelected){
		
	}
	
	public int getColor(int resId){
		return getContext().getResources().getColor(resId);
	}
	
	public Drawable getDrawable(int resID){
		return getContext().getResources().getDrawable(resID);
	}
	
	public final class ViewHolder {
		public ImageView img;
		public ImageView playingImg;
		public TextView title;
		public TextView desc;
	}

}
