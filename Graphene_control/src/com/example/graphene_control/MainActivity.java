package com.example.graphene_control;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;
import sample.ble.sensortag.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.devadvance.circularseekbar.*;
public class MainActivity extends Activity {

	private final static String TAG = MainActivity.class.getSimpleName();
	
	CircularSeekBar cb1;
	private int bar_value=0;
	
	TextView progress_text;
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String deviceName;
    private String deviceAddress;
    private TextView view1,view2,view3;
    
    static BleService bleService=null;
    
    static Date dt1,dt2;
    static boolean m1_lock=true,m2_lock=true,m3_lock=true;
    @SuppressLint("SimpleDateFormat")
	static SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
    static String time_string;
    static int min=0,sec=0;
    Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
		// process incoming messages here
			switch(msg.what)
			{
			case 0:
				progress_text.setText(""+cb1.getProgress());
				break;
			case 1://mode 0
				cb1.setVisibility(View.VISIBLE);
				progress_text.setText("testing mode");
				//progress_text.setText(cb1.getProgress());
				progress_text.setTextColor(Color.BLACK);
				break;
			case 2://mode 1
				cb1.setVisibility(View.INVISIBLE);
				progress_text.setText(time_string);
				progress_text.setTextColor(Color.BLUE);
				view3.setText("1");
				break;
			case 3://mode 2
				cb1.setVisibility(View.INVISIBLE);
				progress_text.setText(time_string);
				progress_text.setTextColor(Color.RED);
				view3.setText("2");
				break;
			case 4://mode 3
				cb1.setVisibility(View.INVISIBLE);
				progress_text.setText(time_string);
				progress_text.setTextColor(Color.GREEN);
				view3.setText("3");
				break;
			}
		}
	};
	
	class mode1 extends Thread{
		@Override
		public void run() {
			dt1=new Date();
			dt2=new Date();
			byte [] send={(byte) 0xa1};
			bleService.write_board(send, 1);
			
			while(m1_lock){
				min=((int)(dt2.getTime()-dt1.getTime())/1000)/60;
				sec=((int)(dt2.getTime()-dt1.getTime())/1000)%60;
				time_string=min+":"+sec+" "+"99% cycle";
				dt2=new Date();
				mHandler.sendEmptyMessage(2);
				
				if(dt2.getTime()-dt1.getTime()>=900000){ //15min atfer
					send[0]=(byte) 0xb1;
					bleService.write_board(send, 1);
					time_string="65% cycle";
					mHandler.sendEmptyMessage(2);
					m1_lock=false;
				}
				
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Log.d(TAG, "--mode1 Thread stop--");
		}
	}
	class mode2 extends Thread{
		@Override
		public void run() {
			boolean m2data_lock=true;
			dt1=new Date();
			dt2=new Date();
			byte [] send={(byte) 0xa2};
			bleService.write_board(send, 1);
			while(m2_lock){
				min=((int)(dt2.getTime()-dt1.getTime())/1000)/60;
				sec=((int)(dt2.getTime()-dt1.getTime())/1000)%60;
				if(m2data_lock)
					time_string=min+":"+sec+" "+"99% cycle";
				else
					time_string=min+":"+sec+" "+"5% cycle";
				dt2=new Date();
				mHandler.sendEmptyMessage(3);
				
				if(dt2.getTime()-dt1.getTime()>=60000){ //15min atfer
					dt1=new Date();
					dt2=new Date();
					m2data_lock=!m2data_lock;
					
					if(m2data_lock)
						send[0]=(byte) 0xa2;
					else
						send[0]=(byte) 0xb2;
					bleService.write_board(send, 1);
				}
				
				try {
					Thread.sleep(15);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Log.d(TAG, "--mode2 Thread stop--");
		}
	}
	class mode3 extends Thread{
		@Override
		public void run() {
			dt1=new Date();
			dt2=new Date();
			byte [] send={(byte) 0xa3};
			bleService.write_board(send, 1);
			
			while(m3_lock){
				min=((int)(dt2.getTime()-dt1.getTime())/1000)/60;
				sec=((int)(dt2.getTime()-dt1.getTime())/1000)%60;
				time_string=min+":"+sec+" "+"5% cycle";
				dt2=new Date();
				mHandler.sendEmptyMessage(4);
				
				if(dt2.getTime()-dt1.getTime()>=300000){ //15min atfer
					send[0]=(byte) 0xb3;
					bleService.write_board(send, 1);
					time_string="65% cycle";
					mHandler.sendEmptyMessage(2);
					m1_lock=false;
				}
				
				
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Log.d(TAG, "--mode3 Thread stop--");
		}
	}
	// Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            boolean aaa= bleService.connect(deviceAddress);
            while(aaa=false){
            	aaa= bleService.connect(deviceAddress);
            }
            
            
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
		
		
	}
	private void init(){
		cb1 = (CircularSeekBar) findViewById(R.id.circularSeekBar1);
		progress_text=(TextView) findViewById(R.id.textView1);
		view1 = (TextView) findViewById(R.id.textView3);
		view2 = (TextView) findViewById(R.id.textView5);
		view3 = (TextView) findViewById(R.id.textView7);
		
		if(DeviceScanActivity.connect_device_lock){
			final Intent intent = getIntent();
	        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
	        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
	        final Intent gattServiceIntent = new Intent(this, BleService.class);
	        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
		}
		
        
        cb1.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(0);
				
				
				
				byte [] s={0x21,(byte) ((int) cb1.getProgress()/16)};
				if(bleService!=null)
					bleService.write_board(s, 1);
				
				return false;
			}
        	
        });
	}
	
	// Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            action = intent.getAction();
            action = intent.getAction();
            if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
                
                updateConnectionState(R.string.connected);
                view1.setText("connected");
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                
                updateConnectionState(R.string.disconnected);
                view1.setText("disconnected");
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	view2.setText("ok");
            	view3.setText("0");
            	 //UUIDText.setText("OK");
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(bleService.getSupportedGattServices());
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
               
               /*curDate2= new Date(System.currentTimeMillis());
               long diff=curDate2.getTime()-curDate1.getTime();
               displayData(diff+"");*/
            }
        }

		
    };
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bleService != null) {
        	
        }
	}
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
        m1_lock=false;
        m2_lock=false;
        m3_lock=false;
    }
    @Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		m1_lock=false;
        m2_lock=false;
        m3_lock=false;
	}
    
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //connectionState.setText(resourceId);
            }
        });
    }

   
 

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@SuppressLint("ShowToast")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			
			return true;
		}
		else if(id ==R.id.menu_item0){
			m1_lock=false;
	        m2_lock=false;
	        m3_lock=false;
			mHandler.sendEmptyMessage(1);
			return true;
		}
		else if(id ==R.id.menu_item1){
			m1_lock=true;
	        m2_lock=false;
	        m3_lock=false;
			mode1 m1=new mode1();
			m1.start();
			return true;
		}
		else if(id ==R.id.menu_item2){
			m1_lock=false;
	        m2_lock=true;
	        m3_lock=false;
			mode2 m2=new mode2();
			m2.start();
			return true;
		}
		else if(id ==R.id.menu_item3){
			m1_lock=false;
	        m2_lock=false;
	        m3_lock=true;
			mode3 m3=new mode3();
			m3.start();
			//mHandler.sendEmptyMessage(4);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
