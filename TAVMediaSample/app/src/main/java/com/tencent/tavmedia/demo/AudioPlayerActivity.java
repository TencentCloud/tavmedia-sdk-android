package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVAudioFrame;
import com.tencent.tavmedia.TAVAudioReader;
import com.tencent.tavmedia.TAVMovie;
import com.tencent.tavmedia.TAVMovieAsset;

// 音频播放
public class AudioPlayerActivity extends AppCompatActivity {

    private static final String TAG = "AudioPlayerActivity";

    private TAVAudioReader audioReader;
    private AudioTrackWrapper audioTrackWrapper;
    private boolean isPlaying = false;

    public static void start(Context context) {
        Intent starter = new Intent(context, AudioPlayerActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        new Thread(new AudioRenderRunnable()).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isPlaying = false;
    }

    private class AudioRenderRunnable implements Runnable {

        @Override
        public void run() {

            if (audioReader == null) {
                initData();
            }
            while (isPlaying) {
                long startTime = System.currentTimeMillis();
                TAVAudioFrame frame = audioReader.readNextFrame();
                audioTrackWrapper.writeData(frame.data, 0, (int) frame.length);
                long timeCons = System.currentTimeMillis() - startTime;
                Log.d(TAG, "run: timeCons = " + timeCons);

                long frameDurationMs = frame.duration / 1000;
                if (timeCons < frameDurationMs) {
                    trySleep(frameDurationMs - timeCons);
                }
            }
        }

        void initData() {
            String path = MainActivity.SELECT_DATA.get(0).mPath;
            Log.d("Log", "path is " + path);
            TAVMovieAsset asset = TAVMovieAsset.MakeFromPath(path);
            TAVMovie media = TAVMovie.MakeFrom(asset, 0, asset.duration());
            media.setDuration(asset.duration());
            audioReader = TAVAudioReader.Make(media);

            audioTrackWrapper = new AudioTrackWrapper(44100, 2);
            audioTrackWrapper.setVolume(1);

            isPlaying = true;
        }

        private void trySleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}