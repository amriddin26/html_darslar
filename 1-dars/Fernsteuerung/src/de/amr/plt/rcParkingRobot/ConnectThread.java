package de.amr.plt.rcParkingRobot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTConnector;
import android.os.Looper;
import android.os.Messenger;
import android.util.Log;

class ConnectThread extends Thread {

	// TODO Beschreibung erg�nzen
	/**
	 * 
	 */
	private final AndroidHmiPLT hmi;
	
	// used to label the logging
	private final static String TAG_BT_CONNECT = "LeJOSDroid";

	/**
	 * @param hmi
	 */
	ConnectThread(AndroidHmiPLT hmi) {
		this.hmi = hmi;
	}


	private NXTConnector connector;

	@Override
	public void run() {
		Looper.prepare();
		connector = new NXTConnector();
		connector.setDebug(true);

		connector.addLogListener(new NXTCommLogListener() {
			public void logEvent(String arg0) {
				Log.e(TAG_BT_CONNECT + " NXJ log:", arg0);
			}

			public void logEvent(Throwable arg0) {
				Log.e(TAG_BT_CONNECT + " NXJ log:", arg0.getMessage(), arg0);
			}
		});			
		
		// If connection can be established connectTo() returns true
		if (connector.connectTo(hmi.nxtName, hmi.nxtAddress, NXTCommFactory.BLUETOOTH)) {
			hmi.dataIn = new DataInputStream(connector.getInputStream());
			hmi.dataOut = new DataOutputStream(connector.getOutputStream());
			if (hmi.dataIn == null)
			{
				hmi.connected = false;
				Log.e(TAG_BT_CONNECT,"Connection failed.");
			}
			else
			{
				hmi.bTCommunicationThread = new BTCommunicationThread(hmi);
				hmi.bTCommunicationThread.setName("readerThread");
				hmi.bTCommunicationThread.setDaemon(true);
				hmi.bTCommunicationThread.start();
				hmi.messenger = new Messenger(hmi.messageHandler);
			}
			hmi.connected = true;
		} 
	
	}
	public void close(){
		try {
			connector.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}