import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Actuator extends Node {

	private static final int NUMBER_OF_SENSORS = 3;
	private static final int NUMBER_OF_ACTUATORS = 3;

	static final int BROKER_PORT = 50001; // Port of the broker (destination)
	static final int DASHBOARD_PORT = 50002;
	static final int FIRST_SENSOR_PORT = 50003; // Port of the Sensor (source)
	static final int FIRST_ACTUATOR_PORT = (FIRST_SENSOR_PORT+NUMBER_OF_SENSORS+1); // Port of the broker (destination)

	static final String DEFAULT_DST_NODE = "localhost";	// Name of the host for the ACTUATOR

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

	Terminal terminal;
	InetSocketAddress dstAddress;

	/*
	 * 
	 */
	Actuator(Terminal terminal, int port) {
		try {
			this.terminal= terminal;
			dstAddress = new InetSocketAddress(DEFAULT_DST_NODE, BROKER_PORT);
			socket= new DatagramSocket(port);
			listener.go();
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
				terminal.println("Message from Broker to Actuator was: "+content);
				executeInstruction(content);
				break;

				// Received ACK from Broker
			case TYPE_ACK:
				terminal.println("Broker received my subscription");
				break;

			default:
				terminal.println("Unexpected packet" + packet.toString());

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
			terminal.println("Continuing as normal");
		}
		else if(instruction.equalsIgnoreCase("lower_temperature")) {
			terminal.println("Lowering temperature");
		}
		else if(instruction.equalsIgnoreCase("raise_temperature")) {
			terminal.println("Raising temperature");
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

		terminal.println("Actuator sending packet to broker...");
		packet= new DatagramPacket(data, data.length);
		packet.setSocketAddress(dstAddress);							// set socketAddress to Broker address
		socket.send(packet);
		terminal.println("Packet sent from Actuator to Broker");
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

		input= terminal.read("Payload: ");
		buffer = input.getBytes();

		data = new byte[HEADER_LENGTH+buffer.length];

		data[TYPE_POS] = ACTUATOR_SUBSCRIBE;						// To show ACTUATOR is sending message

		data[LENGTH_POS] = (byte)buffer.length;

		System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

		terminal.println("Actuator sending packet to broker...");
		packet= new DatagramPacket(data, data.length);
		packet.setSocketAddress(dstAddress);							// set socketAddress to Broker address
		socket.send(packet);
		terminal.println("Packet sent from Actuator to Broker");
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

			terminal.println("|" + content + "|");
			terminal.println("Length: " + content.length());
			// You could test here if the String says "end" and terminate the
			// program with a "this.notify()" that wakes up the start() method.
			data = new byte[HEADER_LENGTH];
			data[TYPE_POS] = TYPE_ACK;
			data[ACKCODE_POS] = ACK_ALLOK;

			DatagramPacket response;
			response = new DatagramPacket(data, data.length);
			response.setSocketAddress(packet.getSocketAddress());
			terminal.println("Actuator sending ACK");
			socket.send(response);
			terminal.println("ACK sent from Actuator");
			return content;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public synchronized void start() throws Exception {
		terminal.println("Waiting for contact");
		this.wait();
	}

	/*
	 * 
	 */
	public static void main(String[] args) {
		try {
			for(int i=0; i<NUMBER_OF_ACTUATORS; i++) {
				Terminal terminal= new Terminal("Actuator " + (i+1));
				Actuator actuator = new Actuator(terminal, FIRST_ACTUATOR_PORT + (i+1));
				actuator.subscribeToBroker();
			}
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}
