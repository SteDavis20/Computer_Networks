/* @author: 	Stephen Davis (code extended from provided code on blackboard by lecturer Stefan Weber) 
 * student id: 	18324401
*/

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Sensor extends Node {

	private static final int NUMBER_OF_SENSORS = 3;
	static final int BROKER_PORT = 50001; // Port of the broker (destination)
	static final int DASHBOARD_PORT = 50002;
	static final int FIRST_SENSOR_PORT = 50003; // Port of the Sensor (source)
	static final int FIRST_ACTUATOR_PORT = (FIRST_SENSOR_PORT+NUMBER_OF_SENSORS+1); // Port of the broker (destination)

	static final String DEFAULT_DST_NODE = "broker";	// Name of the host for the server

	static final int HEADER_LENGTH = 2; // Fixed length of the header
	static final int TYPE_POS = 0; // Position of the type within the header

	static final byte TYPE_UNKNOWN = 0;

	static final byte TYPE_STRING = 1; // Indicating a string payload
	static final int LENGTH_POS = 1;

	static final int ACKCODE_POS = 1; // Position of the acknowledgement type in the header
	static final byte ACK_ALLOK = 10; // Indicating that everything is ok

	static final byte DASHBOARD_SUBSCRIBE = 1; // DASHBOARD
	static final byte BROKER = 2; // broker
	static final byte SENSOR_PUBLISH = 3; // SENSOR
	static final byte ACTUATOR_SUBSCRIBE = 4;
	static final byte ACTUATOR_PUBLISH = 5;
	static final byte DASHBOARD_PUBLISH = 6; // DASHBOARD
	static final byte TYPE_ACK = 7;   // Indicating an acknowledgement


	Scanner scanner;
	InetSocketAddress dstAddress;

	/**
	 * Constructor
	 *
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	Sensor(String dstHost, int dstPort, int srcPort) {
		try {
			dstAddress= new InetSocketAddress(dstHost, dstPort);
			socket= new DatagramSocket(srcPort);
			listener.go();
			scanner = new Scanner(System.in);
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		byte[] data;
		data = packet.getData();
		switch(data[TYPE_POS]) {
		case TYPE_ACK:
			System.out.println("Broker received my packet");
			this.notify();
			break;
		default:
			System.out.println("Unexpected packet: "+packet.toString());
		}
	}


	/**
	 * Sender Method
	 *
	 */
	public synchronized void sendMessage() throws Exception {
		byte[] data= null;
		byte[] buffer= null;
		DatagramPacket packet= null;
		String input;

		System.out.println("Payload: ");
		input = scanner.nextLine();
		buffer = input.getBytes();
		data = new byte[HEADER_LENGTH+buffer.length];
		data[TYPE_POS] = SENSOR_PUBLISH;						
		data[LENGTH_POS] = (byte)buffer.length;
		System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
		packet= new DatagramPacket(data, data.length);
		packet.setSocketAddress(dstAddress);
		socket.send(packet);
		System.out.println("Packet sent from Sensor to Broker");
	}


	/**
	 * Test method
	 *
	 * Sends a packet to a given address
	 */
	public static void main(String[] args) {
		try {
			for(int i=0; i<NUMBER_OF_SENSORS; i++) {
				Sensor Sensor = new Sensor(DEFAULT_DST_NODE, BROKER_PORT, (FIRST_SENSOR_PORT + i));
				Sensor.sendMessage();
			}
		} catch(java.lang.Exception e) {
			e.printStackTrace();
		}
	}
}


