package com.tencent.tavmedia.demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

public class AudioTrackWrapper {

    private static final String TAG = "AudioTrackWrapper";
    private AudioTrack mAudioTrack;

    public AudioTrackWrapper(MediaFormat mediaFormat) {
        try {
            init(mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


   /* public class AudioSample {
        public AudioSample(byte[] data, MediaCodec.BufferInfo bufferInfo) {
            this.data = data;
            this.bufferInfo = bufferInfo;
        }

        byte[] data;
        MediaCodec.BufferInfo bufferInfo;

        public byte[] getData() {
            return data;
        }

        public int getSize() {
            if (bufferInfo != null) {
                return bufferInfo.size;
            }
            return bufferInfo.size;
        }

        public int getOffset() {
            if (bufferInfo != null) {
                return bufferInfo.offset;
            }
            return 0;
        }

        @Override
        public String toString() {
            return "AudioSample{" +
                    "size=" + data.length +
                    ", bufferInfo=" + bufferInfo.presentationTimeUs +
                    '}';
        }
    }
*/


    public AudioTrackWrapper(int rate, int channelCount) {
        try {
            init(rate, channelCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * 初始化音乐播放器，
     *
     * @param sampleRateInHz 采样率
     * @param channelCount
     * @throws Exception
     */
    private void init(int sampleRateInHz, int channelCount) throws Exception {

        if (sampleRateInHz <= 0) {
            return;
        }

        AudioTrackConfig audioTrackConfig = new AudioTrackConfig(sampleRateInHz, channelCount);
        Log.d(TAG, "init:--> " + this);

        //int streamType, int sampleRateInHz, int channelConfig, int audioFormat,int bufferSizeInBytes, int mode}
        try {
            mAudioTrack = new AudioTrack(
                    audioTrackConfig.streamType,
                    audioTrackConfig.sampleRateInHz,
                    audioTrackConfig.channelConfig,
                    audioTrackConfig.audioFormat,
                    audioTrackConfig.bufferSizeInBytes,
                    audioTrackConfig.mode
            );
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            mAudioTrack = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.play();
        }
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

        public AudioTrackConfig(int sampleRateInHz, int channelCount) {
            streamType = AudioManager.STREAM_MUSIC;//音乐
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;//双声道
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;//只有16所有设备才支持
            mode = AudioTrack.MODE_STREAM;//使用流类型
            this.sampleRateInHz = getSampleRateInHz(sampleRateInHz, channelCount);
//            this.bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            // 固定设置为8192（每次读取的数据长度目前固定为8192字节）的整数倍，避免因为长度不同导致出现杂音
            this.bufferSizeInBytes = 8192;

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
