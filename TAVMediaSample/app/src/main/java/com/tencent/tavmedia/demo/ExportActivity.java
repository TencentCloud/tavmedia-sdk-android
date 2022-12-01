package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVExportCallback;
import com.tencent.tavmedia.license.TAVLicense;
import java.io.File;

public class ExportActivity extends AppCompatActivity {

    private static final String TAG = "ExportActivity";
    private Button btnExport;
    private Button btnAuth;
    private ProgressBar progressBar;
    private File newFile;

    public static void start(Context context) {
        Intent starter = new Intent(context, ExportActivity.class);
        context.startActivity(starter);
    }

    TAVLicense.TAVLicenseAuthListener licenseAuthListener;

    String licenseUrl = "replace_your_license_url";
    String licenseAppId = "replace_your_app_id";
    String licenseKey = "replace_your_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBar.setProgress(0);

        btnExport = findViewById(R.id.export);
        btnAuth = findViewById(R.id.auth);

        TAVComposition composition = Utils.makeComposition(720, 1280);

        licenseAuthListener = (errorCode, msg) -> {
            if (errorCode == TAVLicense.LICENSE_AUTH_SUCCESS) {
                Log.d("export", "auth success " + msg);
            } else {
                Log.e("export", "auth failed and errorCode is " + errorCode + " " + msg);
            }
        };

        btnExport.setOnClickListener(v -> {
            btnExport.setEnabled(false);
            newFile = Utils.createNewFile(Utils.OUT_SAVE_EXPORT_DIR, "test_tav_export_video.mp4");
            Utils.runExport(composition, newFile.getAbsolutePath(), new MyTAVExportCallback(newFile));
        });

        btnAuth.setOnClickListener(v -> {
            TAVLicense.getInstance().Auth(getBaseContext(), licenseUrl, licenseKey, licenseAppId, licenseAuthListener);
        });
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
            progressBar.setProgress((int) (progress * 100));
        }

        @Override
        public void onError(int errorCode) {
            Log.i(TAG, "onError() called with: errorCode = [" + errorCode + "]");
            onExportFinish();
        }

        private void onExportFinish() {
            btnExport.post(() -> btnExport.setEnabled(true));
            ExportActivity.this.finish();
        }
    }
}