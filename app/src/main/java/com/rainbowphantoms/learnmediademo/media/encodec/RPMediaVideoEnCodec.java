package com.rainbowphantoms.learnmediademo.media.encodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 作者： MirsFang on 2019/1/4 13:11
 * 邮箱： mirsfang@163.com
 * 类描述：媒体编码
 */
public class RPMediaVideoEnCodec {
    private static final String TAG = "RPMediaDeCodec";

    public static final int VIDEO = 2;
    public static final int AUDIO = 3;

    private MediaCodec mEncoder;
    /** 默认解码器超时事件 **/
    private int mTimeOutUs = 5000;
    /** 媒体分离器 **/
    private MediaExtractor mExtractor;
    /** 当前视频路径 **/
    private String mPath;
    /** 当前解码器类型 **/
    private int mType;
    /** 显示表面 **/
    private Surface mSurface;
    /**  MIME标志 **/
    private String mMime;
    /** 当前轨道下标 **/
    private int mTrackIndex;
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private MediaCodec.BufferInfo mBufferInfo;

    private RPMediaVideoEncoderListener mEncoderListener;


    public RPMediaVideoEnCodec(String mPath){
        this.mPath = mPath;
        this.mType = VIDEO;
    }

    public void setEncoderListener(RPMediaVideoEncoderListener encoderListener){
        this.mEncoderListener = encoderListener;
    }

    public void prepareViewEncoder(int width,int height){
        mMime = "video/avc";

        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
        int frameRate = 30; // 30 fps

        // Set some required properties. The media codec may fail if these aren't defined.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000); // 6Mbps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames


        try {
            mEncoder = MediaCodec.createDecoderByType(mMime);
            mEncoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            this.mSurface = mEncoder.createInputSurface();
            mEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, mMime + "编码器器初始化失败");
        }
    }


    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        if (endOfStream) {
            mEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once

                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

//              添加编码轨道
                mTrackIndex = mEncoderListener.addVideoTrack(newFormat);
//                mMuxer.start();
//                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

//                    合成器写入数据
                    mEncoderListener.writeVideoSampleData(mTrackIndex,mBufferInfo,encodedData);
//                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
//                    if (VERBOSE) {
//                        Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
//                                mBufferInfo.presentationTimeUs);
//                    }
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
//                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }

}
