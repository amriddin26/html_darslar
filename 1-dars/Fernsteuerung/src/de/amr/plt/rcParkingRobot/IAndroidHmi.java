package de.amr.plt.rcParkingRobot;

import parkingRobot.INxtHmi.Mode;
import parkingRobot.hsamr0.GuidanceAT.CurrentStatus;
import android.graphics.PointF;

/**
 * Human Machine Interface module for communication with NXT robot. The device implementing this interface is acting as the client, the robot HMI module is 
 * supposed to be the server. The client receives data like position, speed, and available parking slots. Nevertheless it can send commands to 
 * change mode and selected parking spot as well.
 * @author PLT
 *
 */
public interface IAndroidHmi {
	
	/**
	 * Returns bundled current X and Y location , heading angle phi, robot speed and distance to other objects in all 4 directions.
	 * @return next {@link Position Position} from position message queue.
	 */
	public Position getPosition();
	
	/**
	 * Returns number of detected parking slots (value changes during "Scout" mode).
	 * @return total number of parking slots so far.
	 */
	public int getNoOfParkingSlots();
	
	/**
	 * Returns a {@link ParkingSlot ParkingSlot} with the given ID. This contains further information like start position, end position and 
	 * parking slot status.
	 * @param id identification number of requested parking slot
	 * @return the {@code ParkingSlot} matching the provided ID
	 */
	public ParkingSlot getParkingSlot(int id);
	
	/**
	 * Returns {@link CurrentStatus CurrentStatus} which is one of (DRIVING,PARKING,INACTIVE,EXIT).
	 * @return the robot's {@code CurrentStatus}
	 */
	public CurrentStatus getCurrentStatus();
	
	/**
	 * Changes the robot's driving mode. Possible values are SCOUT, PARK_NOW, PARK_THIS and PAUSE. 
	 * @param mode request driving mode for robot
	 */
	public void setMode(Mode mode);
	
	/**
	 * Selects a parking slot via {@code id}. When parking mode is active, this parking slot will be approached.
	 * @param id ID of selected parking slot
	 */
	public void setSelectedParkingSlot(int id);
	
	/*
	 * Subclasses
	 */
	
	/**
	 * Contains all information about robot position and movement. The current position with X and Y location is related to start position 
	 * in mm. Heading direction (angle) is given in degree, where zero degree is the the positive X axis, thus 90 degree is the positive Y axis etc.
	 * The distance to other objects is given for all directions front, back, left and right in mm. Finally it contains the speed in m/s.
	 * <p>
	 * This class is read-only.
	 * @author PLT
	 *
	 */
	public class Position {
		
		// Position bundle with X and Y location and heading angle phi
		private PointF position;
		private float angle;

		// Distance sensor values in clockwise directions (front, right, back, left) in mm
		private double[] distance = new double[4]; 
		
		/**
		 * Creates a new {@code Position}.
		 * @param position the current position with X and Y coordinate and heading angle
		 * @param angle heading angle
		 * @param distance distance measurements of all distance sensors in the order (front, right, back, left)
		 */
		public Position (PointF position, float angle, double[] distance) {
			if (distance.length == this.distance.length) {
				this.position = position;
				this.angle = angle;
				this.distance = distance;
			} else {
				throw new IllegalArgumentException("Expected 4 distance values, got "+distance.length+".");
			}
		}
		
		/**
		 * Creates a new {@code Position}.
		 * @param x X coordinate
		 * @param y Y coordinate
		 * @param angle heading angle
		 * @param distance distance measurements of all distance sensors in the order (front, right, back, left)
		 */
		public Position (float x, float y, float angle, double[] distance) {
			if (distance.length == this.distance.length) {
				this.position = new PointF(x, y);
				this.angle = angle;
				this.distance = distance;
			} else {
				throw new IllegalArgumentException("Expected 4 distance values, got "+distance.length+".");
			}
		}
		
		/**
		 * Gets the X coordinate.
		 * @return X coordinate in cm with 2 numbers behind comma
		 */
		public float getX() {
			return (float) (Math.round(position.x*10000)/100.0);
		}
		
		/**
		 * Gets the Y coordinate.
		 * @return Y coordinate in cm with 2 numbers behind comma 
		 */
		public float getY() {
			return (float) (Math.round(position.y*10000)/100.0);
		}
		
		/**
		 * Gets the heading direction (angle).
		 * @return the heading direction in degree
		 */
		public float getAngle() {
			return (float) (Math.round(angle*180.0*10.0/Math.PI)/10.0);
		}
		
		/**
		 * Gets distance measurement in front of the robot.
		 * @return distance value of front sensor in cm
		 */
		public double getDistanceFront() {
			return (float) (Math.round(distance[0]*100)/100.0);
		}
		
		/**
		 * Gets distance measurement to the right of the robot.
		 * @return distance value of right sensor in cm
		 */
		public double getDistanceFrontSide() {
			return (float) (Math.round(distance[1]*100)/100.0);
		}
		
		/**
		 * Gets distance measurement behind the robot.
		 * @return distance value of back sensor in cm
		 */
		public double getDistanceBack() {
			return (float) (Math.round(distance[2]*100)/100.0);
		}
		
		/**
		 * Gets distance measurement to the left of the robot.
		 * @return distance value of left sensor in cm
		 */
		public double getDistanceBackSide() {
			return (float) (Math.round(distance[3]*100)/100.0);
		}
		
	}
	
	/**
	 * ParkingSlot re-implementation for Android. This class has the same functionality as ParkingSlot from INavigation.java but uses
	 * only Android classes. This is necessary because e.g. lejos.geom.Point is not available on Android.
	 * 
	 * Contains all necessary information about one parking slot. This is the slot status (whether it is/is not satisfying or has
	 * to be scanned again), its ID, the position of the slot begin front boundary and the position of the slot back boundary.
	 * 
	 * @author PLT
	 */
	public class ParkingSlot {		
		// members
		
		/**
		 * Possible states the parking slot can be in
		 */
		public enum ParkingSlotStatus{
			/**
			 * indicates robot could fit in parking slot
			 */
			GOOD,
			/**
			 * indicates that parking slot space is to small
			 */
			BAD,
			/**
			 * indicates that measurement was not satisfying yet
			 */
			RESCAN
		}
		
		/**
		 * stores the parking slot ID that is the number of recent detected parking spaces incremented by one. When delegating the
		 * robot to a special parking slot a distinction between the spaces is necessary and given by identification.  
		 */
		int ID;
		/**
		 * position of parking slot back boundary, robot passes it first then the front boundary
		 */
		PointF backBoundaryPosition = null;
		/**
		 * position of parking slot front boundary, robot passes it second after the back boundary
		 */
		PointF frontBoundaryPosition   = null;
		
		/**
		 * characterization of the parking slot measurement: GOOD - robot could fit in, BAD - space to small,
		 * RESCAN - measurement not satisfying, has to be scanned again
		 */
		ParkingSlotStatus status = ParkingSlotStatus.RESCAN;
		
		
		// Constructor
		
		/**
		 * generate a ParkingSlot object without initialization of data members.
		 */
		ParkingSlot(int ID){
			this.ID = ID;
		}
		
		/**
		 * generate a ParkingSlot object and initialize all data members.
		 * 
		 * @param ID					stores the parking slot ID that is the number of recent detected parking spaces incremented by one
		 * @param backBoundaryPosition	position of parking slot back boundary, robot passes it first then the front boundary
		 * @param frontBoundaryPosition	position of parking slot front boundary, robot passes it second after the back boundary
		 * @param slotStatus			characterization of the parking slot measurement
		 */
		public ParkingSlot(int ID, PointF backBoundaryPosition, PointF frontBoundaryPosition, ParkingSlotStatus slotStatus){
			this.ID     				= ID;
			this.backBoundaryPosition  	= backBoundaryPosition;
			this.frontBoundaryPosition	= frontBoundaryPosition;
			this.status 				= slotStatus;		
		}
		
		// Set methods
		
		/**
		 * @param ID stores the parking slot ID that is the number of recent detected parking spaces incremented by one
		 */
		public void setID(int ID){
			this.ID = ID;
		}
		/**
		 * @param backBoundaryPosition position of parking slot back boundary, robot passes it first then the front boundary
		 */
		public void setBackBoundaryPosition(PointF backBoundaryPosition){
			this.backBoundaryPosition  = backBoundaryPosition;
		}
		/**
		 * @param frontBoundaryPosition position of parking slot front boundary, robot passes it second after the back boundary
		 */
		public void setFrontBoundaryPosition(PointF frontBoundaryPosition){
			this.frontBoundaryPosition = frontBoundaryPosition;
		}
		/**
		 * @param slotStatus characterization of the parking slot measurement
		 */
		public void setParkingSlotStatus(ParkingSlotStatus slotStatus){
			this.status = slotStatus;
		}
		
		
		// Get methods
		
		/**
		 * @return the parking slot ID that is the number of recent detected parking spaces incremented by one
		 */
		public int getID(){
			return ID;
		}
		/**
		 * @return backBoundaryPosition position of parking slot back boundary, robot passes it first then the front boundary
		 */
		public PointF getBackBoundaryPosition(){
			return backBoundaryPosition;
		}
		/**
		 * @return frontBoundaryPosition position of parking slot front boundary, robot passes it second after the back boundary
		 */
		public PointF getFrontBoundaryPosition(){
			return frontBoundaryPosition;
		}
		/**
		 * @return slotStatus characterization of the parking slot measurement
		 */
		public ParkingSlotStatus getParkingSlotStatus(){
			return status;
		}
	}	

	
}
