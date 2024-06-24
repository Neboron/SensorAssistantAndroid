package com.example.aitavrd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SlimeServerCom {
    private static final String UDP_IP = "192.168.50.3";
    private static final int UDP_WRANGLER_PORT = 4815;
    private static final int UDP_SLIME_SERVER_PORT = 6969;

    private static final byte PACKET_HEARTBEAT = 0;
    private static final byte PACKET_RECEIVE_HEARTBEAT = 1;
    private static final byte PACKET_HANDSHAKE = 3;
    private static final byte PACKET_ACCEL = 4;
    private static final byte PACKET_PING_PONG = 10;
    private static final byte PACKET_SENSOR_INFO = 15;
    private static final byte PACKET_ROTATION_DATA = 17;

    private long packetNumber = 0;

    private DatagramSocket socket;
    private ArrayList<InetAddress> clientsList = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(SlimeServerCom.class.getName());
    private List<Integer> sensorIdList = new ArrayList<>();


    public SlimeServerCom() throws SocketException, UnknownHostException {
        logger.info("Initializing connection");
        //this.socket = new DatagramSocket(UDP_WRANGLER_PORT, InetAddress.getByName(UDP_IP));
        this.socket = new DatagramSocket();

        Thread rxThread = new Thread(this::listener);
        rxThread.start();
        sendHandshake();
    }

    private void listener() {
        byte[] buffer = new byte[1024]; // Adjust buffer size as necessary
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true) {
            try {
                socket.receive(packet);
                parsePacket(packet.getData());
            } catch (IOException e) {
                logger.warning("Error receiving packet: " + e.getMessage());
            }
        }
    }

    private long getPacketNumber() {
        return packetNumber++;
    }

    public void addSensorId(int sensorId) {
        if (!sensorIdList.contains(sensorId)) {
            sensorIdList.add(sensorId);
        }
    }

    private void parsePacket(byte[] packet) {
        if (packet.length > 4) {
            // First 3 bytes always should be 0x00
            if (packet[0] == 0 && packet[1] == 0 && packet[2] == 0) {
                switch (packet[3]) {
                    case PACKET_RECEIVE_HEARTBEAT:
                        //logger.info("Got PACKET_RECEIVE_HEARTBEAT");
                        //sendSensorInfo(0, 1, 0);
                        for (int sensorId : sensorIdList) {
                            sendSensorInfo(sensorId, 1, 0);
                        }
                        break;
                    case PACKET_PING_PONG:
                        //logger.info("Got PACKET_PING_PONG");
                        try {
                            socket.send(new DatagramPacket(packet, packet.length, InetAddress.getByName(UDP_IP), UDP_SLIME_SERVER_PORT));
                        } catch (IOException e) {
                            logger.warning("Error sending PING_PONG packet: " + e.getMessage());
                        }
                        break;
                    default:
                        //logger.info("Invalid packet");
                }
            }
        }
    }

    public void sendHandshake() {
        // Temporary solution to initialize communication,
        // packet got from Joycon Wrangler using Wireshark
        String hexString = "0000000300000000000000000000000000000000000000000000000000000000000000000000000910736c696d6576722d7772616e676c6572000f000f000f";
        byte[] hexData = hexStringToByteArray(hexString);
        try {
            socket.send(new DatagramPacket(hexData, hexData.length, InetAddress.getByName(UDP_IP), UDP_SLIME_SERVER_PORT));
        } catch (IOException e) {
            logger.warning("Error sending HANDSHAKE packet: " + e.getMessage());
        }
    }

    public void sendSensorInfo(int sensorId, int sensorState, int sensorType) {
        ByteBuffer packet = ByteBuffer.allocate(15); // Adjust the size as necessary
        packet.put(new byte[]{0, 0, 0}); // Placeholder for any data you need at the start
        packet.put(PACKET_SENSOR_INFO);
        packet.putLong(getPacketNumber()); // Assuming getPacketNumber() returns a long
        packet.put((byte) sensorId);
        packet.put((byte) sensorState);
        packet.put((byte) sensorType);

        try {
            socket.send(new DatagramPacket(packet.array(), packet.position(), InetAddress.getByName(UDP_IP), UDP_SLIME_SERVER_PORT));
        } catch (IOException e) {
            logger.warning("Error sending SENSOR_INFO packet: " + e.getMessage());
        }
    }

    public void sendRotationData(byte sensorId, float[] quaternion, byte dataType, byte accuracyInfo) {
        ByteBuffer packet = ByteBuffer.allocate(31); // Adjust the size based on the actual data size
        //packet.order(ByteOrder.LITTLE_ENDIAN);

        packet.put(new byte[]{0, 0, 0});
        packet.put(PACKET_ROTATION_DATA);
        packet.putLong(getPacketNumber());
        packet.put(sensorId);
        packet.put(dataType);
        packet.putFloat(quaternion[0]);
        packet.putFloat(quaternion[1]);
        packet.putFloat(quaternion[2]);
        packet.putFloat(quaternion[3]);
        packet.put(accuracyInfo);

        try {
            socket.send(new DatagramPacket(packet.array(), packet.position(), InetAddress.getByName(UDP_IP), UDP_SLIME_SERVER_PORT));
        } catch (IOException e) {
            logger.warning("Error sending ROTATION_DATA packet: " + e.getMessage());
        }
    }

    public void sendSensorAcceleration(byte sensorId, float[] acceleration, byte dataType) {
        ByteBuffer packet = ByteBuffer.allocate(27); // Adjust the size as needed based on actual data size
        //packet.order(ByteOrder.LITTLE_ENDIAN);

        packet.put(new byte[]{0, 0, 0});
        packet.put(PACKET_ACCEL);
        packet.putLong(getPacketNumber());
        packet.putFloat(acceleration[0]);
        packet.putFloat(acceleration[1]);
        packet.putFloat(acceleration[2]);
        packet.put(sensorId);

        try {
            socket.send(new DatagramPacket(packet.array(), packet.position(), InetAddress.getByName(UDP_IP), UDP_SLIME_SERVER_PORT));
        } catch (IOException e) {
            logger.warning("Error sending ACCELERATION_DATA packet: " + e.getMessage());
        }
    }

    // Utility method to convert a hex string to a byte array
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
