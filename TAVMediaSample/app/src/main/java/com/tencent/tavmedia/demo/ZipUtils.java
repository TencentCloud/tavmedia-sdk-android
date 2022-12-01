package com.tencent.tavmedia.demo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    private static final String TAG = "ZipUtils";

    private static final int BUFFER_SIZE = 20 * 1024;

    /**
     * 解压
     */
    public static synchronized void unZip(Context context, String zipFile, String targetDir) {
        InputStream is = null;
        try {
            is = context.getAssets().open(zipFile);
            unZip(is, targetDir);
        } catch (IOException e) {
            Log.e(TAG, "unZip: ", e);
        } finally {
            tryClose(is);
        }
    }

    /**
     * 解压
     */
    public static synchronized void unZip(String zipFile, String targetDir) {
        if (TextUtils.isEmpty(zipFile)) {
            Log.e(TAG, "unZip: 文件路径为空！");
            return;
        }
        // 文件不存在
        File file = new File(zipFile);
        if (!file.exists()) {
            Log.e(TAG, "unZip: 文件不存在！");
            return;
        }
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            unZip(fis, targetDir);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "unZip: ", e);
        } finally {
            tryClose(fis);
        }
    }

    /**
     * 解压
     */
    public static synchronized void unZip(InputStream fis, String targetDir) {

        File targetFolder = new File(targetDir);
        if (!targetFolder.exists()) {
            boolean isMkDirs = targetFolder.mkdirs();
            Log.v(TAG, "[unZip] isMkDirs: " + isMkDirs);
        }

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

        try {
            unzip(targetDir, zis);
        } catch (Exception e) {
            Log.e(TAG, "unZip: ", e);
        } finally {
            close(fis, zis);
        }
    }

    private static void unzip(String targetDir, ZipInputStream zis) throws IOException {

        String dataDir = null;
        String strEntry;

        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            Log.i(TAG, "unZip entry = " + entry);
            strEntry = entry.getName();

            if (strEntry.contains("../")) {
                continue;
            }

            if (entry.isDirectory()) {
                dataDir = unzipDirectory(targetDir, dataDir, strEntry);
            } else {
                unzipFile(targetDir, zis, strEntry);
            }
        }
    }

    private static void unzipFile(String targetDir, ZipInputStream zis, String strEntry) throws IOException {
        int count;
        byte[] data = new byte[BUFFER_SIZE];
        String targetFileDir = targetDir + File.separator + strEntry;
        Log.i(TAG, "unZip entry is file, path = " + targetFileDir);

        File targetFile = new File(targetFileDir);
        FileOutputStream fos = new FileOutputStream(targetFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);

        while ((count = zis.read(data)) != -1) {
            dest.write(data, 0, count);
        }

        dest.flush();
        close(dest, fos);
    }

    private static String unzipDirectory(String targetDir, String dataDir, String strEntry) {
        String entryPath = targetDir + File.separator + strEntry;
        Log.i(TAG, "unZip entry is folder, path = " + entryPath);
        File entryFile = new File(entryPath);
        if (!entryFile.exists()) {
            boolean isMkDirs = entryFile.mkdirs();
            Log.i(TAG, "'[unZip] isMkDirs: " + isMkDirs);
        }
        if (TextUtils.isEmpty(dataDir)) {
            dataDir = entryFile.getPath();
        }
        return dataDir;
    }

    static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            tryClose(closeable);
        }
    }

    static void tryClose(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "tryClose: ", e);
        }
    }


}
