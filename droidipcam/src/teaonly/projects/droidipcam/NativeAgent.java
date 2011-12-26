package teaonly.projects.droidipcam;

import teaonly.projects.task.*;
import java.io.*; 
import java.net.*;

import android.net.*;
import android.util.Log;
import org.xmlpull.v1.XmlSerializer;  
import android.util.Xml;

//public class NativeAgent extends GenericTask{
public class NativeAgent {
    private LocalServerSocket lss;
    private String localAddress;

    private LocalSocket cameraFoo, cameraBar;
    private LocalSocket muxerFoo, muxerBar;

    private native int nativeCheckMedia(String fileName);
    
	public NativeAgent(String addr) {
        try {
            localAddress = addr;
            lss = new LocalServerSocket(localAddress);

            ResetCameraSocket();            

        } catch ( IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean NativeCheckMedia(String filename) {
        if (nativeCheckMedia(filename) > 0)
            return true;
        else
            return false;
    }

    public void ResetDataSocket() throws IOException {
        cameraFoo = new LocalSocket();
        cameraFoo.connect(new LocalSocketAddress(localAddress));
        cameraFoo.setSendBufferSize(1024*128);

        cameraBar = lss.accept();
        cameraBar.setReceiveBufferSize(1024*128);
    }

	public OutputStream GetCameraWriteStream() throws IOException{
        return cameraFoo.getOutputStream();
    }

	public static void LoadLibraries() {
        //Local library .so files before this activity created.
        System.loadLibrary("ipcamera");		
    }

}
