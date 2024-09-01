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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    private List<EyeBlinkDataListener> eyeBlinkDataListeners = new ArrayList<>();
    private List<AttentionDataListener> attentionDataListeners = new ArrayList<>();
    private List<MeditationDataListener> meditationDataListeners = new ArrayList<>();
    private OSC oscClient;

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

        // Initialize the circular buffers with the default size
        attentionBuffer = new CircularBuffer(attentionBufferSize);
        meditationBuffer = new CircularBuffer(meditationBufferSize);
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
        if (filterEyeBlink(intensity)) {
            eyeBlinkCount++;
            notifyEyeBlinkDataListeners(eyeBlinkCount, intensity);
        }
    }

    private void handleAttentionChange(int value) {
        notifyAttentionDataListeners(value);
    }

    private void handleMeditationChange(int value) {
        notifyMeditationDataListeners(value);
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


    // Eye blink
    public void setEyeBlinkDataListener(EyeBlinkDataListener listener) {
        addEyeBlinkDataListener(listener);
    }

    public interface EyeBlinkDataListener {
        void onEyeBlinkDataReceived(int blinkCount, int blinkIntensity);
    }

    public void addEyeBlinkDataListener(EyeBlinkDataListener listener) {
        if (!eyeBlinkDataListeners.contains(listener)) {
            eyeBlinkDataListeners.add(listener);
        }
    }

    public void removeEyeBlinkDataListener(EyeBlinkDataListener listener) {
        eyeBlinkDataListeners.remove(listener);
    }

    private void notifyEyeBlinkDataListeners(int blinkCount, int blinkIntensity) {
        for (EyeBlinkDataListener listener : eyeBlinkDataListeners) {
            listener.onEyeBlinkDataReceived(blinkCount, blinkIntensity);
        }
    }


    // Attention
    public void setAttentionDataListener(AttentionDataListener listener) {
        addAttentionDataListener(listener);
    }

    public interface AttentionDataListener {
        void onAttentionDataReceived(int value);
    }

    public void addAttentionDataListener(AttentionDataListener listener) {
        if (!attentionDataListeners.contains(listener)) {
            attentionDataListeners.add(listener);
        }
    }

    public void removeAttentionDataListener(AttentionDataListener listener) {
        attentionDataListeners.remove(listener);
    }

    private void notifyAttentionDataListeners(int value) {
        for (AttentionDataListener listener : attentionDataListeners) {
            listener.onAttentionDataReceived(value);
        }
    }

    // Meditation
    public void setMeditationDataListener(MeditationDataListener listener) {
        addMeditationDataListener(listener);
    }

    public interface MeditationDataListener {
        void onMeditationDataReceived(int value);
    }

    public void addMeditationDataListener(MeditationDataListener listener) {
        if (!meditationDataListeners.contains(listener)) {
            meditationDataListeners.add(listener);
        }
    }

    public void removeMeditationDataListener(MeditationDataListener listener) {
        meditationDataListeners.remove(listener);
    }

    private void notifyMeditationDataListeners(int value) {
        for (MeditationDataListener listener : meditationDataListeners) {
            listener.onMeditationDataReceived(value);
        }
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

    private Boolean lastConnectionStatus = false;
    public Boolean getConnectionStatus() {
        if (lastConnectionStatus && isConnected) { // TODO: Find better way to read connection status.
            return true;
        } else {
            lastConnectionStatus = isConnected;
            return false;
        }
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



    /*====================================*/
    /*==== FILTERS AND POSTPROCESSING ====*/
    /*====================================*/

    private long lastBlinkTimestamp = 0;
    private int blinkIntensityMin = 0;
    private int blinkIntensityMax = 100;
    private int blinkPeriod = 100;

    // Circular buffers for smoothing
    private int attentionBufferSize = 4; // Default size
    private int meditationBufferSize = 4; // Default size
    private CircularBuffer attentionBuffer;
    private CircularBuffer meditationBuffer;

    // Control points for Attention and Meditation interpolation
    private float[] attentionControlValuesX = new float[5];
    private float[] attentionControlValuesY = new float[5];
    private float[] meditationControlValuesX = new float[5];
    private float[] meditationControlValuesY = new float[5];

    // State variables for Attention trigger
    private int attentionTriggerCounter = 0;
    private int attentionTriggerDecrementCounter = 0;

    // State variables for Meditation trigger
    private int meditationTriggerCounter = 0;
    private int meditationTriggerDecrementCounter = 0;

    /**
     * Set eye blink filter parameters.
     * @param intensityMin Minimum trigger intensity (0 to 100).
     * @param intensityMax Maximum trigger intensity (0 to 100).
     * @param period Minimum time between blinks (ms), if period is small, blink will be ignored.
     * @return true if pass filter.
     */
    public void setEyeBlinkFilterParams(int intensityMin, int intensityMax, int period) {
        blinkIntensityMin = intensityMin;
        blinkIntensityMax = intensityMax;
        blinkPeriod       = period;
    }

    /**
     * Eye blink filter.
     * @param intensity Blink intensity from NeuroSky Algo.
     * @return true if pass filter.
     */
    public Boolean filterEyeBlink(int intensity) {
        long currentTime = System.currentTimeMillis();
        if (intensity >= blinkIntensityMin && intensity <= blinkIntensityMax &&
                (currentTime - lastBlinkTimestamp) >= blinkPeriod) {
            lastBlinkTimestamp = currentTime;
            return true;
        }
        return false;
    }


    /**
     * Sets the buffer size for smoothing Attention values.
     * @param size The buffer size (must be between 2 and 32).
     */
    public void setAttentionBufferSize(int size) {
        if (size < 2 || size > 32) {
            throw new IllegalArgumentException("Buffer size must be between 2 and 32.");
        }
        attentionBufferSize = size;
        attentionBuffer = new CircularBuffer(attentionBufferSize); // Reinitialize with the new size
    }

    /**
     * Sets the buffer size for smoothing Meditation values.
     * @param size The buffer size (must be between 2 and 32).
     */
    public void setMeditationBufferSize(int size) {
        if (size < 2 || size > 32) {
            throw new IllegalArgumentException("Buffer size must be between 2 and 32.");
        }
        meditationBufferSize = size;
        meditationBuffer = new CircularBuffer(meditationBufferSize); // Reinitialize with the new size
    }

    /**
     * Smoothes the given Attention value using the circular buffer.
     * @param newValue The new Attention value to add to the buffer.
     * @return The smoothed Attention value.
     */
    public float smoothAttentionValue(float newValue) {
        attentionBuffer.add(newValue);
        float smoothed = attentionBuffer.getAverage();
        return smoothed;
    }

    /**
     * Smoothes the given Meditation value using the circular buffer.
     * @param newValue The new Meditation value to add to the buffer.
     * @return The smoothed Meditation value.
     */
    public float smoothMeditationValue(float newValue) {
        meditationBuffer.add(newValue);
        return meditationBuffer.getAverage();
    }

    // Circular buffer class
    private static class CircularBuffer {
        private float[] buffer;
        private int index;
        private int size;
        private int count;

        public CircularBuffer(int size) {
            this.size = size;
            this.buffer = new float[size];
            this.index = 0;
            this.count = 0;
        }

        public void add(float value) {
            buffer[index] = value;
            index = (index + 1) % size;
            if (count < size) {
                count++;
            }
        }

        public float getAverage() {
            float sum = 0;
            for (int i = 0; i < count; i++) {
                sum += buffer[i];
            }
            return sum / count;
        }
    }



    /* For more information about Interpolation check this article:
       https://medium.com/yellowme/custom-ease-interpolator-for-meaningful-motion-in-android-4f6503398b89
     */

    /**
     * Set the control points for Attention interpolation.
     * @param xValues The x-values for the control points (length must be 5).
     * @param yValues The y-values for the control points (length must be 5).
     */
    public void setAttentionControlPoints(float[] xValues, float[] yValues) {
        if (xValues.length != 5 || yValues.length != 5) {
            throw new IllegalArgumentException("The length of xValues and yValues must be 5.");
        }
        System.arraycopy(xValues, 0, attentionControlValuesX, 0, 5);
        System.arraycopy(yValues, 0, attentionControlValuesY, 0, 5);
    }

    /**
     * Set the control points for Meditation interpolation.
     * @param xValues The x-values for the control points (length must be 5).
     * @param yValues The y-values for the control points (length must be 5).
     */
    public void setMeditationControlPoints(float[] xValues, float[] yValues) {
        if (xValues.length != 5 || yValues.length != 5) {
            throw new IllegalArgumentException("The length of xValues and yValues must be 5.");
        }
        System.arraycopy(xValues, 0, meditationControlValuesX, 0, 5);
        System.arraycopy(yValues, 0, meditationControlValuesY, 0, 5);
    }

    /**
     * Interpolates a value for Attention using Cubic Bezier interpolation and five control points.
     * @param x The x-value for which to interpolate.
     * @return The interpolated y-value for Attention.
     */
    public float interpolateAttention(float x) {
        return interpolate(x, attentionControlValuesX, attentionControlValuesY);
    }

    /**
     * Interpolates a value for Meditation using Cubic Bezier interpolation and five control points.
     * @param x The x-value for which to interpolate.
     * @return The interpolated y-value for Meditation.
     */
    public float interpolateMeditation(float x) {
        return interpolate(x, meditationControlValuesX, meditationControlValuesY);
    }

    /**
     * Cubic Bezier interpolation using five control points.
     * @param t The interpolation factor (0 to 1).
     * @param p0 The first control point.
     * @param p1 The second control point.
     * @param p2 The third control point.
     * @param p3 The fourth control point.
     * @param p4 The fifth control point.
     * @return The interpolated value.
     */
    private float cubicBezierInterpolation(float t, float p0, float p1, float p2, float p3, float p4) {
        float u = 1 - t;
        return u * u * u * u * p0 +
                4 * u * u * u * t * p1 +
                6 * u * u * t * t * p2 +
                4 * u * t * t * t * p3 +
                t * t * t * t * p4;
    }

    /**
     * Interpolates between five y-values given an x-value.
     * @param x The x-value for which to interpolate (should be between the first and last x-values).
     * @param xValues The array of x-values (control points).
     * @param yValues The array of y-values (control points).
     * @return The interpolated y-value.
     */
    public float interpolate(float x, float[] xValues, float[] yValues) {
        if (xValues.length != 5 || yValues.length != 5) {
            throw new IllegalArgumentException("The length of xValues and yValues must be 5.");
        }

        // Normalize x between 0 and 1
        float t = (x - xValues[0]) / (xValues[4] - xValues[0]);

        // Perform cubic Bezier interpolation
        return cubicBezierInterpolation(t, yValues[0], yValues[1], yValues[2], yValues[3], yValues[4]);
    }


    /**
     * Check if the attention trigger condition is met.
     * @param currentAttention The current attention value.
     * @param triggerValue The attention value that must be reached or exceeded.
     * @param strength The number of consecutive calls required to trigger.
     * @return true if the attention trigger condition is met, false otherwise.
     */
    public boolean checkAttentionTrigger(int currentAttention, int triggerValue, int strength) {
        if (currentAttention >= triggerValue) {
            if (attentionTriggerCounter < strength) {
                attentionTriggerCounter++;
            }
            attentionTriggerDecrementCounter = strength; // Reset decrement counter
        } else {
            if (attentionTriggerDecrementCounter > 0) {
                attentionTriggerDecrementCounter--;
            } else if (attentionTriggerCounter > 0) {
                attentionTriggerCounter--; // Decrease the counter when value drops below trigger
            }
        }

        // Trigger achieved if the counter is equal or greater than strength
        return attentionTriggerCounter >= strength;
    }

    /**
     * Check if the meditation trigger condition is met.
     * @param currentMeditation The current meditation value.
     * @param triggerValue The meditation value that must be reached or exceeded.
     * @param strength The number of consecutive calls required to trigger.
     * @return true if the meditation trigger condition is met, false otherwise.
     */
    public boolean checkMeditationTrigger(int currentMeditation, int triggerValue, int strength) {
        if (currentMeditation >= triggerValue) {
            if (meditationTriggerCounter < strength) {
                meditationTriggerCounter++;
            }
            meditationTriggerDecrementCounter = strength; // Reset decrement counter
        } else {
            if (meditationTriggerDecrementCounter > 0) {
                meditationTriggerDecrementCounter--;
            } else if (meditationTriggerCounter > 0) {
                meditationTriggerCounter--; // Decrease the counter when value drops below trigger
            }
        }

        // Trigger achieved if the counter is equal or greater than strength
        return meditationTriggerCounter >= strength;
    }
}
