package de.amr.plt.rcParkingRobot;

import java.io.IOException;

import parkingRobot.INxtHmi.Mode;
import parkingRobot.hsamr0.GuidanceAT.CurrentStatus;
import parkingRobot.hsamr0.HmiPLT.Command;
import android.graphics.PointF;
import android.os.Message;
import android.util.Log;
import de.amr.plt.rcParkingRobot.IAndroidHmi.ParkingSlot;
import de.amr.plt.rcParkingRobot.IAndroidHmi.Position;

/**
 * Thread spawned by Application main thread to handle reading and writing operations from/to bluetooth data streams.
 * @author PLT
 *
 */
class BTCommunicationThread extends Thread {

	private static final String TAG_COMM_THREAD = "BTCommThread";

	/**
	 * 
	 */
	private final AndroidHmiPLT hmi;

	// local current values received from robot
	float x = 0, y = 0, angle = 0;
	double[] distances = new double[]{0,0,0,0};
	CurrentStatus status;

	/**
	 * @param hmi
	 */
	BTCommunicationThread(AndroidHmiPLT hmi) {
		this.hmi = hmi;
	}


	@Override
	public void run() {

		// Message Code
		Command command = null;

		// Transmitted integer, representing a message code
		int code = -1;
		while(true) {
			if(hmi.connected) {			// Thread begins process of reading

				try {
					// first integer contains message code
					// Careful: read* are blocking methods!
					code = hmi.dataIn.readInt();

					// Check whether the received code has a sane value
					if (code > 0 && code < 100) {
						command = Command.values()[code];
						Log.i("TAG_READER_THREAD", "Command received: "+command.toString());
					}

					// Read position
					if (command == Command.OUT_POSITION) {
						x = hmi.dataIn.readFloat();	
						y = hmi.dataIn.readFloat();
						angle = hmi.dataIn.readFloat();
						Log.i("TAG_READER_THREAD", "Position: x="+x+" y="+y+" angle="+angle);

						// Distance sensor values in clockwise directions (front, right, back, left) in mm
						distances[0] = hmi.dataIn.readDouble();
						distances[1] = hmi.dataIn.readDouble();
						distances[2] = hmi.dataIn.readDouble();
						distances[3] = hmi.dataIn.readDouble();
						Log.i("TAG_READER_THREAD", "Distances = " + distances[0] + ", " + distances[1] + ", " + distances[2] + ", " + distances[3]);

						sendPosition();
					}

					// Read parking slots
					else if (command == Command.OUT_PARKSLOT) {
						int istatus = hmi.dataIn.readInt();
						int id = hmi.dataIn.readInt();
						float xf = hmi.dataIn.readFloat();
						float yf = hmi.dataIn.readFloat();
						float xb = hmi.dataIn.readFloat();
						float yb = hmi.dataIn.readFloat();
						ParkingSlot newSlot = new ParkingSlot(id, new PointF(xf,yf), new PointF(xb,yb), 
								ParkingSlot.ParkingSlotStatus.values()[istatus]);
						sendParkingSlot(newSlot);
					} 

					// Read status
					else if (command == Command.OUT_STATUS) {
						int istatus = hmi.dataIn.readInt();
						status = CurrentStatus.values()[istatus];
						Log.i(TAG_COMM_THREAD, "Status: "+status);
						sendStatus();
					}


				} catch (IOException e)
				{
					Log.e(TAG_COMM_THREAD, "IOExeption: "+e.getMessage());
				}

			} // if connected

			try
			{
				Thread.sleep(50);
			} catch (InterruptedException e)
			{
				Log.e(TAG_COMM_THREAD,"Interrupted Exception: "+e.getMessage());
			} 
			catch (Exception e)
			{
				Log.e(TAG_COMM_THREAD,"Other Exception: "+e.getMessage());
			} 
		} // while true
	}

	public void sendMode(Mode mode) {
		
		try {
			hmi.dataOut.writeInt(Command.IN_SET_MODE.ordinal());
			hmi.dataOut.writeInt(mode.ordinal());
			hmi.dataOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void sendSelectedParkingSlot(int id) {
		
		try {
			hmi.dataOut.writeInt(Command.IN_SELECTED_PARKING_SLOT.ordinal());
			hmi.dataOut.writeInt(id);
			hmi.dataOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void sendPosition() {
		hmi.positionHandler.sendMessage(Message.obtain(hmi.positionHandler, 0, new Position(x, y, angle, distances)));
	}

	private void sendParkingSlot(ParkingSlot newSlot) {
		hmi.parkSlotHandler.sendMessage(Message.obtain(hmi.parkSlotHandler, 0, newSlot));
	}

	private void sendStatus() {
		hmi.statusHandler.sendMessage(Message.obtain(hmi.statusHandler, 0, status));
	}
}