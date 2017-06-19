package com.example.android.wifidirect;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.util.Size;
import android.widget.Toast;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

/**
 * Created by nikhildange on 30/03/16.
 */
public class ProfileReader {
    Activity activity;

    public DeviceProfile profileReader(Activity activity)
    {
        this.activity = activity;
        String id = getDeviceId(activity.getApplicationContext());
        int mem = (int)(getMemoryPercentage());
        int bat = getBatteryPercent();
        int cpu = 0;

        String profileVal = "ID:"+id+" ";//+"GPS:"+1+" "
        profileVal = profileVal+"MEM:"+mem+" "+"BAT:"+bat+" ";

        try {
            profileVal = profileVal+"CPU:" + getCpuFrequency();
            cpu = getCpuFrequency();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        Toast t = Toast.makeText(activity.getApplicationContext(),profileVal, Toast.LENGTH_SHORT);
        t.show();

        System.out.print("Profile of my device : "+profileVal);

        DeviceProfile dev = new DeviceProfile(id,bat,cpu,mem);
        return dev;
    }

    int getBatteryPercent() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.getApplicationContext().registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        return level;
    }

    long getMemoryPercentage() {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) activity.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long availableMegs = mi.availMem / 1048576L; // in megabyte (mb)

            return availableMegs;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
//		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
//		ActivityManager activityManager = (ActivityManager)SystemService(ACTIVITY_SERVICE);
//		activityManager.getMemoryInfo(mi);
//		long availableMegs = mi.availMem / 1048576L;
//		return availableMegs;
    }

    public int getCpuFrequency() throws IOException {
        String cpuMaxFreq = "";
        RandomAccessFile reader = new
                RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
        cpuMaxFreq = reader.readLine();
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int cal =Integer.parseInt(cpuMaxFreq) / 1000;
        return cal;
    }

    public static String getDeviceId(Context context) {
        String device_uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (device_uuid == null) {
            device_uuid = "12356789"; // for emulator testing
        } else {
            try {
                byte[] _data = device_uuid.getBytes();
                MessageDigest _digest = MessageDigest.getInstance("MD5");
                _digest.update(_data);
                _data = _digest.digest();
                BigInteger _bi = new BigInteger(_data).abs();
                device_uuid = _bi.toString(36);
            } catch (Exception e) {
                if (e != null) {
                    e.printStackTrace();
                }
            }
        }
        return device_uuid;
    }

//    public int getCamera(Context context) {
//        CameraDevice camera = CameraDevice.
//        List<Size> sizes = android.hardware.camera2. .getSupportedPictureSizes();
//        Size mSize = null;
//        for (Size size : sizes) {
//            System.out.println("Size =="+size.width+"==Height=="+size.height);
//            break;
//        }
//        CameraManager manager = (CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
//        manager.
//    }

}
