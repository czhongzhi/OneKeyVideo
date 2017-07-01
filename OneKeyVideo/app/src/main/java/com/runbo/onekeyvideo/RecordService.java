package com.runbo.onekeyvideo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class RecordService extends Service {
    private final String TAG = "RecordService";

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder builder;
    private static final int NOTIFY_ID = 0;

    private KeyBroadcastReceiver mKeyBroadcastReceiver;

    public RecordService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerKeyReceiver();
        showNotification();
    }

    @Override
    public void onDestroy() {
        cancelNotification();
        unregisterKeyReceiver();
        super.onDestroy();
    }

    private void registerKeyReceiver(){
        if(mKeyBroadcastReceiver != null)
            return;
        mKeyBroadcastReceiver = new KeyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.MyBroadCast.BROADCAST_VIDEO_DOWN);
        filter.addAction(Constants.MyBroadCast.BROADCAST_VIDEO_UP);
        registerReceiver(mKeyBroadcastReceiver,filter);
    }

    private void unregisterKeyReceiver(){
        if(mKeyBroadcastReceiver != null){
            unregisterReceiver(mKeyBroadcastReceiver);
            mKeyBroadcastReceiver = null;
        }
    }


    private long time_start, time_stop, last, firstClick, lastClick; // 按下，释放的时间
    public static boolean is_recording = false; // 是否在录像
    // 计算点击的次数
    public Timer time;
    private class KeyBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Constants.MyBroadCast.BROADCAST_VIDEO_DOWN.equals(action)){// 录像键按下
                time_start = System.currentTimeMillis();
                start_timer();
            }else if (Constants.MyBroadCast.BROADCAST_VIDEO_UP.equals(action)){// 录像键松开
                stop_timer();
                time_stop = System.currentTimeMillis();
                last = (time_stop - time_start) / 1000;
                if (last < 1.5) {
                    if (is_recording) {
                        sendBroadcast(new Intent(Constants.MyBroadCast.BROADCAST_TAKEPICTURE));// 拍照
                    }
                }
            }
        }
    }

    public void start_timer() {
        time = new Timer();
        time.schedule(new TimerTask() {
            @Override
            public void run() {
                if (is_recording) {
                    sendBroadcast(new Intent(Constants.MyBroadCast.BROADCAST_STOPRECORD));// 停止录像
                    is_recording = false;
                } else {
                    Intent record_intent = new Intent(getApplicationContext(), RecordActivity.class);// 跳转到录像界面并开始
                    record_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(record_intent);
                    is_recording = true;
                }

            }
        }, 2000); // timeTask
    }

    public void stop_timer() {
        if (time != null) {
            time.cancel();
            time = null;
        }
    }

    private void showNotification(){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(getBaseContext(),MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(RecordService.this,0,intent,0);

        builder = new NotificationCompat.Builder(getBaseContext());
        builder.setContentTitle("一键录像")
                .setContentText("Service Running!")
                .setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("一键录像Service");

        builder.setContentIntent(pendingIntent);

        mNotificationManager.notify(NOTIFY_ID, builder.build());
    }

    private void cancelNotification(){
        mNotificationManager.cancel(NOTIFY_ID);
    }
}
