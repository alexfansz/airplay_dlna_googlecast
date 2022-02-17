package com.xindawn.center;


public interface IDMRAction {
	public void onRenderAvTransport(String value, String data);
	public void onRenderPlay(String value, String data);
	public void onRenderPause(String value, String data);
	public void onRenderStop(String value, String data);
	public void onRenderSeek(String value, String data);
	public void onRenderSetMute(String value, String data);
	public void onRenderSetVolume(String value, String data);
	public void onRenderSetCover(String value, byte data[]);
	public void onRenderSetMetaData(String value, String data);
	public void onRenderSetIPAddr(String value, String data);
}

