package teaonly.projects.droidipcam;

import teaonly.projects.droidipcam.NanoHTTPD;

import java.io.*;
import java.util.*;
import android.util.Log;

public class StreamingServer extends NanoHTTPD
{
    private File homeDir;

    public StreamingServer(int port, String wwwroot) throws IOException
    {
        super(port, new File( wwwroot ).getAbsoluteFile() );
        homeDir = new File( wwwroot);

    }

    public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
    {
        Log.d("TEAONLY", method + " '" + uri + "' " );

        if ( uri.equals("") ) {
            return null;                      
        } else {
            return serveFile( uri, header, homeDir, true ); 
        }
    }

}
