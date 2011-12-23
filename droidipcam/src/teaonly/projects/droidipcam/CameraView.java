package teaonly.projects.droidipcam;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Handler;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.System;
import java.lang.Thread;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class CameraView extends View implements SurfaceHolder.Callback, View.OnTouchListener{

    private AudioManager mAudioManager = null; 
    private Camera myCamera = null;
    private SurfaceHolder myCamSHolder;
    private SurfaceView	myCameraSView;
    private MainActivity myActivity;
    private Camera.Size preSize;

    public CameraView(Context c, AttributeSet attr){
        super(c, attr);
        
        myActivity = (MainActivity)c;

        mAudioManager = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true); 
    }

    public void SetupCamera(SurfaceView sv){    	
    	myCameraSView = sv;
    	myCamSHolder = myCameraSView.getHolder();
    	myCamSHolder.addCallback(this);
    	myCamSHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        myCamera = Camera.open();
        Camera.Parameters p = myCamera.getParameters();
        myCamera.setParameters(p);
        preSize = myCamera.new Size(0, 0);

        setOnTouchListener(this);
    }
   
    public void StartStreaming() {
        
    }
    
    public void StopStreaming() {
            
    }

    @Override
    public void surfaceChanged(SurfaceHolder sh, int format, int w, int h){
    	if ( myCamera != null) {
            myCamera.stopPreview();
            try {
                myCamera.setPreviewDisplay(sh);
            } catch ( Exception ex) {
                ex.printStackTrace(); 
            }
            myCamera.startPreview();
        }
    }
    
	@Override
    public void surfaceCreated(SurfaceHolder sh){
    }
    
	@Override
    public void surfaceDestroyed(SurfaceHolder sh){
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        return true;        
    }

}
