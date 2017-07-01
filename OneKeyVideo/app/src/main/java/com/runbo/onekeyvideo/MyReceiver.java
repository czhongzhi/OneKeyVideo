package com.runbo.onekeyvideo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class MyReceiver extends BroadcastReceiver {
    private final String TAG = "MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG,"MyReceiver: "+action);
        if(action.equals("android.intent.action.BOOT_COMPLETED")){
            try {
                Thread.sleep(15000);//睡眠15秒再启动POC,防止电信卡自登不成功
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent recordService = new Intent(context,RecordService.class);
            context.startService(recordService);
        }

    }


}
