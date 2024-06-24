package com.example.aitavrd;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.pwittchen.neurosky.library.exception.BluetoothNotEnabledException;
import com.github.pwittchen.neurosky.library.listener.ExtendedDeviceMessageListener;
import com.github.pwittchen.neurosky.library.message.enums.BrainWave;
import com.github.pwittchen.neurosky.library.message.enums.Signal;
import com.github.pwittchen.neurosky.library.message.enums.State;
import com.github.pwittchen.neurosky.library.NeuroSky;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothNeuroSky {
    private static final String TAG = "BluetoothNeuroSky";
    private static BluetoothNeuroSky instance;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private RawDataListener rawDataListener;
    private BrainWavesDataListener brainWavesDataListener;
    private EyeBlinkDataListener eyeBlinkDataListener;
    private AttentionDataListener attentionDataListener;
    private MeditationDataListener meditationDataListener;

    private int eyeBlinkCount = 0;


    // Watchdog to track raw signal state
    private Runnable watchdogRunnableRawData;
    private Handler watchdogHandlerRawData = new Handler(Looper.getMainLooper());
    private long lastRawDataTimestamp = 0;


    // Raw data buffer
    private int raw_data_index = 0;
    // Raw data chunk buffer
    private short[] raw_data_chunk = new short[8];
    private int raw_data_chunk_index = 0;

    // Flags to track the state
    private boolean isConnected = false; // Track connection status
    private boolean isFeedRawSignal = false; // enable to show raw data on GUI (utilize more CPU resources)

    // Device info
    private String connectedDeviceAddress;


    private NeuroSky neuroSky;
    private final static String LOG_TAG = "NeuroSky";

    private BluetoothNeuroSky(Context context) {
        this.context = context.getApplicationContext();
        neuroSky = createNeuroSky();
        feedRawSignal(false);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
        }

        watchdogRunnableRawData = new Runnable() {
            @Override
            public void run() {
                if (isFeedRawSignal && (System.currentTimeMillis() - lastRawDataTimestamp) > 4000) {
                    feedRawSignal(true);
                }
                watchdogHandlerRawData.postDelayed(this, 4000);
            }
        };
        watchdogHandlerRawData.postDelayed(watchdogRunnableRawData, 4000);
    }

    public static synchronized BluetoothNeuroSky getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothNeuroSky(context);
        }
        return instance;
    }

    @NonNull
    private NeuroSky createNeuroSky() {
        return new NeuroSky(new ExtendedDeviceMessageListener() {
            @Override public void onStateChange(State state) {
                handleStateChange(state);
            }

            @Override public void onSignalChange(Signal signal) {
                handleSignalChange(signal);
            }

            @Override public void onBrainWavesChange(Set<BrainWave> brainWaves) {
                handleBrainWavesChange(brainWaves);
            }
        });
    }



    private void handleStateChange(final State state) {
        if (neuroSky != null && state.equals(State.CONNECTED)) {
            neuroSky.start();
        }
        else {
            isConnected = false;
        }

        //Log.d(LOG_TAG, state.toString());
    }

    private void handleSignalChange(final Signal signal) {
        switch (signal) {
            case ATTENTION:
                handleAttentionChange(signal.getValue());
                //Log.d(LOG_TAG, getFormattedMessage("attention: %d", signal));
                break;
            case MEDITATION:
                handleMeditationChange(signal.getValue());
                //Log.d(LOG_TAG, getFormattedMessage("meditation: %d", signal));
                break;
            case BLINK:
                handleBlinkChange(signal.getValue());
                Log.d(LOG_TAG, getFormattedMessage("blink: %d", signal));
                break;
            case RAW_DATA:
                processRawData((short) signal.getValue());
                break;
        }

        //Log.d(LOG_TAG, String.format("%s: %d", signal.toString(), signal.getValue()));
    }

    private String getFormattedMessage(String messageFormat, Signal signal) {
        return String.format(Locale.getDefault(), messageFormat, signal.getValue());
    }

    private void handleBrainWavesChange(final Set<BrainWave> brainWaves) {
        Map<BrainWave, Integer> brainWavesData = new HashMap<>();
        for (BrainWave brainWave : brainWaves) {
            brainWavesData.put(brainWave, brainWave.getValue());
            //Log.d(LOG_TAG, String.format("%s: %d", brainWave.toString(), brainWave.getValue()));
        }
        if (brainWavesDataListener != null) {
            brainWavesDataListener.onBrainWavesDataReceived(brainWavesData);
        }
    }

    private void processRawData(short rawData) {
        raw_data_chunk[raw_data_chunk_index++] = rawData;

        // Plot data every 8 bytes
        if (raw_data_chunk_index == 8) {
            lastRawDataTimestamp = System.currentTimeMillis(); // Update the timestamp
            if (rawDataListener != null) {
                rawDataListener.onRawDataReceived(raw_data_chunk);
            }
            raw_data_chunk_index = 0; // Reset chunk index
        }
    }

    public void feedRawSignal(Boolean enable) {
        if (neuroSky != null) {
            if (enable) {
                isFeedRawSignal = true;
                disconnect(); //TODO: Check how to enable/disable raw signal without restarting connection
                neuroSky.enableRawSignal();
                connect(getAddress());
                //startMonitoring();
            } else {
                isFeedRawSignal = false;
                disconnect();
                neuroSky.disableRawSignal();
                connect(getAddress());
                //stopMonitoring();
            }
        }

    }

    private void handleBlinkChange(int intensity) {
        eyeBlinkCount++;
        if (eyeBlinkDataListener != null) {
            eyeBlinkDataListener.onEyeBlinkDataReceived(eyeBlinkCount, intensity);
        }
    }

    private void handleAttentionChange(int value) {
        if (attentionDataListener != null) {
            attentionDataListener.onAttentionDataReceived(value);
        }
    }

    private void handleMeditationChange(int value) {
        if (meditationDataListener != null) {
            meditationDataListener.onMeditationDataReceived(value);
        }
    }

    public void setRawDataListener(RawDataListener listener) {
        this.rawDataListener = listener;
    }

    public interface RawDataListener {
        void onRawDataReceived(short[] rawData);
    }

    public void setBrainWavesDataListener(BrainWavesDataListener listener) {
        this.brainWavesDataListener = listener;
    }

    public interface BrainWavesDataListener {
        void onBrainWavesDataReceived(Map<BrainWave, Integer> brainWavesData);
    }

    public void setEyeBlinkDataListener(EyeBlinkDataListener listener) {
        this.eyeBlinkDataListener = listener;
    }

    public interface EyeBlinkDataListener {
        void onEyeBlinkDataReceived(int blinkCount, int blinkIntensity);
    }

    public void setAttentionDataListener(AttentionDataListener listener) {
        this.attentionDataListener = listener;
    }

    public interface AttentionDataListener {
        void onAttentionDataReceived(int value);
    }

    public void setMeditationDataListener(MeditationDataListener listener) {
        this.meditationDataListener = listener;
    }

    public interface MeditationDataListener {
        void onMeditationDataReceived(int value);
    }


    @SuppressLint("MissingPermission")
    public boolean connect(String address) {
        if (isConnected) {
            Log.d(TAG, "Already connected");
            return true; // Return true if already connected
        }

        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter not initialized.");
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        try {
            neuroSky.connect();
            isConnected = true; // Mark as connected
            connectedDeviceAddress = address; // Store the connected device address
            return true;
        } catch (BluetoothNotEnabledException e) {
            Log.d(LOG_TAG, e.getMessage());
            isConnected = false;
            return false;
        }

    }

    public String getAddress() {
        return connectedDeviceAddress;
    }

    public Boolean getConnectionStatus() {
        return isConnected;
    }

    public void disconnect() {
        isConnected = false;
        neuroSky.disconnect();
    }

    public void startMonitoring() {
        neuroSky.start();
    }

    public void stopMonitoring() {
        neuroSky.stop();
    }
}
