package com.xindawn.DLAN.DMR.util;

import java.text.DecimalFormat;

public class Converter {
	
	/**
	 * 如下为将一个文件大小的Long型数据转换成K/M/G类型来表示，且只保留小数点后两位~
	 **/
	public static String convertSizetoStr(long size){
		String temp_str = "";
		DecimalFormat df = new DecimalFormat("########.00");	//取float的小数点后两位   
		//四舍五入   
		
		if(size >= 1024){	//计算 K
			float i =  size / 1024f;
			if(i >= 1024f){			//计算M
				float j = i / 1024f;
				if(j >= 1024f){		//计算G
					float k = j / 1024f;
                                        temp_str += df.format(k);
                                        temp_str += "G";
				}else{
					temp_str += df.format(j);
					temp_str += "M";
				}
			}else{
				temp_str += df.format(i);
				temp_str += "K";
			}
		}else{
			temp_str += size;
			temp_str += "B";
		}
		return temp_str;
	}
}
