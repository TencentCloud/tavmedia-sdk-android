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

        //????????????
        int streamType;
        //?????????
        int sampleRateInHz;
        //?????????
        int channelConfig;
        //??????????????????
        int audioFormat;
        //???????????????
        int bufferSizeInBytes;
        //????????????
        int mode;

        public AudioTrackConfig(int sampleRateInHz, int channelCount, int bufferSizeInBytes) {
            streamType = AudioManager.STREAM_MUSIC;//??????
            if (channelCount == 1) {
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            }
            if (channelCount == 2) {
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;//?????????
            }
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;//??????16?????????????????????
            mode = AudioTrack.MODE_STREAM;//???????????????
            this.sampleRateInHz = getSampleRateInHz(sampleRateInHz, channelCount);
//            this.bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            this.bufferSizeInBytes = bufferSizeInBytes;

        }

        /**
         * ????????????????????????????????????????????????
         *
         * @param sampleRateInHz ??????
         * @param channelCount ?????????
         * @return ??????
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
