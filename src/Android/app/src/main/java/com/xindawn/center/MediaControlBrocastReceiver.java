package com.xindawn.center;

import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class MediaControlBrocastReceiver extends BroadcastReceiver{

	private static final CommonLog log = LogFactory.createLog();
	private MediaControlBrocastFactory.IMediaControlListener mMediaControlListener;

	@Override
	public void onReceive(Context context, Intent intent) {

		  String action = intent.getAction(); 
		  if (action != null && mMediaControlListener != null){
			  TransdelControlCommand(intent);
		  }
	}
	
	public void setMediaControlListener(MediaControlBrocastFactory.IMediaControlListener listener)
	{
		mMediaControlListener = listener;
	}
	
	private void TransdelControlCommand(Intent intent){
		int time  = 0;
		int type  = 0;
	 
		String action = intent.getAction(); 
		if (action.equalsIgnoreCase(MediaControlBrocastFactory.MEDIA_RENDERER_CMD_PLAY)){
			mMediaControlListener.onPlayCommand();
		}else if (action.equalsIgnoreCase(MediaControlBrocastFactory.MEDIA_RENDERER_CMD_PAUSE)){
			mMediaControlListener.onPauseCommand();
		}else if (action.equalsIgnoreCase(MediaControlBrocastFactory.MEDIA_RENDERER_CMD_STOP)){
			type  = intent.getIntExtra(MediaControlBrocastFactory.PARAM_CMD_STOPTYPE, 0);
			mMediaControlListener.onStopCommand(type);
		}else if (action.equalsIgnoreCase(MediaControlBrocastFactory.MEDIA_RENDERER_CMD_SEEKPS)){	
			time  = intent.getIntExtra(MediaControlBrocastFactory.PARAM_CMD_SEEKPS, 0);
			mMediaControlListener.onSeekCommand(time);
		}else if (action.equalsIgnoreCase(MediaControlBrocastFactory.MEDIA_RENDERER_CMD_COVER)){	
		  mMediaControlListener.onCoverCommand(intent.getByteArrayExtra(MediaControlBrocastFactory.PARAM_CMD_COVER));
		}
		else if (action.equalsIgnoreCase(MediaControlBrocastFactory.MEDIA_RENDERER_CMD_METADATA)){	
			mMediaControlListener.onMetaDataCommand(intent.getStringExtra(MediaControlBrocastFactory.PARAM_CMD_METADATA));
		}
		else if (action.equalsIgnoreCase(MediaControlBrocastFactory.MEDIA_RENDERER_CMD_IPADDR)){	
			mMediaControlListener.onIPAddrCommand(intent.getStringExtra(MediaControlBrocastFactory.PARAM_CMD_IPADDR));
		}
			
	}
	
}
