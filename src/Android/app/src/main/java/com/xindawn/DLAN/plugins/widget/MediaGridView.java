/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    MediaPlayListView.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2011-9-2 下午02:20:25  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2011-9-2      xwf         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

import com.rockchip.mediacenter.dlna.dmp.model.MediaItem;
import com.xindawn.DLAN.plugins.model.FileInfo;
import com.xindawn.DLAN.plugins.widget.BaseMediaAdapter.ViewHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MediaGridView extends GridView {

	private List<FileInfo> mFileInfoList = null;
	private BaseMediaAdapter mAdapter;
	
	public MediaGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFastScrollEnabled(true);
	}
	
	public void setMediaListAdapter(BaseMediaAdapter mediaListAdapter){
		mAdapter = mediaListAdapter;
		setAdapter(mAdapter);
		mFileInfoList = mediaListAdapter.getDataList();
	}
	
	/**
	 * 设置数据集
	 */
	public void setDataSource(List<FileInfo> fileInfoList){
		mFileInfoList.clear();
		addFileInfos(fileInfoList);
	}
	
	/**
	 * 清除数据源
	 */
	public void clearDataSource(){
		mFileInfoList.clear();
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 获取数据集
	 */
	public List<FileInfo> getDataSource(){
		return mFileInfoList;
	}
	
	public void refreshDataSource(){
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 根据View获取FileInfo
	 */
	public FileInfo getFileInfoByView(View v){
		int position = getPositionForView(v);
		if(position>=0&&position<mFileInfoList.size()){
			return mFileInfoList.get(position);
		}else{
			return null;
		}
	}
	
	/**
	 * 添加文件
	 * @param fileInfoList 文件列表
	 */
	public synchronized void addFileInfos(List<FileInfo> fileInfoList){
		for(FileInfo fileInfo : fileInfoList){
			if(!isExists(fileInfo)){
				mFileInfoList.add(fileInfo);
			}
		}
		mAdapter.notifyDataSetChanged();
	}
	public synchronized void addFileInfosWithNoCheck(List<FileInfo> fileInfoList){
		mFileInfoList.addAll(fileInfoList);
		mAdapter.notifyDataSetChanged();
	}
	public synchronized void addFileInfo(FileInfo fileInfo){
		if(!isExists(fileInfo)){
			mFileInfoList.add(fileInfo);
			mAdapter.notifyDataSetChanged();
		}
	}
	
	
	/**
	 * 移除文件
	 * @param fileInfoList
	 */
	public synchronized void removeFileInfos(List<FileInfo> fileInfoList){
		Iterator<FileInfo> it = mFileInfoList.iterator();
		while(it.hasNext()){
			for(FileInfo temp : fileInfoList){
				if(it.next().equals(temp)){
					it.remove();
					break;
				}
			}
		}
		mAdapter.notifyDataSetChanged();
	}
	public synchronized void removeFileInfo(FileInfo fileInfo){
		for(FileInfo item : mFileInfoList){
			if(item.equals(fileInfo)){
				mFileInfoList.remove(item);
				break;
			}
		}
		mAdapter.notifyDataSetChanged();
	}
	
	
	/**
	 * 修改文件
	 * @param fileInfoList
	 */
	public synchronized void updateFileInfos(List<FileInfo> fileInfoList){
		removeFileInfos(fileInfoList);
		addFileInfos(fileInfoList);
	}
	public synchronized void updateFileInfo(FileInfo fileInfo){
		removeFileInfo(fileInfo);
		addFileInfo(fileInfo);
	}
	
	/**
	 * 所添加的目录是否已存在
	 * @return
	 */
	public boolean isExists(List<FileInfo> fileInfoList){
		Iterator<FileInfo> it = mFileInfoList.iterator();
		while(it.hasNext()){
			for(FileInfo temp : fileInfoList){
				if(it.next().equals(temp)){
					return true;
				}
			}
		}
		return false;
	}
	public boolean isExists(FileInfo fileInfo){
		Iterator<FileInfo> it = mFileInfoList.iterator();
		while(it.hasNext()){
			if(it.next().equals(fileInfo)){
				return true;
			}
		}
		return false;
	}
	
	public List<FileInfo> getSelectedItems() {
		return mAdapter.getSelectedItems();
	}
	
	/**
	 * 设置正在播放的选项状态、清除上次播放状态
	 * @param fileInfo
	 */
	public void setSelectedPlaying(FileInfo fileInfo){
		setSelectedPlaying(mFileInfoList.indexOf(fileInfo));
	}
	public void setSelectedPlaying(int index){
		if(index<0||index>mFileInfoList.size()||!mFileInfoList.get(index).isMediaItem()){
			return;
		}
		clearSelectedPlaying();//先清除选中状态
		for(int i=0; i<mFileInfoList.size(); i++){
			FileInfo fileInfo = mFileInfoList.get(i);
			if(i==index){
				fileInfo.setSelected(true);
			}else{
				fileInfo.setSelected(false);
			}
		}
		for(int i=0; i< getChildCount(); i++){
			View view = getChildAt(i);
			int position = getPositionForView(view);
			if(position==index){
				MediaItem mediaItem = (MediaItem)mFileInfoList.get(index).getItem();
				ViewHolder holder = (ViewHolder)view.getTag();
				mAdapter.setMediaItemState(mediaItem, holder, true);
			}
		}
	}
	
	//清除选中播放状态
	public void clearSelectedPlaying(){
		List<Integer> selectedList = new ArrayList<Integer>();
		for(int i=0; i<mFileInfoList.size(); i++){
			FileInfo fileInfo = mFileInfoList.get(i);
			if(fileInfo.isSelected()&&fileInfo.isMediaItem()){
				selectedList.add(i);
			}
			fileInfo.setSelected(false);
		}
		if(selectedList.isEmpty()) return;//无选中项
		for(int index :  selectedList){
			for(int i=0; i< getChildCount(); i++){
				View view = getChildAt(i);
				int position = getPositionForView(view);
				if(position==index){
					FileInfo fileInfo = mFileInfoList.get(index);
					MediaItem mediaItem = (MediaItem)fileInfo.getItem();
					ViewHolder holder = (ViewHolder)view.getTag();
					fileInfo.setSelected(false);
					mAdapter.setMediaItemState(mediaItem, holder, false);
				}
			}
		}
	}
	
}
