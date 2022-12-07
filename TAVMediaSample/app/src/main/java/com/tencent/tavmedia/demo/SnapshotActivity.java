package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.opengl.EGL14;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVSurface;
import com.tencent.tavmedia.TAVVideoReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class SnapshotActivity extends AppCompatActivity {

    private static final String TAG = "SnapshotActivity";
    private int imageIndex = 0;
    private ImageView imageView;
    private long lastFrameTimeMs;

    public static void start(Context context) {
        Intent starter = new Intent(context, SnapshotActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapshot);
        imageView = findViewById(R.id.imageView);

        TAVComposition composition = Utils.makeComposition(MainActivity.SELECT_DATA, 400, 300);
        TAVVideoReader videoReader = TAVVideoReader.Make(composition);

        ImageReader imageReader = ImageReader
                .newInstance(composition.width(), composition.height(), PixelFormat.RGBA_8888, 2);

        HandlerThread handlerThread = new HandlerThread("snapshot_thread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        imageReader.setOnImageAvailableListener(new MyOnImageAvailableListener(videoReader, composition), handler);

        videoReader.setSurface(TAVSurface.FromSurface(imageReader.getSurface(), EGL14.EGL_NO_CONTEXT));
        lastFrameTimeMs = System.currentTimeMillis();
        videoReader.readNextFrame();
    }

    private class MyOnImageAvailableListener implements OnImageAvailableListener {

        private final TAVVideoReader videoReader;
        private final TAVComposition composition;
        private long positionTimeUs = 0;

        public MyOnImageAvailableListener(TAVVideoReader videoReader, TAVComposition composition) {
            this.videoReader = videoReader;
            this.composition = composition;
        }

        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.d(TAG, "onImageAvailable: timeCons = " + (System.currentTimeMillis() - lastFrameTimeMs));
            lastFrameTimeMs = System.currentTimeMillis();
            Image image = imageReader.acquireNextImage();
            setToImage(image, imageReader.getWidth(), imageReader.getHeight());
            image.close();
            // 每隔一秒，截取一帧
            positionTimeUs += 1_000_000;
            if (positionTimeUs <= composition.duration()) {
                long startTimeMs = System.currentTimeMillis();
                videoReader.seekTo(positionTimeUs);
                videoReader.readNextFrame();
                Log.d(TAG, "onImageAvailable: readFrame timeCons = " + (System.currentTimeMillis() - startTimeMs));
            }
        }

        private void setToImage(Image image, int width, int height) {
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            imageView.setImageBitmap(bitmap);
            saveToFile(bitmap);
        }

        private void saveToFile(Bitmap bitmap) {
            try {
                File file = Utils.createNewFile(Utils.SNAPSHOT_DIR, "snapshot_" + imageIndex++ + ".png");
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
                os.close();
                bitmap.recycle();
            } catch (IOException e) {
                Log.e(TAG, "saveToFile: ", e);
            }
        }
    }
}