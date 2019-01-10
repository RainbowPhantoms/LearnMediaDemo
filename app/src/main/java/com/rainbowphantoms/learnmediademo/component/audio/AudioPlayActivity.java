package com.rainbowphantoms.learnmediademo.component.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.rainbowphantoms.learnmediademo.R;
import com.rainbowphantoms.learnmediademo.media.decodec.RPMediaDeCodec;

import java.nio.ByteBuffer;

/** 音频播放 **/
public class AudioPlayActivity extends AppCompatActivity {
    private static final String TAG = "AudioPlayActivity";
    private String mPath = Environment.getExternalStorageDirectory().getPath()+"/output.mp4";

    /** 拖动进度视图 **/
    private AppCompatSeekBar lsqSeekBar;
    /** 音频解码 **/
    private RPMediaDeCodec mAudioDecodec;
    /** AudioTrack **/
    private AudioTrack mAudioTrack;

    /** SeekBar 进度回调 **/
    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if(mAudioDecodec == null)return;
            mAudioTrack.pause();
            mAudioDecodec.pause();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mAudioDecodec == null)return;
            mAudioTrack.flush();
            mAudioDecodec.seek((long) (seekBar.getProgress() / (float)100 * mAudioDecodec.getTotalTimeUs()));
            mAudioDecodec.resume();
            mAudioTrack.play();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_play);
        initView();
    }

    private void initView() {
        lsqSeekBar =  findViewById(R.id.lsq_seekBar);
        lsqSeekBar.setMax(100);
        lsqSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
    }

    /**
     * 加载音频
     **/
    public void loadAudio(View view) {
        mAudioDecodec = new RPMediaDeCodec(RPMediaDeCodec.AUDIO,mPath,null);
        mAudioDecodec.setOnDecodecListener(new RPMediaDeCodec.OnDeCodecOutputBufferListener() {
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

                    int minBufferSize=AudioTrack.getMinBufferSize(sampleRate, channelConfig, bitwidth);
                    int inputBufferSize = minBufferSize * 4;
                    int frameSizeInBytes = channelCount * 2;
                    int bufferSize = (inputBufferSize / frameSizeInBytes) * frameSizeInBytes;
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channelConfig,bitwidth,bufferSize, AudioTrack.MODE_STREAM);
                    mAudioTrack.play();
                }
            }

            @Override
            public void onDecodecBuffer(ByteBuffer byteBuffer, final MediaCodec.BufferInfo bufferInfo) {
                byte[] bytes = new byte[byteBuffer.limit()];
                byteBuffer.get(bytes,0,byteBuffer.limit());
                Log.d(TAG,"audio Decodec OutputBufferTimeUs  : "+bufferInfo.presentationTimeUs);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int percent = (int) (bufferInfo.presentationTimeUs/(float)mAudioDecodec.getTotalTimeUs() * 100);
                        Log.d(TAG,"AudioProgress : "+percent);
                        lsqSeekBar.setProgress(percent);
                    }
                });
                if(mAudioTrack != null)mAudioTrack.write(bytes,0, byteBuffer.limit());

            }

            @Override
            public void onDecodecCompleted(MediaCodec.BufferInfo bufferInfo) {
                mAudioTrack.pause();
                mAudioTrack.flush();
                mAudioDecodec.flush();
                mAudioDecodec.seek(0);
                mAudioDecodec.pause();
            }

            @Override
            public void onSeekResult(final long timeUs) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int percent = (int) (timeUs/(float)mAudioDecodec.getTotalTimeUs() * 100);
                        Log.d(TAG,"AudioProgress : "+percent);
                        lsqSeekBar.setProgress(percent);
                    }
                });
            }
        });
    }

    /**
     * 播放音频
     **/
    public void playAudio(View view) {
        mAudioDecodec.resume();
        if(mAudioTrack != null)mAudioTrack.play();
    }

    /**
     * 暂停音频
     **/
    public void pauseAudio(View view) {
        mAudioDecodec.pause();
        if(mAudioTrack != null)mAudioTrack.pause();
    }

    /**
     * 跳转到开头
     **/
    public void seekAudio(View view) {
        mAudioDecodec.seek(0);
    }


}
