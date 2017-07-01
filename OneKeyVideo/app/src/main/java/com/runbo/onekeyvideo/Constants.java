package com.runbo.onekeyvideo;

/**
 * Created by czz on 2017/6/21.
 */

public class Constants {

    public final static class MyBroadCast{
        public final static String BROADCAST_VIDEO_DOWN = "com.runbo.video.key.down";   //	直播开始  2016.12.15 ljb add
        public final static String BROADCAST_VIDEO_UP = "com.runbo.video.key.up";   //
        public final static String BROADCAST_VIDEO_RELEASE = "com.runbo.video.key.release";   //	再次点击finish（）

        public final static String BROADCAST_STOPRECORD = "com.runbo.record.key.stop";   //录像
        public final static String BROADCAST_TAKEPICTURE = "com.runbo.takepicture.key.start";   //拍照
    }
}
