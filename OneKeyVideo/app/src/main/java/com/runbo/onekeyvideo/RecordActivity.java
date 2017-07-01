package com.runbo.onekeyvideo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;


@SuppressLint("NewApi")
public class RecordActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "RecordActivity";
    private SurfaceView mSurfaceview;
    private boolean mStartedFlg = false;//是否正在录像
 //   private boolean mIsPlay = false;//是否正在播放录像
    private MediaRecorder mRecorder;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private String path;

    private  MediaPlayer player;

    private SensorManager sm;
    private Sensor ligthSensor;
    private MySensorListener sensorListener;
    private int currCameraType = 0;
    private int ligthCount = 0;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    Log.e("CZZ","--------");
                    break;
            }
        }
    };

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.record_layout);
        
        registReceiver();

        mSurfaceview = (SurfaceView) findViewById(R.id.capture_surfaceview);
        SurfaceHolder holder = mSurfaceview.getHolder();
        holder.addCallback(this);
        // setType必须设置，要不出错.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //获取Android 光感Sensor的值
        //获取SensorManager对象
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        //获取Sensor对象
        ligthSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorListener = new MySensorListener();
        sm.registerListener(sensorListener,ligthSensor,SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销感光监听器
        sm.unregisterListener(sensorListener,ligthSensor);

        unregistReceiver();
        RecordService.is_recording = false;
        mSurfaceview = null;
        mSurfaceHolder = null;

        if (mRecorder != null) {
            mRecorder.release(); // Now the object cannot be reused
            mRecorder = null;
            Log.d(TAG, "surfaceDestroyed release mRecorder");
        }
        if (camera != null) {
            camera.release();
            camera = null;
        }

        saveFile(20);//保留最近20个文件
    }

    private BroadcastReceiver cameraReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Constants.MyBroadCast.BROADCAST_STOPRECORD)) {//录像结束
                finish();
            }else if(action.equals(Constants.MyBroadCast.BROADCAST_TAKEPICTURE)){//拍照
                if (mStartedFlg) {
                    takePicture();
                    playVoiceHint(R.raw.takepicture);
                }
            }
        }
    };

    /**
     * 播放语音提示 czz add 2017.01.02
     */
    public void playVoiceHint(int rawID) {
        player = MediaPlayer.create(this, rawID);
        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {// 播出完毕事件
            @Override
            public void onCompletion(MediaPlayer arg0) {
                player.release();
            }
        });
    }

    private void registReceiver()
    {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Constants.MyBroadCast.BROADCAST_STOPRECORD);
        mIntentFilter.addAction(Constants.MyBroadCast.BROADCAST_TAKEPICTURE);
        registerReceiver( cameraReceiver, mIntentFilter );
    }

    private  void unregistReceiver() {
        try
        {
            if( cameraReceiver!=null )
            {
                unregisterReceiver( cameraReceiver );
            }
        }
        catch( Exception e )
        {
        }
    }

    
    public void start_stoprecord(int cameraType){
        Log.e("CZZ","start_stoprecord mStartedFlg - "+mStartedFlg);
    	 if (!mStartedFlg) {
             //     handler.postDelayed(runnable,1000);
            //      mImageView.setVisibility(View.GONE);
                  if (mRecorder == null) {
                      mRecorder = new MediaRecorder();
                  }

                    //摄像头信息，分为前置/后置摄像头 Camera.CameraInfo.CAMERA_FACING_FRONT：前置
                   //Camera.CameraInfo.CAMERA_FACING_BACK：后置
                  currCameraType = cameraType;
                  camera = Camera.open(cameraType);
                  if (camera != null) {
                      camera.setDisplayOrientation(90);
                      camera.unlock();
                      mRecorder.setCamera(camera);
                  }


                  try {
                      // 这两项需要放在setOutputFormat之前
                      mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                      mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                      // Set output file format
                      mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

                      // 这两项需要放在setOutputFormat之后
                      mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                      mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

                      mRecorder.setVideoSize(1280, 720);
                      mRecorder.setVideoFrameRate(30);
                      mRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);
                      if(cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT){
                          mRecorder.setOrientationHint(270);
                      }else{
                          mRecorder.setOrientationHint(90);
                      }
                      //设置记录会话的最大持续时间（毫秒）
 //                     mRecorder.setMaxDuration(30 * 1000);
                      mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

                      path = getSDPath();
                      if (path != null) {
                          File dir = new File(path + "/record");
                          if (!dir.exists()) {
                              dir.mkdir();
                          }

                          path = dir + "/" + getDate() + ".mp4";
                          mRecorder.setOutputFile(path);
                          mRecorder.prepare();
                          mRecorder.start();
                          mStartedFlg = true;

                      }
                  } catch (Exception e) {
                      e.printStackTrace();
                  }
              } else {
                  //stop
                  if (mStartedFlg) {
                      try {
         //                 handler.removeCallbacks(runnable);
                          mRecorder.stop();
                          mRecorder.reset();
                          mRecorder.release();
                          mRecorder = null;
                          if (camera != null) {
                              camera.release();
                              camera = null;
                          }
                      } catch (Exception e) {
                          e.printStackTrace();
                      }
                  }
                  mStartedFlg = false;
                  RecordService.is_recording = false;
                  finish();
              }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    public void takePicture() {
    	         if (camera != null) {
    	             try {
    	                 camera.takePicture(null, null, new MyPictureCallback());
    	             } catch (Exception e) {
    	                 e.printStackTrace();
    	             }
    	         }
    	     }


    /**
     * 获取系统时间
     *
     * @return
     */
    public static String getDate() {
    	 Date date = new Date();  
         SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间  
         String filename = format.format(date);  

        return filename;
    }
    
    /** 
     * 将拍下来的照片存放在SD卡中 
     * @param data   
     * @throws IOException 
     */  
    public static void saveToSDCard(byte[] data) throws IOException {  
        String filename = getDate() + ".jpg";  
        File fileFolder = new File(Environment.getExternalStorageDirectory()+ "/record_pic/");  
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"finger"的目录  
            fileFolder.mkdir();  
        }  
        File jpgFile = new File(fileFolder, filename);  
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流  
        outputStream.write(data); // 写入sd卡中  
        outputStream.close(); // 关闭输出流  
        
        Bitmap bitmap = new BitmapFactory().decodeFile(Environment.getExternalStorageDirectory()+ "/record_pic/"+filename);
        Bitmap mp =  rotaingImageView(90, bitmap);
        
        File file=new File(Environment.getExternalStorageDirectory()+"/record_pic/"+filename);//将要保存图片的路径
        try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                mp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
        } catch (IOException e) {
                e.printStackTrace();
        }
    }  
    
    
    public static Bitmap rotaingImageView(int angle , Bitmap bitmap) {  
        //旋转图片 动作  
        Matrix matrix = new Matrix();;
        matrix.postRotate(angle);  
        System.out.println("angle2=" + angle);  
        // 创建新的图片  
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,  
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);  
        return resizedBitmap;  
    }
    
    private final class MyPictureCallback implements PictureCallback {  
    	  
        @Override  
        public void onPictureTaken(byte[] data, Camera camera) {  
            try {  
                saveToSDCard(data); // 保存图片到sd卡中  
  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
    }  

    /**
     * 获取SD path
     *
     * @return
     */
    public String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
            return sdDir.toString();
        }

        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    	mSurfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // 将holder，这个holder为开始在onCreate里面取得的holder，将它赋给mSurfaceHolder
    	mSurfaceHolder = surfaceHolder;
    	start_stoprecord(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceview = null;
        mSurfaceHolder = null;
       
  //      handler.removeCallbacks(runnable);
        if (mRecorder != null) {
            mRecorder.release(); // Now the object cannot be reused
            mRecorder = null;
            Log.d(TAG, "surfaceDestroyed release mRecorder");
        }
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
    
    private File parentFile;
    private static String filepath=Environment.getExternalStorageDirectory().getPath()+File.separator+"record";
    		
          private void saveFile(int n){
        	//保留n个文件
    			parentFile = new File(filepath);
    	        Log.i(TAG, parentFile+"");
    	        File[] files =parentFile.listFiles();
    	        Log.i(TAG, files+"");
    	        ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();//将需要的子文件信息存入到FileInfo里面  
    	                for (int i = 0; i < files.length; i++) {  
    	                    File file = files[i];  
    	                    FileInfo fileInfo = new FileInfo();  
    	                    fileInfo.path = file.getPath();  
    	                    fileInfo.lastModified= file.lastModified();   
    	                    fileList.add(fileInfo); 
    	                    Log.i(TAG, files[i]+"");
    	                }  
    	                Collections.sort(fileList, new FileComparator());//通过重写Comparator的实现类FileComparator来实现按文件创建时间排序。
    	                for (int i = n; i < files.length; i++) {  
    	                    FileInfo file = fileList.get(i);  
    	                    File file_delete = new File(file.getpath());
    	                    file_delete.delete();
    	                } 
          }
          
          public class FileComparator implements Comparator<FileInfo> {  //文件操作
              public int compare(FileInfo file1, FileInfo file2) {  
                  if(file1.lastModified > file2.lastModified)  
                  {  
                      return -1;  
                  }else  
                  {  
                      return 1;  
                  }  
              }  
          }  
          
          public class FileInfo{

          	private String path;
          	private long lastModified;
          	
          	public FileInfo(){
          		
          	}
          	
          	public void setpath(String path){
          		this.path = path;
          	}
          	public String getpath(){
          		return path;
          	}
          	public void stetime(Long time){
          		this.lastModified = time;
          	}
          	public long gettime(){
          		return lastModified;
          	}
          }

    private class MySensorListener implements SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent event) {
            //获取精度
            float acc = event.accuracy;
            //获取光线强度
            float lux = event.values[0];
            StringBuffer sb = new StringBuffer();
            sb.append("acc ----> " + acc);
            sb.append("\n");
            sb.append("lux ----> " + lux);
            sb.append("\n");
            Log.e("CZZ",sb.toString());

            if(lux < 150.0){
                if(currCameraType == 0){//切换到前摄
                    ligthCount = 0;
                    mStartedFlg = false;
                    releaseCamera();
                    start_stoprecord(Camera.CameraInfo.CAMERA_FACING_FRONT);
                }
            }else if(lux > 250.0){
                ligthCount++;
                if(ligthCount > 50 && currCameraType == 1){//切换到后摄
                    ligthCount = 0;
                    mStartedFlg = false;
                    releaseCamera();
                    start_stoprecord(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private void releaseCamera(){
        try {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            if (camera != null) {
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}