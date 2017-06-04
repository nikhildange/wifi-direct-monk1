package com.example.android.wifidirect;

import android.util.Log;

import java.io.Serializable;
import java.net.Socket;

/**
 * Created by nikhildange on 30/03/16.
 */
public class DeviceProfile implements Serializable{
    static int noDevice=0;
    String devId;
    public String result;
    private int cpuVal,memVal,batVal;
    Socket socket;

    DeviceProfile(String devId,int batVal,int cpuVal,int memVal){
        this.devId = devId;
        this.cpuVal = cpuVal;
        this.memVal = memVal;
        this.batVal = batVal;
        this.socket = null;
    }

    int getCpuVal(){
        return cpuVal;
    }

    int getMemVal(){
        return memVal;
    }

    int getBatVal(){
        return batVal;
    }

    String getResult() { return result; }

    public void logDetails() {
        String detailString = "Device ID : "+devId+"\n CPU : "+cpuVal+"\n MEM : "+memVal+"\n BAT : "+batVal+" % Socket : "+socket;
        Log.i("info",detailString);
    }
}
