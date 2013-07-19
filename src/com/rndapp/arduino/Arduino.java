package com.rndapp.arduino;

import java.util.Calendar;
import java.util.Vector;

import processing.core.*; 

import cc.arduino.*;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Arduino extends Activity implements OnClickListener{
	ActionBar ab;
    private static final String PREFS = "com.rndapp.mag.prefs";
	ArduinoAdkUsb arduino;
	long timer = Calendar.getInstance().getTimeInMillis();
	int pressed = 0;
	TextView dataTV;
	ArduinoWriter aw;
	Vector<String> names;
	Vector<String> datas;
	Vector<String> dates;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ab = getActionBar();
		
		SharedPreferences sp = getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
		//Editor editPrefs = sp.edit();
		String name = "";
		String data = "";
		String date = "";
		int i = 1;
		if (!sp.getString("data1", "").equals("")){
			while (!sp.getString("data"+i, "").equals("")){
				name = sp.getString("name"+i, "");
				data = sp.getString("data"+i, "");
				date = sp.getString("date"+i, "");
				names.add(name);
				datas.add(data);
				dates.add(date);
				i++;
			}
		}else{
			name = "No cards saved yet.";
			data = "Press 'Write' to test your connection with an Arduino.";
			date = "";
		}
		
		TextView nameTV = (TextView)findViewById(R.id.name);
		dataTV = (TextView)findViewById(R.id.data);
		TextView dateTV = (TextView)findViewById(R.id.date);

		nameTV.setText(name);
		dataTV.setText(data);
		dateTV.setText(date);
		
		arduino = new ArduinoAdkUsb(this); //init arduino
		String out = "";
		if ( arduino.list() != null ) {
			arduino.connect( arduino.list()[0] ); //if there is an arduino to connect to, do so
			out = "Arduino detected!";
		}else{
			out = "No Arduino :(";
		}
		//nameTV.setText(out);
		
		ArduinoListener ad = new ArduinoListener();
		ad.execute(this);
		
		Button b = (Button)findViewById(R.id.write);
		b.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View arg0) {
		if (arg0.getId() == R.id.write){
			if (arduino.isConnected()){
				//tv.setText("Arduino registered");
				if (pressed == 0){
					pressed = 1;
					aw = new ArduinoWriter();
					//tv.setText("About to write: "+pressed);
					aw.executeOnExecutor(aw.THREAD_POOL_EXECUTOR, pressed);
				}else{
					//tv.setText("Stopping writing");
					pressed = 0;
					aw.cancel(true);
				}
			}
		}
	}

	class ArduinoWriter extends AsyncTask<Integer, Object, Object>{
		@Override
		protected Object doInBackground(Integer... i) {
			while (pressed == 1){
				arduino.write('T');
				arduino.write((byte)pressed);
			}
			arduino.write('T');
			arduino.write((byte)0);
			return null;
		}
		
		protected void onProgressUpdate(String... progress) {
			//nameTV.setText(progress[0]);
		}
	}
	
	class ArduinoListener extends AsyncTask<Context, Integer, Object>{
		Context cxt;
		
		@Override
		protected Object doInBackground(Context... params) {
			cxt = params[0];
			while (arduino != null){
				monitor();
			}
			return null;
		}
		
		private void monitor(){
			int out = 0;
			if (arduino.isConnected() && arduino.list() != null){
				boolean reading = false;
				while ( arduino.available() > 0 ) {
					//read a character from the serial (from arduino)
					char readByte = arduino.readChar();

					if (readByte == 'S') {
						reading = true;
						String dat = "";
						while (reading){
							char d = arduino.readChar(); 
							if (d == 'X'){
								reading = false;
							}else{
								dat += d;
							}
						}
						datas.add(dat);
						//alertbuilder?
					}
				}
			}else{
				if ( arduino.list() != null ) {
					arduino.connect( arduino.list()[0] ); //if there is an arduino to connect to, do so
					out = 1;
				}else{
					out = 0;
					arduino = new ArduinoAdkUsb(cxt);
				}
				publishProgress(out);
			}
		}
		
		protected void onProgressUpdate(int... progress) {
			//tv.setText(progress[0]);
			switch (progress[0]){
			case 0:
				ab.setIcon(R.drawable.icon_);
				dataTV.setText("no arduino");
				break;
			case 1:
				ab.setIcon(R.drawable.icon_hl);
				dataTV.setText("arduino");
				break;
			}
		}
		
		protected void onPostExecute(Object... o){
			this.execute(cxt);
		}
	}
}
