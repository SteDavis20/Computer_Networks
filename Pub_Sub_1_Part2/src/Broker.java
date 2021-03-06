/* @author: 	Stephen Davis (code extended from provided code on blackboard by lecturer Stefan Weber) 
 * student id: 	18324401
*/

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class Broker extends Node {

	private static final int NUMBER_OF_SENSORS = 3;

	static final int BROKER_PORT = 50001; // Port of the broker (destination)
	static final int DASHBOARD_PORT = 50002;
	static final int FIRST_SENSOR_PORT = 50003; // Port of the Sensor (source)
	static final int FIRST_ACTUATOR_PORT = (FIRST_SENSOR_PORT+NUMBER_OF_SENSORS+1); // Port of the broker (destination)

	static final int HEADER_LENGTH = 2; // Fixed length of the header
	static final int TYPE_POS = 0; // Position of the type within the header

	static final byte TYPE_UNKNOWN = 0;

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
	private static HashMap<String, ArrayList<SocketAddress>> subscriberMap = new HashMap<String, ArrayList<SocketAddress>>();
	
	static final int PACKETSIZE = 65536;

	Broker(int port) {
		try {
			socket = new DatagramSocket(port);
			listener.go();
			scanner = new Scanner(System.in);
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			String content;
			byte[] data;
			data = packet.getData();			

			SocketAddress srcAddress;

			switch(data[TYPE_POS]) {

			case DASHBOARD_SUBSCRIBE:
				System.out.println("Broker received subscription request from Dashboard");
				content = sendACK(packet, data);
				srcAddress = packet.getSocketAddress();				
				checkSubscriptionExistsAndUpdate(content, srcAddress);
				break;					

			case TYPE_ACK:
				System.out.println("Broker received ack");
				break;

			case ACTUATOR_SUBSCRIBE:
				System.out.println("Broker received subscription request from Actuator");
				content = sendACK(packet, data);
				srcAddress = packet.getSocketAddress();
				checkSubscriptionExistsAndUpdate(content, srcAddress);
				break;

			case SENSOR_PUBLISH:
				System.out.println("Broker received packet from Sensor");
				content = sendACK(packet, data);
				sendMessage(content);
				break;			

			case DASHBOARD_PUBLISH:
				System.out.println("Broker received packet from Dashboard");
				content = sendACK(packet, data);
				sendMessage(content);				
				break;

			case ACTUATOR_PUBLISH:
				System.out.println("Broker received packet from Actuator");
				content = sendACK(packet, data);
				int instructionsIndex = content.indexOf("Instructions");
				String firstHalf = content.substring(0, instructionsIndex);
				String secondHalf = content.substring(instructionsIndex+12, content.length());
				content = firstHalf+secondHalf;	
				sendMessage(content);
				break;

			default:
				System.out.println("Unexpected packet: " + packet.toString());
			}
		} catch (Exception e) {if (!(e instanceof SocketException)) e.printStackTrace();}
	}


	public static void checkSubscriptionExistsAndUpdate(String content, SocketAddress subscriberAddress) {
		String[] contentWords = content.split(" ");
		String topic = contentWords[0];
		ArrayList<SocketAddress> l = new ArrayList<SocketAddress>();

		// if topic already has some subscribers, then add this new subscriber
		if((l = subscriberMap.get(topic))!=null) {
			if(l.contains(subscriberAddress) == false) {
				ArrayList<SocketAddress>l2 = l;
				l2.add(subscriberAddress);
				subscriberMap.replace(topic, l, l2);
			}
		}
		// if no subscriptions to this topic, add new subscriber to this topic
		else {
			ArrayList<SocketAddress> list = new ArrayList<SocketAddress>();
			list.add(subscriberAddress);
			subscriberMap.put(topic, list);
		}
	}

	public synchronized void start() throws Exception {
		System.out.println("Broker waiting for contact");
		this.wait();
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

			data = new byte[HEADER_LENGTH];
			data[TYPE_POS] = TYPE_ACK;
			data[ACKCODE_POS] = ACK_ALLOK;

			DatagramPacket response;
			response = new DatagramPacket(data, data.length);
			response.setSocketAddress(packet.getSocketAddress());
			socket.send(response);
			System.out.println("ACK sent from Broker");
			return content;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Sender Method
	 *
	 */
	public synchronized void sendMessage(String contentAsString) throws Exception {

		String[] contentWords = contentAsString.split(" ");
		for(String s : contentWords) {
			s.trim();
		}
		String topic = contentWords[0];

		ArrayList<SocketAddress> l = new ArrayList<SocketAddress>();

		// if topic already has some subscribers, then add this new subscriber
		l = subscriberMap.get(topic);

		if(l!=null) {
			byte[] data= null;
			byte[] buffer= contentAsString.getBytes();;
			DatagramPacket packet= null;
			data = new byte[HEADER_LENGTH+buffer.length];
			data[TYPE_POS] = BROKER;
			data[LENGTH_POS] = (byte)buffer.length;
			System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

			packet= new DatagramPacket(data, data.length);
			System.out.println("Forwarding packet from Broker...");

			for(SocketAddress dstAddress : l) {
				packet.setSocketAddress(dstAddress);
				socket.send(packet);
				System.out.println("Packet sent");
			}
		}
	}

	public static void main(String[] args) {
		try {
			Broker broker = new Broker(BROKER_PORT);
			broker.start();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
}
