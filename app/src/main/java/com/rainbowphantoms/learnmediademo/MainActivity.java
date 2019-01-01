package com.rainbowphantoms.learnmediademo;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;

import com.rainbowphantoms.learnmediademo.media.RPMediaCodec;
import com.rainbowphantoms.learnmediademo.media.RPMediaSync;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private SurfaceView sufaceView;
    private EditText edSeek;
    private String mVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/outputTranscode.mp4";
    private RPMediaCodec mVideoCodec;
    private RPMediaCodec mAudioCodec;
    private AudioTrack mAudioTrack;
    private RPMediaSync mSync = new RPMediaSync();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);

        initView();

    }

    public void onClick(View view){
        mVideoCodec = new RPMediaCodec(RPMediaCodec.VIDEO,mVideoPath,sufaceView.getHolder().getSurface());
        mVideoCodec.setOnDecodecListener(new RPMediaCodec.OnDeCodecOutputBufferListener() {

            @Override
            public void extractBuffer(MediaExtractor extractor) {

            }

            @Override
            public void onFormat(MediaFormat mediaFormat) {

            }

            @Override
            public void onDecodecBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                mSync.setVideoTimeUs(bufferInfo.presentationTimeUs);

                if(bufferInfo.presentationTimeUs >= 10 * 1000000){
                    mVideoCodec.seek(4 * 1000000);
                    return;
                }

                long diff = mSync.getDiff();
                long pts = System.nanoTime() + diff * 1000 + mSync.getFirstDiff();

                while (!Thread.interrupted() && System.nanoTime() < pts && !mVideoCodec.isSeek()){
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

        mAudioCodec = new RPMediaCodec(RPMediaCodec.AUDIO,mVideoPath,null);
        mAudioCodec.setOnDecodecListener(new RPMediaCodec.OnDeCodecOutputBufferListener() {
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

                    Log.e(TAG,"audio sampleRate : "+sampleRate+"  channelCount : "+channelCount+" bitwidth"+bitwidth);

                    int minBufferSize=AudioTrack.getMinBufferSize(sampleRate, channelConfig, bitwidth);
                    int inputBufferSize = minBufferSize * 4;
                    int frameSizeInBytes = channelCount * 2;
                    int bufferSize = (inputBufferSize / frameSizeInBytes) * frameSizeInBytes;
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channelConfig,bitwidth,bufferSize, AudioTrack.MODE_STREAM);
                    mAudioTrack.play();
                }
            }

            @Override
            public void onDecodecBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                mSync.setAudioTimeUs(bufferInfo.presentationTimeUs);
                byte[] bytes = new byte[byteBuffer.limit()];
                byteBuffer.get(bytes,0,byteBuffer.limit());
                Log.e(TAG,"audio Decodec  : "+bufferInfo.presentationTimeUs);
                if(mAudioTrack != null)mAudioTrack.write(bytes,0, byteBuffer.limit());
            }

            @Override
            public void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo) {
                Log.e(TAG,"audio DecodecCompleted : "+bufferInfo.presentationTimeUs);
            }

            @Override
            public void onSeekResult(long timeUs) {

            }
        });
    }

    private void initView() {
        sufaceView =  findViewById(R.id.sufaceView);
        edSeek = findViewById(R.id.edSeek);
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
        mAudioCodec.pause();
    }

    public void seek(View view){
        mVideoCodec.seek(Integer.valueOf(edSeek.getText().toString()) * 1000000);
//        mAudioCodec.seek(Integer.valueOf(edSeek.getText().toString()) * 1000000);
        pause(null);
    }
}
