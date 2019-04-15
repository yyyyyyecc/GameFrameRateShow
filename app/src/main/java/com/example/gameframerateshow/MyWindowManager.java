package com.example.gameframerateshow;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.support.v4.util.Pools;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import static android.os.ParcelFileDescriptor.createPipe;

public class MyWindowManager {
    private static FloatWindowView floatWindowView;
    private static WindowManager.LayoutParams windowParams;
    private static WindowManager mWindowManager;
    /**
     * 用于获取手机可用内存
     */
    private static ActivityManager mActivityManager;
    private static FPSmaker fpsmaker;




    /**
     * 将小悬浮窗从屏幕上移除。
     *
     * @param context
     *            必须为应用程序的Context.
     */
    public static void removeWindow(Context context) {
        if (floatWindowView != null) {
            WindowManager windowManager = getWindowManager(context);
            windowManager.removeView(floatWindowView);
            floatWindowView = null;
        }
    }
    public static void updateUsedPercent(Context context) {
        WindowManager windowManager = getWindowManager(context);
        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int screenHeight = windowManager.getDefaultDisplay().getHeight();
        if (floatWindowView == null) {
            floatWindowView = new FloatWindowView(context);
            fpsmaker = new FPSmaker();
            fpsmaker.setNowFPS(System.nanoTime());
            if (windowParams == null) {
                windowParams = new WindowManager.LayoutParams();
                windowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                windowParams.format = PixelFormat.RGBA_8888;
                windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                windowParams.gravity = Gravity.LEFT | Gravity.TOP;
                windowParams.width = FloatWindowView.viewWidth;
                windowParams.height = FloatWindowView.viewHeight;
                windowParams.x = screenWidth;
                windowParams.y = screenHeight / 2;
            }
            floatWindowView.setParams(windowParams);
            windowManager.addView(floatWindowView, windowParams);
        }
        else  {
            TextView percentView = (TextView) floatWindowView.findViewById(R.id.window_text);
            fpsmaker.makeFPS();
            percentView.setText(getUsedPercentValue(context)+"CPU频率:"+MyWindowManager.getCPU(context)+"游戏帧率:"+getFPS(context));
        }
    }
    public static boolean isWindowShowing() {
        return floatWindowView != null ;
    }
    private static WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }
    private static ActivityManager getActivityManager(Context context) {
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mActivityManager;
    }
    /**
     * 计算已使用内存的百分比，并返回。
     *
     * @param context
     *            可传入应用程序上下文。
     * @return 已使用内存的百分比，以字符串形式返回。
     */
    public static String getUsedPercentValue(Context context) {
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
            br.close();
            long totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll("\\D+", ""));
            long availableSize = getAvailableMemory(context) / 1024;
            int percent = (int) ((totalMemorySize - availableSize) / (float) totalMemorySize * 100);
            return "内存:"+percent + "%  ";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "悬浮窗";
    }
    /**
     * 获取当前可用内存，返回数据以字节为单位。
     *
     * @param context
     *            可传入应用程序上下文。
     * @return 当前可用内存。
     */
    private static long getAvailableMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        getActivityManager(context).getMemoryInfo(mi);
        return mi.availMem;
    }
    public static String getCPU(Context context) {
        List<Integer> results = new ArrayList<Integer>();
        String freq = "";
        FileReader fr = null;
        Integer lastFreq = 0;
        try {
            int cpuIndex = 0;

            while(true){
                File file = new File("/sys/devices/system/cpu/cpu"+cpuIndex+"/");
                if(!file.exists()){
                    break;
                }
                file = new File("/sys/devices/system/cpu/cpu"+cpuIndex+"/cpufreq/");
                if(!file.exists()){
                    lastFreq = 0;
                    results.add(0);
                    cpuIndex++;
                    continue;
                }
                file = new File("/sys/devices/system/cpu/cpu"+cpuIndex+"/cpufreq/scaling_cur_freq");
                if(!file.exists()){
                    results.add(lastFreq);
                    cpuIndex++;
                    continue;
                }
                fr = new FileReader(
                        "/sys/devices/system/cpu/cpu"+cpuIndex+"/cpufreq/scaling_cur_freq");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                freq = text.trim();
                lastFreq = Integer.valueOf(freq);
                results.add(lastFreq);
                fr.close();
                cpuIndex++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(fr!=null){
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return lastFreq+"";
    }
    public static String getFPS(Context context){

        Display display = getWindowManager(context).getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        return refreshRate+"fps";
    }

}
