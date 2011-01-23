package com.appdh.webcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

class OverlayView extends View 
{ 
	private String[] msgLines;
	
	Context myContext;
	
	public OverlayView(Context context, AttributeSet attrs) 
    { 
		super(context,attrs); 
		myContext = context;
		msgLines = new String[3];
		addMessage(context.getString(R.string.info_welcome));
    } 

	public void addMessage(int strtingID)
	{
		addMessage( myContext.getString(strtingID));
	}
	
	public void addMessage(String msg)
	{
		for(int i = msgLines.length - 1; i > 0; i--)
			msgLines[i] = msgLines[i-1];
		msgLines[0] = msg;
	}	
	

	
    @Override 
    protected void onDraw(Canvas canvas) 
    {     	    	    	
        Paint paint = new Paint();         
        paint.setStrokeWidth(1);
        paint.setTextSize(14);
        paint.setStyle(Paint.Style.FILL);         
        paint.setColor(Color.GREEN);      
        
        for(int i = 0; i < msgLines.length; i++){
        	if ( msgLines[i] != null)
        		canvas.drawText(msgLines[i], 10, 10+i*16, paint);
        }
        
        super.onDraw(canvas);         
    } 
} 