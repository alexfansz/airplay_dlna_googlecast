/*******************************************************************
* Company:     Fuzhou Rockchip Electronics Co., Ltd
* Filename:    DLNAMediaSupport.java  
* Description:   
* @author:     fxw@rock-chips.com
* Create at:   2012-11-17 上午10:48:34  
* 
* Modification History:  
* Date         Author      Version     Description  
* ------------------------------------------------------------------  
* 2012-11-17      fxw         1.0         create
*******************************************************************/   


package com.xindawn.DLAN.plugins;

import android.net.Uri;

import com.rockchip.mediacenter.common.util.ByteUtils;
import com.rockchip.mediacenter.core.constants.DLNAConst;
import com.rockchip.mediacenter.core.dlna.model.ProtocolInfo;
import com.rockchip.mediacenter.core.http.HTTP;
import com.rockchip.mediacenter.core.http.HTTPRequest;
import com.rockchip.mediacenter.core.http.HTTPResponse;
import com.rockchip.mediacenter.core.upnp.UPnP;
import com.rockchip.mediacenter.dlna.dmp.model.MediaItem;

import java.util.StringTokenizer;

/**
 * 处理DLNA请求相关操作
 * @author fxw
 * @since 1.0
 */
public class DLNAMediaSupport {

	public static final boolean DLNA_CERT_ENABLED = true;
	
	/**
	 * 获取传给SF的参数值
	 * @param mediaItem
	 * @return
	 */
	public static String constructParams(MediaItem mediaItem){
		ProtocolInfo proInfo = mediaItem.getParsedProtocolInfo();
		byte[] paramBytes = new byte[16];
		
		if(!proInfo.hasDlnaOrgPn()){//Non dlna media profile
			paramBytes[13] = 1;//Default bytes_base seek
		}else{
			if(proInfo.hasDlnaOrgFlags()){
				paramBytes[15] = ByteUtils.convertBoolToByte(proInfo.getSpFlag());
				paramBytes[12] = ByteUtils.convertBoolToByte(proInfo.getLopNpt());
				paramBytes[11] = ByteUtils.convertBoolToByte(proInfo.getLopBytes());
			}
	
			if(proInfo.hasDlnaOrgOp()){
				paramBytes[14] = ByteUtils.convertBoolToByte(proInfo.getFopNpt());
				paramBytes[13] = ByteUtils.convertBoolToByte(proInfo.getFopBytes());
				if(paramBytes[14]==1||paramBytes[13]==1){//Full random access
					paramBytes[12] = 0;
					paramBytes[11] = 0;
				}
			}
			paramBytes[10] = ByteUtils.convertBoolToByte(mediaItem.hasIfoFileUri());
		}
		//System.out.println(ByteUtils.convertByteToBin(paramBytes));
		return ByteUtils.convertByteToHex(paramBytes);
	}
	
	/**
	 * 获取Ifo file uri
	 * @param uri
	 */
	public static String getIFOFileURI(Uri uri){
		if(!isHttpURL(uri)) return null;
		
		HTTPRequest request = buildHTTPRequest(uri);
		request.setHeader(DLNAConst.DLNA_PRAGMA, DLNAConst.DLNA_GETIFOFILEURI);
		request.setURI(uri.toString(), true);
		HTTPResponse response = request.post(uri.getHost(), uri.getPort());
		return response.getHeaderValue(DLNAConst.DLNA_IFOFILEURI);
	}
	
	/**
	 * 是否为HTTP URL
	 * @param uri
	 * @return
	 */
	public static boolean isHttpURL(Uri uri){
		return uri!=null&&"http".equalsIgnoreCase(uri.getScheme());
	}
	
	/**
	 * Build http request
	 * @param uri
	 * @return
	 */
	private static HTTPRequest buildHTTPRequest(Uri uri){
		HTTPRequest request = new HTTPRequest();
		request.setMethod(HTTP.HEAD);
		request.setHeader(HTTP.HOST, uri.getHost()+":"+uri.getPort());
		request.setHeader(HTTP.USER_AGENT, UPnP.getServerName());
		return request;
	}

	public static long[] getAvailableSeekRange(Uri uri){
		if(!isHttpURL(uri)) return new long[3];
			
		HTTPRequest request = buildHTTPRequest(uri);
		request.setHeader(DLNAConst.DLNA_GETAVAILABLESEEKRANGE, "1");
		request.setURI(uri.toString(), true);
		request.print();
		HTTPResponse response = request.post(uri.getHost(), uri.getPort());
		response.print();
		String rangeStr = response.getHeaderValue(DLNAConst.DLNA_AVAILABLESEEKRANGE);
		long[] range = getContentRange(rangeStr);
		if(range[2]==0){
			range[2] = Integer.parseInt(response.getHeaderValue(HTTP.CONTENT_LENGTH));
		}
		return range;
//		double startPos = (816277.00/273613952.00)*mPlayer.getDuration();
//		double endPos = (24488320.00/273613952.00)*mPlayer.getDuration();
//		if(msec<startPos||msec>endPos){
//			System.out.println("can not seek......................startPos: "+startPos+", endPos: "+endPos+", msec:"+msec );
//			return;
//		}
	}
	
	public static long[] getContentRange(String rangeStr)
	{
		long range[] = new long[3];
		range[0] = range[1] = range[2] = 0;
		String rangeLine = rangeStr;
		if (rangeLine.length() <= 0)
			return range;
		// Thanks for Brent Hills (10/20/04)
		StringTokenizer strToken = new StringTokenizer(rangeLine, " =");
		// Skip bytes
		if (strToken.hasMoreTokens() == false)
			return range;
		//update by fxw 20111014
		strToken.nextToken(" =");
		strToken.nextToken(" =");
		//String bytesStr = strToken.nextToken(" =");
		// Get first-byte-pos
		if (strToken.hasMoreTokens() == false)
			return range;
		//update by fxw 20111014
		String firstPosStr = strToken.nextToken(" =-");
		try {
			range[0] = Long.parseLong(firstPosStr);
		}
		catch (NumberFormatException e) {};
		if (strToken.hasMoreTokens() == false)
			return range;
		String lastPosStr = strToken.nextToken("-/");
		try {
			range[1] = Long.parseLong(lastPosStr);
		}
		catch (NumberFormatException e) {};
		if (strToken.hasMoreTokens() == false)
			return range;
		String lengthStr = strToken.nextToken("/");
		try {
			range[2] = Long.parseLong(lengthStr);
		}
		catch (NumberFormatException e) {};
		return range;
	}
	
	public static void getContentFeatures(Uri uri){
		if(!isHttpURL(uri)) return;
		
		HTTPRequest request = buildHTTPRequest(uri);
		request.setHeader(DLNAConst.DLNA_GETCONTENTFEATURES, "1");
		request.setURI(uri.toString());
		HTTPResponse response = request.post(uri.getHost(), uri.getPort());
		response.print();
	}
	
	
	
	
	public static void main(String[] args){
		String protocolInfo="http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC_520;DLNA.ORG_FLAGS=2D100000000000000000000000000000";
		MediaItem item = new MediaItem();
		item.setIfoFileUri("test");
		item.setProtocolInfo(protocolInfo);
		System.out.println(constructParams(item));
	}
	
	
//	long[] range = DLNAMediaSupport.getAvailableSeekRange(mUri);
//	
//	double startPos = (range[0]*1.00/range[2])*mMediaPlayer.getDuration();
//	double endPos = (range[1]*1.00/range[2])*mMediaPlayer.getDuration();
//	if(msec<startPos||msec>endPos){
//		System.out.println("can not seek......................startPos: "+startPos+", endPos: "+endPos+", msec:"+msec );
//		return;
//	}else{
//		System.out.println("seek......................startPos: "+startPos+", endPos: "+endPos+", msec:"+msec );
//	}
	
}
