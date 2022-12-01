package com.tencent.tavmedia.demo;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;


public class TAVSystemInfo {

    private static final String TAG = "TAVSystemInfo";

    public static String mProcessorName = "N/A";
    public static String mFeature = ""; //support neon
    public static String mCpuHardware = "";
    public static int mCpuArchitecture = 0;
    private static String sCpuName = "";
    /**
     * To return the maximum CPU frequency at KHz for single core.
     *
     * @return long - The value of maximum frequency
     */
    private static long maxCpuFreq = -1;
    private static long currentCpuFreq = -1;
    /**
     * Get the number of cores available in this device, across all processors. Requires: Ability to
     * peruse the file-system at "/sys/devices/system/cpu" The number of cores, or 1 if failed to
     * get result
     */
    private static int numOfCores = -1;
    private static String deviceID;

    public static void init() {
        getCpuHarewareName();

        getCPUName();

        getMaxCpuFreq();

        getNumCores();

    }

    /**
     * Prepare all CPU information, including processor name, CPU architecture, maximum and current
     * running CPU frequency, the number of logic CPU cores.
     */
    public static void getCpuInfo() {
        InputStreamReader inputStream = null;
        BufferedReader br = null;
        try {
            inputStream = new InputStreamReader(new FileInputStream("/proc/cpuinfo"), "UTF-8");
            br = new BufferedReader(inputStream);
            while (true) {
                String text = br.readLine();
                if (null == text) {
                    break;
                }

                readCpuArchitecture(text);

                if (text.startsWith("Processor")) {
                    readProcessorName(text);
                } else if (text.startsWith("CPU architecture")) {
                    readArchitecture(text);
                } else if (text.startsWith("Features")) {
                    readFeatures(text);
                } else if (text.startsWith("Hardware")) {
                    readHardware(text);
                }
            }

        } catch (Throwable ex) {
            mCpuHardware = "Unknown";
            mCpuArchitecture = 0;
        } finally {
            tryCloseStream(inputStream, br);
        }


    }

    private static void readHardware(String text) {
        // 09/17/2014 Gemini: to extract 'Hardware' string, always it's the CPU product code.
        int index = text.indexOf(':');
        if (index > 1) {
            String temp = text.substring(index + 1, text.length());
            temp = temp.trim();
            mCpuHardware = temp.replace(" ", "");
        }
    }

    public static void readFeatures(String text) {
        int index = text.indexOf(':');
        if (index > 1) {
            String temp = text.substring(index + 1, text.length());
            mFeature = temp.trim();
        }
    }

    private static void readArchitecture(String text) {
        if (mCpuArchitecture == 0) {
            int index = text.indexOf(':');
            if (index > 1) {
                String tmp = text.substring(index + 1, text.length());
                tmp = tmp.trim();
                //Log.d("VcSystemInfo", "mCpuArchitecture:=" + tmp);
                if (tmp != null && tmp.length() > 0 && tmp.length() < 2) {
                    mCpuArchitecture = (int) Long.parseLong(tmp);
                } else if (tmp != null && tmp.length() > 1) {
                    mCpuArchitecture = (int) Long.parseLong(tmp.substring(0, 1));
                }
            }
        }
    }

    private static void readProcessorName(String text) {
        int index = text.indexOf(':');
        if (index > 1) {
            mProcessorName = text.substring(index + 1, text.length());
            mProcessorName = mProcessorName.trim();
            //Log.d("VcSystemInfo", "mProcessorName:=" + mProcessorName);
        }
    }

    private static void readCpuArchitecture(String text) {
        if (text.contains("aarch64") || text.contains("AArch64")) {
            mCpuArchitecture = 64;
        }
    }

    private static void tryCloseStream(InputStreamReader inputStream, BufferedReader br) {
        try {
            if (null != inputStream) {
                inputStream.close();
            }

            if (null != br) {
                br.close();
            }

        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 读取cpu信息
     */
    public static long getMaxCpuFreq() {
        if (-1 != maxCpuFreq) {
            return maxCpuFreq;
        }

        InputStreamReader inputStream = null;
        BufferedReader br = null;

        long maxFreq = 0;
        try {
            String fileName = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
            inputStream = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
            br = new BufferedReader(inputStream);
            String text = br.readLine();
            if (null == text) {
                inputStream.close();
                br.close();
                return 0;
            }
            String result = text.trim();

            if (result != null && result.length() > 0) {
                maxFreq = Long.parseLong(result);
            }
        } catch (IOException ex) {
            maxFreq = 0;
        } catch (Throwable ex) {
            maxFreq = 0;
        } finally {
            tryCloseStream(inputStream, br);
        }
        maxCpuFreq = maxFreq;
        return maxFreq;
    }

    /**
     * To return the current running CPU frequency at KHz for single core.
     *
     * @return long - The value of current running CPU frequency
     */
    public static long getCurrentCpuFreq() {

        if (currentCpuFreq > 0) {
            return currentCpuFreq;
        }

        long currFreq = 1024 * 1000;//默认1G
        InputStreamReader inputStream = null;
        BufferedReader br = null;
        try {

            String fileName = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
            inputStream = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
            br = new BufferedReader(inputStream);

            String text = br.readLine();
            if (null == text) {
                inputStream.close();
                br.close();
                return 0;
            }
            String result = text.trim();
            if (result.length() > 0) {
                currFreq = Long.parseLong(result);
            }

            currentCpuFreq = currFreq;
        } catch (Throwable e) {
            Log.e(TAG, "getCurrentCpuFreq: ", e);
        }

        ZipUtils.close(inputStream, br);

        return currentCpuFreq;
    }

    /**
     * 读取芯片核心数量
     */
    public static int getNumCores() {
        if (-1 != numOfCores) {
            return numOfCores;
        }

        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {

            @Override
            public boolean accept(File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            // Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Log.d("VcSystemInfo", "CPU Count: " + files.length);
            // Return the number of cores (virtual CPU devices)
            if (null == files) {
                numOfCores = 1;
                return numOfCores;
            }

            numOfCores = files.length;
            return numOfCores;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            numOfCores = 1;
            return 1;
        }
    }

    /**
     * Return the phone brand name, e.g.GT-I9300
     *
     * @return string - the full name
     */
    public static String getDeviceName() {
        return Build.MODEL;
    }

    /**
     * 读取设备型号（芯片+model）
     */
    public static String getDeviceNameForConfigSystem() {
        return Build.MANUFACTURER + "_" + Build.MODEL;
    }

    /**
     * 读取cpu名
     */
    public static String getCpuHarewareName() {
        if (TextUtils.isEmpty(mCpuHardware)) {
            getCpuInfo();
        }

        return mCpuHardware;
    }

    /**
     * 读取芯片名
     */
    public static String getCPUName() {
        if (!TextUtils.isEmpty(sCpuName)) {
            return sCpuName;
        }
        BufferedReader br = null;
        InputStreamReader inputStream = null;

        try {
            inputStream = new InputStreamReader(new FileInputStream("/proc/cpuinfo"), "UTF-8");

            br = new BufferedReader(inputStream);

            String text = br.readLine();
            if (null == text) {
                return "";
            }

            String[] array = text.split(":\\s+", 2);
            sCpuName = array[1];
            return array[1];
        } catch (Throwable e) {
            Log.e(TAG, e.toString());
        } finally {
            ZipUtils.close(inputStream, br);
        }

        return "UnKnow";
    }

    /**
     * 读取设备id
     */
    public static String getDeviceID(Context mContext) {
        if (!TextUtils.isEmpty(deviceID)) {
            return deviceID;
        }

        if (mContext == null) {
            return "";
        }
        try {
            deviceID = android.provider.Settings.System
                    .getString(mContext.getContentResolver(), "android_id");
            if (TextUtils.isEmpty(deviceID)) {
                deviceID = "NONE";
            }
        } catch (Throwable e) {
            deviceID = "NONE";
        }

        return deviceID;
    }


}

