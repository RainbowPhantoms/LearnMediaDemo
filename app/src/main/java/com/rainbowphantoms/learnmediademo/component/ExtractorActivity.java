package com.rainbowphantoms.learnmediademo.component;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.rainbowphantoms.learnmediademo.R;
import com.rainbowphantoms.learnmediademo.media.decodec.RPMediaExtractor;

public class ExtractorActivity extends AppCompatActivity {

    private String mVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/output.mp4";
    private RPMediaExtractor mExtractor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extractor);
        mExtractor = new RPMediaExtractor();
        mExtractor.setVideoPath(mVideoPath);
        mExtractor.setOutputFileName("extractor");
    }

    public void startExtractor(View view) {
        mExtractor.startExtract();
    }
}
