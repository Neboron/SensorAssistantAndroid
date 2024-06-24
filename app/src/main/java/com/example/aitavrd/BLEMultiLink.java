package com.example.aitavrd;


import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.neurosky.connection.*;

public class BLEMultiLink {

    private static BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Context context; // Context for permission checking
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 4000; // Stops scanning after 4 seconds.
    private Set<String> discoveredDeviceAddresses = new HashSet<>();
    private static Map<String, Device> discoveredDevices = new HashMap<>();
    private static Map<String, BluetoothGatt> activeGattConnections = new HashMap<>();
    private static SlimeServerCom slimeServerCom;










    public BLEMultiLink(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Ensures Bluetooth is available on the device and it is enabled.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e("BLELink", "Bluetooth is not enabled or not available.");
            // Handle Bluetooth not available or not enabled
        }

        // Check for the fine location permission.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLELink", "Fine location permission not granted.");
            // ActivityCompat.requestPermissions should be called in an Activity, not here
        } else {
            Log.i("BLELink", "Fine location permission already granted.");
        }
    }


    @SuppressLint("MissingPermission")
    public void destroy() {
        // Stop the scanning if it's in progress
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
            bluetoothLeScanner = null;
        }

        // Close all active BluetoothGatt connections
        for (BluetoothGatt gatt : activeGattConnections.values()) {
            if (gatt != null) {
                gatt.close();
            }
        }
        activeGattConnections.clear();

        // Clean up other resources if necessary
        handler.removeCallbacksAndMessages(null);
    }


    public int startScanning() {
        if (bluetoothAdapter == null) {
            Log.e("BLEDBG", "Bluetooth not supported");
            return -2;
        }

        // Initialize the Bluetooth LE scanner
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Check for permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEDBG", "Bluetooth FINE_LOCATION permission not granted");
            return -1;
        }

        // Starts scanning
        bluetoothLeScanner.startScan(leScanCallback);
        Log.i("BLEDBG", "Started scanning");

        // Stops scanning after a predefined scan period.
        handler.postDelayed(() -> {
            Log.i("BLEDBG", "Stop scanning");
            bluetoothLeScanner.stopScan(leScanCallback);
        }, SCAN_PERIOD);
        return 1;
    }

    private int calculateSensorId(String deviceAddress) {
        int sum = 0;
        for (char ch : deviceAddress.toCharArray()) {
            sum += ch;
        }
        return sum % 256;
    }

    private int getUniqueSensorId(int initialId) {
        int sensorId = initialId;
        while (isSensorIdUsed(sensorId)) {
            sensorId = (sensorId + 1) % 256;
        }
        return sensorId;
    }

    private boolean isSensorIdUsed(int sensorId) {
        for (Device device : discoveredDevices.values()) {
            if (device.getSensorId() == sensorId) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String deviceAddress = result.getDevice().getAddress();
            String deviceName = result.getDevice().getName();
            int rssi = result.getRssi();
            long currentTime = System.currentTimeMillis();

            String pattern = "AitaVRT.*";

            if (deviceName != null && deviceName.matches(pattern)) {
                if (!discoveredDevices.containsKey(deviceAddress)) {
                    int initialSensorId = calculateSensorId(deviceAddress);
                    int sensorId = getUniqueSensorId(initialSensorId);

                    Device device = new Device(deviceAddress, deviceName, false, 0, rssi, currentTime);
                    device.setSensorId(sensorId);
                    slimeServerCom.addSensorId(sensorId);
                    discoveredDevices.put(deviceAddress, device);

                    discoveredDeviceAddresses.add(deviceAddress);

                    List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
                    StringBuilder services = new StringBuilder();
                    if (serviceUuids != null && !serviceUuids.isEmpty()) {
                        for (ParcelUuid uuid : serviceUuids) {
                            services.append(uuid.toString()).append(" ");
                        }
                    }

                    Log.i("BLEDBG", "Device found: " + deviceName + " [" + deviceAddress + "], RSSI: " + rssi + " dBm, Time: " + currentTime + ", Sensor ID: " + sensorId + ", Services: " + services.toString().trim());
                }
            }
        }
    };

    public Map<String, Device> getDiscoveredDevices() {
        return discoveredDevices;
    }


    @SuppressLint("MissingPermission")
    public boolean setCharacteristicNotification(String address, UUID serviceUUID, UUID characteristicUUID, boolean enabled) {
        BluetoothGatt bluetoothGatt = activeGattConnections.get(address);
        if (bluetoothGatt == null) {
            Log.e("BLEDBG", "No GATT connection exists for the given address.");
            return false;
        }

        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service == null) {
            Log.e("BLEDBG", "Service not found.");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
        if (characteristic == null) {
            Log.e("BLEDBG", "Characteristic not found.");
            return false;
        }

        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            return bluetoothGatt.writeDescriptor(descriptor);
        }

        return false;
    }


    public static class BluetoothLeService extends Service {

        private final IBinder binder = new LocalBinder();
        private BluetoothGatt bluetoothGatt;
        public static final String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
        public static final String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
        public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
        public static final String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
        public static final String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
        private static final int STATE_DISCONNECTED = 0;
        private static final int STATE_CONNECTED = 2;
        public static final String EXTRA_DEVICE_ADDRESS = "com.example.bluetooth.le.EXTRA_DEVICE_ADDRESS";
        public static final String EXTRA_BATTERY_LEVEL = "com.example.bluetooth.le.EXTRA_BATTERY_LEVEL";

        // This HashMap will hold the device address as key and the count of notifications as value
        private static final HashMap<String, Integer> notificationCountMap = new HashMap<>();
        // This HashMap will hold the last log time for each device
        private static final HashMap<String, Long> lastLogTimeMap = new HashMap<>();
        private int connectionState;

        private static final long RECONNECT_INTERVAL = 2000; // 2 seconds
        private Handler reconnectHandler = new Handler();
        private Map<String, Runnable> reconnectRunnables = new HashMap<>();



        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return binder;
        }

        public class LocalBinder extends Binder {
            public BluetoothLeService getService() {
                return BluetoothLeService.this;
            }
        }


        public boolean initialize() {
            new Thread(() -> {
                try {
                    slimeServerCom = new SlimeServerCom(); // Initialize your SlimeServerCom
                } catch (SocketException e) {
                    Log.e("BLEMultiLink", "Error initializing SlimeServerCom: SocketException", e);
                } catch (UnknownHostException e) {
                    Log.e("BLEMultiLink", "Error initializing SlimeServerCom: UnknownHostException", e);
                }
            }).start();

            //slimeServerCom.sendHandshake();

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Log.e("BLEDBG", "Unable to obtain a BluetoothAdapter.");
                return false;
            }
            return true;
        }


        @Override
        public boolean onUnbind(Intent intent) {
            close();
            return super.onUnbind(intent);
        }



        @SuppressLint("MissingPermission")
        public boolean connect(final String address) {
            if (bluetoothAdapter == null || address == null) {
                Log.w("BLEDBG", "BluetoothAdapter not initialized or unspecified address.");
                return false;
            }
            try {
                final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                if (activeGattConnections.containsKey(address)) {
                    BluetoothGatt gatt = activeGattConnections.get(address);
                    if (gatt != null) {
                        gatt.disconnect();
                        gatt.close();
                        activeGattConnections.remove(address);
                    }
                }
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
                activeGattConnections.put(address, bluetoothGatt);
                return true; // Connection initiated
            } catch (IllegalArgumentException exception) {
                Log.w("BLEDBG", "Device not found with provided address. Unable to connect.");
                return false;
            }
        }


        @SuppressLint("MissingPermission")
        private void close() {
            if (bluetoothGatt != null) {
                String address = bluetoothGatt.getDevice().getAddress();
                bluetoothGatt.close();
                activeGattConnections.remove(address);
                bluetoothGatt = null;
            }
        }


        @SuppressLint("MissingPermission")
        public void readCharacteristicWithPartialUUID(String address, String serviceUUIDPartial, String characteristicUUIDPartial) {
            BluetoothGatt bluetoothGatt = activeGattConnections.get(address);
            if (bluetoothGatt == null) {
                Log.e("BLEDBG", "No GATT connection exists for the given address.");
                return;
            }

            boolean serviceFound = false;
            boolean characteristicFound = false;

            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                if (service.getUuid().toString().contains(serviceUUIDPartial)) {
                    serviceFound = true;
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().toString().contains(characteristicUUIDPartial)) {
                            characteristicFound = true;
                            bluetoothGatt.readCharacteristic(characteristic);
                            break; // Exit the inner loop once the correct characteristic is found
                        }
                    }
                    if (characteristicFound) break; // Exit the outer loop once the correct service and characteristic are found
                }
            }

            if (!serviceFound) {
                Log.e("BLEDBG", "Service containing UUID part " + serviceUUIDPartial + " not found.");
            } else if (!characteristicFound) {
                Log.e("BLEDBG", "Characteristic containing UUID part " + characteristicUUIDPartial + " not found in service.");
            }
        }


        @SuppressLint("MissingPermission")
        public void readCharacteristic(String address, UUID serviceUUID, UUID characteristicUUID) {
            if (bluetoothAdapter == null || address == null) {
                Log.e("BLEDBG", "BluetoothAdapter not initialized or unspecified address.");
                return;
            }
            BluetoothGatt bluetoothGatt = activeGattConnections.get(address);
            if (bluetoothGatt != null) {
                BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
                    if (characteristic != null) {
                        bluetoothGatt.readCharacteristic(characteristic);
                    } else {
                        Log.e("BLEDBG", "Characteristic not found.");
                    }
                } else {
                    Log.e("BLEDBG", "Service not found.");
                }
            } else {
                Log.e("BLEDBG", "No GATT connection exists for the given address.");
            }
        }


        @SuppressLint("MissingPermission")
        private void enableNotifications(BluetoothGatt gatt) {
            for (BluetoothGattService service : gatt.getServices()) {
                if (service.getUuid().toString().contains("4802")) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().toString().contains("4401")) {
                            gatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        private void disableAllNotifications(BluetoothGatt gatt) {
            if (gatt == null) {
                return;
            }
            for (BluetoothGattService service : gatt.getServices()) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    gatt.setCharacteristicNotification(characteristic, false);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }


        @SuppressLint("MissingPermission")
        private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String deviceAddress = gatt.getDevice().getAddress();
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BluetoothLeService", "Connected to GATT server: " + deviceAddress);
                    connectionState = STATE_CONNECTED;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    gatt.discoverServices();

                    reconnectHandler.removeCallbacks(reconnectRunnables.get(deviceAddress));
                    reconnectRunnables.remove(deviceAddress);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BluetoothLeService", "Disconnected from GATT server: " + deviceAddress);
                    connectionState = STATE_DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);

                    if (activeGattConnections.containsKey(deviceAddress)) {
                        BluetoothGatt gattConnection = activeGattConnections.get(deviceAddress);
                        if (gattConnection != null) {
                            disableAllNotifications(gattConnection);
                            gattConnection.close();
                            activeGattConnections.remove(deviceAddress);
                        }
                    }

                    Runnable reconnectRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.d("BluetoothLeService", "Attempting to reconnect to: " + deviceAddress);
                            connect(deviceAddress);
                        }
                    };
                    reconnectRunnables.put(deviceAddress, reconnectRunnable);
                    reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_INTERVAL);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    enableNotifications(gatt); // Ensure notifications are enabled once
                } else {
                    Log.w("BLEDBG", "onServicesDiscovered received: " + status);
                }
            }


            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    final String deviceAddress = gatt.getDevice().getAddress();
                    String partialBatteryUUID = "4203";

                    if (characteristic.getUuid().toString().contains(partialBatteryUUID)) {
                        int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        Log.d("BLEDBG", String.format("Received battery level: %d from %s", batteryLevel, deviceAddress));
                        final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                        intent.putExtra(EXTRA_BATTERY_LEVEL, batteryLevel);
                        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
                        sendBroadcast(intent);
                    }
                }
            }



            /**
             * Unpacks quaternion data from a packed short array.
             */
            private float[] unpackQuaternion(short[] packedQuaternion) {
                float[] quaternion = new float[4];
                for (int i = 0; i < 4; i++) {
                    quaternion[i] = packedQuaternion[i] / 32767.0f;
                }
                return quaternion;
            }

            /**
             * Unpacks acceleration data from a packed byte array.
             */
            private float[] unpackAcceleration(byte[] packedAcceleration) {
                float[] acceleration = new float[3];
                for (int i = 0; i < 3; i++) {
                    acceleration[i] = ((packedAcceleration[i] & 0xFF) * (16.0f / 255.0f)) - 8.0f;
                }
                return acceleration;
            }

            public float[] quaternionToEulerAnglesDegrees(float[] quaternion) {
                float[] eulerAngles = new float[3]; // roll (X-axis), pitch (Y-axis), yaw (Z-axis)

                // Extract the values for easier calculation
                float w = quaternion[0];
                float x = quaternion[1];
                float y = quaternion[2];
                float z = quaternion[3];

                // Roll (x-axis rotation)
                double sinr_cosp = 2 * (w * x + y * z);
                double cosr_cosp = 1 - 2 * (x * x + y * y);
                eulerAngles[0] = (float) Math.toDegrees(Math.atan2(sinr_cosp, cosr_cosp));

                // Pitch (y-axis rotation)
                double sinp = 2 * (w * y - z * x);
                if (Math.abs(sinp) >= 1)
                    eulerAngles[1] = (float) Math.toDegrees(Math.copySign(Math.PI / 2, sinp)); // use 90 degrees if out of range
                else
                    eulerAngles[1] = (float) Math.toDegrees(Math.asin(sinp));

                // Yaw (z-axis rotation)
                double siny_cosp = 2 * (w * z + x * y);
                double cosy_cosp = 1 - 2 * (y * y + z * z);
                eulerAngles[2] = (float) Math.toDegrees(Math.atan2(siny_cosp, cosy_cosp));

                return eulerAngles;
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                String deviceAddress = gatt.getDevice().getAddress();
                String characteristicUuid = characteristic.getUuid().toString();

                Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                intent.putExtra(EXTRA_DATA, characteristic.getValue());
                sendBroadcast(intent);

                final byte[] data = characteristic.getValue();
                StringBuilder hexString = new StringBuilder();
                if (data != null && data.length > 0) {
                    for (byte byteChar : data) {
                        hexString.append(String.format("%02X ", byteChar));
                    }
                }

                // IMU Data characteristic starts with "00004401"
                if (characteristicUuid.startsWith("00004401")) {
                    int count = notificationCountMap.getOrDefault(deviceAddress, 0) + 1;
                    notificationCountMap.put(deviceAddress, count);

                    short[] packedQuaternion = new short[4];
                    for (int i = 0; i < 4; i++) {
                        packedQuaternion[i] = (short) ((data[1 + i * 2 + 1] << 8) | (data[1 + i * 2] & 0xFF));
                    }
                    float[] quaternion = unpackQuaternion(packedQuaternion);

                    byte[] packedAcceleration = {data[9], data[10], data[11]};
                    float[] acceleration = unpackAcceleration(packedAcceleration);

                    float[] reorderedQuaternion = new float[4];
                    reorderedQuaternion[0] = quaternion[1];
                    reorderedQuaternion[1] = quaternion[2];
                    reorderedQuaternion[2] = quaternion[3];
                    reorderedQuaternion[3] = quaternion[0];

                    // Get the sensorId for the device
                    Device device = discoveredDevices.get(deviceAddress);

                    slimeServerCom.sendRotationData(
                            (byte) device.getSensorId(),
                            reorderedQuaternion,
                            (byte) 0x01,
                            (byte) 0x01
                    );

                    slimeServerCom.sendSensorAcceleration(
                            (byte) device.getSensorId(),
                            acceleration,
                            (byte) 0x01
                    );

                    if (count % 120 == 0) {
                        long now = System.currentTimeMillis();
                        long lastLogTime = lastLogTimeMap.getOrDefault(deviceAddress, now);
                        long timeDiffMillis = now - lastLogTime;
                        double timeDiffSeconds = timeDiffMillis / 1000.0;
                        double frequency = 120d / timeDiffSeconds;

                        Log.i("BLEDBG", "Address: " +  deviceAddress  + " Sensor ID: " + device.getSensorId() + " Characteristic: " + characteristicUuid +
                                " Value: " + hexString + " Notifications Count: " + count +
                                " Frequency: ~" + String.format("%.2f", frequency) + " NPS");

                        float[] eulerAngles = quaternionToEulerAnglesDegrees(quaternion);

                        lastLogTimeMap.put(deviceAddress, now);
                    }
                }
            }
        };


        public List<BluetoothGattService> getSupportedGattServices() {
            if (bluetoothGatt == null) return null;
            return bluetoothGatt.getServices();
        }

        private void broadcastUpdate(final String action) {
            final Intent intent = new Intent(action);
            sendBroadcast(intent);
        }
    }











}
