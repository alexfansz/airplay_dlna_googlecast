package com.xindawn.airgl;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.xindawn.RenderApplication;
import com.xindawn.ScreenCast.ScreenCastService;
import com.xindawn.center.DMRCenter;
import com.xindawn.center.NALPacket;
import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;

import java.nio.ByteBuffer;

public class AudioPlayer {
    private static final CommonLog log = LogFactory.createLog();

    private static final String TAG = "AudioPlayer";
    private Handler decoderHandler;
    private HandlerThread decoderCallbackThread;
    private MediaCodec decoder;
    private boolean isDecoding;

    private MediaFormat mMediaFormat;

    public AudioPlayer(String mime) {
        mMediaFormat = MediaFormat.createAudioFormat(mime, 44100, 2);
        //mMediaFormat.setInteger(MediaFormat.);
        mMediaFormat.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP,1);
        //mMediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE,44100);
        //mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT,2);
        //mMediaFormat.setInteger(MediaFormat.KEY_AAC_SBR_MODE,0);
        //mMediaFormat.setInteger(MediaFormat.,16);
        //mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        //mMediaFormat.setInteger(MediaFormat.K);
        //mMediaFormat.setInteger(MediaFormat.KEY_AUDIO_SESSION_ID,-1);
        mMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS,1);
        //mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        //mMediaFormat.setInteger(MediaFormat.,16);

        /*byte[] csd_0= DMRCenter.audBuf.getTopPacker();
        if(csd_0==null){
            return;
        }else{
            log.d("audio header "+csd_0[0] + "*" + csd_0[1] + "*" + csd_0[2]+ "*" + csd_0[3]+ "*" + csd_0[4]+ "*" + csd_0[5]+ "*" + csd_0[6]);

        }*/

        try {
            decoder = MediaCodec.createDecoderByType(mime);

            log.d("create decoder with mime " + mime);
        } catch (java.io.IOException e) {
            log.e("Create codec from type ERROR !!! ");

            return;
        }

        decoderCallbackThread = new HandlerThread("DecoderHanlderThread");
        decoderCallbackThread.start();
        decoderHandler = new Handler(decoderCallbackThread.getLooper());

        setupDecoderCallback(decoderHandler);

        /*byte[] bytes = new byte[]{csd_0[7],csd_0[8]};
        //byte[] bytes = new byte[]{(byte) 0x12, (byte)0x12};
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        //mMediaFormat.setByteBuffer("csd-0", bb);
*/
        decoder.configure(mMediaFormat, null, null, 0);
        isDecoding = false;
        decoder.start();
    }

    public void stopRun(){
        DMRCenter.audBuf.PocketNotifyAll();
        if(null != decoder){
            try {
                //if(isDecoding) {
                    decoder.flush();
                //}

                decoder.stop();
                decoder.release();
            }catch(Exception e) {
                Log.e(TAG, "error while releasing audioDecoder", e);
            }

            decoderCallbackThread.quitSafely();
            try {
                decoderCallbackThread.join();

                Log.e(TAG, "audioDecoder thread done");
            } catch(InterruptedException  ex) {
            }
        }
    }

    private void setupDecoderCallback(Handler handle) {
        decoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(MediaCodec mc, int inputBufferId) {
                try {
                    ByteBuffer buffer = mc.getInputBuffer(inputBufferId);

                    NALPacket bb = DMRCenter.audBuf.popPacker(true);
                    if(null != bb) {
                        int size = bb.nalData.length;
                        long presentationTimeUs = bb.pts;
                        buffer.put(bb.nalData);

                        //Log.d(TAG, "audio decoder: data  <---- " + size);

                        mc.queueInputBuffer(inputBufferId, 0, size, presentationTimeUs, 0);

                        bb = null;
                    }else{
                        mc.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onOutputBufferAvailable(MediaCodec mc, int outputBufferId, MediaCodec.BufferInfo info) {

                Log.d(TAG, "audio decoder: data--> " + info.size);
                ByteBuffer buffer = mc.getOutputBuffer(outputBufferId);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mc.releaseOutputBuffer(outputBufferId, false);
                }
                else
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "audio decoder: EOS");
                }else {
                    DMRCenter.getInstance().audio_process(buffer.array(),info.presentationTimeUs,0);

                    buffer.clear();
                    mc.releaseOutputBuffer(outputBufferId, false);
                }

 /*               curTimeStamp = info.presentationTimeUs;


                if (info.size > 0 && !muteFlag) {
                    if (info.size < bufferSize) {
                        if (formatInited == true && mplayAudioTrack != null && !eosThread)
                            mplayAudioTrack.write(buffer, info.size, AudioTrack.WRITE_NON_BLOCKING);
                        flag = 4;
                    } else
                        Log.v(TAG, "Audio buffer " + info.size + "over buffersize " + bufferSize + " " + audioPath);
                    buffer.clear();
                }
                if (mStartRelease == false)
                    mc.releaseOutputBuffer(outputBufferId, false);

  */
            }

            @Override
            public void onOutputFormatChanged(MediaCodec mc, MediaFormat format) {
                log.d("New format " + decoder.getOutputFormat());
                isDecoding = true;
                mMediaFormat = format;

                int channelCount = mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                int channelMaskConfig = 0;

                switch (channelCount) {
                    case 1:
                        channelMaskConfig = AudioFormat.CHANNEL_OUT_MONO;
                        break;
                    case 2:
                        channelMaskConfig = AudioFormat.CHANNEL_OUT_STEREO;
                        break;
                    case 3:
                        channelMaskConfig = (AudioFormat.CHANNEL_OUT_STEREO | AudioFormat.CHANNEL_OUT_FRONT_CENTER);
                        break;
                    case 4:
                        channelMaskConfig = AudioFormat.CHANNEL_OUT_QUAD;
                        break;
                    case 5:
                        channelMaskConfig = (AudioFormat.CHANNEL_OUT_QUAD | AudioFormat.CHANNEL_OUT_FRONT_CENTER);
                        break;
                    case 6:
                        channelMaskConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                        break;
                    case 7:
                        channelMaskConfig = (AudioFormat.CHANNEL_OUT_5POINT1 | AudioFormat.CHANNEL_OUT_BACK_CENTER);
                        break;
                    case 8:
                        channelMaskConfig = AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;
                        break;
                    default:
                        channelMaskConfig = AudioFormat.CHANNEL_OUT_STEREO;
                        break;
                }

                int sampleRate = mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int bitRate = mMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);

                Intent intent = new Intent(ScreenCastService.MSG_filter);
                intent.putExtra("command", "audio_init");
                intent.putExtra("bit",bitRate);
                intent.putExtra("samprate",sampleRate);
                intent.putExtra("channel",channelCount);

                RenderApplication.getInstance().sendBroadcast(intent);
            }

            @Override
            public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                isDecoding = false;
                Log.d(TAG, "audio decoder: onError--> " );
                e.printStackTrace();
            }
        }, handle);
    }
}
