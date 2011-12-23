package teaonly.projects.droidipcam;

import teaonly.projects.droidipcam.R;
import teaonly.projects.task.*;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final int MENU_EXIT = 0xCC882201;
    
    NativeAgent myAgent;
    CameraView myCamView;
    TextView myMessage;
    Button btnPrepare;
    Button btnStart;

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
    protected void finalize() throws Throwable
    {
        //do finalization here
        super.finalize();
        clearResource();
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
        btnStart.setEnabled(false);
        btnPrepare = (Button)findViewById(R.id.btn_prepare);
        btnPrepare.setOnClickListener(prepareAction);
        btnPrepare.setEnabled(true);

        View  v = (View)findViewById(R.id.layout_setup);
        v.setVisibility(View.VISIBLE);
    }

 	private void handleNativeEvent(NativeMessage nativeMsg) {
		
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

     private OnClickListener prepareAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
            myCamView.PrepareMedia();
            btnPrepare.setEnabled(false);
            boolean ret = myCamView.StartRecording(checkingFile);
            
            if ( ret ) {
                new Handler().postDelayed(new Runnable(){ 
                    public void run() { 
                        myCamView.StopMedia();
                        if ( myAgent.NativeCheckMedia(checkingFile) ) {
                            myMessage.setText( getString(R.string.msg_prepare_ok));
                            btnStart.setEnabled(true);                            
                        } else {
                            myMessage.setText( getString(R.string.msg_prepare_error));
                        }
                        btnPrepare.setEnabled(true);
                    } 
                }, 3000); // 3 seconds to release 
            } else {
                btnPrepare.setEnabled(true);
                myMessage.setText( getString(R.string.msg_prepare_error));
            }

        }
     };

     private OnClickListener startAction = new OnClickListener() { 
        @Override
        public void onClick(View v) {
            
        }
    }; 


}
