package com.xindawn.util;

import android.content.Context;

import com.xindawn.center.DlnaMediaModel;
import com.xindawn.center.NALPacket;
import com.xindawn.datastore.LocalConfigSharePreference;
import com.xindawn.jni.PlatinumReflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DlnaUtils {

	private static final CommonLog log = LogFactory.createLog();

	private /*static*/ ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private /*static*/ List<NALPacket> mListBuffer = Collections.synchronizedList(new ArrayList<NALPacket>());
	private boolean isWaitting = false;//对于java 的这个属性了解不深入，不知道是否可以不用，测试看好像可以不用，目前先放到这里

	public static boolean setDevName(Context context, String friendName){
		return LocalConfigSharePreference.commintDevName(context, friendName);
	}
	
	public static String getDevName(Context context){
		return LocalConfigSharePreference.getDevName(context);
	}
	
	
	public static String creat12BitUUID(Context context){
		String defaultUUID  = "123456789abc";
		
		String mac = CommonUtil.getLocalMacAddress(context);
	
		mac = mac.replace(":","");
		mac = mac.replace(".","");
		
		if (mac.length() != 12){
			mac = defaultUUID;
		}
		
		mac += "-^@^";
		return mac;
	}
	
	

	public static int parseSeekTime(String data) throws Exception{
		
		int seekPos = 0;		
	
		String[] seektime = data.split("=");
		if(2 != seektime.length){
			return seekPos;
		}
		String timetype = seektime[0];
		String position = seektime[1];
		if (PlatinumReflection.MEDIA_SEEK_TIME_TYPE_REL_TIME.equals(timetype)){
			seekPos = convertSeekRelTimeToMs(position);
		}else{
			log.e("timetype = " + timetype + ", position = " + position);
		}

	
		return seekPos;
	}
	
	public static int convertSeekRelTimeToMs(String reltime){
		int sec=0;
		int ms=0;
		String[] times=reltime.split(":");
		if(3!=times.length)
			return 0;
		if(!isNumeric(times[0]))
			return 0;
		int hour=Integer.parseInt(times[0]);
		if(!isNumeric(times[1]))
			return 0;
		int min=Integer.parseInt(times[1]);
		String[] times2=times[2].split("\\.");
		if(2==times2.length){//00:00:00.000
			if(!isNumeric(times2[0]))
				return 0;
			sec=Integer.parseInt(times2[0]);
			if(!isNumeric(times2[1]))
				return 0;
			ms=Integer.parseInt(times2[1]);
		}else if(1==times2.length){//00:00:00
			if(!isNumeric(times2[0]))
				return 0;
			sec=Integer.parseInt(times2[0]);
		}
		return (hour*3600000+min*60000+sec*1000+ms);
	}

	public static boolean isNumeric(String str){
		if("".equals(str))
			return false;
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if( !isNum.matches() ){
			return false;
		}
		return true;
	} 
	
	public static String formatTimeFromMSInt(int time){
		String hour="00";
		String min="00";
		String sec="00";
		String split=":";
		int tmptime=time;
		int tmp=0;
		if(tmptime>=3600000){
			tmp=tmptime/3600000;
			hour=formatHunToStr(tmp);
			tmptime-=tmp*3600000;
		}
		if(tmptime>=60000){
			tmp=tmptime/60000;
			min=formatHunToStr(tmp);
			tmptime-=tmp*60000;
		}
		if(tmptime>=1000){
			tmp=tmptime/1000;
			sec=formatHunToStr(tmp);
			tmptime-=tmp*1000;
		}
		
		String ret=hour+split+min+split+sec;
		return ret;
	}
	
	private static String formatHunToStr(int hun){
		hun=hun%100;
		if(hun>9)
			return (""+hun);
		else
			return ("0"+hun);
	}
	
	
	public static String formateTime(long millis)
	{
		String str = "";
		int hour = 0;
		int time = (int) (millis / 1000);
		int second = time % 60;
		int minute = time / 60;
		if (minute >= 60){
			hour = minute / 60;
			minute %= 60;
			str = String.format("%02d:%02d:%02d", hour, minute, second);
		}else{
			str = String.format("%02d:%02d", minute, second);
		}

		
		return str;
		
	}

	public final static String DLNA_OBJECTCLASS_MUSICID = "object.item.audioItem";
	public final static String DLNA_OBJECTCLASS_VIDEOID = "object.item.videoItem";
	public final static String DLNA_OBJECTCLASS_PHOTOID = "object.item.imageItem";
	public final static String DLNA_OBJECTCLASS_SCREEN  = "object.item.screenItem";
	public final static String DLNA_OBJECTCLASS_U2  = "object.item.u2Item";
	
	public static boolean isAudioItem(DlnaMediaModel item){
		String objectClass = item.getObjectClass();
		if (objectClass.contains(DLNA_OBJECTCLASS_MUSICID)){
			return true;
		}
		
		return false;
	}
	
	public static boolean isVideoItem(DlnaMediaModel item){
		String objectClass = item.getObjectClass();
		if (objectClass.contains(DLNA_OBJECTCLASS_VIDEOID)){
			return true;
		}
		return false;
	}
	
	public static boolean isImageItem(DlnaMediaModel item){
		String objectClass = item.getObjectClass();
		if (objectClass.contains(DLNA_OBJECTCLASS_PHOTOID)){
			return true;
		}
		return false;
	}
	
	public static boolean isScreenItem(DlnaMediaModel item){
		String objectClass = item.getObjectClass();
		if (objectClass.contains(DLNA_OBJECTCLASS_SCREEN)){
			return true;
		}
		return false;
	}

	public static boolean isU2Item(DlnaMediaModel item){
		String objectClass = item.getObjectClass();
		if (objectClass.contains(DLNA_OBJECTCLASS_U2)){
			return true;
		}
		return false;
	}

	public /*static*/ void initPacker() {
		rwl.writeLock().lock();
		try {
			mListBuffer.clear();
			//synchronized(mListBuffer){
			//	mListBuffer.notify();
			//}
		}finally {
			rwl.writeLock().unlock();
		}
	}

	public /*static*/ void addPacker(NALPacket nalPacket) {
		rwl.writeLock().lock();
		try {
			if (null != nalPacket.nalData) mListBuffer.add(nalPacket);

			synchronized(mListBuffer){
				if(isWaitting) {
					mListBuffer.notify();
				}
			}
		}finally {
			rwl.writeLock().unlock();
		}
	}

	public /*static*/ NALPacket popPacker(boolean bWait) throws InterruptedException {
		NALPacket bb = null;
		rwl.writeLock().lock();
		if(mListBuffer.isEmpty()){
			if(bWait){
				rwl.writeLock().unlock();
				synchronized(mListBuffer){
					isWaitting = true;
					mListBuffer.wait();
					isWaitting = false;
				}

				rwl.writeLock().lock();
				bb = mListBuffer.isEmpty() ? null : mListBuffer.remove(0);
			}
		}else{
			bb = mListBuffer.remove(0);
		}
		rwl.writeLock().unlock();

		return bb;
	}

	public /*static*/ byte[] getTopPacker() {
		rwl.writeLock().lock();
		try {


			if (mListBuffer.isEmpty()) {

				return null;
			} else {
				return mListBuffer.get(0).nalData;
			}
		} finally {
			rwl.writeLock().unlock();
		}
	}

	public void PocketNotifyAll()
	{
		rwl.writeLock().lock();
		try {
			//mListBuffer.clear();
			synchronized(mListBuffer){
				if(isWaitting) mListBuffer.notify();
			}
		}finally {
			rwl.writeLock().unlock();
		}
	}
}
