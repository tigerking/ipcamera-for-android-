package teaonly.projects.droidipcam;

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

    
	public NativeAgent(String addr) {
        try {
            localAddress = addr;
            lss = new LocalServerSocket(localAddress);
        } catch ( IOException ex) {
            ex.printStackTrace();
        }

    }

    public void BuildCameraSocket(){
        try {
            cameraFoo = new LocalSocket();
            cameraFoo.connect(new LocalSocketAddress(localAddress));
            cameraFoo.setSendBufferSize(1024*128);

            cameraBar = lss.accept();
            cameraBar.setReceiveBufferSize(1024*128);
        } catch ( IOException ex) {
            ex.printStackTrace();                
        }
    }
    public FileDescriptor GetCameraWriteFD(){
        return cameraFoo.getFileDescriptor();
    }
    public InputStream GetCameraReadStream() throws IOException{
        return cameraFoo.getInputStream();
    }
    public void ReleaseCameraSocket() {
         try {
            cameraFoo.close();
            cameraBar.close();
        } catch ( IOException ex) {
            ex.printStackTrace();                
        }
        cameraFoo = null;
        cameraBar = null;
    }

    public void BuildMuxerSocket(){
        try {
            muxerFoo = new LocalSocket();
            muxerFoo.connect(new LocalSocketAddress(localAddress));
            muxerFoo.setSendBufferSize(1024*128);

            muxerBar = lss.accept();
            muxerBar.setReceiveBufferSize(1024*128);
        } catch ( IOException ex) {
            ex.printStackTrace();                
        }
    }
    public FileDescriptor GetMuxerWriteStream() {
        return muxerFoo.getFileDescriptor();
    }
    public InputStream GetMuxerReadStream() throws IOException {
        return muxerFoo.getInputStream();
    }
    public void ReleaseMuxerSocket() {
        try {
            if ( muxerFoo != null)
                muxerFoo.close();
            if ( muxerBar != null)
                muxerBar.close();
        } catch ( IOException ex) {
            ex.printStackTrace();                
        }
        muxerFoo = null;
        muxerBar = null;
    }


    static private native int nativeCheckMedia(String fileName);
    static public boolean NativeCheckMedia(String filename) {
        if (nativeCheckMedia(filename) > 0)
            return true;
        else
            return false;
    }

    public static void LoadLibraries() {
        //Local library .so files before this activity created.
        System.loadLibrary("ipcamera");		
    }

}
