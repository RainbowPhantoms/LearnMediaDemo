package com.rainbowphantoms.learnmediademo.media;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.annotation.IntRange;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC;

/**
 * 作者： MirsFang on 2018/12/31 00:28
 * 邮箱： mirsfang@163.com
 * 类描述：媒体解码
 */
public class RPMediaCodec {
    private static final String TAG = "RPMediaCodec";

    public static final int VIDEO = 2;
    public static final int AUDIO = 3;


    public interface OnDeCodecOutputBufferListener{

        void extractBuffer(MediaExtractor extractor);

        void onFormat(MediaFormat mediaFormat);

        void onDecodecBuffer(ByteBuffer byteBuffer ,MediaCodec.BufferInfo bufferInfo);

        void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo);

        void onSeekResult(long timeUs);
    }

    /** 默认解码器超时事件 **/
    private int mTimeOutUs = 5000;
    /** 媒体分离器 **/
    private MediaExtractor mExtractor;
    /** 解码器 **/
    private MediaCodec mMediaCodec;
    /**  MIME标志 **/
    private String mMime;
    /** 当前轨道下标 **/
    private int mTrackIndex;
    /** 当前媒体信息 **/
    private MediaFormat mMediaFormat;
    /** 当前视频路径 **/
    private String mPath;
    /** 当前解码器类型 **/
    private int mType;
    /** 显示表面 **/
    private Surface mSurface;
    /** 解码线程 **/
    private _CodecThread mDecodecThread;
    /** 输出缓存队列 */
    private ByteBuffer[] mOutputBuffers;
    private OnDeCodecOutputBufferListener mOnDecodecListener;
    /** 是否释放 **/
    private boolean isRelease = false;
    /** 是否暂停 **/
    private boolean isPause = true;
    /** Seek的时间 **/
    private long mSeekTimeUs = -1;
    /** 当前在播放的时间 **/
    private long mPlayTimeUs = 0;


    public RPMediaCodec(@IntRange(from = 2, to = 3) int type, String mPath, Surface surface) {
        this.mPath = mPath;
        this.mType = type;
        this.mSurface = surface;
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mPath);
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(type == VIDEO ? "video/" : "audio/")) {
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
            mMediaCodec = MediaCodec.createDecoderByType(mMime);
            mMediaCodec.configure(mMediaFormat,surface,null,0);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, mMime + " 解码器器初始化失败");
        }

        /** 开启解码线程 **/
        mDecodecThread = new _CodecThread();
        mDecodecThread.start();

        mOutputBuffers = mMediaCodec.getOutputBuffers();

    }


    public void setOnDecodecListener(OnDeCodecOutputBufferListener onDecodecListener) {
        this.mOnDecodecListener = onDecodecListener;
    }

    public void resume(){
       isPause = false;
    }

    public void pause(){
        isPause = true;
    }


    public void release(){
        this.isRelease = true;
    }


    public void seek(long seekTimeUs) {
        mSeekTimeUs = seekTimeUs;
    }

    /** 正在Seek **/
    public boolean isSeek(){
        return mSeekTimeUs > -1;
    }

    /** 当前正在播放的时间 **/
    public long getPlayTimeUs(){
        return mPlayTimeUs;
    }

    /** 媒体解码线程 **/
    class _CodecThread extends Thread{
        @Override
        public void run() {
            super.run();
            while (!Thread.interrupted() && !isRelease) {
                decodecUntilEnd();
            }
        }

        private void decodecUntilEnd() {

            if(mOnDecodecListener != null) mOnDecodecListener.extractBuffer(mExtractor);

            if(mSeekTimeUs > -1){
                mExtractor.seekTo(mSeekTimeUs,SEEK_TO_CLOSEST_SYNC);
                mMediaCodec.flush();
                mSeekTimeUs = -1;
                if(mOnDecodecListener != null)mOnDecodecListener.onSeekResult(mExtractor.getSampleTime());
                Log.e(TAG,"---------------- Seek "+(mType == 2?"Video   ":"Audio   ")+mExtractor.getSampleTime()+" ---------------");
            }



            /** ================ 喂入解码器数据 ================= **/
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(mTimeOutUs);

            if(inputBufferIndex < 0)return;

            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            int sampleSize = mExtractor.readSampleData(inputBuffer,0);
            if(sampleSize > 0){
                long timeStampUs = mExtractor.getSampleTime();
                mMediaCodec.queueInputBuffer(inputBufferIndex,0,sampleSize,timeStampUs,0);
                mExtractor.advance();
            }else {
                //如果sampleSize < 0 则代表为最后一帧
                mMediaCodec.queueInputBuffer(inputBufferIndex,0,0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }


            /** ================= 从编码器获取数据 =================== **/
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, mTimeOutUs);

            switch (outputBufferIndex){
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                    //FormatChange format改变可能会引起绿边问题
                    Log.e(TAG,"Out put format changed ");
                    if(mOnDecodecListener != null)mOnDecodecListener.onFormat(mMediaCodec.getOutputFormat());
                    break;
                }
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    // TLog.d("%s process Output Buffers Changed", TAG);
                    // 直接渲染到Surface时使用不到outputBuffer
                    mOutputBuffers = mMediaCodec.getOutputBuffers();
                    break;
                default: {
                    if (outputBufferIndex < 0) break;

                    if(bufferInfo.size > 0){
                        ByteBuffer outputBuffer = mOutputBuffers[outputBufferIndex];
                        if(mOnDecodecListener != null)mOnDecodecListener.onDecodecBuffer(outputBuffer,bufferInfo);
                    }
                    //送显
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex,mType == VIDEO);

                    while (!Thread.interrupted() && isPause){ }
                }

            }

            mPlayTimeUs = bufferInfo.presentationTimeUs;

            //编码到结束
            if(bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                if(mOnDecodecListener != null)mOnDecodecListener.onDecodecCompleted(bufferInfo);
            }
        }
    }
}
