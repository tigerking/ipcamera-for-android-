package com.appdh.webcamera;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.SystemClock;
import android.util.Log;


public class StreamingKernel implements Runnable
{
	//Local data loopback
	private LocalSocket receiver,sender;			
	private LocalServerSocket lss;		
	private String localAddress;
	private long frameDuration;

	//internal video buffer and frame timing
	static VideoBuffer videoBuffer = null;
	static TimeStampEstimator frameTimeStamp = null;
	
	/*
	static FileOutputStream localFLV;
	static byte[] localBuffer = new byte[1024*64];
	*/
	
	public StreamingKernel (String addr, long duration)	
	{
		localAddress = addr;
		frameDuration = duration;
		
		if ( videoBuffer == null )
			videoBuffer = new VideoBuffer(128, 64*1024);				//about two seconds
		if ( frameTimeStamp == null)
			frameTimeStamp = new TimeStampEstimator(frameDuration);
	}
	
	
	public void repareStreaming()
	{
		initLoopback();
		videoBuffer.reset();
		frameTimeStamp.reset(frameDuration);
	}

	public FileDescriptor getTargetFileDescriptor()
	{
		return sender.getFileDescriptor();
	}
	
	public void stopStreaming()
	{
		releaseLoopback();
	}
	public int getVideoFlag()
	{
		return videoBuffer.getVideoFlag();
	}
	public long getTimeStamp()
	{
		return videoBuffer.getTimeStamp();
	}
	public byte[] getReadBuffer()
	{
		return videoBuffer.getReadBuffer();
	}
	public int getReadLength()
	{
		return videoBuffer.getReadLength();
	}
	public void releaseRead()
	{
		videoBuffer.releaseRead();
	}
	
	private void releaseLoopback()
	{
		try {
			if ( lss != null){
				lss.close();
			}
			if ( receiver != null){
				receiver.close();
			}
			if ( sender != null){
				sender.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.d("TEAONLY", e1.toString());			
		}

		lss = null;
		sender = null;
		receiver = null;
	}

	private boolean initLoopback()
	{		
		try {
			if ( lss != null){
				lss.close();
			}
			if ( receiver != null){
				receiver.close();
			}
			if ( sender != null){
				sender.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.d("TEAONLY", e1.toString());
			return false;
		}

		receiver = new LocalSocket();
		try {
			lss = new LocalServerSocket(localAddress);
			receiver.connect(new LocalSocketAddress(localAddress));
			receiver.setReceiveBufferSize(1000);
			receiver.setSendBufferSize(1000);
			sender = lss.accept();
			sender.setReceiveBufferSize(1000);
			sender.setSendBufferSize(1000);
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.d("TEAONLY", e1.toString());			
			return false;
		}		

		return true;
	}

	public void run()
	{
		InputStream fis;
		final int frame_size = 64*1024;                                                          
		byte[] buffer = new byte[frame_size];                   
		int dlen;
		int package_size;

		//Create video byte stream object               
		try {
			fis = receiver.getInputStream();
			
			/*
			File targetFLV = new File("/sdcard/temp.flv");
			localFLV = new FileOutputStream(targetFLV);
			
			localFLV.write(MediaPackage.FlvHeader);
			MediaPackage.buildVideoHeader(MediaDetect.spsData, MediaDetect.spsDataLength, MediaDetect.ppsData, MediaDetect.ppsDataLength );
			localFLV.write(MediaPackage.videoHeader);
			*/			
			
		} catch (IOException e1) {                              
			return;
		}   

		//jump header offset
		try {                                                                                                  
			dlen = -1;
			dlen = fis.read(buffer,0,32);
			if(dlen != 32){                     
				fis.close();
				return;
			}                                       
		} catch (IOException e) {                   
			return;
		}   
		
		//First frame duration computing
		frameTimeStamp.setFirstFrameTiming();

		while(true){
			dlen = fillBuffer(buffer,0,4,fis);
			if(dlen != 4 ){
				Log.d("TEAONLY", "Reader Package's Header error!");
				break;
			}

			package_size = (buffer[1]&0xFF)*65536 + (buffer[2]&0xFF)*256 + (buffer[3]&0xFF);

			dlen = fillBuffer(buffer, 4, package_size,fis);
			if(dlen != package_size){
				Log.d("TEAONLY", "Reader Package's data error dlen = " + dlen);
				break;
			}

			while(true)
			{
				if ( videoBuffer.isEmptySpace() ){					
					addNewPackage(buffer, package_size +4);
					break;
				}
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}

				//Exiting service
				if ( lss == null){
					return;
				}
				
			}
		}	

	}
	
	private void addNewPackage(byte[] buf, int size)
	{			
		videoBuffer.writeFrame(buf, size, frameTimeStamp.getSequenceTimeStamp(), 0);
		frameTimeStamp.update();
	}

	private int fillBuffer(byte [] buf,int offset, int size, InputStream fis)
	{   
		int dlen;
		int buf_len = 0;
		int target_size = size;
		while(target_size > 0)
		{    
			try {
				dlen = fis.read(buf, offset + buf_len, target_size);
			} catch (IOException e) {    
				e.printStackTrace();    
				Log.d("TEAONLY", "Read streaming exception!");    
				return -1; 
			}   

			if(dlen >= 0){ 
				buf_len += dlen;
				target_size -= dlen;
			} else {
				return -1; 
			}   
		}   

		return size;
	} 

	//Video Ring Buffer manager
	public class VideoBuffer
	{
		private class VideoPackage
		{
			byte[] data;
			int size;
			boolean flag;
			int vflag;
			long ts;
		}
		VideoPackage[] buffers;

		int readIndex = 0;
		int writeIndex = 0;

		public VideoBuffer(int bufferNum, int pkgSize)
		{
			buffers = new VideoPackage[bufferNum];

			for(int i=0; i < bufferNum; i++){		
				VideoPackage vPkg = new VideoPackage();
				vPkg.data = new byte[pkgSize];
				buffers[i] = vPkg;
			}
			reset();
		}

		public void reset()
		{			
			for(int i = 0; i < buffers.length; i++ ){
				VideoPackage vPkg = buffers[i];
				vPkg.size = 0;
				vPkg.flag = false;
				vPkg.ts = 0;
			}
			readIndex = 0;
			writeIndex = 0;			
		}

		private void updateWriteIndex()
		{
			if ( writeIndex < (buffers.length - 1) )
				writeIndex ++;
			else
				writeIndex = 0;
		}
		
		public int getVideoFlag()
		{
			VideoPackage vPkg = buffers[readIndex];
			if ( vPkg.flag == true)
				return vPkg.vflag;

			return -1;
		}
		
		public long getTimeStamp()
		{
			VideoPackage vPkg = buffers[readIndex];
			if ( vPkg.flag == true)
				return vPkg.ts;

			return 0;
		}
		public byte[] getReadBuffer()
		{
			VideoPackage vPkg = buffers[readIndex];
			if ( vPkg.flag == true)
				return vPkg.data;

			return null;
		}
		public int getReadLength()
		{
			VideoPackage vPkg = buffers[readIndex];
			if ( vPkg.flag == true)
				return vPkg.size;

			return -1;
		}
		public void releaseRead()
		{
			synchronized(this){
				VideoPackage vPkg = buffers[readIndex];
				if ( vPkg.flag == false)
					return;

				vPkg.flag = false;

				if ( readIndex < (buffers.length - 1) )
					readIndex ++;
				else
					readIndex = 0;
			}
		}
		
		public boolean isEmptySpace()
		{
			VideoPackage vPkg = buffers[writeIndex];
			if ( vPkg.flag == true)
				return false;
			
			return true;
		}
		
		public boolean writeFrame(byte[] newData, int size, long ts, int vflag)
		{					

			VideoPackage vPkg = buffers[writeIndex];
			if ( vPkg.flag == true)
				return false;

			System.arraycopy(newData, 0, vPkg.data, 0, size);							
			vPkg.size = size;
			vPkg.ts = ts;
			vPkg.vflag = vflag;
			
			synchronized(this){
				vPkg.flag = true;
				updateWriteIndex();
				return true;
			}
		}

	}

	
	//Frame smooth timestamp generator
	class TimeStampEstimator
	{
		final int durationHistoryLength = 2048;
		private long durationHistory[];
		int durationHistoryIndex = 0;
		long durationHistorySum = 0;				
		long lastFrameTiming = 0;			
		long sequenceDuration = 0; 
	
		public void update()
		{
			long currentFrameTiming = SystemClock.elapsedRealtime();
			long newDuration = currentFrameTiming - lastFrameTiming;
			lastFrameTiming = currentFrameTiming;
			
			durationHistorySum -= durationHistory[durationHistoryIndex];
			durationHistorySum += newDuration;
			durationHistory[durationHistoryIndex] = newDuration;
			durationHistoryIndex ++;
			if ( durationHistoryIndex >= durationHistoryLength)
				durationHistoryIndex = 0;

			//ºÁÃëÎªµ¥Î»
			sequenceDuration += (int)(( 1.0 * durationHistorySum / durationHistoryLength));
		}
		
		public void setFirstFrameTiming()
		{
			lastFrameTiming = SystemClock.elapsedRealtime() - durationHistorySum/durationHistoryLength;
			sequenceDuration = 0;
		}

		public long getSequenceTimeStamp()
		{
			return sequenceDuration;
		}

		public void reset(long frameDuration)
		{
			if ( durationHistory == null)
				durationHistory = new long[durationHistoryLength];
			durationHistorySum = 0;
			for(int i = 0; i < durationHistoryLength; i++)
			{
				durationHistory[i] = frameDuration;			//us
				durationHistorySum += frameDuration;
			}

			lastFrameTiming = 0;
			sequenceDuration = 0;
			durationHistoryIndex = 0;
		}
		
		public TimeStampEstimator(long frameDuration)
		{
			reset( frameDuration );
		}
	}

}
