package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVExport;
import com.tencent.tavmedia.TAVExportCallback;
import com.tencent.tavmedia.TAVExportConfig;
import com.tencent.tavmedia.TAVExportConfig.Builder;
import com.tencent.tavmedia.license.TAVLicense;
import com.tencent.tavmedia.license.TAVLicense.TAVLicenseAuthListener;
import java.io.File;

public class ExportActivity extends AppCompatActivity {

    private static final String TAG = "ExportActivity";
    private static final int MAX_PROGRESS = 1000;
    private Button btnExport;
    private ProgressBar progressBar;
    private TAVExport tavExport;

    public static void start(Context context) {
        Intent starter = new Intent(context, ExportActivity.class);
        context.startActivity(starter);
    }


    String licenseUrl = "replace_your_license_url";
    String licenseAppId = "replace_your_app_id";
    String licenseKey = "replace_your_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(MAX_PROGRESS);
        progressBar.setProgress(0);
        btnExport = findViewById(R.id.export);
    }

    public void auth(View view) {
        TAVLicense.getInstance()
                .Auth(getBaseContext(), licenseUrl, licenseKey, licenseAppId, new MyTAVLicenseAuthListener());
    }

    public void export(View view) {
        btnExport.setEnabled(false);
        File newFile = Utils.createNewFile(Utils.OUT_SAVE_EXPORT_DIR, "test_tav_export_video.mp4");
        TAVComposition composition = Utils.makeComposition(720, 1280);
        TAVExportConfig config = new Builder()
                .setVideoWidth(composition.width())
                .setVideoHeight(composition.height())
                .setOutFilePath(newFile.getAbsolutePath())
                .setFrameRate(30)
                .setUseHWEncoder(true)
                .build();
        tavExport = new TAVExport(composition, config, new MyTAVExportCallback(newFile));
        new Thread(() -> tavExport.export()).start();
    }

    public void cancel(View view) {
        if (tavExport != null) {
            tavExport.cancel();
            onExportFinish();
        }
    }

    private synchronized void onExportFinish() {
        Log.d(TAG, "onExportFinish() called");
        // 变量置空，及时释放内存
        tavExport = null;
        runOnUiThread(() -> {
            Log.d(TAG, "onExportFinish() runOnUiThread");
            btnExport.setEnabled(true);
            progressBar.setProgress(0);
        });
    }

    private synchronized void setProgress(float progress) {
        if (tavExport == null) {
            return;
        }
        progressBar.setProgress((int) (progress * MAX_PROGRESS));
    }

    private static class MyTAVLicenseAuthListener implements TAVLicenseAuthListener {

        @Override
        public void onLicenseAuthResult(int errorCode, String msg) {
            if (errorCode == TAVLicense.LICENSE_AUTH_SUCCESS) {
                Log.d("export", "auth success " + msg);
            } else {
                Log.e("export", "auth failed and errorCode is " + errorCode + " " + msg);
            }
        }
    }


    private class MyTAVExportCallback extends TAVExportCallback {

        private final File newFile;

        public MyTAVExportCallback(File newFile) {
            this.newFile = newFile;
        }

        @Override
        public void onCompletion() {
            Log.i(TAG, "onCompletion() called");
            Utils.toast(ExportActivity.this, "outputPath = " + newFile.getAbsolutePath());
            onExportFinish();
        }

        @Override
        public void onProgress(float progress) {
            Log.v(TAG, "onProgress() called with: progress = [" + progress + "]");
            setProgress(progress);
        }

        @Override
        public void onError(int errorCode) {
            Log.i(TAG, "onError() called with: errorCode = [" + errorCode + "]");
            onExportFinish();
        }
    }


}