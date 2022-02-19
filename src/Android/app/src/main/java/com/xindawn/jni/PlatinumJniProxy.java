package com.xindawn.jni;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.xindawn.RenderApplication;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PlatinumJniProxy {

    static {
			System.loadLibrary("jniinterface");
    }   

    public static native int startMediaRender(String friendname,String libpath,String activecode,int width,int height ,int airtunes_port,int airplay_port,int rcv_size,Context obj );
    public static native int stopMediaRender();  
    public static native boolean responseGenaEvent(int cmd, byte[] value ,byte[] data);  
    public static native boolean enableLogPrint(boolean flag);
 
    
    public static String findLibrary1(Context context, String libName) {  
        String result = null;  
        ClassLoader classLoader = (context.getClassLoader());  
        if (classLoader != null) {  
            try {  
                Method findLibraryMethod = classLoader.getClass().getMethod("findLibrary", new Class<?>[] { String.class });     
                if (findLibraryMethod != null) {  
                    Object objPath = findLibraryMethod.invoke(classLoader, new Object[] { libName });  
                    if (objPath != null && objPath instanceof String) {  
                        result = (String) objPath;  
                    }  
                }  
            } catch (NoSuchMethodException e) {  
                //Log.e("findLibrary1", e.toString());  
            } catch (IllegalAccessException e) {  
                //Log.e("findLibrary1", e.toString());  
            } catch (IllegalArgumentException e) {  
                //Log.e("findLibrary1", e.toString());  
            } catch (InvocationTargetException e) {  
               // Log.e("findLibrary1", e.toString());  
            } catch (Exception e) {  
                //Log.e("findLibrary1", e.toString());  
            }  
        }  
      
        return result;  
    }  
    
    
    public static  int startMediaRender_Java(String friendname){
    	if (friendname == null)friendname = "";
    
    	int ret = -1;
    	
    	String objpath = findLibrary1(RenderApplication.getInstance(),"mediaserver");

    	if(RenderApplication.getInstance().mDeviceInfo.aiplayMirrorUsingHw){
            ret = startMediaRender(friendname,objpath,RenderApplication.getInstance().mDeviceInfo.mMacAddress+"@"+ Build.BRAND +" "+ Build.MODEL,RenderApplication.getInstance().mDeviceInfo.width,RenderApplication.getInstance().mDeviceInfo.height,RenderApplication.getInstance().mDeviceInfo.airtunes_port, RenderApplication.getInstance().mDeviceInfo.airplay_port,128*1024,RenderApplication.getInstance());
        }else {
            ret = startMediaRender(friendname,objpath,RenderApplication.getInstance().mDeviceInfo.mMacAddress+"@"+Build.BRAND +" "+ Build.MODEL,1280, 720, RenderApplication.getInstance().mDeviceInfo.airtunes_port,RenderApplication.getInstance().mDeviceInfo.airplay_port,128*1024,RenderApplication.getInstance());
        }

        return ret;
    }
    
    public static  boolean responseGenaEvent(int cmd, String value, String data){
    	if (value == null)value = "";
    	if (data == null)data = "";
    	boolean ret = false;
    	try {
			ret = responseGenaEvent(cmd, value.getBytes("utf-8"), data.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	return ret;
    }
    
    
}
