package teaonly.projects.droidipcam;

import teaonly.projects.task.*;
import java.io.*; 
import java.net.*;

import android.net.*;
import android.util.Log;
import org.xmlpull.v1.XmlSerializer;  
import android.util.Xml;

public class NativeAgent extends GenericTask{
    private LocalSocket jSock, nSock; 
    private LocalServerSocket lss;
    private String localAddress;

    private LocalSocket dSend, dRecv;
    private LocalServerSocket dlss;
	
    private native int checkMedia(String fileName);
    
	public NativeAgent(String addr) {
        try {
            localAddress = addr;
            jSock = new LocalSocket();
            
            lss = new LocalServerSocket(localAddress);
            jSock.connect(new LocalSocketAddress(localAddress));
            jSock.setReceiveBufferSize(1000);
            jSock.setSendBufferSize(1000);

            nSock = lss.accept();
            nSock.setReceiveBufferSize(1000);
            nSock.setSendBufferSize(1000);
        
        } catch ( IOException ex) {
            ex.printStackTrace();
        }
    }

	public void Release() {
		try {
		
            jSock.close();            
            lss.close();
			nSock.close();
            dSend.close();
			dRecv.close();
			
        } catch ( IOException ex) {
            ex.printStackTrace();
        }
	}
	
	public void NativeBegin(String server, int port, String user, String passwd, String resource) {
		
    }

    public void NativeEnd() {
		
    }

    public boolean NativeCheckMedia(String filename) {
        if (checkMedia(filename) > 0)
            return true;
        else
            return false;
    }

    public void ResetDataSocket() throws IOException {
        dSend = new LocalSocket();
        dSend.connect(new LocalSocketAddress(localAddress));
        dSend.setSendBufferSize(1024*128);

        dRecv = lss.accept();
        dRecv.setReceiveBufferSize(1024*128);
    }

	public OutputStream GetDataOutputStream() throws IOException{
        return dSend.getOutputStream();
    }

    private void SendNativeMessage(String msg){
        Log.d("TEAONLY", "JAVA: Send " + msg + " to native!");
        byte[] sendData = msg.getBytes();
        try {
           jSock.getOutputStream().write( sendData );
        } catch ( IOException ex) {
           ex.printStackTrace();
        }        
    }

    @Override
    protected TaskResult _doInBackground(TaskParams... params) {
        byte[] receiveData = new byte[1024]; 
        int recvOffset = 0;

        while(true) {
            try {
                int ret = jSock.getInputStream().read( receiveData, recvOffset, 1);
                if ( ret < 0)
                    break;
                if ( ret == 0)
                    continue;
                if ( receiveData[recvOffset] == (byte)('>') ) {
                    String xmlBuffer = new String(receiveData, 0, recvOffset+1);
                    publishProgress(xmlBuffer);
                    recvOffset = 0;
                } else {
                    recvOffset++;
                }
                
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
        }
        
        return TaskResult.OK;
    }   

	public static void LoadLibraries() {
        //Local library .so files before this activity created.
		
    }

}
