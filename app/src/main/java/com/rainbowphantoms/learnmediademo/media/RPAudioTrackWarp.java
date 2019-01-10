package com.rainbowphantoms.learnmediademo.media;

import android.annotation.TargetApi;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * 作者： MirsFang on 2019/1/2 10:47
 * 邮箱： mirsfang@163.com
 * 类描述：
 */
public class RPAudioTrackWarp {

    private static final String TAG = "TuSdkAudioTrackWrap";

    /** 最小音频间隔时间戳 **/
    private static final int MIN_TIMESTAMP_SAMPLE_INTERVAL_US = 250000;
    /** 音频播放器 **/
    private AudioTrack mAudioTrack;
    /** 暂停时长 **/
    private long pasueTimeNs = -1;
    /** 音频的PTS **/
    private long mAudioBufferPts = 0;
    private long mVideoBufferPts = 0;
    /** 第一帧的时差 **/
    private long mDiffSyncTimeUs = 0;

    /*** 提高精度 ***/
    /** LatencyMethod **/
    private Method mLatencyMethod;
    /** 最后一帧采样时间戳 **/
    private long mLastTimestampSampleTimeUs;
    /** 播放时延 **/
    private long mLatencyUs;
    /** AudioTimeStamp **/
    private AudioTimestamp mAudioTimestamp;
    private long mSampleRate;
    private long mSeekPts = 0;

    /** 设置音频播放器 **/
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setAudioTrack(AudioTrack audioTrack, long sampleRate) {
        this.mAudioTrack = audioTrack;
        try {
            mLatencyMethod = AudioTrack.class.getMethod("getLatency", (Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        mAudioTimestamp = new AudioTimestamp();
        this.mSampleRate = sampleRate;
    }

    /** 获取视频显示时间 **/
    public long getAudioPlayTimeUs(){
        int numFramesPlayed = mAudioTrack.getPlaybackHeadPosition();
        if (mLatencyMethod != null) {
            try {
                mLatencyUs = (Integer) mLatencyMethod.invoke(mAudioTrack, (Object[]) null) * 1000L /2;
                mLatencyUs = Math.max(mLatencyUs, 0);
            } catch (Exception e){
                mLatencyMethod = null;
            }
        }

        long playUs = mSeekPts + (numFramesPlayed * 1000000L) / mSampleRate - mLatencyUs;
        return playUs;
    }

    public void setSeekPts(long audioPts){
        mSeekPts = audioPts;
    }

    /** 暂停 **/
    public void pause() {
    }

    /** 恢复 **/
    public void resume() {
    }

    /** 重置 **/
    public void reset() {
    }


}
