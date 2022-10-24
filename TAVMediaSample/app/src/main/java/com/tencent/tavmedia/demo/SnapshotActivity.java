package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVVideoReader;

public class SnapshotActivity extends AppCompatActivity {

    private static final String TAG = "Snapshot-seekfix-";
    private TextureView textureView;

    public static void start(Context context) {
        Intent starter = new Intent(context, SnapshotActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapshot);
        textureView = findViewById(R.id.textureView);
        textureView.setKeepScreenOn(true);
        textureView.setSurfaceTextureListener(new MySurfaceTextureListener());
    }

    private void initReader(int width, int height) {
        TAVComposition composition = Utils.makeComposition(MainActivity.SELECT_DATA, width, height);
        TAVVideoReader reader = TAVVideoReader.Make(composition);
        long startTimeMs = System.currentTimeMillis();
        // 每隔一秒，截取一帧
        for (long positionTimeUs = 0; positionTimeUs < composition.duration(); positionTimeUs += 1_000_000) {

            reader.seekTo(positionTimeUs);
            reader.readNextFrame();
        }
        Log.d(TAG, "total time cons = " + (System.currentTimeMillis() - startTimeMs));
        reader.setSurface(null);

    }

    private class MySurfaceTextureListener implements SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            new Thread(() -> initReader(width, height)).start();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

}