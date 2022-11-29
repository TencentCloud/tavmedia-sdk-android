package com.tencent.tavmedia.demo;

import android.Manifest.permission;
import android.content.Intent;
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
import com.tencent.libav.LocalAlbumActivity;
import com.tencent.libav.PhotoSelectorProxyConsts;
import com.tencent.libav.model.TinLocalImageInfoBean;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static List<TinLocalImageInfoBean> SELECT_DATA;

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 10086;

    private Runnable onSelectedVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.initCacheDir(this);
        requestPermissions();

//        try {
//            SELECT_DATA = Collections.singletonList(
//                    new TinLocalImageInfoBean("/storage/emulated/0/tavmedia_demo/seal.mp4"));
//        } catch (InvalidImageException e) {
//            Log.e(TAG, "onCreate: new TinLocalImageInfoBean ", e);
//        }
//        MultiClipActivity.start(this);
    }

    public void jumpExportActivity(View view) {
        selectVideo(false, () -> ExportActivity.start(view.getContext()));
    }

    public void jumpAudioPlayerActivity(View view) {
        selectVideo(true, () -> AudioPlayerActivity.start(view.getContext()));
    }

    public void jumpTemplateActivity(View view) {
        TemplateActivity.start(this);
    }

    public void jumpMultiClipActivity(View view) {
        selectVideo(false, () -> MultiClipActivity.start(view.getContext()));
    }

    public void jumpSnapshotActivity(View view) {
        selectVideo(false, () -> SnapshotActivity.start(view.getContext()));
    }

    public void jumpUpdateRenderSizeActivity(View view) {
        selectVideo(false, () -> UpdateRenderSizeActivity.start(view.getContext()));
    }

    public void jumpColorTuningActivity(View view) {
        selectVideo(false, () -> ColorTuningActivity.start(view.getContext()));
    }

    public void jumpSerializableActivity(View view) {
        SerializableActivity.start(view.getContext());
    }

    public void jumpPAGTemplateActivity(View view) {
        Toast.makeText(this, "请选3个素材", Toast.LENGTH_LONG).show();
        selectVideo(false, () -> PAGTemplateActivity.start(view.getContext()));
    }

    private void selectVideo(boolean onlyVideo, Runnable onSelectedVideo) {
        this.onSelectedVideo = onSelectedVideo;
        if (onlyVideo) {
            LocalAlbumActivity.startChooseVideo(this, 0, REQUEST_CODE);
        } else {
            LocalAlbumActivity.startChoosePhotoAndVideo(this, 0, REQUEST_CODE);
        }
    }


    private void requestPermissions() {

        AcpOptions options = new Builder()
                .setPermissions(permission.WRITE_EXTERNAL_STORAGE, permission.READ_PHONE_STATE)
                .build();
        Acp.getInstance(this).request(options, new AcpListener() {
            @Override
            public void onGranted() {
                Log.i(TAG, "onGranted() called 权限ok");
                new AssetsCopyHelper(MainActivity.this).run();
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
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            SELECT_DATA = (ArrayList<TinLocalImageInfoBean>) data
                    .getSerializableExtra(PhotoSelectorProxyConsts.KEY_SELECTED_DATA);
            StringBuilder sb = new StringBuilder();
            sb.append("selected items:\n");
            for (TinLocalImageInfoBean datum : SELECT_DATA) {
                sb.append(datum.mPath);
                sb.append(";\n");
            }
            Log.i(TAG, "onActivityResult: " + sb);

            if (onSelectedVideo != null) {
                onSelectedVideo.run();
                onSelectedVideo = null;
            }
        }
    }
}