package teaonly.projects.droidipcam;

import java.io.*; 
import java.net.*;

import android.net.*;
import android.util.Log;
import org.xmlpull.v1.XmlSerializer;  
import android.util.Xml;

//public class NativeAgent extends GenericTask{
public class NativeAgent {
    
	public NativeAgent() {
    
    }
    
    static private native int nativeCheckMedia(String fileName);
    static public boolean NativeCheckMedia(String filename) {
        if (nativeCheckMedia(filename) > 0)
            return true;
        else
            return false;
    }
    
    static private native int nativeStartFormatingMedia(FileDescriptor in, FileDescriptor out);
    static public void NativeStartFormatingMedia(FileDescriptor in, FileDescriptor out) {
        nativeStartFormatingMedia(in, out);
    }


    public static void LoadLibraries() {
        //Local library .so files before this activity created.
        System.loadLibrary("teaonly");		
        System.loadLibrary("ipcamera");		
    }

}
