package com.tencent.tavmedia.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import com.tencent.libav.model.TinLocalImageInfoBean;
import com.tencent.tavmedia.TAVAsset;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVExport;
import com.tencent.tavmedia.TAVExportCallback;
import com.tencent.tavmedia.TAVExportConfig;
import com.tencent.tavmedia.TAVExportConfig.Builder;
import com.tencent.tavmedia.TAVImageAsset;
import com.tencent.tavmedia.TAVMovie;
import com.tencent.tavmedia.TAVMovieAsset;
import com.tencent.tavmedia.TAVPAGEffect;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Utils {


    private static final String TAG = "TAVMedia_Utils";
    public static String OUT_SAVE_DIR = "/sdcard/tavmedia_demo/";
    public static final String OUT_SAVE_EXPORT_DIR = OUT_SAVE_DIR + "export/";
    public static final String OUT_SAVE_VIDEOS_DIR = OUT_SAVE_DIR + "resources/";
    private static String TARGET_BASE_PATH;

    public static void initCacheDir(Context context) {
//        Android Q 逻辑
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//            File file = context.getExternalFilesDir("tavmedia_demo");
//            file.mkdirs();
//            OUT_SAVE_DIR = file.getAbsolutePath() + "/";
//        }
    }

    public static String loadJSONFromAssets(Context context, String path) {
        try {
            InputStream is = context.getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadJsonFromFile(String path) {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, path);
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            return text.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 复制asset到scared
     */
    @SuppressLint("CheckResult")
    public static void copyAssetFileOrDir(final Context context, final String path,
            final String outputDir, final Callback callback) {
        TARGET_BASE_PATH = outputDir;
        Single
                .create((SingleOnSubscribe<String>) emitter -> {
                    try {
                        copyFileOrDir(context, path);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                    emitter.onSuccess(outputDir);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> callback.copyFinish(true), throwable -> callback.copyFinish(false));
    }


    private static void copyFileOrDir(Context context, String path) throws IOException {
        Log.i(TAG, "copyFileOrDir() " + path);

        String[] assets = context.getAssets().list(path);

        if (assets == null || assets.length == 0) {
            copyFile(context, path);
        } else {
            copyFileOrDir(context, path, assets);
        }

    }

    private static void copyFileOrDir(Context context, String path, String[] assets) throws IOException {
        mackDirs(path);
        for (String asset : assets) {
            copyFileOrDir(context, path, asset);
        }
    }

    private static void copyFileOrDir(Context context, String path, String asset) throws IOException {
        String p;
        if (path.equals("")) {
            p = "";
        } else {
            p = path + "/";
        }

        if (!path.startsWith("images") && !path.startsWith("sounds") && !path
                .startsWith("webkit")) {
            copyFileOrDir(context, p + asset);
        }
    }

    private static void copyFile(Context context, String filename) throws IOException {
        Log.i("tag", "copyFile() " + filename);

        AssetManager assetManager = context.getAssets();
        InputStream in = assetManager.open(filename);
        String newFileName = TARGET_BASE_PATH + filename;
        OutputStream out = new FileOutputStream(newFileName);

        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
    }

    private static void mackDirs(String path) {
        String fullPath = TARGET_BASE_PATH + path;
        Log.i("tag", "path=" + fullPath);
        File dir = new File(fullPath);
        if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path
                .startsWith("webkit")) {
            if (!dir.mkdirs()) {
                Log.i("tag", "could not create dir " + fullPath);
            }
        }
    }

    /**
     * 获取asset names
     */

    public static String[] getAssetsFileNames(Context context, String path) throws IOException {
        return getAssetsFileNames(context, path, "");
    }

    /**
     * 获取asset names
     */
    public static String[] getAssetsFileNames(Context context, String path, String suffix)
            throws IOException {
        AssetManager assets = context.getResources().getAssets();
        String[] files = assets.list(path);
        Log.d(TAG, "initFileNames: files = " + Arrays.toString(files));
        if (files == null) {
            return null;
        }
        List<String> strings = new ArrayList<>();
        for (String file : files) {
            String[] list = assets.list(file);
            if (list != null && list.length != 0) {
                Log.e(TAG, "getAssetsFileNames: 包含文件夹：" + file);
                continue;
            }
            if (file.endsWith(suffix)) {
                strings.add(file);
            }
        }
        // 转换成数组
        String[] fileNames = new String[strings.size()];
        for (int i = 0; i < fileNames.length; i++) {
            fileNames[i] = strings.get(i);
        }
        return fileNames;
    }

    /**
     * 创建一个文件
     */

    public static File createNewFile(String dirPath, String fileName) {
        final File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }

        final File file = new File(dir, fileName);

        try {
            if (file.delete()) {
                Log.d(TAG, "export: 文件已存在，删除");
            }

            if (!file.createNewFile()) {
                Log.e(TAG, "export: 创建输出文件失败:" + file.getAbsolutePath());
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "export: 创建输出文件失败, e = ", e);
            return null;
        }
        return file;
    }

    /**
     * 获取输出的视频文件
     */
    public static String getOutputVideoName() {
        String name = new SimpleDateFormat("MMdd_HHmmss")
                .format(new Date(System.currentTimeMillis()));
        name += ".mp4";
        return name;
    }

    /**
     * 删除文件
     */
    public static void deleteAllFiles(String path) {
        deleteAllFiles(new File(path));
    }

    /**
     * 删除文件
     */
    public static void deleteAllFiles(File root) {
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            // 判断是否为文件夹
            if (f.isDirectory()) {
                deleteAllFiles(f);
                f.delete();
                continue;
            }
            // 判断是否存在
            if (!f.exists()) {
                continue;
            }
            deleteAllFiles(f);
            f.delete();
        }
    }


    /**
     * 执行复制
     */
    public static void doCopy(String[] fileNames, final Context context, final Callback callback) {
        new File(OUT_SAVE_VIDEOS_DIR).mkdirs();
        Observable.fromArray(fileNames)
                .subscribeOn(Schedulers.io())
                .map(new Function<String, Boolean>() {
                    @Override
                    public Boolean apply(String s) throws Exception {
                        return copyAssets(context, s, OUT_SAVE_VIDEOS_DIR);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    boolean result = true;

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        result &= aBoolean;
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.copyFinish(false);
                    }

                    @Override
                    public void onComplete() {
                        callback.copyFinish(result);
                    }
                });
    }

    /**
     * 将Assetts 里面的文件copy到sd卡，需要有sd卡权限才行
     */
    public static boolean copyAssets(Context pContext, String pAssetFilePath, String pDestDirPath) {
        AssetManager assetManager = pContext.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(pAssetFilePath);
            File outFile = new File(pDestDirPath, pAssetFilePath);
            out = new FileOutputStream(outFile);
            copyAssetFile(in, out);
            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e("tag", "Failed to copy asset file: " + pAssetFilePath, e);
            return false;
        }
        return true;
    }

    private static void copyAssetFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 16];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * 判断文件是否存在
     */
    public static boolean fileExists(String dir, String[] fileNames) {
        for (String fileName : fileNames) {
            if (!new File(dir, fileName).exists()) {
                return false;
            }
        }
        return true;
    }

    public static TAVComposition makeComposition(float width, float height) {
        return makeComposition(MainActivity.SELECT_DATA, width, height);
    }

    public static TAVComposition makeComposition(List<TinLocalImageInfoBean> selectData, float width,
            float height) {
        long totalTime = 0;
        ArrayList<TAVMovie> movies = new ArrayList<>();
        for (TinLocalImageInfoBean selectDatum : selectData) {
            TAVMovie movie = makeMovie(selectDatum);
            Utils.fitToTarget(movie, width, height);
            // 设置时间，首尾相接
            movie.setStartTime(totalTime);
            totalTime += movie.duration();

            movies.add(movie);
        }

        TAVComposition composition = TAVComposition
                .Make((int) width, (int) height, 0, totalTime);
        composition.setDuration(totalTime);
        for (TAVMovie movie : movies) {
            composition.addClip(movie);
        }
        return composition;
    }

    public static TAVMovie makeMovie(TinLocalImageInfoBean selectDatum) {

        if (selectDatum.isVideo()) {
            TAVAsset asset = TAVMovieAsset.MakeFromPath(selectDatum.mPath);
            TAVMovie movie = TAVMovie.MakeFrom(asset, 0, asset.duration());
            movie.setDuration(asset.duration());
            return movie;
        }
        if (selectDatum.isImage()) {
            TAVAsset asset = TAVImageAsset.MakeFromPath(selectDatum.mPath);
            // 图片默认两秒
            TAVMovie movie = TAVMovie.MakeFrom(asset, 0, 2_000_000);
            movie.setDuration(2_000_000);
            return movie;
        }
        throw new RuntimeException("不支持的type:" + selectDatum.mimeType + ", 谢谢。");

    }

    public static void fitToTarget(TAVMovie movie, float targetWidth, float targetHeight) {
        Matrix matrix = new Matrix();
        // 暂时只实现宽对齐的模式
        float scale = targetWidth / movie.width();
        matrix.postScale(scale, scale);
        // 平移到中点
        matrix.postTranslate(0, (targetHeight - movie.height() * scale) / 2f);
        movie.setMatrix(matrix);
    }

    public static TAVPAGEffect makePAGEffect(String name, float width, float height) {
        TAVPAGEffect effect = TAVPAGEffect.MakeFromPath(OUT_SAVE_DIR + name);
        Matrix matrix = new Matrix();
        // 暂时只实现宽对齐的模式
        matrix.postScale(width / effect.width(), width / effect.width());
        effect.setMatrix(matrix);
        return effect;
    }

    public static void toast(Activity activity, final String text) {
        activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_LONG).show());
    }

    public static void runExport(TAVMovie media, TAVExportCallback callback) {
        String outputFilePath = Utils.createNewFile(Utils.OUT_SAVE_EXPORT_DIR, "tav_export_video.mp4")
                .getAbsolutePath();
        runExport(media, outputFilePath, callback);
    }

    public static void runExport(TAVMovie media, String outputFilePath, TAVExportCallback callback) {
        TAVExportConfig config = new Builder()
                .setVideoWidth(media.width())
                .setFrameRate(24)
                .setVideoHeight(media.height())
                .setOutFilePath(outputFilePath)
                .setUseHWEncoder(true)
                .build();
        runExport(media, config, callback);
    }

    public static void runExport(TAVMovie media, TAVExportConfig config, TAVExportCallback callback) {
        new Thread(() -> new TAVExport(media, config, callback).export()).start();

    }

    public interface Callback {

        /**
         * 复制结束
         *
         * @param success 是否复制成功
         */
        void copyFinish(boolean success);
    }
}
