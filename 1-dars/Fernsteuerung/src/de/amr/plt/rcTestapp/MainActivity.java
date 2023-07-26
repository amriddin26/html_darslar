package de.amr.plt.rcTestapp;

import java.util.Timer;
import java.util.TimerTask;

import parkingRobot.INxtHmi.Mode;
import parkingRobot.hsamr0.GuidanceAT.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import de.amr.plt.rcParkingRobot.AndroidHmiPLT;

/**
 * The activity visualizes NXT data such as bluetooth connection, current status, x-,y-coordinate, angle and distance values. 
 * @author PLT
 */

public class MainActivity extends Activity {

	//representing local Bluetooth adapter
	BluetoothAdapter mBtAdapter = null;
	//representing the bluetooth hardware device
	BluetoothDevice btDevice = null;
	//instance handels bluetooth communication to NXT
	
	AndroidHmiPLT hmiModule = null;	
	//request code 
	final int REQUEST_SETUP_BT_CONNECTION = 1;		
	
					
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //get the BT-Adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();       
     
        //If the adapter is null, then Bluetooth is not supported
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }       
        
        final Button connectButton = (Button) findViewById(R.id.buttonSetupBluetooth);
        //on click call the BluetoothActivity to choose a listed device
        connectButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		Intent serverIntent = new Intent(getApplicationContext(),BluetoothActivity.class);
				startActivityForResult(serverIntent, REQUEST_SETUP_BT_CONNECTION);
        	}
        });
        
        //toggle button allows user to set mode of the NXT device
        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleMode);
        //disable button initially 
        toggleButton.setEnabled(false);
        //on click change mode 
        toggleButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){       		
    		    boolean checked = ((ToggleButton) v).isChecked();    		    
    		    if (checked) {
    		        //if toggle is checked change mode to SCOUT 
    		    	hmiModule.setMode(Mode.SCOUT);
    		    	Log.e("Toggle","Toggled to Scout");
    		    } else{
    		    	// otherwise change mode to PAUSE
    		    	hmiModule.setMode(Mode.PAUSE); 
    		    	Log.e("Toggle","Toggled to Pause");
    		    }
        	}
        });
        
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return true;
    }
	
    @Override
	public void onDestroy(){
		super.onDestroy();
    	if(mBtAdapter != null){
    		//release resources  
    		mBtAdapter.cancelDiscovery();
    	}
    	
	}
	
	/**
	 * handle pressing button with alert dialog if connected(non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		
	    if (hmiModule != null && hmiModule.connected) {
	    	//creating new AlertDialog
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to terminate the connection?")
			       .setCancelable(false)
			       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   //disconnect and return to initial screen
			        	   terminateBluetoothConnection();
			        	   restartActivity();
			           }
			       })
			       .setNegativeButton("No", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
	    }
	}
	
	/**
	 * instantiating AndroidHmiPlt object and display received data(non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		switch(resultCode){
		
		//user pressed back button on bluetooth activity, so return to initial screen 
		case Activity.RESULT_CANCELED:
			break;
		//user chose device
		case Activity.RESULT_OK:
			//connect to chosen NXT
			establishBluetoothConnection(data);							
			//display received data from NXT
			if(hmiModule.connected){			
				//After establishing the connection make sure the start mode of the NXT is set to PAUSE
//				hmiModule.setMode(Mode.PAUSE);
				
				//enable toggle button
				final ToggleButton toggleMode = (ToggleButton) findViewById(R.id.toggleMode);
				toggleMode.setEnabled(true);
				
				//disable connect button
				final Button connectButton = (Button) findViewById(R.id.buttonSetupBluetooth);
				connectButton.setEnabled(false);
				
				displayDataNXT();
				break;
			} else{
				Toast.makeText(this, "Bluetooth connection failed!", Toast.LENGTH_SHORT).show();
				Toast.makeText(this, "Is the selected NXT really present & switched on?", Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	/**
	 * Connect to the chosen device 
	 * @param data
	 */
	private void establishBluetoothConnection(Intent data){
		//get instance of the chosen bluetooth device
		String address = data.getExtras().getString(BluetoothActivity.EXTRA_DEVICE_ADDRESS);	
		btDevice = mBtAdapter.getRemoteDevice(address);		
		
		//get name and address of the device
		String btDeviceAddress = btDevice.getAddress();		
		String btDeviceName = btDevice.getName();
		
		//instantiate client modul
		hmiModule = new AndroidHmiPLT(btDeviceName, btDeviceAddress);
		
		//connect to the specified device
		hmiModule.connect();
		
		//wait till connection really is established and 
		int i = 0;
		while (!hmiModule.isConnected()&& i<100000000/2) {
			i++;
		}
	}
	
	/**
     * Display the current data of NXT
     */
	private void displayDataNXT(){
		
		new Timer().schedule(new TimerTask() {
			
			@Override
            public void run() {
				
                runOnUiThread(new Runnable() {
                    public void run() {
                    	if(hmiModule != null){
                    		//display x value
                        	final TextView fld_xPos = (TextView) findViewById(R.id.textViewValueX);
                    		fld_xPos.setText(String.valueOf(hmiModule.getPosition().getX()+" cm"));
                    		//display y value
                    		final TextView fld_yPos = (TextView) findViewById(R.id.textViewValueY);
                    		fld_yPos.setText(String.valueOf(hmiModule.getPosition().getY()+" cm"));
                    		//display angle value
                    		final TextView fld_angle = (TextView) findViewById(R.id.TextViewValueAngle); 
                    		fld_angle.setText(String.valueOf(hmiModule.getPosition().getAngle()+"°"));
                    		//display status of NXT
                    		final TextView fld_status = (TextView) findViewById(R.id.textViewValueStatus);
                    		fld_status.setText(String.valueOf(hmiModule.getCurrentStatus()));
                    		//display distance front
                    		final TextView fld_distance_front = (TextView) findViewById(R.id.textViewValueDistanceFront);
                    		fld_distance_front.setText(String.valueOf(hmiModule.getPosition().getDistanceFront())+" mm");
                    		//display distance back
                    		final TextView fld_distance_back = (TextView) findViewById(R.id.textViewValueDistanceBack);
                    		fld_distance_back.setText(String.valueOf(hmiModule.getPosition().getDistanceBack())+" mm");
                    		//display distance right	
                    		final TextView fld_distance_front_side = (TextView) findViewById(R.id.textViewValueDistanceFrontSide);
                    		fld_distance_front_side.setText(String.valueOf(hmiModule.getPosition().getDistanceFrontSide())+" mm");
                    		//display distance left
                    		final TextView fld_distance_back_side = (TextView) findViewById(R.id.textViewValueDistanceBackSide);
                    		fld_distance_back_side.setText(String.valueOf(hmiModule.getPosition().getDistanceBackSide())+" mm");
                    		//display bluetooth connection status
                    		final TextView fld_bluetooth = (TextView) findViewById(R.id.textViewValueBluetooth);
                    		//display connection status
                    		if(hmiModule.isConnected()){
                    			fld_bluetooth.setText("connected");
                    		} else {
                    			fld_bluetooth.setText("not connected");
                    		}
                    		//restart activity when disconnecting
                    		if(hmiModule.getCurrentStatus()==CurrentStatus.EXIT){
                    			terminateBluetoothConnection();
                    			restartActivity();
                    		}
                    	}
                    }
                });
            }
        }, 200, 100);
				
	}
	
	/**
	 * Terminate the bluetooth connection to NXT
	 */
	private void terminateBluetoothConnection(){
		Toast.makeText(this, "Bluetooth connection was terminated!", Toast.LENGTH_LONG).show();
		hmiModule.setMode(Mode.DISCONNECT);
		hmiModule.disconnect();
		
		while(hmiModule.isConnected()){
			//wait until disconnected
		}
		hmiModule = null;
	}
	
	/**
	 * restart the activity
	 */
	private void restartActivity(){
		Intent restartIntent = new Intent(getApplicationContext(),MainActivity.class);                    			
		startActivity(restartIntent);
		finish();
	}

}

