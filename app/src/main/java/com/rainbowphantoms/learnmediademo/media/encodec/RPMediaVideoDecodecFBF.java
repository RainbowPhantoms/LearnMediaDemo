package com.rainbowphantoms.learnmediademo.media.encodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.annotation.IntRange;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 作者： MirsFang on 2019/1/9 14:28
 * 邮箱： mirsfang@163.com
 * 类描述：逐帧解码
 */
public class RPMediaVideoDecodecFBF {
    private static final String TAG = "RPMediaVideoDecodecFBF";

    public static final int VIDEO = 2;

    public interface DeCodecBufferListener {

        void onExtractor(MediaFormat mediaFormat);

        void onDecodecBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

        void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo);

    }

    /** 默认解码器超时事件 **/
    private int mTimeOutUs = 5000;
    /** 媒体分离器 **/
    private MediaExtractor mExtractor;
    /** 解码器 **/
    private MediaCodec mMediaCodec;
    /** MIME标志 **/
    private String mMime;
    /** 当前轨道下标 **/
    private int mTrackIndex;
    /** 当前媒体信息 **/
    private MediaFormat mMediaFormat;
    /** 当前视频路径 **/
    private String mPath;
    /** 显示表面 **/
    private Surface mSurface;
    /** 输出缓存队列 */
    private ByteBuffer[] mOutputBuffers;
    /** 解码回调 **/
    private DeCodecBufferListener mOnDecodecListener;
    /** 当前在播放的时间 **/
    private long mPlayTimeUs = 0;

    public RPMediaVideoDecodecFBF(String mPath, Surface surface) {
        this.mPath = mPath;
        this.mSurface = surface;
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mPath);
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    mTrackIndex = i;
                    mMime = mime;
                    mMediaFormat = mediaFormat;
                }
            }
            mExtractor.selectTrack(mTrackIndex);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, mMime + " 分离器初始化失败");
        }

        try {
            mMediaCodec = MediaCodec.createByCodecName("OMX.google.h264.decoder");
            mMediaCodec.configure(mMediaFormat, surface, null, 0);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, mMime + " 解码器器初始化失败");
        }

        /** 开启解码线程 **/
        mOutputBuffers = mMediaCodec.getOutputBuffers();
    }

    /** 获取视频的MediaFormat **/
    public MediaFormat getMediaFormat() {
        return mMediaFormat;
    }

    /** 设置解码回调 **/
    public void setOnDecodecListener(DeCodecBufferListener onDecodecListener) {
        this.mOnDecodecListener = onDecodecListener;
    }

    /** 刷新解码器 **/
    public void flush(){
        if(mMediaCodec == null)return;
        mMediaCodec.flush();
    }

    /** 当前正在播放的buffer时间 **/
    public long getPlayTimeUs() {
        return mPlayTimeUs;
    }

    /** 喂入一帧数据并且跳转到下一帧 **/
    public void advans(){
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(mTimeOutUs);

        if (inputBufferIndex > 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize > 0) {
                long timeStampUs = mExtractor.getSampleTime();
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, timeStampUs, 0);
                mExtractor.advance();
            } else {
                //如果sampleSize < 0 则代表为最后一帧
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
        }
    }

    /** 获取一帧数据并且显示 **/
    public void getFrameData(){
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, mTimeOutUs);

        switch (outputBufferIndex) {
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                Log.e(TAG, "Out put format changed ");
                if (mOnDecodecListener != null) mOnDecodecListener.onExtractor(mMediaCodec.getOutputFormat());
                break;
            }
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                // 直接渲染到Surface时使用不到outputBuffer
                mOutputBuffers = mMediaCodec.getOutputBuffers();
                break;
            default: {
                if (outputBufferIndex < 0) {
                    break;
                }

                if (bufferInfo.size > 0) {
                    ByteBuffer outputBuffer = mOutputBuffers[outputBufferIndex];
                    if (mOnDecodecListener != null) mOnDecodecListener.onDecodecBuffer(outputBuffer, bufferInfo);
                }
                //送显
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            }
        }

        mPlayTimeUs = bufferInfo.presentationTimeUs;

        //编码到结束
        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
            if (mOnDecodecListener != null) mOnDecodecListener.onDecodecCompleted(bufferInfo);
        }
    }

}
