package com.rainbowphantoms.learnmediademo.component.video;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;

import com.rainbowphantoms.learnmediademo.R;
import com.rainbowphantoms.learnmediademo.media.decodec.RPMediaDeCodec;

import java.nio.ByteBuffer;

public class VideoPlayActivity extends AppCompatActivity {

    /** 播放视图 **/
    private SurfaceView sufaceView;
    /** 解码器 **/
    private RPMediaDeCodec mVideoCodec;
    /** 视频格式 **/
    private MediaFormat mVideoMediaFormat;
    /** 帧间隔 **/
    private long mFrameInterval = 0;
    /** 上一帧的时间 **/
    private long mPreFrameTimeNS = 0;
    /** 视频路径 **/
    private String mVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/lsq_20190105_144604170.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        initView();
    }

    private void initView() {
        sufaceView = findViewById(R.id.sufaceView);


    }
    /** 记载视频 **/
    public void start(View view){
        mVideoCodec = new RPMediaDeCodec(RPMediaDeCodec.VIDEO,mVideoPath,sufaceView.getHolder().getSurface());
        mVideoCodec.setOnDecodecListener(new RPMediaDeCodec.OnDeCodecOutputBufferListener() {
            @Override
            public void extractBuffer(MediaExtractor extractor) {

            }

            @Override
            public void onFormat(MediaFormat mediaFormat) {
                mVideoMediaFormat = mediaFormat;
                //获取帧率MediaFormat可能获取不到帧率 设置一个默认的时间
                float frameRate = 30;
                if(mediaFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    frameRate = mediaFormat.getFloat(MediaFormat.KEY_FRAME_RATE);
                }
                //计算帧间隔
                mFrameInterval = (long) (1000000/frameRate);
            }

            @Override
            public void onDecodecBuffer(ByteBuffer byteBuffer, final MediaCodec.BufferInfo bufferInfo) {
                //同步视频
                snycVideo();
            }

            @Override
            public void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo) {
                /** 播放完成 **/
                mVideoCodec.pause();
                mVideoCodec.flush();
                mVideoCodec.seek(0);
            }

            @Override
            public void onSeekResult(long timeUs) {

            }
        });
    }

    /** 视频同步 **/
    private void snycVideo() {
        long currentFrameTimeNS = System.nanoTime();
        while (currentFrameTimeNS + mFrameInterval * 1000 >System.nanoTime()){
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /** 播放 **/
    public void play(View view){
        mVideoCodec.resume();
    }

    /** 暂停 **/
    public void pause(View view){
        mVideoCodec.pause();
    }

}
