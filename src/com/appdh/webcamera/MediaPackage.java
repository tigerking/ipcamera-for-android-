package com.appdh.webcamera;


class MediaPackage
{
	public static final byte[] FlvHeader={
		(byte)0x46,(byte)0x4c,(byte)0x56,(byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x09,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x12,(byte)0x00,(byte)0x00
		,(byte)0xb6,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x02,(byte)0x00,(byte)0x0a,(byte)0x6f,(byte)0x6e,(byte)0x4d,(byte)0x65,(byte)0x74
		,(byte)0x61,(byte)0x44,(byte)0x61,(byte)0x74,(byte)0x61,(byte)0x08,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x07,(byte)0x00,(byte)0x08,(byte)0x64,(byte)0x75,(byte)0x72,(byte)0x61
		,(byte)0x74,(byte)0x69,(byte)0x6f,(byte)0x6e,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x77
		,(byte)0x69,(byte)0x64,(byte)0x74,(byte)0x68,(byte)0x00,(byte)0x40,(byte)0x84,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x06,(byte)0x68
		,(byte)0x65,(byte)0x69,(byte)0x67,(byte)0x68,(byte)0x74,(byte)0x00,(byte)0x40,(byte)0x7e,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x09
		,(byte)0x66,(byte)0x72,(byte)0x61,(byte)0x6d,(byte)0x65,(byte)0x72,(byte)0x61,(byte)0x74,(byte)0x65,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
		,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x0c,(byte)0x76,(byte)0x69,(byte)0x64,(byte)0x65,(byte)0x6f,(byte)0x63,(byte)0x6f,(byte)0x64,(byte)0x65,(byte)0x63,(byte)0x69,(byte)0x64
		,(byte)0x00,(byte)0x40,(byte)0x1c,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x0d,(byte)0x76,(byte)0x69,(byte)0x64,(byte)0x65,(byte)0x6f
		,(byte)0x64,(byte)0x61,(byte)0x74,(byte)0x61,(byte)0x72,(byte)0x61,(byte)0x74,(byte)0x65,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
		,(byte)0x00,(byte)0x00,(byte)0x07,(byte)0x65,(byte)0x6e,(byte)0x63,(byte)0x6f,(byte)0x64,(byte)0x65,(byte)0x72,(byte)0x02,(byte)0x00,(byte)0x0b,(byte)0x4c,(byte)0x61,(byte)0x76
		,(byte)0x66,(byte)0x35,(byte)0x32,(byte)0x2e,(byte)0x38,(byte)0x37,(byte)0x2e,(byte)0x31,(byte)0x00,(byte)0x08,(byte)0x66,(byte)0x69,(byte)0x6c,(byte)0x65,(byte)0x73,(byte)0x69
		,(byte)0x7a,(byte)0x65,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x09,(byte)0x00,(byte)0x00
		,(byte)0x00,(byte)0xc1
	};
	
	static byte[] videoHeader = null;

	static public void buildVideoHeader(byte[] sps, int spsLen, byte[] pps, int ppsLen)
	{
		if ( videoHeader == null)
			videoHeader = new byte[spsLen + ppsLen + 31];

		//FLV_TAG_TYPE_VIDEO
		//{
		videoHeader[0] = (byte)0x09;			
		//	frame data size
		videoHeader[1] = 0; 
		videoHeader[2] = 0 ; 
		videoHeader[3] = (byte)(16 + spsLen + ppsLen);
		//	frame timestatmp
		videoHeader[4] = 0;
		videoHeader[5] = 0;
		videoHeader[6] = 0;
		videoHeader[7] = 0;
		//	StreamID
		videoHeader[8] = 0;
		videoHeader[9] = 0;
		videoHeader[10] = 0;
		//	frame data begin
		//	{
		//		Frametype and CodecID
		videoHeader[11] = (byte)0x17;
		videoHeader[12] = 0x00;
		//		Composition time
		videoHeader[13] = 0x00;
		videoHeader[14] = 0x00;
		videoHeader[15] = 0x00;
		//		Version
		videoHeader[16] = 0x01;
		//		profile&level
		videoHeader[17] = sps[1];
		videoHeader[18] = sps[2];
		videoHeader[19] = sps[3];
		//		reserved
		videoHeader[20] = (byte)0xff;
		videoHeader[21] = (byte)0xe1;
		//		sps_size&data
		videoHeader[22] = (byte)(spsLen >> 8);
		videoHeader[23] = (byte)(spsLen & 0xFF);
		for(int i = 0; i < spsLen; i++)
			videoHeader[24+i] = sps[i];
		//		pps_size&data
		videoHeader[24+spsLen] = 0x01;
		videoHeader[25+spsLen] = (byte)(ppsLen >> 8);
		videoHeader[26+spsLen] = (byte)(ppsLen & 0xFF);
		for(int i = 0; i < ppsLen; i++)
			videoHeader[27+spsLen+i] = pps[i];
		//	}
		//	LastTagsize
		videoHeader[27+spsLen+ppsLen] = 0;
		videoHeader[28+spsLen+ppsLen] = 0;
		videoHeader[29+spsLen+ppsLen] = 0;
		videoHeader[30+spsLen+ppsLen] = (byte)(27+spsLen+ppsLen);
		//}
	}

	static int buildFlvPackage(byte[] nal, int nalLength,long ts,int flag, byte[] output)
	{
		//frame tag
		output[0] = (byte)0x09;
		//frame data length
		output[1] = (byte)(((nalLength + 5)>>16)&0xff);
		output[2] = (byte)(((nalLength + 5)>>8)&0xff);
		output[3] = (byte)((nalLength + 5)&0xff);
		//frame time stamp
		output[4] = (byte)((ts>>16)&0xff);
		output[5] = (byte)((ts>>8)&0xff);
		output[6] = (byte)(ts&0xff);
		//frame time stamp extend
		output[7] = (byte)((ts>>24)&0xff);
		//frame reserved
		output[8] = 0;
		output[9] = 0;
		output[10] = 0;


		if ( flag == 1)
			output[11] = (byte)0x17;
	   	else
			output[11] = (byte)0x27;

		output[12] = 1;
		
		output[13] = 0;
		output[14] = 0;
		output[15] = 0;
		
		//System.arraycopy(output, 16, nal, 0, nalLength);
		for(int i = 0; i < nalLength; i++)
			output[16+i] = nal[i];

		int tl = 16 + nalLength;
		output[16 + nalLength] = (byte)((tl>>24)&0xff);  
		output[17 + nalLength] = (byte)((tl>>16)&0xff);  
		output[18 + nalLength] = (byte)((tl>>8)&0xff);  
		output[19 + nalLength] = (byte)(tl&0xff);  

		return 20 + nalLength;
	}
	
/**********************MKV implement*******************************************/
	
	//Internal buffer for output
	final static int MAX_FRAME_SIZE = 64*1024;
	static public byte[] mediaBuffer = new byte[MAX_FRAME_SIZE];
	static public int	mediaLength;	
	static char[]  clusterBuffer = new char[1024];
	static int clusterBufferLength;	
	
	//736 480	
	static final int tc_scale = 20;		

	static private void clusterAppendData(char[] data, int offset, int size)
	{
		/*
		System.arraycopy(clusterBuffer, clusterBufferLength, data, offset, size);
		clusterBufferLength += size;
		*/
		
		for(int i = 0; i < size; i++)
		{
			clusterBuffer[clusterBufferLength] = data[i+offset];
			clusterBufferLength++;
		}
		
	}
	
	static private void rootAppendByteData( byte[] data, int offset, int size )
	{
		/*
		System.arraycopy(mediaBuffer, mediaLength, data, offset, size);			
		mediaLength += size;
		*/
		
		
		for(int i = 0; i < size; i++)
		{
			mediaBuffer[mediaLength] = data[i+offset];
			mediaLength++;
		}		
		
	}
	static private void rootAppendData( char[] data, int offset, int size )
	{
		for(int i = 0; i < size; i++)
		{
			mediaBuffer[mediaLength] = (byte)data[i+offset];
			mediaLength++;
		}
	}
	
	static private void rootWriteID(long id )
	{
		char[] c_id = { (char)((id >> 24)&0xFF), (char)((id >> 16)&0xFF), (char)((id >> 8)&0xFF), (char)(id&0xFF) };
				
		if( c_id[0] != 0 )
		{
			rootAppendData(c_id, 0, 4 );
			return;
		}
		if( c_id[1] != 0)
		{
			rootAppendData(c_id, 1, 3 );
			return;
		}
		if( c_id[2] != 0)
		{
			rootAppendData(c_id, 2, 2 );
			return;
		}
		rootAppendData(c_id, 3, 1 );
		
	}
	
	static private void rootWriteSize(long size )
	{
		char[] c_size = { (char)0x08, (char)((size >> 24)&0xff), (char)((size >> 16)&0xff), (char)((size >> 8)&0xff), (char)(size&0xff) };

		if( size < 0x7f )
		{
			c_size[4] |= 0x80;
			rootAppendData( c_size,4, 1 );
			return;
		}
		if( size < 0x3fff )
		{
			c_size[3] |= 0x40;
			rootAppendData( c_size,3, 2 );
			return;
		}
		if( size < 0x1fffff )
		{
			c_size[2] |= 0x20;
			rootAppendData(c_size, 2, 3 );
			return;
		}
		if( size < 0x0fffffff )
		{         
			c_size[1] |= 0x10;
			rootAppendData( c_size, 1, 4 );
			return;
		}

		rootAppendData( c_size, 0, 5 );
	}
	
	
	static private void clusterWriteID(long id )
	{
		char[] c_id = { (char)((id >> 24)&0xFF), (char)((id >> 16)&0xFF), (char)((id >> 8)&0xFF), (char)(id&0xFF) };
				
		if( c_id[0] != 0 )
		{
			clusterAppendData(c_id, 0, 4 );
			return;
		}
		if( c_id[1] != 0)
		{
			clusterAppendData(c_id, 1, 3 );
			return;
		}
		if( c_id[2] != 0)
		{
			clusterAppendData(c_id, 2, 2 );
			return;
		}
		clusterAppendData(c_id, 3, 1 );
		
	}
	
	static private void clusterWriteSize(long size )
	{
		char[] c_size = { (char)0x08, (char)((size >> 24)&0xff), (char)((size >> 16)&0xff), (char)((size >> 8)&0xff), (char)(size&0xff) };

		if( size < 0x7f )
		{
			c_size[4] |= 0x80;
			clusterAppendData( c_size,4, 1 );
			return;
		}
		if( size < 0x3fff )
		{
			c_size[3] |= 0x40;
			clusterAppendData( c_size,3, 2 );
			return;
		}
		if( size < 0x1fffff )
		{
			c_size[2] |= 0x20;
			clusterAppendData(c_size, 2, 3 );
			return;
		}
		if( size < 0x0fffffff )
		{         
			c_size[1] |= 0x10;
			clusterAppendData( c_size, 1, 4 );
			return;
		}

		clusterAppendData( c_size, 0, 5 );
	}
	
	static private void clusterWriteUNIT(long id, long ui)
	{		
		char[] c_ui = { (char)((ui >> 56)&0xFF), (char)((ui >> 48)&0xFF), (char)((ui >> 40)&0xFF), (char)((ui >> 32)&0xFF), (char)((ui >> 24)&0xFF), (char)((ui >> 16)&0xFF), (char)((ui >> 8)&0xFF), (char)(ui&0xFF) };
		int i = 0;
 
		clusterWriteID( id );
		
		while( i < 7 && (c_ui[i] == 0) )
			++i;
			
		//CHECK( mk_write_size( c, 8 - i ) );
		clusterWriteSize(8-i);
		//CHECK( mk_append_context_data( c, c_ui+i, 8 - i ) );     
		clusterAppendData(c_ui,i,8-i);
	}
	
	//Adding new video frame begin with 0x00, [0x**, 0x**, 0x**] + NAL 
	static public void writeFrame(byte[] data, int size, int intraFlag, long ts_ms)
	{
		//init
		clusterBufferLength = 0;
		mediaLength = 0;
		
		//update cluster's timestamp
		long cluster_ts = ts_ms * tc_scale;
		clusterWriteUNIT(0xe7, cluster_ts);	
		
		//package header info
		clusterWriteID(0xa3);			//a video block
		clusterWriteSize(size+4);		//whoel video block size
		clusterWriteSize(1);			//track number
		char[] deltaFlags = {(char)0, (char)0, (char)((intraFlag<<7)&0xff)};
		clusterAppendData(deltaFlags,0,3);
		
		//building whole cluster package
		rootWriteID( 0x1f43b675 );				//cluster ID
		rootWriteSize( clusterBufferLength );			
		rootAppendData( clusterBuffer, 0, clusterBufferLength);			
		rootAppendByteData( data, 0, size);		
	}
	
/*
	public static void main(String[] a) throws Exception {
		
		//Header info output
		FileOutputStream fos = new FileOutputStream (new File("./test.flv"));
		fos.write(FlvHeader, 0, FlvHeader.length);
		fos.flush();
		
		byte[] fileBuffer = new byte[64*1024];
		byte[] packageBuffer = new byte[64*1024];
		int n;
		int packageLength;

		File headers = new File("../subs/output_0.h264");
		InputStream fheader = new FileInputStream(headers.getAbsolutePath());
		fheader.read(fileBuffer, 0, 4+9+4+4);
		
		byte[] sps = new byte[9];
		for(int i = 0; i < 9; i++)
			sps[i] = fileBuffer[4+i];
		
		byte[] pps = new byte[4];

		for(int i = 0; i < 4; i++)
			pps[i] = fileBuffer[4+9+4+i];

		buildVideoHeader(sps,pps);
		fos.write(videoHeader,0,videoHeader.length);
		fos.flush();
		
		n = fheader.read(fileBuffer);
		fheader.close();

		n = n - 4;
		fileBuffer[0] = (byte)((n>>24)&0xff);
		fileBuffer[1] = (byte)((n>>16)&0xff);
		fileBuffer[2] = (byte)((n>>8)&0xff);
		fileBuffer[3] = (byte)(n&0xff);

		packageLength = buildPackage(fileBuffer, n+4, 0, 1, packageBuffer);
		fos.write(packageBuffer, 0, packageLength);
		fos.flush();

		for(int i = 1; i < 10; i++)
		{
			File h264 = new File("../subs/output_" + i%10 + ".h264");
			InputStream fin = new FileInputStream( h264.getAbsolutePath());
			n = fin.read(fileBuffer);
			fin.close();
			
			n = n - 4;
			fileBuffer[0] = (byte)((n>>24)&0xff);
			fileBuffer[1] = (byte)((n>>16)&0xff);
			fileBuffer[2] = (byte)((n>>8)&0xff);
			fileBuffer[3] = (byte)(n&0xff);

			packageLength = buildPackage(fileBuffer, n+4, i*40, 0, packageBuffer);
			fos.write(packageBuffer, 0, packageLength);
			fos.flush();
		}
		fos.close();
	}
*/

}
