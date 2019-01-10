package com.rainbowphantoms.learnmediademo;

import android.Manifest;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;

import com.rainbowphantoms.learnmediademo.component.audio.AudioPlayActivity;
import com.rainbowphantoms.learnmediademo.component.video.VideoPlayActivity;
import com.rainbowphantoms.learnmediademo.component.video.VideoPlayFbFActivity;
import com.rainbowphantoms.learnmediademo.media.RPAudioTrackWarp;
import com.rainbowphantoms.learnmediademo.media.decodec.RPMediaDeCodec;
import com.rainbowphantoms.learnmediademo.media.decodec.RPMediaSync;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private SurfaceView sufaceView;
    private EditText edSeek;
    private AppCompatSeekBar mSeekBar;

    private String mVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/lsq_cut_20190107_210249912.mp4";
//    private String mVideoPath = "/storage/emulated/0/DCIM/Camera/lsq_20190104_172151525.mp4";
    private RPMediaDeCodec mVideoCodec;
    private RPMediaDeCodec mAudioCodec;
    private AudioTrack mAudioTrack;
    private RPMediaSync mSync = new RPMediaSync();
    private RPAudioTrackWarp mAudioTrackWarp = new RPAudioTrackWarp();
    private MediaExtractor mVideoExtractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);

        Log.e(TAG ,"CPU : "+ Build.HARDWARE);

        initView();

    }

    public void onClick(View view){
        mVideoCodec = new RPMediaDeCodec(RPMediaDeCodec.VIDEO,mVideoPath,sufaceView.getHolder().getSurface());
        mVideoCodec.setOnDecodecListener(new RPMediaDeCodec.OnDeCodecOutputBufferListener() {
            @Override
            public void extractBuffer(MediaExtractor extractor) {
                mVideoExtractor = extractor;
            }

            @Override
            public void onFormat(MediaFormat mediaFormat) {
            }

            @Override
            public void onDecodecBuffer(ByteBuffer byteBuffer, final MediaCodec.BufferInfo bufferInfo) {
                mSync.setVideoTimeUs(bufferInfo.presentationTimeUs);

//                if(bufferInfo.presentationTimeUs >= 10 * 1000000){
//                    mVideoCodec.seek(4 * 1000000);
//                    return;
//                }


                long diff = mSync.getDiff();
                long pts = System.nanoTime() + diff * 1000 + mSync.getFirstDiff();

                if(mAudioTrack != null)
//                Log.e(TAG,"AudioTrakWarpTimeUs : "+mAudioTrackWarp.getAudioPlayTimeUs());

                while (!Thread.interrupted() && System.nanoTime() < pts && !mVideoCodec.isSeek()){
                }

                if(mVideoCodec.getMediaFormat() != null){
                    long duration = mVideoCodec.getMediaFormat().getLong(MediaFormat.KEY_DURATION);
                    final int percent = (int) (bufferInfo.presentationTimeUs/(float)duration * 100);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSeekBar.setProgress(percent);
                        }
                    });
                }


            }

            @Override
            public void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo) {

            }

            @Override
            public void onSeekResult(long timeUs) {
                mAudioCodec.seek(timeUs);
            }
        });

        mAudioCodec = new RPMediaDeCodec(RPMediaDeCodec.AUDIO,mVideoPath,null);
        mAudioCodec.setOnDecodecListener(new RPMediaDeCodec.OnDeCodecOutputBufferListener() {
            @Override
            public void extractBuffer(MediaExtractor extractor) {

            }

            @Override
            public void onFormat(MediaFormat mediaFormat) {

                if(mAudioTrack == null) {
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int channelConfig = channelCount < 2 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
                    //bit-width
                    int bitwidth = AudioFormat.ENCODING_PCM_16BIT;

//                    Log.e(TAG,"audio sampleRate : "+sampleRate+"  channelCount : "+channelCount+" bitwidth"+bitwidth);

                    int minBufferSize=AudioTrack.getMinBufferSize(sampleRate, channelConfig, bitwidth);
                    int inputBufferSize = minBufferSize * 4;
                    int frameSizeInBytes = channelCount * 2;
                    int bufferSize = (inputBufferSize / frameSizeInBytes) * frameSizeInBytes;
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channelConfig,bitwidth,bufferSize, AudioTrack.MODE_STREAM);
                    mAudioTrackWarp.setAudioTrack(mAudioTrack,sampleRate);
                    mAudioTrack.play();
                }
            }

            @Override
            public void onDecodecBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                mSync.setAudioTimeUs(bufferInfo.presentationTimeUs);
                byte[] bytes = new byte[byteBuffer.limit()];
                byteBuffer.get(bytes,0,byteBuffer.limit());
//                Log.e(TAG,"audio Decodec  : "+bufferInfo.presentationTimeUs);
                if(mAudioTrack != null)mAudioTrack.write(bytes,0, byteBuffer.limit());

               mAudioTrackWarp.getAudioPlayTimeUs();
            }

            @Override
            public void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo) {
//                Log.e(TAG,"audio DecodecCompleted : "+bufferInfo.presentationTimeUs);
            }

            @Override
            public void onSeekResult(long timeUs) {
                mAudioTrackWarp.setSeekPts(timeUs);
            }
        });
    }

    private void initView() {
        sufaceView =  findViewById(R.id.sufaceView);
        edSeek = findViewById(R.id.edSeek);
        mSeekBar = findViewById(R.id.seekBar);
        mSeekBar.setMax(100);
    }

    public void resume(View view) {
        mVideoCodec.resume();
        mAudioTrack.play();
        mAudioCodec.resume();
    }

    public void pause(View view) {
        mVideoCodec.pause();
        mAudioTrack.pause();
        mAudioTrack.flush();
        mAudioTrackWarp.setSeekPts(mAudioCodec.getPlayTimeUs());
        mAudioCodec.pause();
    }

    public void seek(View view){
        mVideoCodec.seek(Integer.valueOf(edSeek.getText().toString()) * 1000000);
        mAudioCodec.seek(Integer.valueOf(edSeek.getText().toString()) * 1000000);
//        pause(null);
    }

    /** 跳转到视频播放 **/
    public void gotoVideoPlay(View view) {
        Intent intent = new Intent(this,VideoPlayActivity.class);
        startActivity(intent);
    }

    /** 跳转到视频逐帧播放 **/
    public void gotoVideoFBFPlay(View view) {
        Intent intent = new Intent(this,VideoPlayFbFActivity.class);
        startActivity(intent);
    }

    /** 跳转到音频播放 **/
    public void gotoAudioPlay(View view) {
        Intent intent = new Intent(this,AudioPlayActivity.class);
        startActivity(intent);

    }

    /** 跳转到PCM播放 **/
    public void gotoPCMPlay(View view) {

    }
}
