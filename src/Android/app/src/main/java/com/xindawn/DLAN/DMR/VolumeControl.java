package com.xindawn.DLAN.DMR;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

public class VolumeControl {
    private static String TAG = "VolumeControl";
    private AudioManager audioManager;
    private VolumeChangeListener volumeChangeListener = null;
    private static float MAX_SYSTEM_VOLUME;
    private static int MAX_VOLUME = 100;
    private int currentVolume = 0;
    private boolean currentMute = false;
    private final Handler handler = new Handler();
    private boolean stopVolumeMonitor = false;

    public void setVolumeChangeListener(VolumeChangeListener volumeChangeListener) {
        this.volumeChangeListener = volumeChangeListener;
    }

    public VolumeControl(Context context) {
        Log.v(TAG, "The VolumeControl is created.");
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        MAX_SYSTEM_VOLUME = this.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    private int getVolume() {
        this.currentVolume = getSystemVolume();
        return this.currentVolume;
    }

    private int getSystemVolume() {
        return Math.round(this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                / MAX_SYSTEM_VOLUME * MAX_VOLUME);
    }

    private boolean getMute() {
        return currentMute;
    }

    public void setVolume(int vol) {
        this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                (int) (Math.ceil(MAX_SYSTEM_VOLUME * vol / MAX_VOLUME)), AudioManager.FLAG_SHOW_UI);
    }

    public void setMute(boolean on) {
        if (currentMute != on) {
           currentMute = on;
           audioManager.setStreamMute(AudioManager.STREAM_MUSIC, currentMute);
        }
        else {
            Log.v(TAG, "The setMute is called without changed. Mute: " + on);
        }
    }

    public static interface VolumeChangeListener {
        public void volumeChanged(int vol);
        public void muteChanged(boolean bMute);
    }

    public void startMonitor() {
        stopVolumeMonitor = false;
        notifyMuteChanged(currentMute);
        exeVolumeUpdater();
    }

    public void stopMonitor() {
        stopVolumeMonitor = true;
    }

    private void notifyVolumeChanged(final int vol) {
        if (volumeChangeListener != null)
        {
            volumeChangeListener.volumeChanged(vol);
        }
    }

    private void notifyMuteChanged(final boolean bMute) {
        if (volumeChangeListener != null)
        {
            volumeChangeListener.muteChanged(bMute);
        }
    }
    
    private void exeVolumeUpdater() {
        if (this.stopVolumeMonitor) {
            return;
        }

        int volumeNow = this.getSystemVolume();
        if (volumeNow != this.currentVolume) {
            this.currentVolume = volumeNow;
            this.notifyVolumeChanged(volumeNow);
            
            boolean muteNow = this.currentVolume <= 0;
            if (this.currentMute != muteNow)
            {
                this.currentMute = muteNow;
                this.notifyMuteChanged(muteNow);
            }
        }

        handler.postDelayed(new Runnable() {
            public void run() {
                exeVolumeUpdater();
            }
        }, 1000);
    }
}
