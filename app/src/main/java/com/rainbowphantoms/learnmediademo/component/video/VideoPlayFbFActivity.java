package com.rainbowphantoms.learnmediademo.component.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import com.rainbowphantoms.learnmediademo.R;
import com.rainbowphantoms.learnmediademo.media.encodec.RPMediaVideoDecodecFBF;

import java.nio.ByteBuffer;

/** 逐帧播放视频 **/
public class VideoPlayFbFActivity extends AppCompatActivity {

    /** 播放视图 **/
    private SurfaceView sufaceView;
    /** 解码器 **/
    private RPMediaVideoDecodecFBF mVideoCodec;
    /** 视频路径 **/
    private String mVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/output.mp4";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play_fb_f);

        initView();
    }

    private void initView() {
        sufaceView = findViewById(R.id.sufaceView);
    }

    public void start(View view) {
        mVideoCodec = new RPMediaVideoDecodecFBF(mVideoPath,sufaceView.getHolder().getSurface());
        mVideoCodec.setOnDecodecListener(new RPMediaVideoDecodecFBF.DeCodecBufferListener() {
            @Override
            public void onExtractor(MediaFormat mediaFormat) {

            }

            @Override
            public void onDecodecBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {


            }

            @Override
            public void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo) {
            }

        });

    }

    public void input(View view) {
        mVideoCodec.advans();
    }

    public void output(View view) {
        mVideoCodec.getFrameData();
    }
}
