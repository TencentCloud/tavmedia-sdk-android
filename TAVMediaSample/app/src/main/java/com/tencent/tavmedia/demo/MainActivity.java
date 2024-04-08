package com.tencent.tavmedia.demo;

import android.Manifest.permission;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.mylhyl.acp.Acp;
import com.mylhyl.acp.AcpListener;
import com.mylhyl.acp.AcpOptions;
import com.mylhyl.acp.AcpOptions.Builder;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static List<MediaItem> SELECT_DATA;

    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGES_AND_VIDEOS_REQUEST = 10011;

    private Runnable onSelectedVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.initCacheDir(this);
        requestPermissions();
    }

    private void onAssetsCopyDone() {
        // 这里可以放启动app直接进入的逻辑
//        try {
//            SELECT_DATA = Collections.singletonList(
//                    new MediaItem(OUT_SAVE_DIR + "video-640x360.mp4"));
//        } catch (InvalidImageException e) {
//            Log.e(TAG, "onCreate: new MediaItem ", e);
//        }
//        MultiClipActivity.start(this);
    }

    public void jumpExportActivity(View view) {
        selectVideo(false, () -> ExportActivity.start(MainActivity.this));
    }

    public void jumpAudioPlayerActivity(View view) {
        selectVideo(true, () -> AudioPlayerActivity.start(MainActivity.this));
    }

    public void jumpTemplateActivity(View view) {
        TemplateActivity.start(this);
    }

    public void jumpMultiClipActivity(View view) {
        selectVideo(false, () -> MultiClipActivity.start(MainActivity.this));
    }

    public void jumpSnapshotActivity(View view) {
        selectVideo(false, () -> SnapshotActivity.start(MainActivity.this));
    }

    public void jumpUpdateRenderSizeActivity(View view) {
        selectVideo(false, () -> UpdateRenderSizeActivity.start(MainActivity.this));
    }

    public void jumpTextActivity(View view) {
        this.startActivity(new Intent("com.tencent.tavmedia.demo.test.TextActivity"));
    }

    public void jumpColorTuningActivity(View view) {
        selectVideo(false, () -> ColorTuningActivity.start(MainActivity.this));
    }

    public void jumpSerializableActivity(View view) {
        SerializableActivity.start(MainActivity.this);
    }

    public void jumpPAGTemplateActivity(View view) {
        Toast.makeText(this, "请选3个素材", Toast.LENGTH_LONG).show();
        selectVideo(false, () -> PAGTemplateActivity.start(MainActivity.this));
    }

    public void jumpAutoTestActivity(View view) {
        AutoTestActivity.start(MainActivity.this);
    }

    private void selectVideo(boolean onlyVideo, Runnable onSelectedVideo) {
        this.onSelectedVideo = onSelectedVideo;
        Set<MimeType> typeSet = onlyVideo ? MimeType.ofVideo() : MimeType.ofAll();
        Matisse.from(this)
                .choose(typeSet)
                .countable(true)
                .maxSelectable(10)
                .thumbnailScale(0.85f)
                .imageEngine(new GlideEngine())
                .forResult(PICK_IMAGES_AND_VIDEOS_REQUEST);

    }


    private void requestPermissions() {

        AcpOptions options = new Builder()
                .setPermissions(permission.WRITE_EXTERNAL_STORAGE, permission.READ_PHONE_STATE)
                .build();
        Acp.getInstance(this).request(options, new AcpListener() {
            @Override
            public void onGranted() {
                Log.i(TAG, "onGranted() called 权限ok");
                new AssetsCopyHelper(MainActivity.this, () -> onAssetsCopyDone()).run();
            }

            @Override
            public void onDenied(List<String> permissions) {
                Toast.makeText(MainActivity.this, "需要权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_AND_VIDEOS_REQUEST && resultCode == RESULT_OK && data != null) {
            // 处理选中的图片和视频
            SELECT_DATA = new ArrayList<>();
            List<Uri> selectedUris = Matisse.obtainResult(data);
            for (Uri uri : selectedUris) {
                String mimeType = getContentResolver().getType(uri);
                MediaItem.MediaType type = mimeType.startsWith("image/") ?
                        MediaItem.MediaType.IMAGE :
                        MediaItem.MediaType.VIDEO;
                SELECT_DATA.add(new MediaItem(Utils.getPathFromUri(uri, this), type));
            }
            Log.i(TAG, "onActivityResult: " + SELECT_DATA);
            if (onSelectedVideo != null) {
                onSelectedVideo.run();
                onSelectedVideo = null;
            }
        }
    }
}