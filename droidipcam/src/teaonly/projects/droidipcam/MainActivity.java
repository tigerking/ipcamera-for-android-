package teaonly.projects.droidipcam;

import teaonly.projects.droidipcam.R;
import teaonly.projects.task.*;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final int MENU_EXIT = 0xCC882201;
    
    NativeAgent myAgent;
    CameraView myCamView;
    StreamingServer strServer;
    
    TextView myMessage;
    Button btnStart;
    boolean inServer = false;
    boolean inStreaming = false;

    final String checkingFile = "/sdcard/ipcam/myvideo.mp4";
    final String resourceDirectory = "/sdcard/ipcam";

    // memory object for encoder
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        
        setContentView(R.layout.main);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu m){
    	m.add(0, MENU_EXIT, 0, "Exit");
    	return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem i){
    	switch(i.getItemId()){
		    case MENU_EXIT:
                finish();
                System.exit(0);
		    	return true;	    	
		    default:
		    	return false;
		}
    }

    @Override
    public void onDestroy(){
    	super.onDestroy();
    }

    @Override
    public void onStart(){
    	super.onStart();
        setup();
    }

    @Override
    public void onResume(){
    	super.onResume();
    }

    @Override
    public void onPause(){    	
    	super.onPause();
        finish();
        System.exit(0);
    }
    
    private void clearResource() {
        String[] str ={"rm", "-r", resourceDirectory};

        try { 
            Process ps = Runtime.getRuntime().exec(str);
            try {
                ps.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildResource() {
        String[] str ={"mkdir", resourceDirectory};

        try { 
            Process ps = Runtime.getRuntime().exec(str);
            try {
                ps.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } 
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setup() {
        clearResource();
        buildResource(); 

        NativeAgent.LoadLibraries();
        myAgent = new NativeAgent("teaonly.project");
        myAgent.setListener( nativeAgentListener );
        myAgent.execute();

    	myCamView = (CameraView)findViewById(R.id.surface_overlay);
        SurfaceView sv = (SurfaceView)findViewById(R.id.surface_camera);
    	myCamView.SetupCamera(sv);
       
        myMessage = (TextView)findViewById(R.id.label_msg);

        btnStart = (Button)findViewById(R.id.btn_start);
        btnStart.setOnClickListener(startAction);
        btnStart.setEnabled(true);

        View  v = (View)findViewById(R.id.layout_setup);
        v.setVisibility(View.VISIBLE);
    }
    
    private void startServer() {
        inServer = true;
        btnStart.setText( getString(R.string.action_stop) );
        btnStart.setEnabled(true);    
        NetInfoAdapter.Update(this);
        myMessage.setText( getString(R.string.msg_prepare_ok) + "http://" + NetInfoAdapter.getInfo("IP")  + ":8080" );

        try {
            strServer = new StreamingServer(8080, resourceDirectory); 
        } catch( IOException e ) {
            e.printStackTrace();
            showToast(this, "Can't start http server..");
        }
    }

    private void stopServer() {
       inServer = false;
       btnStart.setText( getString(R.string.action_start) );
       btnStart.setEnabled(true);    
       myMessage.setText( getString(R.string.msg_idle));
       if ( strServer != null) {
            strServer.stop();
            strServer = null;
       }
    }

 	private void handleNativeEvent(NativeMessage nativeMsg) {
		
	}

    private void doAction() {
         if ( inServer == false) {
            myCamView.PrepareMedia();
            boolean ret = myCamView.StartRecording(checkingFile);
            btnStart.setEnabled(false);

            if ( ret ) {
                new Handler().postDelayed(new Runnable() { 
                    public void run() { 
                        myCamView.StopMedia();
                        if ( myAgent.NativeCheckMedia(checkingFile) ) {
                            startServer();    
                        } else {
                            btnStart.setEnabled(true);
                            showToast(MainActivity.this, getString(R.string.msg_prepare_error));
                        }
                    } 
                }, 2000); // 2 seconds to release 
            } else {
                btnStart.setEnabled(true);
                showToast(this, getString(R.string.msg_prepare_error));
            }
        } else {
            stopServer();
        }
    
    }

    private void showToast(Context context, String message) { 
        // create the view
        LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = vi.inflate(R.layout.message_toast, null);

        // set the text in the view
        TextView tv = (TextView)view.findViewById(R.id.message);
        tv.setText(message);

        // show the toast
        Toast toast = new Toast(context);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }   


    private TaskListener nativeAgentListener = new TaskAdapter() { 
         @Override
         public String getName() {
             return "NativeAgent";
         }   

         @Override
         public void onProgressUpdate(GenericTask task, Object param) {
             String xmlMessage = (String) param;
             Log.d("TEAONLY", "JAVA:  Get message = " + xmlMessage);
             NativeMessage msgParser = new NativeMessage();
             if (msgParser.parse(xmlMessage))
                 handleNativeEvent(msgParser);
         }   
     };   

     private OnClickListener startAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
            doAction();
        }
     };

}
