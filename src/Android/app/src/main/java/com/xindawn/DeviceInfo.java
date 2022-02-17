package com.xindawn;

public class DeviceInfo {

	public String dev_name;
	public String uuid;
	public String applicationId;
	public boolean status;
	public boolean aiplayMirrorUsingHw;
	public int airtunes_port;
	public int airplay_port;
	public int width;
	public int height;
	public String mMacAddress;

	public String APP_CHANNEL;
	public String BuglyID;
	
	public DeviceInfo(){
		dev_name = "";
		uuid = "";
		applicationId = "";
		status = false;
		width = 1920;
		height = 1080;
		aiplayMirrorUsingHw = true;
		airtunes_port = 10088;
		airplay_port = 10099;

		APP_CHANNEL = "www.basicgo.net";
		//APP_CHANNEL = "www.touying.com";
		//APP_CHANNEL = "www.znds.com";	//当贝
		//APP_CHANNEL = "www.hdpfans.com";
		//APP_CHANNEL = "ShangHaiJianLong";//上海建隆
		BuglyID = "a2efc4c0e5";
	}
}
