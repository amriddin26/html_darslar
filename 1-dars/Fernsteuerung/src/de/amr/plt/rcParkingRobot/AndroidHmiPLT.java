package de.amr.plt.rcParkingRobot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import parkingRobot.INxtHmi.Mode;
import parkingRobot.hsamr0.GuidanceAT.CurrentStatus;
import parkingRobot.hsamr0.HmiPLT.Command;
import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * Reference implementation of Android HMI module for communication with NXT robot. The module is used by instantiating it with  
 * name and address of a NXT, and calling {@link #connect() connect()}. Once connection is established and NXT is working, information can be requested
 * commands can be sent.
 * @author PLT
 *
 */
public class AndroidHmiPLT implements IAndroidHmi { 

	/**
	 * Message identifier for a mode change request.
	 */
	public static final int MSG_MODE = 0;
	/**
	 * Message identifier for a parking slot selection request.
	 */
	public static final int MSG_SELECT_PS = 1;

	// Name and bluetooth MAC address of NXT device
	String nxtName;
	String nxtAddress;
	// Data stream for incoming bluetooth data
	DataInputStream dataIn;
	// Data stream for outgoing bluetooth data
	DataOutputStream dataOut;

	/**
	 * Whether the device is currently connected to a LEGO NXT
	 */
	public boolean connected = false;

	// This thread handles bluetooth connection to remote device.
	private ConnectThread connectThread;

	// Thread listens for incoming bluetooth data and sends outgoing data
	BTCommunicationThread bTCommunicationThread;

	// Message handler for position data from ReaderThread
	PositionHandler positionHandler;
	// Message handler for parking slot data from ReaderThread
	ParkSlotHandler parkSlotHandler;
	// Message handler for status data from ReaderThread
	StatusHandler statusHandler;
	// Messenger for request messages
	protected Messenger messenger;

	/**
	 * Message handler for receiving commands from main thread, forwarding requests to bluetooth output stream
	 */
	public MessageHandler messageHandler = new MessageHandler();

	/**
	 * Creates a new Android HMI module. Each module instance can connect to only one NXT device, provided via name and address.
	 * @param nxtName
	 * @param nxtAddress
	 */
	public AndroidHmiPLT(String nxtName, String nxtAddress) {
		this.nxtName = nxtName;
		this.nxtAddress = nxtAddress;
		this.positionHandler = new PositionHandler();
		this.parkSlotHandler = new ParkSlotHandler();
		this.statusHandler = new StatusHandler();
	}

	/**
	 * Connects to the NXT remote device specified by {@code nxtName} and {@code nxtAddress}.
	 */
	public synchronized void connect() {
		if (!isConnected()) {
			connectThread = new ConnectThread(this);
			connectThread.setName("connectThread");
			connectThread.setDaemon(true);
			connectThread.start();
		}
	}

	/**
	 * Disconnects from the current device. 
	 */
	public synchronized void disconnect() {
		try {
			dataIn.close();
			dataOut.close();
			connectThread.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			dataIn = null;
			dataOut = null;
			bTCommunicationThread = null;
			connectThread = null;
		}
	
		connected = false;
		
	}

	/**
	 * Checks whether connection is established or not.
	 * @return check result
	 */
	public synchronized boolean isConnected() {
		return connected;
	}

	public Position getPosition() {
		return positionHandler.getPosition();
	}

	public int getNoOfParkingSlots() {
		return parkSlotHandler.getNoOfParkingSlots();
	}

	public ParkingSlot getParkingSlot(int id) {
		return parkSlotHandler.getParkingSlot(id);
	}

	public CurrentStatus getCurrentStatus() {
		return statusHandler.getStatus();
	}

	public void setMode(Mode mode) {
		bTCommunicationThread.sendMode(mode);
	}

	public void setSelectedParkingSlot(int id) {
		bTCommunicationThread.sendSelectedParkingSlot(id);
	}

	/**
	 * Message handler for position data from {@link BTCommunicationThread ReaderThread}.
	 * @author PLT
	 *
	 */
	static class PositionHandler extends Handler {

		private Position position = new Position(new PointF(1, 1), 45, new double[]{0.0, 0.0, 0.0, 0.0});

		/**
		 * Returns the latest position information received from NXT device.
		 * @return latest position information
		 */
		public Position getPosition() {
			return position;
		}

		@Override
		public void handleMessage(Message msg) {
			position = (Position)msg.obj;
		}

	};

	/**
	 * Message handler for parking slot data from {@code ReaderThread}.
	 * @author PLT
	 *
	 */
	static class ParkSlotHandler extends Handler {

		private List<ParkingSlot> parkingSlots = new ArrayList<ParkingSlot>();

		/**
		 * Returns the latest number of parking slots found by robot.
		 * @return number of parking found slots 
		 */
		public int getNoOfParkingSlots() {
			return parkingSlots.size();
		}

		/**
		 * Returns a specific parking slot, retrieved via an ID number.
		 * @param id ID number of the specific parking slot
		 * @return the parking slot matching this ID, or null.
		 */
		public ParkingSlot getParkingSlot(int id) {
			for (ParkingSlot p : parkingSlots) {
				if (p.getID() == id) {
					return p;
				}
			}
			return null;
		}

		@Override
		public void handleMessage(Message msg) {
			parkingSlots.add((ParkingSlot)msg.obj);
		}

	};

	/**
	 * Message handler for status data from {@code ReaderThread}.
	 * @author PLT
	 *
	 */
	static class StatusHandler extends Handler {

		private CurrentStatus status;

		/**
		 * Returns the latest status received from the robot.
		 * @return Status the robot is in
		 */
		public CurrentStatus getStatus() {
			return status;
		}

		@Override
		public void handleMessage(Message msg) {
			status = (CurrentStatus)msg.obj;
		}

	};

	/**
	 * Message handler for receiving commands from main thread, forwarding requests to bluetooth output stream.
	 * @author PLT
	 *
	 */
	@SuppressLint("HandlerLeak")
	class MessageHandler extends Handler {


		public void handleMessage(Message msg) {
			try {
				switch(msg.what) {

				case MSG_MODE:
					dataOut.writeInt(Command.IN_SET_MODE.ordinal());
					dataOut.writeInt(((Mode)msg.obj).ordinal());
					dataOut.flush();
					break;

				case MSG_SELECT_PS:
					dataOut.writeInt(Command.IN_SELECTED_PARKING_SLOT.ordinal());
					dataOut.writeInt((Integer)msg.obj);
					dataOut.flush();
					break;
				}
			} catch (IOException e) {
				Log.e("MessageHandler","IOException: "+e.getMessage());
			}	
		}	
	}

}
