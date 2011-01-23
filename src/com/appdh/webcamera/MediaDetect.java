package com.appdh.webcamera;

import java.io.*;

import android.util.Log;

class MediaDetect
{
	//We should check 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x**, 'A', 'v', 'c', 'C', 0x01 
	final static int MAX_HEADER_LENGTH = 128;
	public static byte[] spsData = new byte[MAX_HEADER_LENGTH];
	public static int spsDataLength = 0;
	public static byte[] ppsData = new byte[MAX_HEADER_LENGTH];
	public static int ppsDataLength = 0;
	public static int mdataPosition = 32;
	
	private static byte[] checkBuffer = new byte[1024*4];
	private static int headerLength;
	
	public static void writeConfig(String discFile)
	{		
		File newFile = new File(discFile);
		try{
			OutputStream os = new FileOutputStream( newFile.getAbsolutePath());
			os.write(checkBuffer, 0, headerLength);
			os.flush();
			os.close();
		}catch(IOException e){
			
		}
	}
	
	public static void getHeaderData(byte[] buffer)
	{
		/*
		put_byte(pb, 1);      // version 
        put_byte(pb, sps[1]); // profile 
        put_byte(pb, sps[2]); // profile compat 
        put_byte(pb, sps[3]); // level 
        put_byte(pb, 0xff);   // 6 bits reserved (111111) + 2 bits nal size length - 1 (11)

        put_byte(pb, 0xe1);   // 3 bits reserved (111) + 5 bits number of sps (00001) 


        put_be16(pb, sps_size);
        put_buffer(pb, sps, sps_size);
        put_byte(pb, 1);      // number of pps 
        put_be16(pb, pps_size);
        put_buffer(pb, pps, pps_size);		
		*/
		
		mdataPosition = (int)(buffer[0]&0xFF)*256 + (int)(buffer[1]&0xFF);
		
		spsDataLength = (int)(buffer[6]&0xFF)*256 + (int)(buffer[7]&0xFF);
		for(int i = 0; i < spsDataLength; i++)
			spsData[i] = buffer[8+i];
		
		ppsDataLength = (int)(buffer[9+spsDataLength]&0xFF)*256 + (int)(buffer[10+spsDataLength]&0xFF);		
		for(int i = 0; i < ppsDataLength; i++)
			ppsData[i] = buffer[11+spsDataLength+i];
		
		Log.d("TEAONLY","spsDataLength = " + spsDataLength + " ppsDataLength = " + ppsDataLength);
		
		headerLength = 6+2+2+1+spsDataLength+ppsDataLength;
		for(int i = 0; i <headerLength; i++){
			checkBuffer[i] = buffer[i];
		}
		
		checkBuffer[0] = (byte)( (mdataPosition >> 8) & 0xFF);
		checkBuffer[1] = (byte)( mdataPosition & 0xFF);
		
	}
	
	public static boolean checkMP4_MDAT(String fileName) throws IOException
	{
		File fin = new File(fileName);
		
		if( fin.isFile() ){
			InputStream is = new FileInputStream(fin.getAbsolutePath());
			int pos;
			int fms = 0;
			boolean isOK;
			int n;
			int fpos = 0;
			while(true){				
				isOK = false;
				
				n = is.read(checkBuffer);
				if ( n < 0){
					break;
				}
				
				for(pos = 0; pos < n; pos++){
					fpos++;
					byte tmp = checkBuffer[pos];
					switch(tmp)
					{
					case (byte)'m':
						if( fms == 0)
							fms = 1;
						else
							fms = 0;
						break;
					
					case (byte)'d':
						if( fms == 1)
							fms = 2;
						else
							fms = 0;
						break;
						
					case (byte)'a':
						if( fms == 2)
							fms = 3;
						else
							fms = 0;
						break;
					
					case (byte)'t':
						if ( fms == 3)
							fms = 4;
						else
							fms = 0;
						break;
						
					default:
						fms = 0;
						break;					
					}
					if(fms == 4){
						isOK = true;
						break;
					}
				}				
				if( isOK ){
			        Log.d("TEAONLY","MP4 file MDAT position is OK.**************");	
			        mdataPosition = fpos;
					return true;
				}
			}	
		}
		return false;
	}
	
	public static boolean checkMP4_MOOV(String fileName) throws IOException
	{
		File fin = new File(fileName);
		
		if( fin.isFile() ){
			InputStream is = new FileInputStream(fin.getAbsolutePath());
			int pos;
			int fms = 0;
			boolean isOK;
			int n;
			while(true){				
				isOK = false;
				
				n = is.read(checkBuffer);
				if ( n < 0){
					break;
				}
				
				for(pos = 0; pos < n; pos++){
					byte tmp = checkBuffer[pos];
					switch(tmp)
					{
					case (byte)'a':
						if( fms == 0)
							fms = 1;
						else
							fms = 0;
						break;
					
					case (byte)'v':
						if( fms == 1)
							fms = 2;
						else
							fms = 0;
						break;
						
					case (byte)'c':
						if( fms == 2)
							fms = 3;
						else
							fms = 0;
						break;
					
					case (byte)'C':
						if ( fms == 3)
							fms = 4;
						else
							fms = 0;
						break;
					case (byte)0x01:
						if ( fms == 4)
							fms = 5;
						else
							fms = 0;
						break;
					default:
						fms = 0;
						break;					
					}
					if(fms == 5){
						isOK = true;
						break;
					}
				}				
				if( isOK ){
			        Log.d("TEAONLY","MP4 file SPS PPS is OK.**************");
					//开始获取PPS以及SPS数据	
					for(int i=0; i < checkBuffer.length - pos; i++)
					{
						checkBuffer[i] = checkBuffer[i+pos];
					}
					if ( pos > checkBuffer.length - MAX_HEADER_LENGTH)
					{
						is.read(checkBuffer, checkBuffer.length - pos, MAX_HEADER_LENGTH);
					}
					
					getHeaderData(checkBuffer);
					return true;
				}
			}	
		}
		return false;	
	}

}


