package teaonly.projects.droidipcam;

import teaonly.projects.task.*;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends Activity {
	private static final int MENU_EXIT = 0xCC882201;
    SharedPreferences prefs ;
    
    NativeAgent myAgent;
    CameraView myCV;

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

    private void setup() {
        NativeAgent.LoadLibraries();
        myAgent = new NativeAgent("teaonly.project");
        myAgent.setListener( nativeAgentListener );
        myAgent.execute();

    	myCV = (CameraView)findViewById(R.id.surface_overlay);
        SurfaceView sv = (SurfaceView)findViewById(R.id.surface_camera);
    	myCV.SetupCamera(sv);
        
        View  v = (View)findViewById(R.id.layout_setup);
        v.setVisibility(View.VISIBLE);
        
        Button myButton = (Button)findViewById(R.id.btn_start);
        myButton.setOnClickListener(startAction);
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

     private OnClickListener startAction = new OnClickListener() { 
        @Override
        public void onClick(View v) {
        	myCV.StartStreaming();
		}
    }; 


}
