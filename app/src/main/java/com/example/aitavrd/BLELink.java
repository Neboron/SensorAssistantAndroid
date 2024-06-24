package com.example.aitavrd;


import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class BLELink {

    // Private Variables:
    private TextView logTextView;
    private ScrollView scrollViewLog;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 8000; // Stops scanning after 4 seconds.
    private Set<String> discoveredDeviceAddresses = new HashSet<>();
    private Map<String, DeviceInfo> discoveredDevicesInfo = new HashMap<>();
    private Context context; // Context for permission checking
    private Map<String, BluetoothGatt> bluetoothGattMap = new HashMap<>();
    private List<BluetoothGattService> discoveredServices = new ArrayList<>();
    private List<BluetoothGattCharacteristic> discoveredCharacteristics = new ArrayList<>();


    public class DeviceInfo {
        private String name;
        private int rssi;
        private long lastDiscoveredTime; // Time in milliseconds

        public DeviceInfo(String name, int rssi, long lastDiscoveredTime) {
            this.name = name;
            this.rssi = rssi;
            this.lastDiscoveredTime = lastDiscoveredTime;
        }

        public String getName() {
            return name;
        }
        public int getRssi() {
            return rssi;
        }
        public long getLastDiscoveredTime() {
            return lastDiscoveredTime;
        }
    }


    public BLELink(Context context) {
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

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.w("BLEDBG", "Lacking required permissions.");
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void Destroy()
    {
        // Close all BluetoothGatt connections
        for (BluetoothGatt gatt : bluetoothGattMap.values()) {
            if (gatt != null) {
                gatt.close();
            }
        }
        bluetoothGattMap.clear();
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

        //Stops scanning after a predefined scan period.
        handler.postDelayed(() -> {
            Log.i("BLEDBG", "Stop scanning");
            bluetoothLeScanner.stopScan(leScanCallback);
        }, SCAN_PERIOD);
        return 1;
    }

    public Map<String, DeviceInfo> getDiscoveredDevicesInfo() {
        return discoveredDevicesInfo;
    }

    public int connectToDevice(String address) {
        Log.e("BLEDBG", "Connecting!!!");
        // Check for permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEDBG", "Bluetooth FINE_LOCATION permission not granted");
            return -1;
        }

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            BluetoothGatt gatt = device.connectGatt(context, false, bluetoothGattCallback);
            bluetoothGattMap.put(address, gatt);
        } catch (IllegalArgumentException e) {
            Log.e("BLEDBG", "Invalid Bluetooth address: " + address, e);
            return -2;
        }
        Log.e("BLEDBG", "Connecting return!!!");
        return 1;
    }

    @SuppressLint("MissingPermission")
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String deviceAddress = result.getDevice().getAddress();
            String deviceName = result.getDevice().getName();
            int rssi = result.getRssi(); // Get the RSSI value
            long currentTime = System.currentTimeMillis(); // Get the current time

            // Regex pattern to check if the device name contains "AitaVRT" followed by zero or more numbers
            String pattern = "AitaVRT\\d*";
            //String pattern = "P2PSRV1";

            // Check if the device name matches the pattern
            if (deviceName != null && deviceName.matches(pattern)) {
                // Update the discoveredDevicesInfo with the latest RSSI and current time
                discoveredDevicesInfo.put(deviceAddress, new DeviceInfo(deviceName, rssi, currentTime));

                // Add to discoveredDeviceAddresses if it's not already there
                discoveredDeviceAddresses.add(deviceAddress);

                // Retrieve a list of service UUIDs from the advertisement data
                List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
                StringBuilder services = new StringBuilder();
                if (serviceUuids != null && !serviceUuids.isEmpty()) {
                    for (ParcelUuid uuid : serviceUuids) {
                        services.append(uuid.toString()).append(" ");
                    }
                }

                Log.i("BLEDBG", "Device found: " + deviceName + " [" + deviceAddress + "], RSSI: " + rssi + " dBm, Time: " + currentTime + ", Services: " + services.toString().trim());
                // Connect to the device.
                //connectToDevice(deviceAddress);
            }
        }
    };



    @SuppressLint("MissingPermission")
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            String deviceAddress = gatt.getDevice().getAddress();
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("BLEDBG", "Connected to GATT server: " + deviceAddress);
                gatt.discoverServices();
                //writeToSpecificCharacteristic(gatt);
                //characteristicNotifyEnable(gatt);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i("BLEDBG", "Disconnected from GATT server: " + deviceAddress);
                bluetoothGattMap.remove(deviceAddress);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e("BLEDBG", "Services discovered!!!");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("BLEDBG", "Services discovered");
                // Read the services and characteristics
                //readServices(gatt.getServices());
                // Optionally, write to a specific characteristic after discovery
                //writeToSpecificCharacteristic(gatt);
                //characteristicNotifyEnable(gatt);
            } else {
                Log.w("BLEDBG", "onServicesDiscovered received: " + status);
            }
        }

        private void readServices(List<BluetoothGattService> services) {
            Log.e("BLEDBG", "Services read!!!");
            for (BluetoothGattService service : services) {
                Log.i("BLEDBG", "Service: " + service.getUuid());
                discoveredServices.add(service);

                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    Log.i("BLEDBG", "  Characteristic: " + characteristic.getUuid());
                    discoveredCharacteristics.add(characteristic);
                }
            }
        }

        private void writeToSpecificCharacteristic(BluetoothGatt gatt) {
            // Define the partial UUID to search for
            String partialUuidString = "4201"; // Replace with the part of your characteristic UUID

            for (BluetoothGattCharacteristic characteristic : discoveredCharacteristics) {
                if (characteristic.getUuid().toString().contains(partialUuidString)) {
                    // Write to the characteristic
                    characteristic.setValue("1"); // Replace with the value you want to write
                    gatt.writeCharacteristic(characteristic);
                    Log.i("BLEDBG", "Writing to characteristic: " + characteristic.getUuid());
                    break;
                }
            }
        }

        private void characteristicNotifyEnable(BluetoothGatt gatt) {
            // Define the partial UUID to search for
            String partialUuidString = "4401"; // Replace with the part of your characteristic UUID

            for (BluetoothGattCharacteristic characteristic : discoveredCharacteristics) {
                if (characteristic.getUuid().toString().contains(partialUuidString)) {
                    Log.i("BLEDBG", "Found characteristic 4401");

                    // Enable notifications for this characteristic
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                    else {

                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLEDBG", "Descriptor write successful: " + descriptor.getUuid());
            } else {
                Log.e("BLEDBG", "Descriptor write failed: " + descriptor.getUuid() + " with status " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Characteristic notification received
            byte[] data = characteristic.getValue();
            Log.i("BLEDBG", "Notification received: " + new String(data));
        }
    };


}
