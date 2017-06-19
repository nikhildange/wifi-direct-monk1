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
    private int cpuVal,memVal,batVal,camVal;
    Socket socket;
    private boolean isBusy = false;

    DeviceProfile(String devId,int batVal,int cpuVal,int memVal){
        this.devId = devId;
        this.cpuVal = cpuVal;
        this.memVal = memVal;
        this.batVal = batVal;
        this.camVal = 10;
        this.isBusy = false;
        this.socket = null;
    }

    boolean getAvailiability() {
        if (this.batVal > 10 && !this.isBusy) {
            return true;
        }
        return false;
    }

    boolean setBusy(Boolean state) {
        if (this.isBusy != state) {
            this.isBusy = state;
            return true;
        }
        return false;
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

    int getCamVal(){
        return camVal;
    }

    String getResult() { return result; }

    public void logDetails() {
        String detailString = "Device ID : "+devId+"\n CPU : "+cpuVal+"\n MEM : "+memVal+"\n BAT : "+batVal+" % Socket : "+socket;
        Log.i("info",detailString);
    }
}
