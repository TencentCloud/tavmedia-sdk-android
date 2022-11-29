///////////////////////////////////////////////////////////////////////////////////////////////////
//
//  The MIT License (MIT)
//
//  Copyright (c) 2016-present, Tencent. All rights reserved.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
//  and associated documentation files (the "Software"), to deal in the Software without
//  restriction, including without limitation the rights to use, copy, modify, merge, publish,
//  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
//  Software is furnished to do so, subject to the following conditions:
//
//      The above copyright notice and this permission notice shall be included in all copies or
//      substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
//  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package com.tencent.tavmedia.demo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import com.tencent.tavmedia.TAVAudioFrame;
import com.tencent.tavmedia.TAVAudioReader;
import com.tencent.tavmedia.TAVMovie;
import com.tencent.tavmedia.TAVSurface;
import com.tencent.tavmedia.TAVVideoReader;

public class TAVTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = "TAVTextureView";
    private final int fps = 20;
    private final int frameDurationMs = 1000 / fps;
    private boolean isAttachedToWindow = false;
    private TAVSurface mediaSurface;
    private TAVVideoReader videoReader;
    private TAVMovie media;
    private boolean isPlaying = false;


    public TAVTextureView(Context context) {
        this(context, null);
    }

    public TAVTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOpaque(false);
        setSurfaceTextureListener(this);
        setKeepScreenOn(true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mediaSurface != null) {
            mediaSurface.release();
            mediaSurface = null;
        }
        if (videoReader != null) {
            videoReader.release();
            videoReader = null;
        }

        mediaSurface = TAVSurface.FromSurfaceTexture(surface);
        if (!isPlaying) {
            play();
        }
    }

    public void setMedia(TAVMovie media) {
        if (media == null) {
            throw new RuntimeException("media is null");
        }
        this.media = media;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (isPlaying) {
            stop();
        }
        if (mediaSurface != null) {
            mediaSurface.release();
            mediaSurface = null;
            videoReader = null;
        }
        if (videoReader != null) {
            videoReader.release();
            videoReader = null;
        }
        if (!isPlaying) {
            stop();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    protected void onAttachedToWindow() {
        isAttachedToWindow = true;
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        isAttachedToWindow = false;
        super.onDetachedFromWindow();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void stop() {
        isPlaying = false;
    }

    public void play() {
        if (!isAttachedToWindow) {
            return;
        }
        isPlaying = true;
        new Thread(new RenderRunnable(), "tav_demo_play_thread").start();
        new Thread(new AudioRenderRunnable(), "tav_demo_play_audio_thread").start();
    }


    private class RenderRunnable implements Runnable {

        private long positionUs;

        @Override
        public void run() {
            if (mediaSurface == null) {
                return;
            }
            videoReader = TAVVideoReader.Make(media, fps);
            if (videoReader == null) {
                return;
            }
            videoReader.setSurface(mediaSurface);
            runLoop(videoReader);
        }

        private void runLoop(TAVVideoReader videoReader) {
            while (isPlaying()) {
                long startTime = System.currentTimeMillis();
                videoReader.readNextFrame();
                long timeCons = System.currentTimeMillis() - startTime;
                positionUs += frameDurationMs;
                if (positionUs >= media.duration() / 1000) {
                    videoReader.seekTo(0);
                    positionUs = 0;
                }
                Log.d(TAG, "video read: timeCons = " + timeCons + ", positionUs = " + positionUs + ", duration = " + media.duration());
                if (timeCons < frameDurationMs) {
                    trySleep(frameDurationMs - timeCons);
                }
            }
        }


        private void trySleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class AudioRenderRunnable implements Runnable {

        @Override
        public void run() {

            TAVAudioReader audioReader = TAVAudioReader.Make(media);
            AudioTrackWrapper audioTrackWrapper = new AudioTrackWrapper(44100, 2);
            audioTrackWrapper.setVolume(1);
            // 开始
            runLoop(audioReader, audioTrackWrapper);
            // 结束
            audioTrackWrapper.release();
        }

        private void runLoop(TAVAudioReader audioReader, AudioTrackWrapper audioTrackWrapper) {
            if (audioReader == null || audioTrackWrapper == null) {
                return;
            }
            while (isPlaying()) {
                long startTime = System.currentTimeMillis();
                TAVAudioFrame frame = audioReader.readNextFrame();
                audioTrackWrapper.writeData(frame.data, 0, (int) frame.length);
                if (frame.timestamp >= media.duration()) {
                    audioReader.seekTo(0);
                }
                long timeCons = System.currentTimeMillis() - startTime;
                Log.i(TAG, "audio read: timeCons = " + timeCons + ", timestamp = " + frame.timestamp + ", duration = "
                        + frame.duration);

                long frameDurationMs = frame.duration / 1000;
                if (timeCons < frameDurationMs) {
                    trySleep(frameDurationMs - timeCons);
                }
            }
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
