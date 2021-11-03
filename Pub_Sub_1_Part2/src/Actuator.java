/* @author: 	Stephen Davis (code extended from provided code on blackboard by lecturer Stefan Weber) 
 * student id: 	18324401
*/

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Actuator extends Node {

	private static final int NUMBER_OF_SENSORS = 3;
//	private static final int NUMBER_OF_ACTUATORS = 3;

	static final int BROKER_PORT = 50001; // Port of the broker (destination)
	static final int DASHBOARD_PORT = 50002;
	static final int FIRST_SENSOR_PORT = 50003; // Port of the Sensor (source)
	static final int FIRST_ACTUATOR_PORT = (FIRST_SENSOR_PORT+NUMBER_OF_SENSORS+1); // Port of the broker (destination)

	static final String DEFAULT_DST_NODE = "broker";	// Name of the host for the ACTUATOR

	static final int HEADER_LENGTH = 2;
	static final int TYPE_POS = 0;

	static final byte TYPE_UNKNOWN = 0;

	static final int LENGTH_POS = 1;

	static final int ACKCODE_POS = 1;
	static final byte ACK_ALLOK = 10;

	static final byte DASHBOARD_SUBSCRIBE = 1; // DASHBOARD
	static final byte BROKER = 2; // broker
	static final byte SENSOR_PUBLISH = 3; // SENSOR
	static final byte ACTUATOR_SUBSCRIBE = 4;
	static final byte ACTUATOR_PUBLISH = 5;
	static final byte DASHBOARD_PUBLISH = 6; // DASHBOARD
	static final byte TYPE_ACK = 7;   // Indicating an acknowledgement

	Scanner scanner;
	InetSocketAddress dstAddress;

	Actuator(int port) {
		try {
			dstAddress = new InetSocketAddress(DEFAULT_DST_NODE, BROKER_PORT);
			socket= new DatagramSocket(port);
			listener.go();
			scanner = new Scanner(System.in);
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets are sent from Broker.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			String content;
			byte[] data;

			data = packet.getData();			
			switch(data[TYPE_POS]) {
			// Broker has forwarded message from Dashboard
			case BROKER:
				content = sendACK(packet, data);
				executeInstruction(content);
				break;
			case TYPE_ACK:
				System.out.println("Broker received my packet");
				break;
			default:
				System.out.println("Unexpected packet: "+packet.toString());

			}

		}
		catch(Exception e) {e.printStackTrace();}
	}

	/**
	 * Sender Method - connect me to broker, i.e., makeConnection
	 *
	 */
	public synchronized void executeInstruction(String content) throws Exception {
		String[] contentWords = content.split(" ");
		for(String s : contentWords) {
			s.trim();
		}
		String topic = contentWords[0];
		String instruction = contentWords[1];

		if(instruction.equalsIgnoreCase("good_job")) {
			System.out.println("Continuing as normal");
		}
		else if(instruction.equalsIgnoreCase("lower_temperature")) {
			System.out.println("Lowering temperature");
		}
		else if(instruction.equalsIgnoreCase("raise_temperature")) {
			System.out.println("Raising temperature");
		}

		byte[] data= null;
		byte[] buffer= null;
		DatagramPacket packet= null;
		String reply = topic + " Instructions_completed_as_per_request";
		buffer = reply.getBytes();
		data = new byte[HEADER_LENGTH+buffer.length];
		data[TYPE_POS] = ACTUATOR_PUBLISH;						// To show ACTUATOR is sending message
		data[LENGTH_POS] = (byte)buffer.length;

		System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

		packet= new DatagramPacket(data, data.length);
		packet.setSocketAddress(dstAddress);							// set socketAddress to Broker address
		socket.send(packet);
		System.out.println("Packet sent from Actuator to Broker");
	}

	/**
	 * Sender Method - connect me to broker, i.e., makeConnection
	 *
	 */
	public synchronized void subscribeToBroker() throws Exception {
		byte[] data= null;
		byte[] buffer= null;
		DatagramPacket packet= null;
		String input;
		
		System.out.println("Payload: ");
		input = scanner.nextLine();
//		scanner.close();
		buffer = input.getBytes();
		data = new byte[HEADER_LENGTH+buffer.length];
		data[TYPE_POS] = ACTUATOR_SUBSCRIBE;						
		data[LENGTH_POS] = (byte)buffer.length;

		System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

		packet= new DatagramPacket(data, data.length);
		packet.setSocketAddress(dstAddress);							// set socketAddress to Broker address
		socket.send(packet);
		System.out.println("Packet sent from Actuator to Broker");
	}

	/**
	 * ACK Sender Method
	 *
	 */
	public synchronized String sendACK(DatagramPacket packet, byte[] data) throws Exception {
		try {
			String content;
			byte[] buffer = new byte[data[LENGTH_POS]];
			buffer= new byte[data[LENGTH_POS]];
			System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
			content= new String(buffer);
			System.out.println("Received packet from Broker\nMessage from Broker to Actuator was: "+content);
			data = new byte[HEADER_LENGTH];
			data[TYPE_POS] = TYPE_ACK;
			data[ACKCODE_POS] = ACK_ALLOK;

			DatagramPacket response;
			response = new DatagramPacket(data, data.length);
			response.setSocketAddress(packet.getSocketAddress());
			socket.send(response);
			System.out.println("ACK sent from Actuator");
			return content;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public synchronized void start() throws Exception {
		System.out.println("Waiting for contact");
		subscribeToBroker();
		this.wait();
	}

	/*
	 * 
	 */
	public static void main(String[] args) {
		try {
//			for(int i=0; i<NUMBER_OF_ACTUATORS; i++) {
//				Actuator actuator = new Actuator(FIRST_ACTUATOR_PORT + (i+1));
				Actuator actuator = new Actuator(FIRST_ACTUATOR_PORT + (1));
				actuator.start();
//			}
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}
