package com.example.aitavrd;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OSC {

    private static final String TAG = "OSC";
    private static OSC instance;
    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private InetAddress address;
    private int outPort;
    private int inPort;
    private Handler sendHandler;
    private OSCMessageListener messageListener;
    private Thread singleMessageReceiveThread;


    private int receivedMessageCount = 0;
    private int transmittedMessageCount = 0;

    public interface OSCMessageListener {
        void onMessageReceived(String address, List<Object> arguments);
    }

    private OSC(Context context) {
        HandlerThread sendThread = new HandlerThread("OSCSendThread");
        sendThread.start();
        sendHandler = new Handler(sendThread.getLooper());
    }

    public static synchronized OSC getInstance(Context context) {
        if (instance == null) {
            instance = new OSC(context.getApplicationContext());
        }
        return instance;
    }

    public void initialize(String ipAddress, int outPort, int inPort) throws Exception {
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        address = InetAddress.getByName(ipAddress);
        this.outPort = outPort;
        this.inPort = inPort;
        sendSocket = new DatagramSocket();
        receiveSocket = new DatagramSocket(inPort);
        startReceiving();
        Log.d(TAG, "Initialized OSC with IP: " + ipAddress + " and ports: out=" + outPort + ", in=" + inPort);
    }

    public void sendMessage(String address, List<Object> arguments) {
        Log.d(TAG, "Sending message to address: " + address + " with arguments: " + arguments);
        sendHandler.post(() -> {
            try {
                ByteBuffer messageBuffer = ByteBuffer.allocate(1024); // Adjust size as needed

                // Add OSC address
                addOSCString(messageBuffer, address);

                // Add OSC type tag string
                StringBuilder typeTagBuilder = new StringBuilder();
                typeTagBuilder.append(',');
                for (Object arg : arguments) {
                    if (arg instanceof Integer) {
                        typeTagBuilder.append('i');
                    } else if (arg instanceof Float) {
                        typeTagBuilder.append('f');
                    } else if (arg instanceof String) {
                        typeTagBuilder.append('s');
                    } else if (arg instanceof Boolean) {
                        typeTagBuilder.append((Boolean) arg ? 'T' : 'F');
                    }
                }
                addOSCString(messageBuffer, typeTagBuilder.toString());

                // Add OSC arguments
                for (Object arg : arguments) {
                    if (arg instanceof Integer) {
                        messageBuffer.putInt((Integer) arg);
                    } else if (arg instanceof Float) {
                        messageBuffer.putFloat((Float) arg);
                    } else if (arg instanceof String) {
                        addOSCString(messageBuffer, (String) arg);
                    }
                }

                byte[] messageBytes = new byte[messageBuffer.position()];
                messageBuffer.flip();
                messageBuffer.get(messageBytes);

                DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, this.address, outPort);
                sendSocket.send(packet);
                transmittedMessageCount++;
                Log.d(TAG, "OSC message sent: " + address + " with args: " + arguments);
            } catch (Exception e) {
                Log.e(TAG, "Error sending OSC message", e);
            }
        });
    }

    private void addOSCString(ByteBuffer buffer, String str) {
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.put(strBytes);
        buffer.put((byte) 0); // Null-terminate
        // Pad to 4-byte boundary
        int pad = (4 - (strBytes.length + 1) % 4) % 4;
        for (int i = 0; i < pad; i++) {
            buffer.put((byte) 0);
        }
    }

    private void startReceiving() {
        HandlerThread receiveThread = new HandlerThread("OSCReceiveThread");
        receiveThread.start();
        Handler receiveHandler = new Handler(receiveThread.getLooper());
        receiveHandler.post(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (!receiveSocket.isClosed()) {
                    try {
                        receiveSocket.receive(packet);
                        ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                        String address = extractOSCString(byteBuffer);
                        String typeTag = extractOSCString(byteBuffer);
                        List<Object> arguments = parseOSCArguments(byteBuffer, typeTag);
                        receivedMessageCount++;
                        if (messageListener != null) {
                            messageListener.onMessageReceived(address, arguments);
                        }
                    } catch (Exception e) {
                        if (receiveSocket.isClosed()) {
                            Log.d(TAG, "Receive socket closed");
                            break;
                        }
                        Log.e(TAG, "Error receiving OSC message", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up receive socket", e);
            }
        });
    }

    public void cancelReceiveSingleMessage() {
        if (singleMessageReceiveThread != null && singleMessageReceiveThread.isAlive()) {
            singleMessageReceiveThread.interrupt();
            singleMessageReceiveThread = null;
            Log.d(TAG, "Single message receive thread canceled");
        }
    }

    public void receiveSingleMessage(OSCMessageListener listener) {
        singleMessageReceiveThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                if (!receiveSocket.isClosed()) {
                    receiveSocket.receive(packet);
                    ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                    String address = extractOSCString(byteBuffer);
                    String typeTag = extractOSCString(byteBuffer);
                    List<Object> arguments = parseOSCArguments(byteBuffer, typeTag);
                    receivedMessageCount++;
                    if (listener != null) {
                        listener.onMessageReceived(address, arguments);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error receiving OSC message", e);
            }
        });
        singleMessageReceiveThread.start();
    }

    private String extractOSCString(ByteBuffer buffer) {
        StringBuilder builder = new StringBuilder();
        char c;
        while ((c = (char) buffer.get()) != 0) {
            builder.append(c);
        }
        // Align to 4-byte boundary
        while (buffer.position() % 4 != 0) {
            buffer.get();
        }
        return builder.toString();
    }

    private List<Object> parseOSCArguments(ByteBuffer buffer, String typeTag) {
        List<Object> arguments = new java.util.ArrayList<>();
        for (int i = 1; i < typeTag.length(); i++) { // Start from 1 to skip the leading comma
            char type = typeTag.charAt(i);
            switch (type) {
                case 'i':
                    arguments.add(buffer.getInt());
                    break;
                case 'f':
                    arguments.add(buffer.getFloat());
                    break;
                case 's':
                    arguments.add(extractOSCString(buffer));
                    break;
                case 'T':
                    arguments.add(true);
                    break;
                case 'F':
                    arguments.add(false);
                    break;
                // Add other types as needed
            }
        }
        return arguments;
    }

    public void setMessageListener(OSCMessageListener listener) {
        this.messageListener = listener;
    }

    public int getReceivedMessageCount() {
        return receivedMessageCount;
    }

    public int getTransmittedMessageCount() {
        return transmittedMessageCount;
    }

    public void close() {
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        Log.d(TAG, "Closed OSC connection");
    }
}
