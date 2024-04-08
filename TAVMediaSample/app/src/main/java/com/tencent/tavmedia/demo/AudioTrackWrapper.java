package com.tencent.tavmedia.demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

public class AudioTrackWrapper {

    private static final String TAG = "AudioTrackWrapper";
    private final AudioTrack mAudioTrack;

    public AudioTrackWrapper(int rate, int channelCount, int bufferSizeInBytes) {
        AudioTrackConfig audioTrackConfig = new AudioTrackConfig(rate, channelCount, bufferSizeInBytes);
        Log.d(TAG, "init:--> " + this);
        mAudioTrack = new AudioTrack(
                audioTrackConfig.streamType,
                audioTrackConfig.sampleRateInHz,
                audioTrackConfig.channelConfig,
                audioTrackConfig.audioFormat,
                audioTrackConfig.bufferSizeInBytes,
                audioTrackConfig.mode
        );
        mAudioTrack.play();
    }

    public boolean allow() {
        return mAudioTrack != null;
    }

    public void stop() {
        if (!allow()) {
            return;
        }

        int state = mAudioTrack.getState();
        if (state == AudioTrack.PLAYSTATE_PLAYING || mAudioTrack.getState() == AudioTrack.PLAYSTATE_PAUSED) {
            mAudioTrack.stop();
        }
    }

    public void parse() {
        if (!allow()) {
            return;
        }
        if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.pause();
        }
    }

    public void writeData(byte[] data, int offset, int length) {
        if (!allow()) {
            return;
        }
        if (mAudioTrack != null) {
            try {
                mAudioTrack.write(data, offset, length);
                if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    mAudioTrack.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void flush() {
        if (!allow()) {
            return;
        }
        try {
            if (mAudioTrack != null) {
                mAudioTrack.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "flush: ", e);
        }
    }

    public void release() {
        if (!allow()) {
            return;
        }
        stop();
        mAudioTrack.release();
        Log.d(TAG, "release:--> " + this);
    }

    public void setVolume(float volume) {
        if (!allow()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioTrack.setVolume(volume);
        } else {
            mAudioTrack.setStereoVolume(volume, volume);
        }

    }

    private static class AudioTrackConfig {

        //音乐类型
        int streamType;
        //采样率
        int sampleRateInHz;
        //双声道
        int channelConfig;
        //音频编码格式
        int audioFormat;
        //缓冲区大小
        int bufferSizeInBytes;
        //数据类型
        int mode;

        public AudioTrackConfig(int sampleRateInHz, int channelCount, int bufferSizeInBytes) {
            streamType = AudioManager.STREAM_MUSIC;//音乐
            if (channelCount == 1) {
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            }
            if (channelCount == 2) {
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;//双声道
            }
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;//只有16所有设备才支持
            mode = AudioTrack.MODE_STREAM;//使用流类型
            this.sampleRateInHz = getSampleRateInHz(sampleRateInHz, channelCount);
//            this.bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            // 固定设置为8192（每次读取的数据长度目前固定为8192字节）的整数倍，避免因为长度不同导致出现杂音
            this.bufferSizeInBytes = bufferSizeInBytes;

        }

        /**
         * 根据音轨数和频率来确定最终的帧率
         *
         * @param sampleRateInHz 帧率
         * @param channelCount 音轨数
         * @return 帧率
         */
        private int getSampleRateInHz(int sampleRateInHz, int channelCount) {
            if (channelCount == 1) {
                return sampleRateInHz / 2;
            }
            return sampleRateInHz;
        }

        @Override
        public String toString() {
            return "AudioTrackConfig{" +
                    "streamType=" + streamType +
                    ", sampleRateInHz=" + sampleRateInHz +
                    ", channelConfig=" + channelConfig +
                    ", audioFormat=" + audioFormat +
                    ", bufferSizeInBytes=" + bufferSizeInBytes +
                    ", mode=" + mode +
                    '}';
        }
    }


}
