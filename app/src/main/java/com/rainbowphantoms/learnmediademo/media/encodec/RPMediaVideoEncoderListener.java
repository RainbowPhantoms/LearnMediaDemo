package com.rainbowphantoms.learnmediademo.media.encodec;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * 作者： MirsFang on 2019/1/9 15:41
 * 邮箱： mirsfang@163.com
 * 类描述：视频编码回调类
 */
public interface RPMediaVideoEncoderListener {

    /**
     * 添加视频编码轨道
     * @return 轨道下标
     */
    int addVideoTrack(MediaFormat videoFormat);

    /**
     * 写入合成器数据
     * @param trackIndex    下标
     * @param bufferInfo    buffer信息
     * @param encodedData   数据
     */
    void writeVideoSampleData(int trackIndex, MediaCodec.BufferInfo bufferInfo, ByteBuffer encodedData);

}
