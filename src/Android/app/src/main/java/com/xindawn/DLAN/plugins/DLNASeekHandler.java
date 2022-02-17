/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    DLNASeekHandler.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-12-19 下午04:08:33  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-12-19      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins;

import android.net.Uri;

/**
 *
 * @author fxw
 * @since 1.0
 */
public interface DLNASeekHandler {

	public boolean onSeekBefore(Uri uri, int msec);
	
	public void onSeekAfter(Uri uri, int msec);
	
}
