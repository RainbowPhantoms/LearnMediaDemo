package com.rainbowphantoms.learnmediademo.media;

/**
 * 作者： MirsFang on 2018/12/31 10:42
 * 邮箱： mirsfang@163.com
 * 类描述：音视频同步器
 */
public class RPMediaSync {

    public long mAudioTimeUs;
    public long mVideoTimeUs;
    private long firstDiff = -1;

    public void setAudioTimeUs(long audioTimeUs){
        this.mAudioTimeUs = audioTimeUs;
    }

    public void setVideoTimeUs(long videoTimeUs){
        this.mVideoTimeUs = videoTimeUs;
    }

    public long getDiff(){
        if(firstDiff < 0){
            firstDiff =   mVideoTimeUs - mAudioTimeUs;
        }
        return mVideoTimeUs - mAudioTimeUs;
    }

    public long getFirstDiff(){
        return firstDiff;
    }


}
