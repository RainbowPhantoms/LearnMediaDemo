package com.rainbowphantoms.learnmediademo.media.decodec;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 媒体分离器
 * @author MirsFang
 */
public class RPMediaExactor {
    private static final String TAG = "RPMediaExactor";

    /** 媒体分离器 **/
    private MediaExtractor mExtractor;
    /** 视频录制 **/
    private String mVideoPath;
    private String mOutputFileName;

    /** 设置视频数据源 **/
    public void setVideoPath(String videoPath) {
        this.mVideoPath = videoPath;
    }

    /** 设置输出的文件名 **/
    public void outputFileName(String fileName){
        this.mOutputFileName = fileName;
    }

    /** 开始分离 **/
    public void startExtract(){
        mExtractor = new MediaExtractor();
        FileOutputStream videoOutputStream = null;
        FileOutputStream audioOutputStream = null;
        try {
            //初始化视频输出和音频输出的路径
            String outputVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + mOutputFileName + ".h264";
            String outputAudioPath = Environment.getExternalStorageDirectory().getAbsolutePath() + mOutputFileName + ".aac";

            //初始化文件输出流
            videoOutputStream = new FileOutputStream(outputVideoPath);
            audioOutputStream = new FileOutputStream(outputAudioPath);

            //向分离器里设置数据源
            mExtractor.setDataSource(mVideoPath);

            int videoTrackIndex = -1;
            int audioTrackIndex = -1;

            //获取音频轨道和视频轨道
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("video/")){
                   videoTrackIndex = i;
                }

                if(mime.startsWith("audio/")){
                    audioTrackIndex = i;
                }
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            //分离视频
            extractVideo(videoOutputStream, videoTrackIndex, byteBuffer);
            //分离音频
            extractAudio(audioOutputStream, audioTrackIndex, byteBuffer);

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            mExtractor.release();
            try {
                videoOutputStream.close();
                audioOutputStream.close();
            }catch (Exception e){
                e.printStackTrace();
            }
            Log.e(TAG,"out put end ");
        }

    }

    /**
     *
     * 分离视频数据
     *
     * @param videoOutputStream 视频输出流
     * @param videoTrackIndex   视频轨道下标
     * @param byteBuffer        buffer
     * @throws IOException
     */
    private void extractVideo(FileOutputStream videoOutputStream, int videoTrackIndex, ByteBuffer byteBuffer) throws IOException {
        if(videoTrackIndex > -1) {
            mExtractor.selectTrack(videoTrackIndex);
            while (true) {
                int bufferSize = mExtractor.readSampleData(byteBuffer, 0);
                if (bufferSize < 0) {
                    break;
                }
                //保存视频信道信息
                byte[] buffer = new byte[bufferSize];
                byteBuffer.get(buffer);
                videoOutputStream.write(buffer);
                byteBuffer.clear();
                mExtractor.advance();
            }
        }
    }

    /**
     * 分离音频数据
     *
     * @param audioOutputStream 输出文件流
     * @param audioTrackIndex   音频轨道下标
     * @param byteBuffer        buffer
     * @throws IOException
     */
    private void extractAudio(FileOutputStream audioOutputStream, int audioTrackIndex, ByteBuffer byteBuffer) throws IOException {
        if(audioTrackIndex > -1) {
            //切换到音频信道
            mExtractor.selectTrack(audioTrackIndex);
            while (true) {
                int bufferSize = mExtractor.readSampleData(byteBuffer, 0);

                if (bufferSize < 0) {
                    break;
                }
                //保存音频信息
                byte[] buffer = new byte[bufferSize];
                byteBuffer.get(buffer);
                /* aac添加adts */
                byte[] aacaudiobuffer = new byte[bufferSize + 7];
                addADTStoPacket(aacaudiobuffer, bufferSize + 7);
                System.arraycopy(buffer, 0, aacaudiobuffer, 7, bufferSize);
                audioOutputStream.write(aacaudiobuffer);
                byteBuffer.clear();
                mExtractor.advance();
            }
        }
    }


    private static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = getFreqIdx(44100);
        int chanCfg = 2; // CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    private static int getFreqIdx(int sampleRate) {
        int freqIdx;

        switch (sampleRate) {
            case 96000:
                freqIdx = 0;
                break;
            case 88200:
                freqIdx = 1;
                break;
            case 64000:
                freqIdx = 2;
                break;
            case 48000:
                freqIdx = 3;
                break;
            case 44100:
                freqIdx = 4;
                break;
            case 32000:
                freqIdx = 5;
                break;
            case 24000:
                freqIdx = 6;
                break;
            case 22050:
                freqIdx = 7;
                break;
            case 16000:
                freqIdx = 8;
                break;
            case 12000:
                freqIdx = 9;
                break;
            case 11025:
                freqIdx = 10;
                break;
            case 8000:
                freqIdx = 11;
                break;
            case 7350:
                freqIdx = 12;
                break;
            default:
                freqIdx = 8;
                break;
        }

        return freqIdx;
    }


}
