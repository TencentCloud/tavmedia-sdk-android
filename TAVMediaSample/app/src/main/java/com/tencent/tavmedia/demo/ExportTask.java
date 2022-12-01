package com.tencent.tavmedia.demo;


import android.graphics.Point;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.widget.Toast;
import com.tencent.tavmedia.TAVExport;
import com.tencent.tavmedia.TAVExportCallback;
import com.tencent.tavmedia.TAVExportConfig.Builder;
import com.tencent.tavmedia.TAVMovie;
import com.tencent.tavmedia.TAVMovieAsset;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExportTask {

    static final String TAG = "ExportTask";

    private static final String[] bitModeNames = {"QR", "VBR", "CBR"};
    private static final int[] iFrameIntervals = {1};
    private static final int[] audioCounts = {1};
    private static float[] bitRates = {8 * 1024 * 1024};
    private static final boolean[] qualitys = {true};
    private static final boolean[] bFrames = {true};
    private static final String[] mimes = {MediaFormat.MIMETYPE_VIDEO_AVC};
    /**
     * {MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ,
     * MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR,
     * MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR}
     */
    private static final int[] bitmodes = {MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR};
    private static Point[] sizes = {new Point(720, 1280)};
    static String[] fileNames;

    static {
//        initBitRates();

//        initSizes();

//        EncoderWriter.IGNORE_HIGH_LINE_LIMIT = true;
//        EncoderWriter.SET_B_FRAME_COUNT = true;
//        EncoderWriter.SET_BITRATE_MODE = true;
//        EncoderWriter.SET_BASELINE = true;
    }

    private final JSONArray exportJsonArray = new JSONArray();
    private final JSONArray readFrameJsonArray = new JSONArray();
    private final JSONArray previewJsonArray = new JSONArray();
    private final JSONArray writeFrameJsonArray = new JSONArray();
    private final JSONArray extraInfoJsonArray = new JSONArray();

    private final ExecutorService executorService;
    private final AutoTestActivity context;
    private String currentOutputFileName;

    private boolean exportFinished = false;
    private boolean previewFinished = true;

    public ExportTask(AutoTestActivity context) {
        this.context = context;
        executorService = Executors.newFixedThreadPool(3);
    }

    private synchronized void putValues(HashMap<String, String> values, JSONArray previewJsonArray) {
        HashMap<String, String> resultMap = processValues(values);
        Log.d(TAG, "onReport: resultMap = " + resultMap);
        previewJsonArray.put(new JSONObject(resultMap));
    }

    private HashMap<String, String> processValues(HashMap<String, String> values) {
        // 先clone一下
        values = new HashMap<>(values);
        values.put("cpu_name", TAVSystemInfo.getCpuHarewareName());
        values.put("device_name", TAVSystemInfo.getDeviceNameForConfigSystem());
        HashMap<String, String> resultMap = new HashMap<>();
        for (String key : values.keySet()) {
            String str = values.get(key);
            if (str == null) {
                continue;
            }
            String result = str.replaceAll("\n", "-").replaceAll(",", "_");
            resultMap.put(key, result);
        }
        return resultMap;
    }

    private static void initSizes() {
        Point base = new Point(720, 720);
        Point offset = new Point(160, 160);
        sizes = new Point[20];
        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = new Point(base.x, base.y);
            base.x += offset.x;
            base.y += offset.y;
        }
    }

    private static void initBitRates() {
        float base = 8 * 1024 * 1024;
        float offset = 0.5f * 1024 * 1024;
        bitRates = new float[1];
        for (int i = 0; i < bitRates.length; i++) {
            bitRates[i] = base;
            base += offset;
        }
    }

    public void start() {
        tryDeleteAllFiles(new File(Utils.AUTO_TEST_DIR));
        // 手动指定需要的文件
        TAVSystemInfo.init();
        tryInitFileNames();
        copyDemoVideos();
    }

    private void tryInitFileNames() {
        try {
            initFileNames();
        } catch (IOException e) {
            Log.e(TAG, "tryInitFileNames: initFileNames error", e);
        }
    }

    private void initFileNames() throws IOException {
        fileNames = Utils.getAssetsFileNames(context, "", ".mp4");
        Log.d(TAG,
                "initFileNames: ExportTask.fileNames = " + Arrays.toString(ExportTask.fileNames));
    }

    private void tryDeleteAllFiles(File root) {
        try {
            showToast("清空文件夹:" + root.getPath());
            deleteAllFiles(root);
        } catch (Exception e) {
            showToast("deleteAllFiles error:" + e);
            Log.e(TAG, "tryDeleteAllFiles: ", e);
        }
    }

    private void deleteAllFiles(File root) {
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

    private void copyDemoVideos() {
        Utils.doCopy(fileNames, context, success -> {
            if (success) {
                showToast("复制成功");
                startExport();
            } else {
                showToast("复制失败");
            }
        });
    }

    private void startExport() {
        // 单次合成代码
        if (true) {
            Builder config = new Builder();
            for (int i = 0; i < 10; i++) {
                export("video-640x360.mp4", "single_export_" + i + ".mp4", config);
            }
            context.onTestFinish();
            return;
        }
        executorService.execute(this::dispatchExport);
    }

    private void dispatchExport() {

        for (Point size : sizes) {
            // 然后开始分发各种不同的参数执行导出
            Builder config = new Builder()
                    .setVideoWidth(size.x)
                    .setVideoHeight(size.y);
            traverseBitRates(config);
        }
        // 执行结束
        context.onTestFinish();
        exportFinished = true;
        checkFinish();
    }

    private void checkFinish() {
        Log.d(TAG,
                "checkFinish() called, exportFinished = " + exportFinished + ", previewFinished = " + previewFinished);
        if (exportFinished && previewFinished) {
            writeToFile();
            context.finish();
        }
    }

    private synchronized void writeToFile() {
        writeToFile(exportJsonArray.toString(), "export_report.json");
        writeToFile(previewJsonArray.toString(), "preview_report.json");
        writeToFile(writeFrameJsonArray.toString(), "write_frame_report.json");
        writeToFile(readFrameJsonArray.toString(), "read_frame_report.json");
        writeToFile(extraInfoJsonArray.toString(), "extra_info.json");
    }

    private void writeToFile(String data, String fileName) {
        File file = Utils.createNewFile(Utils.AUTO_TEST_DIR, fileName);
        if (file == null) {
            return;
        }
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(data.getBytes());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void traverseBitRates(Builder config) {
        for (float bitRate : bitRates) {
            config.setVideoBitrateBps((int) bitRate);
            // 遍历GOP
            traverseIFrameIntervals(config);
        }
    }

    private void traverseIFrameIntervals(Builder config) {
//        for (int iFrameInterval : iFrameIntervals) {
//            config.setVideoIFrameInterval(iFrameInterval);
        // 遍历音频轨道
        traverseAudioCounts(config);
//        }
    }

    private void traverseAudioCounts(Builder config) {
        for (int audioCount : audioCounts) {
            config.setChannels(audioCount);
            // 遍历编码模式
            traverseBitModes(config);
        }
    }

    private void traverseBitModes(Builder config) {
        for (int bitMode : bitmodes) {
            // 编码模式设置暂时不用，保留这个方法做调试
            traverseQualities(config);
        }
    }

    private void traverseQualities(Builder config) {
//        for (boolean quality : qualitys) {
//            config.setHighProfile(quality);
        traverseBFrames(config);
//        }
    }

    private void traverseBFrames(Builder config) {
//        for (boolean bFrame : bFrames) {
//            config.setEnableBFrame(bFrame);
        // 开启B帧调试
        traverseMimes(config);
//        }
    }

    private void traverseMimes(Builder config) {
//        for (String mime : mimes) {
//            config.setOutputVideoMimeType(mime);
        traverseFileNames(config);
//        }
    }

    private void traverseFileNames(Builder outputConfig) {
        for (String inputFileName : fileNames) {
            String outputName = buildFileName(inputFileName, outputConfig, true);
            export(inputFileName, outputName, outputConfig);
        }
    }

    private void export(String inputFileName, String outputFileName,
            Builder outputConfig) {
        String inputFilePath = Utils.OUT_SAVE_VIDEOS_DIR + inputFileName;
        doExport(inputFilePath, outputConfig, outputFileName);
        context.onExportStart();
        // 等5 * 60秒就算了
        lockThread(5 * 60 * 1000);
    }


    private int threadId = 0;

    private void doExport(final String videoPath, Builder outputConfig,
            String fileName) {
        Log.d(TAG, "doExport() called with: videoPath = [" + videoPath + "], outputConfig = [" + outputConfig
                + "], fileName = [" + fileName + "]");

        File file = Utils.createNewFile(Utils.AUTO_TEST_DIR, fileName);
        if (file == null) {
            return;
        }

        outputConfig.setOutFilePath(file.getAbsolutePath());
//        String json = Utils.loadJSONFromAssets(context, "tavmedia.json");
//        TAVComposition movie = TAVComposition.MakeFromJson(json);
        TAVMovieAsset asset = TAVMovieAsset.MakeFromPath(videoPath);
        TAVMovie movie = TAVMovie.MakeFrom(asset, 0, asset.duration());
        movie.setDuration(asset.duration());

        Log.d(TAG, "doExport: movie volume = " + movie.volume());
        outputConfig.setVideoWidth(movie.width());
        outputConfig.setVideoHeight(movie.height());

        TAVExport export = new TAVExport(movie, outputConfig.build(), new MyTAVExportCallback(outputConfig));
        executorService.execute(export::export);
        showToast("export:" + fileName);
        currentOutputFileName = fileName;

    }

    private String buildFileName(String inputName, Builder outputConfig) {
        return buildFileName(inputName, outputConfig, false);
    }

    private String buildFileName(String inputName, Builder outputConfig,
            boolean appendSize) {

        String sourceName = inputName.split("[.]")[0] + "-";
        if (appendSize) {
            sourceName +=
                    outputConfig.getVideoWidth() + "x" + outputConfig.getVideoHeight() + "-";
        }
        String bitName = String.format("%.1fM", outputConfig.getVideoBitrateBps() / 1024 / 1024f);
//        String iFrameName = "-" + outputConfig.getVideoIFrameInterval() + "s";
        String audioCountName = outputConfig.getChannels() == 1 ? ""
                : "-audioChannel_" + outputConfig.getChannels();
//        String qualityName = outputConfig.isHighProfile() ? "" : "-baseline";
//        String mime = "-" + outputConfig.getOutputVideoMimeType().split("[/]")[1];
        // 选择需要拼接的字符串
        return ""
                + sourceName
                + bitName
//                + iFrameName
//                + audioCountName
//                + mime
//                + qualityName
                + ".mp4";
    }

    private void showToast(String text) {
        context.runOnUiThread(() -> {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        });
    }

    private void lockThread(int timeMs) {
        synchronized (this) {
            try {
                Log.d(TAG, "lockThread() called with: timeMs = [" + timeMs + "]");
                this.wait(timeMs);
            } catch (Exception e) {
                Log.e(TAG, "lockThread: ", e);
            }
        }
    }

    private void unlockThread() {
        synchronized (this) {
            Log.d(TAG, "unlockThread() called");
            notifyAll();
        }
    }

    private class MyTAVExportCallback extends TAVExportCallback {

        private final Builder outputConfig;

        public MyTAVExportCallback(Builder outputConfig) {
            this.outputConfig = outputConfig;
        }

        @Override
        public void onCompletion() {
            String outFilePath = outputConfig.getOutFilePath();
            TAVMovieAsset asset = TAVMovieAsset.MakeFromPath(outFilePath);
            if (asset == null || asset.width() <= 0 || asset.duration() <= 0) {
                throw new RuntimeException("导出文件异常:" + outFilePath);
            }
            String text = "导出成功:" + outFilePath;
            Log.d(TAG, "onCompletion() called, " + text);
            ExportTask.this.showToast(text);
            onExportDone(0);
        }

        @Override
        public void onProgress(float progress) {
            Log.v(TAG, "onProgress() called with: progress = [" + progress + "]");
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, "onError() called with: errorCode = [" + errorCode + "]");
            onExportDone(errorCode);
        }

        private void onExportDone(int code) {
            HashMap<String, String> extraInfoMap = new HashMap<>();
            extraInfoMap.put("file_name", currentOutputFileName);
            if (code != 0) {
                extraInfoMap.put("error_code", code + "");
                extraInfoMap.put("error_msg", "i do not know");
            }
            String bitrate = getBitrate(outputConfig.getOutFilePath());
            extraInfoMap.put("bitrate", bitrate);
            extraInfoJsonArray.put(new JSONObject(processValues(extraInfoMap)));
            ExportTask.this.unlockThread();
        }

        private String getBitrate(String file) {
            try {
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(file);
                return metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            } catch (Exception e) {
                Log.e(TAG, "getBitrate: ", e);
                return -1 + "";
            }
        }
    }

}
