package com.appdh.webcamera;

import java.io.*;
import java.net.*;
import java.util.*;


class WebServer implements Runnable{
	/*Do streaming object*/
	public static MainActivity callBacker = null;
	
	/* Where worker threads stand idle */
    static Vector<Worker> threads = new Vector<Worker>();
    static Worker currentStreamingWorker = null;
   

    /* timeout on client connections */
    int timeout = 5000;

    /* max # worker threads */
    static int workers = 5;
    
    /* Working directory*/
    static File root;
    
    static void prepare(){    
    	/* start worker threads */
        for (int i = 0; i < workers; ++i) {
            Worker w = new Worker();
            (new Thread(w, "worker #"+i)).start();
            threads.addElement(w);
        }
        root = new File("/sdcard/webcamera/");
    }

    public synchronized void run() {
        final int port = 8080;
        
        prepare();        
        try{
        	ServerSocket ss = new ServerSocket(port);
	        while (true) {	
	            Socket s = ss.accept();
	            //Log.d("TEAONLY", "Accept a Client");
	            Worker w = null;
	            synchronized (threads) {
	                if (threads.isEmpty()) {
	                    Worker ws = new Worker();
	                    ws.setSocket(s);
	                    (new Thread(ws, "additional worker")).start();
	                } else {
	                    w = threads.elementAt(0);
	                    threads.removeElementAt(0);
	                    w.setSocket(s);
	                }
	            }
	        }
        } catch (IOException e){
        	e.printStackTrace();
        	return;
        }
    }
    
    public static String getInterfaces (){
        try {
           Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();

           while(e.hasMoreElements()) {
              NetworkInterface ni = e.nextElement();
              
              Enumeration<InetAddress> e2 = ni.getInetAddresses();

              while (e2.hasMoreElements()){
                 InetAddress ip = e2.nextElement();                 
                 if( ip.toString().equalsIgnoreCase("/127.0.0.1") ){
                	 continue;
                 }
                 return ip.toString();
              }
           }
        }
        catch (Exception e) {
           e.printStackTrace();
           return null;
        }
        
        return null;
     }
    
    
}


class Worker extends WebServer implements HttpConstants, Runnable {
    final static int BUF_SIZE = 2048;

    static final byte[] EOL = {(byte)'\r', (byte)'\n' };

    /* buffer to use for requests */
    byte[] buf;
    /* Socket to client we're handling */
    private Socket s;

    Worker() {
        buf = new byte[BUF_SIZE];
        s = null;
    }
  
    synchronized void setSocket(Socket s) {
        this.s = s;
        notify();
    }

    public synchronized void run() {
        while(true) {
            if (s == null) {
                /* nothing to do */
                try {
                    wait();
                } catch (InterruptedException e) {
                    /* should not happen */
                    continue;
                }
            }
            try {
                handleClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
            /* go back in wait queue if there's fewer
             * than numHandler connections.
             */
            s = null;
            Vector<Worker> pool = threads;
            synchronized (pool) {
                if (pool.size() >= workers) {
                    /* too many threads, exit this one */
                    return;
                } else {
                    pool.addElement(this);
                }
            }
        }
    }

    void handleClient() throws IOException {
        InputStream is = new BufferedInputStream(s.getInputStream());
        PrintStream ps = new PrintStream(s.getOutputStream());
        /* we will only block in read for this many milliseconds
         * before we fail with java.io.InterruptedIOException,
         * at which point we will abandon the connection.
         */
        s.setSoTimeout(timeout);
        s.setTcpNoDelay(true);
        /* zero out the buffer from last time */
        for (int i = 0; i < BUF_SIZE; i++) {
            buf[i] = 0;
        }
        try {
            /* We only support HTTP GET/HEAD, and don't
             * support any fancy HTTP options,
             * so we're only interested really in
             * the first line.
             */
            int nread = 0, r = 0;

outerloop:
            while (nread < BUF_SIZE) {
                r = is.read(buf, nread, BUF_SIZE - nread);
                if (r == -1) {
                    /* EOF */
                    return;
                }
                int i = nread;
                nread += r;
                for (; i < nread; i++) {
                    if (buf[i] == (byte)'\n' || buf[i] == (byte)'\r') {
                        /* read one line */
                        break outerloop;
                    }
                }
            }

            /* are we doing a GET or just a HEAD */
            boolean doingGet;
            /* beginning of file name */
            int index;
            if (buf[0] == (byte)'G' &&
                buf[1] == (byte)'E' &&
                buf[2] == (byte)'T' &&
                buf[3] == (byte)' ') {
                doingGet = true;
                index = 4;
            } else if (buf[0] == (byte)'H' &&
                       buf[1] == (byte)'E' &&
                       buf[2] == (byte)'A' &&
                       buf[3] == (byte)'D' &&
                       buf[4] == (byte)' ') {
                doingGet = false;
                index = 5;
            } else {
                /* we don't support this method */
                ps.print("HTTP/1.0 " + HTTP_BAD_METHOD +
                           " unsupported method type: ");
                ps.write(buf, 0, 5);
                ps.write(EOL);
                ps.flush();
                s.close();
                return;
            }

            int i = 0;
            /* find the file name, from:
             * GET /foo/bar.html HTTP/1.0
             * extract "/foo/bar.html"
             */
            for (i = index; i < nread; i++) {
                if (buf[i] == (byte)' ') {
                    break;
                }
            }
            String fname = (new String(buf, 0, index,
                      i-index)).replace('/', File.separatorChar);
            if (fname.startsWith(File.separator)) {
                fname = fname.substring(1);
            }
            
			//Log.d("TEAONLY", "###########FILENAME = " + fname);
			if ( fname.equalsIgnoreCase("live.flv")) {				
				//Log.d("TEAONLY","Handling live streaming request!");
				if( callBacker != null){
					printStreamingHeaders(ps);
					callBacker.doStreaming(s.getOutputStream());
				} else {				
					//直接报告404错误
					ps.print("HTTP/1.0 " + HTTP_NOT_FOUND + " not found");
		            ps.write(EOL);
		            ps.print("Server: Simple java");
		            ps.write(EOL);
		            ps.print("Date: " + (new Date()));
		            ps.write(EOL);
		            send404(null, ps);
				}
				s.close();
				return;
			}
			
			File targ = new File(WebServer.root, fname);
            if (targ.isDirectory()) {
                File ind = new File(targ, "index.html");
                if (ind.exists()) {
                    targ = ind;
                }
            }
            boolean OK = printHeaders(targ, ps);
            if (doingGet) {
                if (OK) {
                    sendFile(targ, ps);
                } else {
                    send404(targ, ps);
                }
            }
        } finally {
            s.close();
        }
    }


    private void printStreamingHeaders(PrintStream ps) throws IOException
    {
        int rCode = 0;
        
        rCode = HTTP_OK;
        ps.print("HTTP/1.0 " + rCode +" OK");
        ps.write(EOL);
        
        //Log.d("TEAONLY","From " +s.getInetAddress().getHostAddress()+": "+rCode + "  Start streaming");
    
        ps.print("Server: Simple java");
        ps.write(EOL);
        ps.print("Date: " + (new Date()));
        ps.write(EOL);
        
        //ps.print("Content-length: "+ 1024*1024*500);
        ps.print("Content-length: "+ -1);
        ps.write(EOL);
        ps.print("Last Modified: " + (new Date()));
        ps.write(EOL);
        String ct = "application/octet-stream";        
        ps.print("Content-type: " + ct);
        ps.write(EOL);
        ps.write(EOL);
    }

   
    
    boolean printHeaders(File targ, PrintStream ps) throws IOException {
        boolean ret = false;
        int rCode = 0;
   
        if (!targ.exists()) {
            rCode = HTTP_NOT_FOUND;
            ps.print("HTTP/1.0 " + rCode + " not found");
            ps.write(EOL);
            ret = false;            
        }  else {
            rCode = HTTP_OK;
            ps.print("HTTP/1.0 " + rCode +" OK");
            ps.write(EOL);
            ret = true;
        }
        
        //Log.d("TEAONLY","From " +s.getInetAddress().getHostAddress()+": GET " +
        //    targ.getAbsolutePath()+"-->"+rCode);
        
        ps.print("Server: Simple java");
        ps.write(EOL);
        ps.print("Date: " + (new Date()));
        ps.write(EOL);
        if (ret) {
            if (!targ.isDirectory()) {
                ps.print("Content-length: "+targ.length());
                ps.write(EOL);
                ps.print("Last Modified: " + (new
                              Date(targ.lastModified())));
                ps.write(EOL);
                String name = targ.getName();
                int ind = name.lastIndexOf('.');
                String ct = null;
                if (ind > 0) {
                    ct = map.get(name.substring(ind));
                }
                if (ct == null) {
                    ct = "unknown/unknown";
                }
                ps.print("Content-type: " + ct);
                ps.write(EOL);
            } else {
                ps.print("Content-type: text/html");
                ps.write(EOL);
            }
        }
        return ret;
    }

    void send404(File targ, PrintStream ps) throws IOException {
        ps.write(EOL);
        ps.write(EOL);
        ps.println("Not Found\n\n"+
                   "The requested resource was not found.\n");
    }

    void sendFile(File targ, PrintStream ps) throws IOException {
        InputStream is = null;
        ps.write(EOL);
        if (targ.isDirectory()) {
            listDirectory(targ, ps);
            return;
        } else {
            is = new FileInputStream(targ.getAbsolutePath());
        }

        try {
            int n;
            while ((n = is.read(buf)) > 0) {
                ps.write(buf, 0, n);
            }
        } finally {
            is.close();
        }
    }

    /* mapping of file extensions to content-types */
    static Hashtable<String, String> map = new Hashtable<String, String>();

    static {
        fillMap();
    }
    static void setSuffix(String k, String v) {
        map.put(k, v);
    }

    static void fillMap() {
        setSuffix("", "content/unknown");
        setSuffix(".uu", "application/octet-stream");
        setSuffix(".exe", "application/octet-stream");
        setSuffix(".ps", "application/postscript");
        setSuffix(".zip", "application/zip");
        setSuffix(".sh", "application/x-shar");
        setSuffix(".tar", "application/x-tar");
        setSuffix(".snd", "audio/basic");
        setSuffix(".au", "audio/basic");
        setSuffix(".wav", "audio/x-wav");
        setSuffix(".gif", "image/gif");
        setSuffix(".jpg", "image/jpeg");
        setSuffix(".jpeg", "image/jpeg");
        setSuffix(".htm", "text/html");
        setSuffix(".html", "text/html");
        setSuffix(".text", "text/plain");
        setSuffix(".c", "text/plain");
        setSuffix(".cc", "text/plain");
        setSuffix(".c++", "text/plain");
        setSuffix(".h", "text/plain");
        setSuffix(".pl", "text/plain");
        setSuffix(".txt", "text/plain");
        setSuffix(".java", "text/plain");
    }

    void listDirectory(File dir, PrintStream ps) throws IOException {
        ps.println("<TITLE>Directory listing</TITLE><P>\n");
        ps.println("<A HREF=\"..\">Parent Directory</A><BR>\n");
        String[] list = dir.list();
        for (int i = 0; list != null && i < list.length; i++) {
            File f = new File(dir, list[i]);
            if (f.isDirectory()) {
                ps.println("<A HREF=\""+list[i]+"/\">"+list[i]+"/</A><BR>");
            } else {
                ps.println("<A HREF=\""+list[i]+"\">"+list[i]+"</A><BR");
            }
        }
        ps.println("<P><HR><BR><I>" + (new Date()) + "</I>");
    }

}

interface HttpConstants {
    /** 2XX: generally "OK" */
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_PARTIAL = 206;

    /** 3XX: relocation/redirect */
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_USE_PROXY = 305;

    /** 4XX: client error */
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /** 5XX: server error */
    public static final int HTTP_SERVER_ERROR = 500;
    public static final int HTTP_INTERNAL_ERROR = 501;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION = 505;
}



