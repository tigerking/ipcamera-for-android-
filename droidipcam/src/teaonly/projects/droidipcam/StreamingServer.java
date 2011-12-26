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
        /*
        Log.d("TEAONLY", method + " '" + uri + "' " );

        String msg = "<html><body><h1>Hello server</h1>\n";
        if ( parms.getProperty("username") == null )
            msg +=
                "<form action='?' method='get'>\n" +
                "  <p>Your name: <input type='text' name='username'></p>\n" +
                "</form>\n";
        else
            msg += "<p>Hello, " + parms.getProperty("username") + "!</p>";

        msg += "</body></html>\n";
        return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, msg );
        */
        return serveFile( uri, header, homeDir, true ); 
    }

}
