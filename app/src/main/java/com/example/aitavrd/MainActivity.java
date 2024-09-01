package com.example.aitavrd;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ScrollView;







public class MainActivity extends AppCompatActivity implements BluetoothNeuroSky.EyeBlinkDataListener,
        BluetoothNeuroSky.AttentionDataListener, BluetoothNeuroSky.MeditationDataListener {

    private BLEMultiLink.BluetoothLeService bluetoothService;
    private Logger logger;
    private BLEMultiLink bleMultiLink;
    private BluetoothNeuroSky bluetoothNeuroSky;
    private final Handler scanHandler = new Handler();
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<Device> deviceList = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private ImageView arrowIcon;
    private String deviceAddress;
    private OSC oscClient;
    private SettingsActivity settingsActivity;
    private NeuroSkyDetailsActivity neuroSkyDetailsActivity;
    private final Handler updateSettingsHandler = new Handler();


    private final Handler updateDeviceInfoHandler = new Handler();
    private final long UPDATE_DEVICE_INFO_INTERVAL = 10000; // 10 seconds


    private boolean eyeBlinkOscEnable = false;
    private boolean eyeBlinkUseIntegers = false;
    private List<String> eyeBlinkOscAddressList;
    private int eyeBlinkTriggerIntensityMin = 0;
    private int eyeBlinkTriggerIntensityMax = 100;
    private int eyeBlinkReleaseTime = 100;
    private int eyeBlinkUseIntegersTrue = 1;
    private int eyeBlinkUseIntegersFalse = 0;
    private boolean attentionOscEnable = false;
    private boolean meditationOscEnable = false;
    private List<String> attentionOscAddressList;
    private List<String> meditationOscAddressList;
    private String attentionFilterName;
    private String meditationFilterName;
    private int attentionSmoothingValue;
    private int meditationSmoothingValue;
    private Number[]  attentionInterpolationPointValues = {0, 25, 50, 75, 100};
    private Number[]  meditationInterpolationPointValues = {0, 25, 50, 75, 100};
    private List<String> emotionTriggerOscAddressList;
    private boolean emotionTriggerOscEnable = false;
    private boolean attentionTriggerEnable;
    private boolean attentionTriggered;
    private int attentionTriggerValue;
    private int attentionTriggerEmotionId;
    private int attentionTriggerStrength;
    private boolean meditationTriggerEnable;
    private boolean meditationTriggered;
    private int meditationTriggerValue;
    private int meditationTriggerEmotionId;
    private int meditationTriggerStrength;
    private boolean emotionSet;

    // Variables to hold the previous values for attention and meditation
    private float previousAttentionValue = 0f;
    private float previousMeditationValue = 0f;

    // Handler and Runnables for the interpolation steps
    private final Handler attentionHandler = new Handler();
    private final Handler meditationHandler = new Handler();

    // Constants for smoothing
    private final int smoothingSteps = 5; // Number of steps for smoothing
    private final int smoothingInterval = 200; // Interval between steps in milliseconds (5Hz)



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView logTextView = findViewById(R.id.DebugTerminal);
        ScrollView scrollViewLog = findViewById(R.id.scrollViewLog);
        recyclerView = findViewById(R.id.device_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        drawerLayout = findViewById(R.id.drawer_layout);
        arrowIcon = findViewById(R.id.arrowIcon);
        arrowIcon.setAlpha(0.8f);

        logger = new Logger(logTextView, scrollViewLog);
        logger.log("Application started.");
        logger.showLogo();


        bleMultiLink = new BLEMultiLink(this);

        // Initialize and connect BluetoothNeuroSky
        bluetoothNeuroSky = BluetoothNeuroSky.getInstance(this);
        bluetoothNeuroSky.addEyeBlinkDataListener(this);
        bluetoothNeuroSky.addAttentionDataListener(this);
        bluetoothNeuroSky.addMeditationDataListener(this);
        updateNeuroSkySettings();

        settingsActivity = new SettingsActivity();
        neuroSkyDetailsActivity = new NeuroSkyDetailsActivity();

        // Start the Settings update checker
        updateSettingsHandler.post(updateSettingsRunnable);

        // Initialize OSC and connect to user PC
        oscClient = OSC.getInstance(this);
        initializeOscClient();


        logger.log("Found Bluetooth adapter");

        // Array of permissions to request
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE// Add other permissions as needed
        };

        // Check if any of the permissions are not granted
        boolean shouldRequestPermissions = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                shouldRequestPermissions = true;
                break;
            }
        }

        if (shouldRequestPermissions) {
            // Request the permissions
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            // All permissions already granted
            logger.log("All necessary permissions are already granted");
        }

        // Check for the fine location permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        } else {
            // Permission already granted, proceed with BLE scanning.
            logger.log("Permission already granted, proceed with BLE scanning");
        }

        Button connectButton = findViewById(R.id.Scan);
        connectButton.setOnClickListener((v) -> {
            // Check for permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BLEDBG", "Bluetooth scanning permission not granted");
                return;
            }

            int result = bleMultiLink.startScanning();
            if (result == -2) {
                logger.log("Bluetooth not supported");
            } else if (result == -1) {
                logger.log("Permission not granted, scan interrupted");
            } else if (result == 1) {
                logger.log("[LOAD]Scanning started...");
            }

            startPeriodicScanning();
        });

        Button settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            logger.log("[LOAD]Loading Settings...");
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        //disconnectFromNeuroSkyDevice();


        // When user clicked on some adapter from the list this function will be called
        adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) {
                deviceClicked(device);
            }
        });
        recyclerView.setAdapter(adapter);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // Move the arrow icon with the drawer
                arrowIcon.setTranslationX(-slideOffset * drawerView.getWidth());
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                arrowIcon.setAlpha(0.0f);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                arrowIcon.setAlpha(1.0f);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        Intent gattServiceIntent = new Intent(this, BLEMultiLink.BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        // Start the periodic task for updating battery levels
        updateDeviceInfoHandler.post(updateDeviceInfoTask);
    }



    @Override
    protected void onStop() {
        super.onStop();
        // Stop the periodic task when the activity is no longer visible
        updateDeviceInfoHandler.removeCallbacks(updateDeviceInfoTask);
    }



    private void updateDeviceParams(String deviceId, String newName, boolean newIsPaired, int newBatteryLevel, int newSignalStrength, long newLastDiscoveryTime) {
        for (Device device : deviceList) {
            if (device.getId().equals(deviceId)) {
                device.setName(newName);
                device.setPaired(newIsPaired);
                device.setBatteryLevel(newBatteryLevel);
                device.setSignalStrength(newSignalStrength);
                device.setLastDiscoveryTime(newLastDiscoveryTime);
                break; // Break out of the loop once the device is found and updated
            }
        }
    }

    private void updateDevicePairedStatus(String deviceId, boolean isPaired) {
        for (Device device : deviceList) {
            if (device.getId().equals(deviceId)) {
                Log.d("BLEDBG", "Setting paired status for " + deviceId + " to " + isPaired);
                device.setPaired(isPaired);
                adapter.notifyDataSetChanged();
                Log.d("BLEDBG", "Updated paired status for " + deviceId + " to " + isPaired);
                break;
            }
        }
    }

    private void updateDeviceBatteryLevel(String deviceId, int newBatteryLevel) {
        for (Device device : deviceList) {
            if (device.getId().equals(deviceId)) {
                device.setBatteryLevel(newBatteryLevel);
                adapter.notifyDataSetChanged(); // Notify the adapter to refresh the views.
                Log.d("BLEDBG", "Updated battery level for " + deviceId + " to " + newBatteryLevel + "%");
                break; // Break out of the loop once the device is found and updated
            }
        }
    }

    private void deviceClicked(Device device) {
        deviceAddress = device.getId();
        String deviceName = device.getName();
        Log.e("BLEDBG", "Address: " + deviceAddress);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (device.getPaired()) {
            Log.e("BLEDBG", "Device already connected: " + deviceAddress);
            // Open DeviceDetailsActivity with the device information and telemetry data
            if (deviceName.contains("NeuroSky"))
            {
                logger.log("[LOAD]Loading NeuroSky device data...");
                Intent intent = new Intent(MainActivity.this, NeuroSkyDetailsActivity.class);
                intent.putExtra("DEVICE_ADDRESS", deviceAddress);
                intent.putExtra("DEVICE_NAME", device.getName());
                startActivity(intent);
            }
            return;
        }

        if (bluetoothService != null) {
            Log.e("BLEDBG", "Connecting to " + deviceAddress);

            // 50 milliseconds haptic feedback
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(50);
            }

            boolean result = bluetoothService.connect(deviceAddress);
            if (result) {
                device.setPaired(true);
            }
        }
    }


    private void updateDeviceInfo() {
        for (Device device : deviceList) {
            if (device.getPaired()) {
                if (bluetoothService != null) {
                    if (device.getName().equals("NeuroSky"))
                    {

                    }
                    else {
                        String serviceUUIDPartial = "4801";
                        String characteristicUUIDPartial = "4203";
                        bluetoothService.readCharacteristicWithPartialUUID(device.getId(), serviceUUIDPartial, characteristicUUIDPartial);
                    }
                }
            }
        }
    }


    private final Runnable updateDeviceInfoTask = new Runnable() {
        @Override
        public void run() {
            updateDeviceInfo();
            // Schedule the next execution
            updateDeviceInfoHandler.postDelayed(this, UPDATE_DEVICE_INFO_INTERVAL);
        }
    };

    private void initializeOscClient() {
        SharedPreferences preferences = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        String ipAddress = preferences.getString(SettingsActivity.OSC_IP_KEY, "127.0.0.1");
        int txPort = Integer.parseInt(preferences.getString(SettingsActivity.OSC_TX_PORT_KEY, "9001"));
        int rxPort = Integer.parseInt(preferences.getString(SettingsActivity.OSC_RX_PORT_KEY, "9000"));

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                oscClient.initialize(ipAddress, rxPort, txPort);
                oscClient.setMessageListener((address, arguments) -> {
                    //Log.d("OSC", "Received message: " + address + " with arguments: " + arguments);
                    runOnUiThread(() -> {
                        // Handle the received message
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500);
    }

    private void connectToNeuroSkyDevice() {
        boolean alreadyInTheList = false;
        String neuroSkyDeviceAddress = "04:23:07:13:12:E0"; //Just saved mine for testing purpose
        //boolean isConnected = bluetoothNeuroSky.connect(null);
        boolean isConnected = bluetoothNeuroSky.connect(neuroSkyDeviceAddress);
        if (isConnected) {
            for (Device device : deviceList) {
                String deviceName = device.getName();
                if (deviceName != null && deviceName.equals("NeuroSky")) {
                    alreadyInTheList = true;
                    break;
                }
            }

            if (!alreadyInTheList)
            {
                String neuroSkyAddress = bluetoothNeuroSky.getAddress();
                if (neuroSkyAddress != null) {
                    Device device = new Device(neuroSkyAddress, "NeuroSky");
                    device.setPaired(true);
                    deviceList.add(device);
                    adapter.notifyDataSetChanged();
                    logger.log("Connected to NeuroSky device");
                }
            }
        } else {
            Log.i("NeuroSky", "Failed to connect to NeuroSky device");
        }
    }

    private void disconnectFromNeuroSkyDevice() {
        bluetoothNeuroSky.disconnect();
        logger.log("Disconnected from NeuroSky device");
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((BLEMultiLink.BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (!bluetoothService.initialize()) {
                    Log.e("BLEDBG", "Unable to initialize Bluetooth");
                    finish();
                }
                // perform device connection
                bluetoothService.connect(deviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };

    private void startPeriodicScanning() {
        final Runnable scanningTask = new Runnable() {
            Map<String, Device> previousDevices = new HashMap<>(); // Store the previous state of devices

            @Override
            public void run() {
                Map<String, Device> currentDevices = bleMultiLink.getDiscoveredDevices();

                if (currentDevices != null) {
                    // Check for new devices or updates to existing devices
                    for (String address : currentDevices.keySet()) {
                        Device newDevice = currentDevices.get(address);
                        boolean deviceExists = false;
                        for (Device device : deviceList) {
                            if (device.getId().equals(address)) {
                                // Update existing device, preserve paired status
                                boolean pairedStatus = device.getPaired();
                                device.setName(newDevice.getName());
                                device.setPaired(pairedStatus); // Preserve paired status
                                //device.setBatteryLevel(newDevice.getBatteryLevel());
                                device.setSignalStrength(newDevice.getSignalStrength());
                                device.setLastDiscoveryTime(newDevice.getLastDiscoveryTime());
                                deviceExists = true;
                                Log.d("BLEDBG", "Updated existing device: " + address + " with paired status: " + pairedStatus);
                                break;
                            }
                        }
                        if (!deviceExists) {
                            // Add new device
                            deviceList.add(new Device(address, newDevice.getName(), newDevice.getPaired(), newDevice.getBatteryLevel(), newDevice.getSignalStrength(), newDevice.getLastDiscoveryTime()));
                            logger.log("New device discovered: " + newDevice.getName() + " " + address + ", RSSI: " + newDevice.getSignalStrength() + "dBm, ToD: " + newDevice.getLastDiscoveryTime());
                        }
                    }

                    // Check for removed devices
                    for (String address : previousDevices.keySet()) {
                        if (!currentDevices.containsKey(address)) {
                            Device device = previousDevices.get(address);
                            logger.log("Device removed: " + device.getName() + " " + address + ", RSSI: " + device.getSignalStrength() + "dBm, ToD: " + device.getLastDiscoveryTime());
                            deviceList.remove(device);
                        }
                    }

                    adapter.notifyDataSetChanged(); // Notify the adapter to refresh the views
                }

                // Update the previousDevices for the next scan
                previousDevices = new HashMap<>(currentDevices);


                connectToNeuroSkyDevice();

                // Schedule the next execution
                scanHandler.postDelayed(this, 5000); // 5 seconds
            }
        };

        // Initial execution
        scanHandler.post(scanningTask);
    }



    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Log.i("BLEDBG", "Action: " + action);

            if (BLEMultiLink.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Iterate through the services to find the specified service and characteristic
                for (BluetoothGattService gattService : bluetoothService.getSupportedGattServices()) {
                    Log.i("BLEDBG", "Checking services");
                    if (gattService.getUuid().toString().contains("4802")) { // Check if the service contains "4802"
                        Log.i("BLEDBG", "Service found");
                        for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                            Log.i("BLEDBG", "Checking characteristics");
                            if (characteristic.getUuid().toString().contains("4401")) { // Check if the characteristic contains "4401"
                                // Enable notifications for this characteristic
                                bleMultiLink.setCharacteristicNotification(deviceAddress, gattService.getUuid(), characteristic.getUuid(), true);
                                Log.i("BLEDBG", "Notifications enabled for characteristic: " + characteristic.getUuid().toString());
                            }
                        }
                    }
                }
            }

            if (BLEMultiLink.BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("BLEDBG", "connected = true");
                updateDevicePairedStatus(deviceAddress, true);

                // TODO: Rewrite this part, try to avoid delay
                // Post a delayed task to update device information after a specified delay
                updateDeviceInfoHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateDeviceInfo();
                    }
                }, 1000);

                //connected = true;
                //updateConnectionState(R.string.connected);
            } else if (BLEMultiLink.BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("BLEDBG", "connected = flase");
                updateDevicePairedStatus(deviceAddress, false);
                //connected = false;
                //updateConnectionState(R.string.disconnected);
            } else if (BLEMultiLink.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i("BLEDBG", "ACTION_GATT_SERVICES_DISCOVERED");
                // Show all the supported services and characteristics on the user interface.
                List<BluetoothGattService> supportedGattServices = bluetoothService.getSupportedGattServices();
                if (supportedGattServices == null) {
                    Log.i("BLEDBG", "No GATT Services found");
                    return;
                }
                for (BluetoothGattService gattService : supportedGattServices) {
                    String serviceUuid = gattService.getUuid().toString();
                    Log.i("BLEDBG", "Service: " + serviceUuid);

                    // If you also want to log the characteristics of each service
                    for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                        String charUuid = characteristic.getUuid().toString();
                        Log.i("BLEDBG", "  Characteristic: " + charUuid);
                    }
                }
            }

            if (BLEMultiLink.BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String deviceAddress = intent.getStringExtra(BLEMultiLink.BluetoothLeService.EXTRA_DEVICE_ADDRESS);
                int batteryLevel = intent.getIntExtra(BLEMultiLink.BluetoothLeService.EXTRA_BATTERY_LEVEL, 0);
                //Log.i("BLEDBG", "ACTION_DATA_AVAILABLE");
                updateDeviceBatteryLevel(deviceAddress, batteryLevel);
            }

            /*
            if (BLEMultiLink.BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BLEMultiLink.BluetoothLeService.EXTRA_DATA);
                StringBuilder hexString = new StringBuilder();
                if (data != null && data.length > 0) {
                    for (byte byteChar : data) {
                        hexString.append(String.format("%02X ", byteChar));
                    }
                }
                logger.log("Characteristic Data: " + hexString.toString());
            }

             */
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEMultiLink.BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEMultiLink.BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEMultiLink.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEMultiLink.BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



    /*====================================*/
    /*========= SETTINGS READER ==========*/
    /*====================================*/

    public void updateNeuroSkySettings() {
        SharedPreferences preferences = getSharedPreferences(settingsActivity.PREFS_NAME, MODE_PRIVATE);

        eyeBlinkOscAddressList = getMatchingStreamAddresses("NEUROSKY EYE BLINK");
        if (!eyeBlinkOscAddressList.isEmpty()) {
            eyeBlinkOscEnable = true;
        } else {
            eyeBlinkOscEnable = false;
        }

        attentionOscAddressList = getMatchingStreamAddresses("NEUROSKY ATTENTION");
        if (!attentionOscAddressList.isEmpty()) {
            attentionOscEnable = true;
        } else {
            attentionOscEnable = false;
        }

        meditationOscAddressList = getMatchingStreamAddresses("NEUROSKY MEDITATION");
        if (!meditationOscAddressList.isEmpty()) {
            meditationOscEnable = true;
        } else {
            meditationOscEnable = false;
        }

        emotionTriggerOscAddressList = getMatchingStreamAddresses("NEUROSKY EMOTION");
        if (!emotionTriggerOscAddressList.isEmpty()) {
            emotionTriggerOscEnable = true;
        } else {
            emotionTriggerOscEnable = false;
        }

        for (int i = 0; i < neuroSkyDetailsActivity.ATTENTION_FIXED_X_POINTS; i++) {
            float savedValue = preferences.getFloat(neuroSkyDetailsActivity.ATTENTION_INTERPOLATION_POINT_KEY + i, attentionInterpolationPointValues[i].floatValue());
            attentionInterpolationPointValues[i] = savedValue;
        }

        for (int i = 0; i < neuroSkyDetailsActivity.MEDITATION_FIXED_X_POINTS; i++) {
            float savedValue = preferences.getFloat(neuroSkyDetailsActivity.MEDITATION_INTERPOLATION_POINT_KEY + i, meditationInterpolationPointValues[i].floatValue());
            meditationInterpolationPointValues[i] = savedValue;
        }

        eyeBlinkTriggerIntensityMin = preferences.getInt(neuroSkyDetailsActivity.BLINK_TRIGGER_INTENSITY_MIN_KEY, 0);
        eyeBlinkTriggerIntensityMax = preferences.getInt(neuroSkyDetailsActivity.BLINK_TRIGGER_INTENSITY_MAX_KEY, 100);
        eyeBlinkReleaseTime         = preferences.getInt(neuroSkyDetailsActivity.BLINK_RELEASE_TIME_KEY, 100);
        eyeBlinkUseIntegers         = preferences.getBoolean(neuroSkyDetailsActivity.BLINK_USE_INTEGERS_KEY, false);
        eyeBlinkUseIntegersTrue     = preferences.getInt(neuroSkyDetailsActivity.BLINK_USE_INTEGERS_TRUE_KEY, 0);
        eyeBlinkUseIntegersFalse    = preferences.getInt(neuroSkyDetailsActivity.BLINK_USE_INTEGERS_FALSE_KEY, 1);
        attentionFilterName         = preferences.getString(neuroSkyDetailsActivity.ATTENTION_FILTER_KEY, "SMOOTHING 4X");
        meditationFilterName        = preferences.getString(neuroSkyDetailsActivity.MEDITATION_FILTER_KEY, "SMOOTHING 4X");
        attentionTriggerEnable      = preferences.getBoolean(neuroSkyDetailsActivity.ATTENTION_TRIGGER_ENABLE_KEY, false);
        attentionTriggerValue       = preferences.getInt(neuroSkyDetailsActivity.ATTENTION_TRIGGER_VALUE_KEY, 90);
        attentionTriggerEmotionId   = preferences.getInt(neuroSkyDetailsActivity.ATTENTION_TRIGGER_EMOTION_ID_KEY, 0);
        attentionTriggerStrength    = preferences.getInt(neuroSkyDetailsActivity.ATTENTION_TRIGGER_STRENGTH_KEY, 2);
        meditationTriggerEnable     = preferences.getBoolean(neuroSkyDetailsActivity.MEDITATION_TRIGGER_ENABLE_KEY, false);
        meditationTriggerValue      = preferences.getInt(neuroSkyDetailsActivity.MEDITATION_TRIGGER_VALUE_KEY, 90);
        meditationTriggerEmotionId  = preferences.getInt(neuroSkyDetailsActivity.MEDITATION_TRIGGER_EMOTION_ID_KEY, 0);
        meditationTriggerStrength   = preferences.getInt(neuroSkyDetailsActivity.MEDITATION_TRIGGER_STRENGTH_KEY, 2);

        switch (attentionFilterName) {
            case "SMOOTHING 2X":
                attentionSmoothingValue = 2;
                break;
            case "SMOOTHING 4X":
                attentionSmoothingValue = 4;
                break;
            case "SMOOTHING 8X":
                attentionSmoothingValue = 8;
                break;
            case "SMOOTHING 16X":
                attentionSmoothingValue = 16;
                break;
            case "SMOOTHING 32X":
                attentionSmoothingValue = 32;
                break;
            default:
                attentionSmoothingValue = 0;
                break;
        }

        switch (meditationFilterName) {
            case "SMOOTHING 2X":
                meditationSmoothingValue = 2;
                break;
            case "SMOOTHING 4X":
                meditationSmoothingValue = 4;
                break;
            case "SMOOTHING 8X":
                meditationSmoothingValue = 8;
                break;
            case "SMOOTHING 16X":
                meditationSmoothingValue = 16;
                break;
            case "SMOOTHING 32X":
                meditationSmoothingValue = 32;
                break;
            default:
                meditationSmoothingValue = 0;
                break;
        }

        Log.d("MainActivity", "Smoothing Value For Attention: " + attentionSmoothingValue);

        bluetoothNeuroSky.setEyeBlinkFilterParams(eyeBlinkTriggerIntensityMin, eyeBlinkTriggerIntensityMax, eyeBlinkReleaseTime);
        if (attentionSmoothingValue > 0) {
            bluetoothNeuroSky.setAttentionBufferSize(attentionSmoothingValue);
        }
        if (meditationSmoothingValue > 0) {
            bluetoothNeuroSky.setMeditationBufferSize(meditationSmoothingValue);
        }

        Number[]  InterpolationPointValuesX = {0, 25, 50, 75, 100}; // Assume that we have 5 points with equal spacing
        float[] floatInterpolationPointValuesX = new float[InterpolationPointValuesX.length];
        float[] floatAttentionInterpolationPointValues = new float[attentionInterpolationPointValues.length];
        float[] floatMeditationInterpolationPointValues = new float[meditationInterpolationPointValues.length];

        for (int i = 0; i < InterpolationPointValuesX.length; i++) {
            floatInterpolationPointValuesX[i] = InterpolationPointValuesX[i].floatValue();
        }
        for (int i = 0; i < attentionInterpolationPointValues.length; i++) {
            floatAttentionInterpolationPointValues[i] = attentionInterpolationPointValues[i].floatValue();
        }
        for (int i = 0; i < meditationInterpolationPointValues.length; i++) {
            floatMeditationInterpolationPointValues[i] = meditationInterpolationPointValues[i].floatValue();
        }
        bluetoothNeuroSky.setAttentionControlPoints(floatInterpolationPointValuesX, floatAttentionInterpolationPointValues);
        bluetoothNeuroSky.setMeditationControlPoints(floatInterpolationPointValuesX, floatMeditationInterpolationPointValues);
    }

    private List<String> getMatchingStreamAddresses(String targetValue) {
        List<String> matchingAddresses = new ArrayList<>();

        SharedPreferences preferences = getSharedPreferences(settingsActivity.PREFS_NAME, MODE_PRIVATE);

        checkPreferenceForMatch(preferences, settingsActivity.OSC_STREAM1_INPUT_KEY, settingsActivity.OSC_STREAM1_ADDRESS_KEY, targetValue, matchingAddresses);
        checkPreferenceForMatch(preferences, settingsActivity.OSC_STREAM2_INPUT_KEY, settingsActivity.OSC_STREAM2_ADDRESS_KEY, targetValue, matchingAddresses);
        checkPreferenceForMatch(preferences, settingsActivity.OSC_STREAM3_INPUT_KEY, settingsActivity.OSC_STREAM3_ADDRESS_KEY, targetValue, matchingAddresses);
        checkPreferenceForMatch(preferences, settingsActivity.OSC_STREAM4_INPUT_KEY, settingsActivity.OSC_STREAM4_ADDRESS_KEY, targetValue, matchingAddresses);

        return matchingAddresses;
    }

    private void checkPreferenceForMatch(SharedPreferences preferences, String inputKey, String addressKey, String targetValue, List<String> matchingAddresses) {
        String inputValue = preferences.getString(inputKey, "");
        String address = preferences.getString(addressKey, "");

        if (inputValue.contains(targetValue)) {
            matchingAddresses.add(address);
        }
    }

    private final Runnable updateSettingsRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPreferences preferences = getSharedPreferences(settingsActivity.PREFS_NAME, MODE_PRIVATE);
            Boolean neuroskySettingsChanged = preferences.getBoolean(neuroSkyDetailsActivity.SETTINGS_UPDATE_FLAG, false);
            Boolean settingsChanged = preferences.getBoolean(settingsActivity.SETTINGS_UPDATE_FLAG, false);
            if (settingsChanged || neuroskySettingsChanged) {
                updateNeuroSkySettings();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(neuroSkyDetailsActivity.SETTINGS_UPDATE_FLAG, false);
                editor.putBoolean(settingsActivity.SETTINGS_UPDATE_FLAG, false);
                editor.apply();
            }
            updateSettingsHandler.postDelayed(this, 2000); // Repeat every 2 seconds
        }
    };



    /*====================================*/
    /*======= NEUROSKY ACTIVITIES ========*/
    /*====================================*/


    @Override
    public void onEyeBlinkDataReceived(int blinkCount, int blinkIntensity) {
        if (eyeBlinkOscEnable && !emotionSet) {
            for (String address : eyeBlinkOscAddressList) {
                List<Object> args;
                if (!eyeBlinkUseIntegers) {
                    args = Arrays.asList(true);
                } else {
                    args = Arrays.asList(eyeBlinkUseIntegersTrue);
                }
                oscClient.sendMessage(address, args);
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Send "false" message after the release time
                    for (String address : eyeBlinkOscAddressList) {
                        List<Object> args;
                        if (!eyeBlinkUseIntegers) {
                            args = Arrays.asList(false);
                        } else {
                            args = Arrays.asList(eyeBlinkUseIntegersFalse);
                        }
                        if (!emotionSet) { //TODO: may cause stuck of eyelids in close position if address is differ from emotion trigger
                            oscClient.sendMessage(address, args);
                        }
                    }
                }
            }, eyeBlinkReleaseTime);
        }


        Log.d("MainActivity", "Blink Count: " + blinkCount + " Blink Intensity: " + blinkIntensity + " eyeBlinkReleaseTime: " + eyeBlinkReleaseTime);
    }

    @Override
    public void onAttentionDataReceived(int value) {
        float currentAttention = value;
        if (attentionSmoothingValue > 0) {
            currentAttention = bluetoothNeuroSky.smoothAttentionValue(value);
        }

        // Interpolate the current attention value
        float interpolatedAttention = bluetoothNeuroSky.interpolateAttention(currentAttention);
        final float capturedAttention = currentAttention;

        // Interpolation steps between the previous and current attention values
        attentionHandler.removeCallbacksAndMessages(null); // Clear previous tasks
        for (int i = 0; i <= smoothingSteps; i++) {
            float stepValue = (previousAttentionValue + ((interpolatedAttention - previousAttentionValue) / smoothingSteps) * i) / 100.0f;
            attentionHandler.postDelayed(() -> {
                // Send OSC message
                for (String address : attentionOscAddressList) {
                    List<Object> args = Arrays.asList(stepValue);
                    oscClient.sendMessage(address, args);
                }

                // Check and handle emotion trigger logic
                if (emotionTriggerOscEnable) {
                    for (String address : emotionTriggerOscAddressList) {
                        if (attentionTriggerEnable) {
                            attentionTriggered = bluetoothNeuroSky.checkAttentionTrigger(Math.round(capturedAttention), attentionTriggerValue, attentionTriggerStrength);
                            List<Object> argsTrigger;
                            if (attentionTriggered && !meditationTriggered) { // Meditation has higher priority
                                emotionSet = true;
                                argsTrigger = Arrays.asList(attentionTriggerEmotionId);
                            } else if (!attentionTriggered && !meditationTriggered && emotionSet) {
                                emotionSet = false;
                                argsTrigger = Arrays.asList(0); // Reset emotion
                            } else {
                                continue;
                            }
                            oscClient.sendMessage(address, argsTrigger);
                        }
                    }
                }
            }, i * smoothingInterval);
        }

        previousAttentionValue = interpolatedAttention; // Update the previous value
    }

    @Override
    public void onMeditationDataReceived(int value) {
        float currentMeditation = value;
        if (meditationSmoothingValue > 0) {
            currentMeditation = bluetoothNeuroSky.smoothMeditationValue(value);
        }


        // Interpolate the current meditation value
        float interpolatedMeditation = bluetoothNeuroSky.interpolateMeditation(currentMeditation);
        final float capturedMeditation = currentMeditation;

        // Interpolation steps between the previous and current meditation values
        meditationHandler.removeCallbacksAndMessages(null); // Clear previous tasks
        for (int i = 0; i <= smoothingSteps; i++) {
            float stepValue = (previousMeditationValue + ((interpolatedMeditation - previousMeditationValue) / smoothingSteps) * i)/100.0f;
            meditationHandler.postDelayed(() -> {
                // Send OSC message
                for (String address : meditationOscAddressList) {
                    List<Object> args = Arrays.asList(stepValue);
                    oscClient.sendMessage(address, args);
                }

                // Check and handle emotion trigger logic
                if (emotionTriggerOscEnable) {
                    for (String address : emotionTriggerOscAddressList) {
                        if (meditationTriggerEnable) {
                            meditationTriggered = bluetoothNeuroSky.checkMeditationTrigger(Math.round(capturedMeditation), meditationTriggerValue, meditationTriggerStrength);
                            List<Object> argsTrigger;
                            if (meditationTriggered) {  // Meditation has higher priority
                                emotionSet = true;
                                argsTrigger = Arrays.asList(meditationTriggerEmotionId);
                            } else if (!meditationTriggered && !attentionTriggered && emotionSet) {
                                emotionSet = false;
                                argsTrigger = Arrays.asList(0); // Reset emotion
                            } else {
                                continue;
                            }
                            oscClient.sendMessage(address, argsTrigger);
                        }
                    }
                }
            }, i * smoothingInterval);
        }

        previousMeditationValue = interpolatedMeditation; // Update the previous value
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close all BluetoothGatt connections
        bleMultiLink.destroy();
        oscClient.close();
        bluetoothNeuroSky.disconnect();
        bluetoothNeuroSky.removeEyeBlinkDataListener(this);
    }

    @Override
    protected void onResume() {
        Log.d("MainActivity", "Resuming");
        logger.log("[LOAD OK]");
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

}

