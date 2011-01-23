package com.appdh.webcamera;

import java.io.FileDescriptor;
import java.io.IOException;
import android.util.Log;
import android.media.MediaRecorder;

/*
 * 使用方法：
 * 1. prepareMedia()
 * 2.    setupOutput()
 * 3.	 
 */

public class MediaSource {

	//Camera 数据对象
	private MediaRecorder mRecorder;	
	private CameraView	mViewer;	
		
	//状态控制
	private boolean bInited = false;
	private boolean bPrepared = false;	
	private boolean bStreaming = false;
		
	public MediaSource(CameraView showView)
	{
		mViewer = showView;	
		bInited = false;
		bPrepared = false;
		bStreaming = false;
	}
	public boolean isInited()
	{
		return bInited;
	}
	public boolean isStreaming()
	{
		return bStreaming;
	}
	public boolean isPrepared()
	{
		return bPrepared;
	}
	public void prepareOutput(FileDescriptor targetFile)
	{
		mRecorder.setOutputFile(targetFile);
		try {
			mRecorder.prepare();
			bPrepared = true;
			return;
		} catch (IllegalStateException e) {
			e.printStackTrace();			
		} catch (IOException e) {
			e.printStackTrace();			
		}
		
		releaseMedia();		
	}
	public void prepareOutput(String targetFile)
	{	
		mRecorder.setOutputFile(targetFile);
		
		try {
			mRecorder.prepare();
			bPrepared = true;
			return;
		} catch (IllegalStateException e) {
			e.printStackTrace();			
		} catch (IOException e) {
			e.printStackTrace();			
		}
		
		releaseMedia();		
	}
	
	public void startCapture()
	{
		mRecorder.start();
		bStreaming = true;
	}
	
	public void stopCapture()
	{
		mRecorder.stop();
		releaseMedia();
	}
	
	public void initMedia()
	{					
		bInited = false;
		initCamera();		
		
		bStreaming = false;
		bPrepared = false;		
	}

	public void releaseMedia()
	{
		if ( mRecorder != null)
		{
			mRecorder.reset();
			mRecorder.release();	
			mRecorder = null;
		}
		
		bInited = false;
		bPrepared = false;
		bStreaming = false;
	}
	
	private void initCamera()
	{						
		if ( mRecorder != null){
			mRecorder.reset();
			mRecorder.release();
		}
		mRecorder = new MediaRecorder();
    	mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);  
    	   	
    	mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setVideoFrameRate(10);
        mRecorder.setVideoSize(640, 480);    
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if(mViewer.hasSurface)
		{			
			mRecorder.setPreviewDisplay(mViewer.holder.getSurface());
		}
        bInited = true;
        Log.d("TEAONLY","init Camera setting is OK! ");
	}
}
