package com.xindawn.datastore;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class LocalConfigSharePreference {

	public static final String preference_name = "LocalConfigSharePreference"; //+applicationID  context.getPackageName()
	public static final String dev_name = "dev_name";
	
	public static boolean commintDevName(Context context, String devName){
	
		SharedPreferences sharedPreferences = context.getSharedPreferences(preference_name+context.getPackageName(), Context.MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.putString(dev_name, devName);
		editor.commit();
		return true;
	}
	
	public static String getDevName(Context context){
		SharedPreferences sharedPreferences = context.getSharedPreferences(preference_name+context.getPackageName(), Context.MODE_PRIVATE);
		String value = sharedPreferences.getString(dev_name, "TVCast player");
		return value;
	}

	public static boolean commitSettingsVal(Context context,String settings,String val){

		SharedPreferences sharedPreferences = context.getSharedPreferences(preference_name+context.getPackageName(), Context.MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.putString(settings, val);
		editor.commit();
		return true;
	}

	public static String getSettingsVal(Context context,String setting){
		SharedPreferences sharedPreferences = context.getSharedPreferences(preference_name+context.getPackageName(), Context.MODE_PRIVATE);
		String value = sharedPreferences.getString(setting, "false");
		return value;
	}
}
