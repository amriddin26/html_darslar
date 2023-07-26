package de.amr.plt.rcTestapp;

import java.util.Set;

import de.amr.plt.rcTestapp.R;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * providing MAC-address of the chosen device to the PositionService
 * @author PLT
 */

public class BluetoothActivity extends Activity {
	
	/**
	 * Return Intent extra
	 */
	public static String EXTRA_DEVICE_ADDRESS = "device_adress";
	
	// Member fields
	BluetoothAdapter mBtAdapter = null;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	
	// The intent
	private Intent enableBtIntent = null;
	
	//containing the MAC-Address of the device
	private String address; 
	//request code
	private final int REQUEST_ENABLE_BT	= 1;
	//result code
	private final int RESULT_BT_NOT_ENABLED = 3;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);        
        
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Initialize array adapters for already paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_name);
        
        //Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
		
		//Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        
        //Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        //add paired devices to the ArrayAdapter
        addPairedDevices();
    }
	
	public void onStart(){ 
		//ensure bluetooth is enabled 
		ensureBluetoothIsEnabled();
		super.onStart(); 	
	}
	
	public void onResume(){
		//necessary if bluetooth-enabling-dialog appears   
		addPairedDevices();
		super.onResume();
	}
	
	private void ensureBluetoothIsEnabled(){
		//if Bluetooth is not enabled, enable it now
		if (!mBtAdapter.isEnabled()) {    		
    	    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    	}
	}
	
	private void addPairedDevices(){
		// ensure devices wont be added twice
		mPairedDevicesArrayAdapter.clear();
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		//bluetooth has to be enabled
		if(mBtAdapter.isEnabled()){
			//there are no paired devices ...
			if(pairedDevices.size() == 0){
	    		//inform user to pair devices manually
	    		Toast.makeText(this, "No paired devices detected. Please pair devices!", Toast.LENGTH_SHORT).show();
	    		returnToPriviousActivityWithoutDevice();
	    	} 
			//there are paired devices ...
			else{
				//inform user
	    		Toast.makeText(this, "Paired devices detected.", Toast.LENGTH_SHORT).show();
	    		findViewById(R.id.paired_devices).setVisibility(View.VISIBLE);    		
	    	    // Loop through paired devices
	    	    for (BluetoothDevice device : pairedDevices) {
	    	        // Add the name and address to an array adapter to show in a ListView	    	    	
	    	        mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	    	    }
	    	}
		}
	}
    
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	if(!mBtAdapter.isEnabled()){
			Toast.makeText(this, "Please ensure bluetooth is enabled!", Toast.LENGTH_LONG).show();
			
			Intent finishIntent = new Intent();
            finishIntent.putExtra(EXTRA_DEVICE_ADDRESS, "finish");
            setResult(RESULT_BT_NOT_ENABLED, finishIntent);
			BluetoothActivity.this.finish();
		}
    }
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		//handle click on back button
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	//return to previous screen
	    	returnToPriviousActivityWithoutDevice();
	    }
	    return super.onKeyDown(keyCode, event);
	}
    
 // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener(){
    	public void onItemClick(AdapterView<?> av,View v, int arg2, long arg3){
			// Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
           
            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            
            finish();
            
    	}
    };
    
    private void returnToPriviousActivityWithoutDevice(){
    	Intent finishIntent = new Intent();
        finishIntent.putExtra(EXTRA_DEVICE_ADDRESS, "finish");
        setResult(Activity.RESULT_CANCELED, finishIntent);
        BluetoothActivity.this.finish();
    }

	 public void discoverDevices(){
    	// If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        
        Toast.makeText(this, "Listining for paired devices.", Toast.LENGTH_LONG).show();
    	mBtAdapter.startDiscovery();   
    }
		
	//The BroadcastReceiver that listens for discovered devices 
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's paired, add it to the list
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            
        }
    };

	@Override
	protected void onPause() {
		super.onPause();
		if (enableBtIntent != null) {
		    unregisterReceiver(mReceiver);
		    enableBtIntent = null;
		}
		// unregisterReceiver(mReceiver);
	}
}
