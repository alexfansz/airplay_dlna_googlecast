package com.xindawn.airgl;

import static com.xindawn.DLAN.plugins.videoplay.MediaController.FULLSCREEN_MAINTIANXY;
import static com.xindawn.DLAN.plugins.videoplay.MediaController.SCREEN_MODE_169;
import static com.xindawn.DLAN.plugins.videoplay.MediaController.SCREEN_MODE_43;
import static com.xindawn.DLAN.plugins.videoplay.MediaController.SCREEN_MODE_FULL;
import static com.xindawn.DLAN.plugins.videoplay.MediaController.SCREEN_MODE_ORIG;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.xindawn.RenderApplication;
import com.xindawn.center.DMRCenter;
import com.xindawn.center.MediaControlBrocastFactory;
import com.xindawn.center.NALPacket;
import com.xindawn.datastore.LocalConfigSharePreference;
import com.xindawn.util.CommonLog;
import com.xindawn.util.LogFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoPlayer extends Thread {
    private static final CommonLog log = LogFactory.createLog();

    private static final String TAG = "VideoPlayer";
    private static final boolean VERBOSE = false; // lots of logging

    private String mMimeType = MediaFormat.MIMETYPE_VIDEO_AVC;//"video/avc";
    private int mVideoWidth  = RenderApplication.getInstance().mDeviceInfo.width;
    private int mVideoHeight = RenderApplication.getInstance().mDeviceInfo.height;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec mDecoder = null;
    private Surface mSurface = null;
    private int surfaceW,surfaceH;      //最初创建是对宽度和高度，中间实际宽高度会随setScreenSize变化，这里用来保留初始数据
    private/* synchronized*/ boolean mIsEnd = false;

    public VideoPlayer(Surface surface,int width, int height) {
        mSurface = surface;
        surfaceH = height;
        surfaceW = width;
        Log.d(TAG, "surface: "+surfaceW+":"+surfaceH);
        //initDecoder();
    }

    public void onConfigurationChanged(Configuration newConfig){
        if (LocalConfigSharePreference.getSettingsVal(RenderApplication.getInstance(),"forceFullScreen").equals("false")) {

            if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                //切换到竖屏
                //修改布局文件
                //setContentView(R.layout.activity_main);
                //findViewById ....
                //TODO something
                Log.d(TAG, "ActivityInfo.SCREEN_ORIENTATION_PORTRAIT"+" mVideoWidth: " + mVideoWidth + " mVideoHeight: "+mVideoHeight);

                int tmp = surfaceH;
                surfaceH = surfaceW;
                surfaceW = tmp;

                setScreenSize(SCREEN_MODE_FULL, mVideoWidth, mVideoHeight, false);
            } else {
                //切换到横屏
                //修改布局文件
                //setContentView(R.layout.activity_main);
                //findViewById ....
                //TODO something
                Log.d(TAG, "!ActivityInfo.SCREEN_ORIENTATION_PORTRAIT"+" mVideoWidth: " + mVideoWidth + " mVideoHeight: "+mVideoHeight);
                int tmp = surfaceH;
                surfaceH = surfaceW;
                surfaceW = tmp;

                setScreenSize(SCREEN_MODE_FULL, mVideoWidth, mVideoHeight, false);
            }
        }
    }

    public void setScreenSize(int width, int height) {
        /*mHolder.setFixedSize(0x28062806, 0x28062806); //enable size changed
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if(width > surfaceW){
            width = surfaceW;
        }
        if(height > surfaceH){
            height = surfaceH;
        }
        mHolder.setFixedSize(width, height);*/

        String strCmd = width+":"+height;
        MediaControlBrocastFactory.sendIPAddrBrocast(RenderApplication.getInstance(),strCmd);
    }

    public void initDecoder() {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(mMimeType, mVideoWidth, mVideoHeight);

            //仅仅对realtek平台OMX.realtek设置videoLowDelay
            //format.setInteger(MediaFormat.KEY_VideoLowLatency,0);  ///*"vldm"*/

            format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP,1);
            //MediaMetadata.

            /*mDecoder = MediaCodec.createDecoderByType(mMimeType);
            mDecoder.configure(format, mSurface, null, 0);
            mDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mDecoder.start();*/

            mDecoder = createVideoDecoder(format,mSurface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public void addPacker(NALPacket nalPacket) {
        mListBuffer.add(nalPacket);
    }
    public void addPacker(NALPacket nalPacket) {
        doDecode(nalPacket);
    }*/
    
    @Override
    public void run() {
        super.run();
        initDecoder();
        /*
        while (!mIsEnd) {
            if (mListBuffer.size() == 0) {
                try {
                    sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            doDecode(mListBuffer.remove(0));
        }*/

        while (!mIsEnd) {

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }


        //mVideoDecoderHandler.removeCallbacks(mVideoDecoderHandlerThread);
        //mVideoDecoderHandlerThread.quitSafely();

        try {
            mDecoder.flush();
            mDecoder.stop();
            mDecoder.release();
        }catch(Exception e) {
            Log.e(TAG, "error while releasing videoDecoder", e);
        }

        mVideoDecoderHandlerThread.quitSafely();
        try {
            mVideoDecoderHandlerThread.join();
        } catch(InterruptedException  ex) {
        }

        mVideoDecoderHandler.removeCallbacks(mVideoDecoderHandlerThread);

        log.d("VideoPlayer quitSafely");
    }

    public void stopRUN(){
        mIsEnd = true;

        //DlnaUtils.initPacker();
        DMRCenter.videoBuf.PocketNotifyAll();
    }

    private void doDecode(NALPacket nalPacket) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = mDecoder.getInputBuffers();
        int inputBufIndex = -10000;
        try {
            inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inputBufIndex >= 0) {
            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
            inputBuf.clear();
            inputBuf.put(nalPacket.nalData);
            mDecoder.queueInputBuffer(inputBufIndex, 0, nalPacket.nalData.length, nalPacket.pts, 0);
        } else {
            Log.d(TAG, "dequeueInputBuffer failed");
        }

        int outputBufferIndex = -10000;
        try {
            outputBufferIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (outputBufferIndex >= 0) {
            mDecoder.releaseOutputBuffer(outputBufferIndex, true);
            /*try{
                Thread.sleep(50);
            }  catch (InterruptedException ie){
                ie.printStackTrace();
            }*/
        } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            /*try{
                Thread.sleep(10);
            }  catch (InterruptedException ie){
                ie.printStackTrace();
            }*/
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            // not important for us, since we're using Surface

        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

        } else {

        }

        /*while (outputBufferIndex>=0) {
            mDecoder.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        }*/
    }


    /*---------------------------------------------------------------------------------------------*/
    /*  async mode                                                                                 */

    /*---------------------------------------------------------------------------------------------*/
    private void setScreenSize(int mode,int width,int height,boolean orientationChanged)
    {
        int maxWidth = orientationChanged ? surfaceH : surfaceW;
        int maxHeight = orientationChanged ? surfaceW : surfaceH;
        switch(mode){
            case SCREEN_MODE_ORIG:
                if(FULLSCREEN_MAINTIANXY){
                    if(width > maxWidth || height > maxHeight){
                        float degree = (float)width / (float)height;
                        int tmpWidth1 = maxWidth;
                        int tmpHeight1 = (int)(tmpWidth1 / degree);

                        int tmpHeight2 = maxHeight;
                        int tmpWidth2 = (int)(tmpHeight2 * degree);

                        if(tmpHeight1 > maxHeight && tmpWidth2 <= maxWidth){
                            setScreenSize(tmpWidth2, tmpHeight2);
                        }else if(tmpWidth2 > maxWidth && tmpHeight1 <= maxHeight){
                            setScreenSize(tmpWidth1, tmpHeight1);
                        }else if(tmpHeight1 <= maxHeight && tmpWidth2 <= maxWidth){
                            if(tmpWidth1 * tmpHeight1 > tmpWidth2 * tmpHeight2){
                                setScreenSize(tmpWidth1, tmpHeight1);
                            }else{
                                setScreenSize(tmpWidth2, tmpHeight2);
                            }
                        }
                        else{
                            setScreenSize(maxWidth,maxHeight);
                        }
                    }else{
                        setScreenSize(width, height);
                    }
                }else{
                    setScreenSize(width, height);
                }
                break;
            case SCREEN_MODE_169:
                setScreenSize(maxWidth,maxWidth/16*9);
                break;
            case SCREEN_MODE_43:
                setScreenSize(maxHeight/3*4,maxHeight);
                break;
            case SCREEN_MODE_FULL:
                if(width == 0 || height == 0){
                    setScreenSize(maxWidth,maxHeight);
                    break;
                }


                Log.d(TAG, "setScreenSize"+" maxWidth: " + maxWidth + " maxHeight: "+maxHeight + " width: "+width + " height: "+height);

                if(FULLSCREEN_MAINTIANXY){
                    float degree = (float)width / (float)height;
                    int tmpWidth1 = maxWidth;
                    int tmpHeight1 = (int)(tmpWidth1 / degree);

                    int tmpHeight2 = maxHeight;
                    int tmpWidth2 = (int)(tmpHeight2 * degree);

                    if(tmpHeight1 > maxHeight && tmpWidth2 <= maxWidth){
                        setScreenSize(tmpWidth2, tmpHeight2);
                    }else if(tmpWidth2 > maxWidth && tmpHeight1 <= maxHeight){
                        setScreenSize(tmpWidth1, tmpHeight1);
                    }else if(tmpHeight1 <= maxHeight && tmpWidth2 <= maxWidth){
                        //#if 1
                        /*float degree_w = (float) maxWidth / width;
                        float degree_h = (float) maxHeight / height;

                        int tmpHeight3 = (int)(height * degree_w);
                        int tmpWidth3 = (int)(width * degree_h);
                        Log.d(TAG, "setScreenSize"+" maxWidth: " + maxWidth + " maxHeight: "+maxHeight + " tmpWidth3: "+tmpWidth3 + " tmpHeight3: "+tmpHeight3);

                        if(tmpHeight3 <= maxHeight){
                            setScreenSize(tmpWidth1, tmpHeight3);
                        }
                        else if(tmpWidth3 <= maxWidth){
                            setScreenSize(tmpWidth3, tmpHeight2);
                        }

                        #else*/
                        if(tmpWidth1 * tmpHeight1 > tmpWidth2 * tmpHeight2){
                            setScreenSize(tmpWidth1, tmpHeight1);
                        }else{
                            setScreenSize(tmpWidth2, tmpHeight2);
                        }
                        /*#endif */
                    }
                    else{
                        setScreenSize(maxWidth,maxHeight);
                    }
                }else{
                    setScreenSize(maxWidth,maxHeight);
                }

                break;
        }
    }
    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }
    static class CallbackHandler extends Handler {
        CallbackHandler(Looper l) {
            super(l);
        }
        private MediaCodec mCodec;
        private boolean mEncoder;
        private MediaCodec.Callback mCallback;
        private String mMime;
        private boolean mSetDone;
        @Override
        public void handleMessage(Message msg) {
            try {
                mCodec = mEncoder ? MediaCodec.createEncoderByType(mMime) : MediaCodec.createDecoderByType(mMime);

                Log.d(TAG, "createCodec "+mMime+", "+mCodec.getName());
            } catch (IOException ioe) {
            }
            mCodec.setCallback(mCallback);
            synchronized (this) {
                mSetDone = true;
                notifyAll();
            }
        }
        void create(boolean encoder, String mime, MediaCodec.Callback callback) {
            mEncoder = encoder;
            mMime = mime;
            mCallback = callback;
            mSetDone = false;
            sendEmptyMessage(0);
            synchronized (this) {
                while (!mSetDone) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
        MediaCodec getCodec() {
            return mCodec;
        }
    }
    private HandlerThread mVideoDecoderHandlerThread;
    private CallbackHandler mVideoDecoderHandler;
    private MediaFormat mDecoderOutputVideoFormat = null;
    /**
     * Creates a decoder for the given format, which outputs to the given surface.
     *
     * @param inputFormat the format of the stream to decode
     * @param surface into which to decode the frames
     */
    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws IOException {
        mVideoDecoderHandlerThread = new HandlerThread("DecoderThread");
        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new CallbackHandler(mVideoDecoderHandlerThread.getLooper());
        MediaCodec.Callback callback = new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
                Log.d(TAG, "video decoder: error");
                exception.printStackTrace();
            }
            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                mDecoderOutputVideoFormat = codec.getOutputFormat();
                if (LocalConfigSharePreference.getSettingsVal(RenderApplication.getInstance(),"forceFullScreen").equals("false")) {
                    log.d("video decoder: output format changed: "
                            + mDecoderOutputVideoFormat);

                    Rect cropRect = new Rect();
                    int width = mDecoderOutputVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
                    if (mDecoderOutputVideoFormat.containsKey("crop-left") && mDecoderOutputVideoFormat.containsKey("crop-right")) {
                        width = mDecoderOutputVideoFormat.getInteger("crop-right") + 1 - mDecoderOutputVideoFormat.getInteger("crop-left");
                    }
                    int height = mDecoderOutputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    if (mDecoderOutputVideoFormat.containsKey("crop-top") && mDecoderOutputVideoFormat.containsKey("crop-bottom")) {
                        height = mDecoderOutputVideoFormat.getInteger("crop-bottom") + 1 - mDecoderOutputVideoFormat.getInteger("crop-top");
                    }

                    mVideoWidth = width;
                    mVideoHeight = height;
                    setScreenSize(SCREEN_MODE_FULL,width,height,false);
                }
            }
            public void onInputBufferAvailable(MediaCodec codec, int index) {
                // Extract video from file and feed to decoder.
                // We feed packets regardless of whether the muxer is set up or not.
                // If the muxer isn't set up yet, the encoder output will be queued up,
                // finally blocking the decoder as well.

                /* it is must,,,not good fix me!!!!
                while (DlnaUtils.mListBuffer.size() == 0 && !mIsEnd) {
                    try {
                        sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/

                if (/*DlnaUtils.mListBuffer.size() > 0 &&*/ !mIsEnd){
                    //ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    //inputBuf.clear();
                    //inputBuf.put(nalPacket.nalData);

                    try {
                        ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);

                        NALPacket bb = DMRCenter.videoBuf.popPacker(true);
                        if(null != bb) {
                            int size = bb.nalData.length;
                            long presentationTimeUs = bb.pts;
                            decoderInputBuffer.put(bb.nalData);

                            codec.queueInputBuffer(index, 0, size, presentationTimeUs, 0);

                            bb = null;
                        }else{
                            mIsEnd = true;
                            /*codec.queueInputBuffer(
                                    index,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);*/
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (mIsEnd) {
                    if (VERBOSE) Log.d(TAG, "video extractor: EOS");
                    try {
                        codec.queueInputBuffer(
                                index,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } catch (Exception e) {
                        //虽然设置format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP,1);
                        //华为mate 10 仍然会抛出这个异常
                        //所以还是得catch
                        e.printStackTrace();
                    }
                }
            }
            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned output buffer: " + index);
                    Log.d(TAG, "video decoder: returned buffer of size " + info.size);
                    Log.d(TAG, "video decoder: returned buffer for time "
                            + info.presentationTimeUs);
                }

                try {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (VERBOSE) Log.d(TAG, "video decoder: codec config buffer");
                        codec.releaseOutputBuffer(index, false);
                        return;
                    } else if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "video decoder: EOS");

                        mIsEnd = true;
                    } else {
                        boolean render = info.size != 0;
                        if (!mIsEnd) codec.releaseOutputBuffer(index, render);
                    }
                }catch (IllegalStateException e){

                    e.printStackTrace();
                }
            }
        };
        // Create the decoder on a different thread, in order to have the callbacks there.
        // This makes sure that the blocking waiting and rendering in onOutputBufferAvailable
        // won't block other callbacks (e.g. blocking encoder output callbacks), which
        // would otherwise lead to the transcoding pipeline to lock up.

        // Since API 23, we could just do setCallback(callback, mVideoDecoderHandler) instead
        // of using a custom Handler and passing a message to create the MediaCodec there.

        // When the callbacks are received on a different thread, the updating of the variables
        // that are used for state logging (mVideoExtractedFrameCount, mVideoDecodedFrameCount,
        // mVideoExtractorDone and mVideoDecoderDone) should ideally be synchronized properly
        // against accesses from other threads, but that is left out for brevity since it's
        // not essential to the actual transcoding.
        mVideoDecoderHandler.create(false, getMimeTypeFor(inputFormat), callback);
        MediaCodec decoder = mVideoDecoderHandler.getCodec();
        decoder.configure(inputFormat, surface, null, 0);
        //decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        decoder.start();
        return decoder;
    }
}
