import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class Sensor extends Node {

	/**
	 *
	 * Sensor class
	 *
	 * An instance accepts input from the user, marshalls this into a datagram, sends
	 * it to a server instance and then waits for a reply. When a packet has been
	 * received, the type of the packet is checked and if it is an acknowledgement,
	 * a message is being printed and the waiting main method is being notified.
	 *
	 */


	private static final int NUMBER_OF_SENSORS = 3;

	static final int BROKER_PORT = 50001; // Port of the broker (destination)
	static final int DASHBOARD_PORT = 50002;
	static final int FIRST_SENSOR_PORT = 50003; // Port of the Sensor (source)
	static final int FIRST_ACTUATOR_PORT = (FIRST_SENSOR_PORT+NUMBER_OF_SENSORS+1); // Port of the broker (destination)


	static final String DEFAULT_DST_NODE = "localhost";	// Name of the host for the server

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


	Terminal terminal;
	InetSocketAddress dstAddress;

	/**
	 * Constructor
	 *
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	Sensor(Terminal terminal, String dstHost, int dstPort, int srcPort) {
		try {
			this.terminal= terminal;
			dstAddress= new InetSocketAddress(dstHost, dstPort);
			socket= new DatagramSocket(srcPort);
			listener.go();
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
			terminal.println("Broker received my packet");
			this.notify();
			break;

		default:
			terminal.println("Unexpected packet" + packet.toString());
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

		input= terminal.read("Payload: ");
		buffer = input.getBytes();

		data = new byte[HEADER_LENGTH+buffer.length];

		data[TYPE_POS] = SENSOR_PUBLISH;						

		data[LENGTH_POS] = (byte)buffer.length;

		System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

		terminal.println("Sensor sending packet to broker...");
		packet= new DatagramPacket(data, data.length);
		packet.setSocketAddress(dstAddress);
		socket.send(packet);
		terminal.println("Packet sent from Sensor to Broker");
	}


	/**
	 * Test method
	 *
	 * Sends a packet to a given address
	 */
	public static void main(String[] args) {
		try {
			for(int i=0; i<NUMBER_OF_SENSORS; i++) {
				Terminal terminal= new Terminal("Sensor "+(i+1));
				Sensor Sensor = new Sensor(terminal, DEFAULT_DST_NODE, BROKER_PORT, (FIRST_SENSOR_PORT + i));
				Sensor.sendMessage();
			}
		} catch(java.lang.Exception e) {
			e.printStackTrace();
		}
	}
}


