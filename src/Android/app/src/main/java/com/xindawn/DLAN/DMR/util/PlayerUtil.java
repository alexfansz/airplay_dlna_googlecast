/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    PlayerUtil.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-6-16 下午04:56:15  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-6-16      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.DMR.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.rockchip.mediacenter.common.util.StringUtils;
import com.rockchip.mediacenter.core.constants.MediaPlayConsts;
import com.rockchip.mediacenter.core.dlna.enumeration.MediaClassType;
import com.xindawn.DLAN.plugins.musicplay.MusicPlayer;
import com.xindawn.DLAN.plugins.pictureplay.PictureViewer;
import com.xindawn.DLAN.plugins.videoplay.VideoPlayer;

import java.io.File;
import java.lang.reflect.Method;

/**
 *
 * @author fxw
 * @since 1.0
 */
public class PlayerUtil {

	public static boolean startPlayer(Context context, String title, String url, String mimeType){
		if(StringUtils.isEmptyObj(url)){
			return false;
		}
		Uri uri = null;
		if(isHttpURL(url)){
			uri = Uri.parse(url);
		}else{
			uri = Uri.fromFile(new File(url));
		}
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		MediaClassType itemClass = MediaClassType.getMediaClassTypeByMime(mimeType);
		if(MediaClassType.IMAGE==itemClass){
			//intent.setClass(context, com.xindawn.DLAN.plugins.pictureplay.PictureViewer.class);
			intent.setClass(context, PictureViewer.class);
		}else if(MediaClassType.AUDIO==itemClass){
			//intent.setClass(context, com.xindawn.DLAN.plugins.musicplay.MusicPlayer.class);
			intent.setClass(context, MusicPlayer.class);
		}else if(MediaClassType.VIDEO==itemClass){
			//intent.setClass(context, com.xindawn.DLAN.plugins.videoplay.VideoPlayer.class);
			intent.setClass(context, VideoPlayer.class);
		}
		intent.putExtra(MediaPlayConsts.KEY_MEDIA_TITLE, title);
		intent.putExtra("isRenderPlay",true);
		intent.setData(uri);

		try {
			//Just for Realtek DMP Platform
			Class<?> cSystemProperties = Class.forName("android.os.SystemProperties");
			Method set = cSystemProperties.getDeclaredMethod("set", String.class, String.class);
			set.invoke(null, "persist.media.USE_RTMediaPlayer", "1");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try{
			context.startActivity(intent);
			return true;
		}catch(ActivityNotFoundException anfe){
			return false;
		}
	}
	
	public static boolean isHttpURL(String url){
		String httpURL = "http";
		if(StringUtils.isEmptyObj(url)){
			return false;
		}
		if(url.length()>httpURL.length()
				&&httpURL.equalsIgnoreCase(url.substring(0, httpURL.length()))){
			return true;
		}
		return false;
	}
	
}
