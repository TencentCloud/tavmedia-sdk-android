package com.tencent.tavmedia.demo;

import static com.tencent.tavmedia.demo.Utils.OUT_SAVE_DIR;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;
import java.io.IOException;

class AssetsCopyHelper {

    private final Context context;
    private final Runnable callback;
    private ProgressDialog progressDialog;

    public AssetsCopyHelper(Context context) {
        this(context, null);
    }

    public AssetsCopyHelper(Context context, Runnable callback) {
        this.context = context;
        this.callback = callback;
    }


    public void run() {
        String[] fileNames;
        // 获取权限后，拷贝asset文件到本地
        try {
            fileNames = Utils.getAssetsFileNames(context, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (fileNames == null || Utils.fileExists(OUT_SAVE_DIR, fileNames)) {
            finish();
            return;
        }
        doCopy();
    }

    private void doCopy() {
        progressDialog = ProgressDialog.show(context, "复制文件", "复制中. 请稍后...", true);
        Utils.copyAssetFileOrDir(context, "", OUT_SAVE_DIR, success -> {
            if (success) {
                Toast.makeText(context, "复制成功", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(context, "复制失败", Toast.LENGTH_LONG).show();
            }
            progressDialog.dismiss();

        });
    }

    private void finish() {
        if (callback != null) {
            callback.run();
        }
    }

}
