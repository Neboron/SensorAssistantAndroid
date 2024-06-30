package com.example.aitavrd;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.pwittchen.neurosky.library.message.enums.BrainWave;
import com.mohammedalaa.seekbar.DoubleValueSeekBarView;
import com.mohammedalaa.seekbar.OnDoubleValueSeekBarChangeListener;
import com.mohammedalaa.seekbar.OnRangeSeekBarChangeListener;
import com.mohammedalaa.seekbar.RangeSeekBarView;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NeuroSkyDetailsActivity extends AppCompatActivity
        implements BluetoothNeuroSky.RawDataListener, BluetoothNeuroSky.BrainWavesDataListener,
        BluetoothNeuroSky.EyeBlinkDataListener, BluetoothNeuroSky.AttentionDataListener, BluetoothNeuroSky.MeditationDataListener {


    public static final String PREFS_NAME = "aitaPrefsDroid";
    public static final String BLINK_TRIGGER_INTENSITY_MIN_KEY = "blinkTriggerIntensityMin";
    public static final String BLINK_TRIGGER_INTENSITY_MAX_KEY = "blinkTriggerIntensityMax";
    public static final String BLINK_RELEASE_TIME_KEY = "blinkReleaseTime";

    private ImageButton returnButton;
    private ImageView connectionStatusIcon;
    private XYPlot rawPlot;
    private XYPlot recognitionPlot;
    private SimpleXYSeries seriesRaw;
    private SimpleXYSeries seriesAttention;
    private SimpleXYSeries seriesMeditation;
    private RadarChart brainWavesRadarChart;
    private final Handler handler = new Handler();
    private static final int X_RANGE_RAW = 600;  // Number of data points displayed in the plot
    private static final int X_RANGE_AM = 100;  // Number of data points displayed in the recognition plot (Attention, Meditation)
    private BluetoothNeuroSky bluetoothNeuroSky;
    private boolean isRawPlotVisible = false;
    private LinkedList<RadarDataSet> brainWavesRadarDataSets = new LinkedList<>();
    private TextView lowAlphaVariable, highAlphaVariable, lowBetaVariable, highBetaVariable;
    private TextView lowGamaVariable, midGamaVariable, deltaVariable, thetaVariable;
    private TextView blinkCountVariable, blinkIntensityVariable;


    private DoubleValueSeekBarView blinkRangeSeekBar;
    private RangeSeekBarView blinkReleaseTimeSeekBar;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neurosky_details);

        // Get the actual device name and address from the intent
        String deviceName = getIntent().getStringExtra("DEVICE_NAME");
        String deviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");

        // Set the device name and address to the TextViews
        TextView deviceNameTextView      = findViewById(R.id.deviceName);
        TextView deviceAddressTextView   = findViewById(R.id.deviceAddress);
        connectionStatusIcon             = findViewById(R.id.connectionStatusIcon);
        rawPlot                          = findViewById(R.id.rawPlot);
        lowAlphaVariable                 = findViewById(R.id.lowAlphaVariable);
        highAlphaVariable                = findViewById(R.id.highAlphaVariable);
        lowBetaVariable                  = findViewById(R.id.lowBetaVariable);
        highBetaVariable                 = findViewById(R.id.highBetaVariable);
        lowGamaVariable                  = findViewById(R.id.lowGamaVariable);
        midGamaVariable                  = findViewById(R.id.midGamaVariable);
        deltaVariable                    = findViewById(R.id.deltaVariable);
        thetaVariable                    = findViewById(R.id.thetaVariable);
        blinkCountVariable               = findViewById(R.id.blinkCountVariable);
        blinkIntensityVariable           = findViewById(R.id.blinkIntensityVariable);
        recognitionPlot                  = findViewById(R.id.recognitionPlot);
        brainWavesRadarChart             = findViewById(R.id.brainWavesRadarChart);
        blinkRangeSeekBar                = findViewById(R.id.blinkRangeSeekBar);
        blinkReleaseTimeSeekBar          = findViewById(R.id.blinkReleaseTimeSeekBar);



        // Load saved state
        SharedPreferences preferences        = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedBlinkTriggerIntensityMin    = preferences.getInt(BLINK_TRIGGER_INTENSITY_MIN_KEY, 40);
        int savedBlinkTriggerIntensityMax    = preferences.getInt(BLINK_TRIGGER_INTENSITY_MAX_KEY, 80);
        int savedBlinkReleaseTime            = preferences.getInt(BLINK_RELEASE_TIME_KEY, 10);


        blinkRangeSeekBar.setCurrentMinValue(savedBlinkTriggerIntensityMin);
        blinkRangeSeekBar.setCurrentMaxValue(savedBlinkTriggerIntensityMax);
        blinkReleaseTimeSeekBar.setCurrentValue(Math.round(savedBlinkReleaseTime/10));


        deviceNameTextView.setText(deviceName);
        deviceAddressTextView.setText(deviceAddress);


        // Set up the plots
        configureRawPlot(rawPlot, "Raw Data");
        configureRecognitionPlot(recognitionPlot, "Algo Plot");
        configureBrainWavesRadarChart(brainWavesRadarChart);

        // Initialize the series
        seriesRaw = new SimpleXYSeries("Raw Data");
        seriesAttention = new SimpleXYSeries("Attention");
        seriesMeditation = new SimpleXYSeries("Meditation");
        seriesRaw.useImplicitXVals();
        seriesAttention.useImplicitXVals();
        seriesMeditation.useImplicitXVals();

        // Set up the series format
        LineAndPointFormatter seriesRawFormat = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf1);
        LineAndPointFormatter seriesAttentionFormat = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf3);
        LineAndPointFormatter seriesMeditationFormat = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf2);

        // Add the series to the plots
        rawPlot.addSeries(seriesRaw, seriesRawFormat);
        recognitionPlot.addSeries(seriesAttention, seriesAttentionFormat);
        recognitionPlot.addSeries(seriesMeditation, seriesMeditationFormat);

        // Set up the dropdown layout
        LinearLayout rawDropdownLayout = findViewById(R.id.rawPlotDropdownLayout);
        ImageView dropdownIcon = findViewById(R.id.rawPlotDropdownIcon);

        rawDropdownLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRawPlotVisible) {
                    bluetoothNeuroSky.feedRawSignal(false);
                    rawPlot.setVisibility(View.GONE);
                    dropdownIcon.setRotation(0);
                } else {
                    bluetoothNeuroSky.feedRawSignal(true);
                    rawPlot.setVisibility(View.VISIBLE);
                    dropdownIcon.setRotation(180);
                }
                isRawPlotVisible = !isRawPlotVisible;
            }
        });

        returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Initialize BluetoothNeuroSky and set the listeners
        bluetoothNeuroSky = BluetoothNeuroSky.getInstance(this);
        bluetoothNeuroSky.setRawDataListener(this);
        bluetoothNeuroSky.setBrainWavesDataListener(this);
        bluetoothNeuroSky.setEyeBlinkDataListener(this);
        bluetoothNeuroSky.setAttentionDataListener(this);
        bluetoothNeuroSky.setMeditationDataListener(this);


        Handler checkConnectionHandler = new Handler(Looper.getMainLooper());
        Runnable checkConnectionRunnable = new Runnable() {
            @Override
            public void run() {
                if (bluetoothNeuroSky.getConnectionStatus()) {
                    connectionStatusIcon.setImageResource(R.drawable.ic_ble_connected);
                } else {
                    connectionStatusIcon.setImageResource(R.drawable.ic_ble_disconnected);
                }
                checkConnectionHandler.postDelayed(this, 1000);
            }
        };


        blinkRangeSeekBar.setOnRangeSeekBarViewChangeListener(new OnDoubleValueSeekBarChangeListener() {
            @Override
            public void onValueChanged(@Nullable DoubleValueSeekBarView seekBar, int min, int max, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(@Nullable DoubleValueSeekBarView seekBar, int min, int max) {

            }

            @Override
            public void onStopTrackingTouch(@Nullable DoubleValueSeekBarView seekBar, int min, int max) {
                SharedPreferences.Editor editor = preferences.edit();
                SharedPreferences preferences   = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                editor.putInt(BLINK_TRIGGER_INTENSITY_MIN_KEY, min);
                editor.putInt(BLINK_TRIGGER_INTENSITY_MAX_KEY, max);
                editor.apply();
                bluetoothNeuroSky.setEyeBlinkFilterParams(min, max, preferences.getInt(BLINK_RELEASE_TIME_KEY, 100)+20);
            }
        });

        blinkReleaseTimeSeekBar.setOnRangeSeekBarViewChangeListener(new OnRangeSeekBarChangeListener() {
            @Override
            public void onProgressChanged(@Nullable RangeSeekBarView seekBar, int time, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(@Nullable RangeSeekBarView seekBar, int time) {

            }

            @Override
            public void onStopTrackingTouch(@Nullable RangeSeekBarView seekBar, int time) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(BLINK_RELEASE_TIME_KEY, (time*10));
                editor.apply();
                bluetoothNeuroSky.setEyeBlinkFilterParams(preferences.getInt(BLINK_TRIGGER_INTENSITY_MIN_KEY, 0),
                                                          preferences.getInt(BLINK_TRIGGER_INTENSITY_MAX_KEY, 100), (time*10)+20);
            }
        });



        checkConnectionHandler.postDelayed(checkConnectionRunnable, 1000);

    }

    @Override
    public void onRawDataReceived(short[] rawData) {
        runOnUiThread(() -> {
            // Calculate the average value of the incoming raw data
            int sum = 0;
            for (short data : rawData) {
                sum += data;
            }
            float average = (float) sum / rawData.length;

            // Add the approximated value (average) to the series
            if (seriesRaw.size() >= X_RANGE_RAW) {
                seriesRaw.removeFirst();
            }
            seriesRaw.addLast(null, average);

            // Redraw the plot to update it with the new data point
            rawPlot.redraw();
        });
    }

    @Override
    public void onBrainWavesDataReceived(Map<BrainWave, Integer> brainWavesData) {
        runOnUiThread(() -> {
            List<RadarEntry> entries = new ArrayList<>();
            for (BrainWave brainWave : BrainWave.values()) {
                int value = brainWavesData.getOrDefault(brainWave, 1); // Ensure no zero values for log transformation
                entries.add(new RadarEntry((float) Math.log10(value)));
            }

            RadarDataSet newDataSet = new RadarDataSet(entries, "Brain Waves Data");
            newDataSet.setColor(Color.GREEN);
            newDataSet.setFillColor(Color.GREEN);
            newDataSet.setDrawFilled(true);
            newDataSet.setDrawValues(false);
            brainWavesRadarChart.setWebAlpha(80); //TODO: Find better way to save transparency of the grid
            newDataSet.setFillAlpha(120);


            // Add new dataset to the list
            brainWavesRadarDataSets.addFirst(newDataSet);

            // Update alpha for existing datasets
            for (int i = 0; i < brainWavesRadarDataSets.size(); i++) {
                RadarDataSet dataSet = brainWavesRadarDataSets.get(i);
                int alpha = 90 - (i * 30);  // Decrease alpha by 60 for each older dataset
                if (alpha <= 0) {
                    brainWavesRadarDataSets.removeLast();
                } else {
                    dataSet.setFillAlpha(alpha);
                    dataSet.setColor(Color.GREEN, alpha+120);
                }
            }

            // Create RadarData object and add all datasets
            RadarData data = new RadarData();
            for (RadarDataSet dataSet : brainWavesRadarDataSets) {
                data.addDataSet(dataSet);
            }

            brainWavesRadarChart.setData(data);
            brainWavesRadarChart.invalidate(); // Refresh the chart

            // Update TextView elements
            lowAlphaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.LOW_ALPHA)));
            highAlphaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.HIGH_ALPHA)));
            lowBetaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.LOW_BETA)));
            highBetaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.HIGH_BETA)));
            lowGamaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.LOW_GAMMA)));
            midGamaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.MID_GAMMA)));
            deltaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.DELTA)));
            thetaVariable.setText(String.valueOf(brainWavesData.get(BrainWave.THETA)));
        });
    }

    @Override
    public void onEyeBlinkDataReceived(int blinkCount, int blinkIntensity) {
        runOnUiThread(() -> {
            blinkCountVariable.setText(String.valueOf(blinkCount));
            blinkIntensityVariable.setText(String.valueOf(blinkIntensity));
        });
    }

    @Override
    public void onAttentionDataReceived(int value) {
        runOnUiThread(() -> {
            // Add the value to the attention series
            if (seriesAttention.size() >= X_RANGE_AM) {
                seriesAttention.removeFirst();
            }
            seriesAttention.addLast(null, value);

            // Redraw the plot to update it with the new data point
            recognitionPlot.redraw();
        });
    }

    @Override
    public void onMeditationDataReceived(int value) {
        runOnUiThread(() -> {
            // Add the value to the meditation series
            if (seriesMeditation.size() >= X_RANGE_AM) {
                seriesMeditation.removeFirst();
            }
            seriesMeditation.addLast(null, value);

            // Redraw the plot to update it with the new data point
            recognitionPlot.redraw();
        });
    }

    private void configureRawPlot(XYPlot plot, String title) {
        plot.setTitle(title);
        plot.setRangeBoundaries(-2100, 2100, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, X_RANGE_RAW, BoundaryMode.FIXED);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(((Number) obj).intValue());
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
    }

    private void configureRecognitionPlot(XYPlot plot, String title) {
        plot.setTitle(title);
        plot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, X_RANGE_AM, BoundaryMode.FIXED);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(((Number) obj).intValue());
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
    }

    private void configureBrainWavesRadarChart(RadarChart radarChart) {
        radarChart.getDescription().setEnabled(false);
        radarChart.getLegend().setEnabled(false);
        radarChart.setRotationEnabled(false);
        radarChart.setWebLineWidth(1f);
        radarChart.setWebLineWidthInner(1f);
        radarChart.setWebColor(ContextCompat.getColor(this, R.color.aperture_term_main_st));
        radarChart.setWebLineWidthInner(1f);
        radarChart.setWebColorInner(ContextCompat.getColor(this, R.color.aperture_term_main_st));
        radarChart.setWebAlpha(200);

        XAxis xAxis = radarChart.getXAxis();
        xAxis.setTextSize(9f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getBrainWavesLabels()));
        xAxis.setTextColor(Color.WHITE);

        YAxis yAxis = radarChart.getYAxis();
        yAxis.setLabelCount(6, true);
        yAxis.setTextSize(9f);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(7.4f);
        yAxis.setGranularity(1f);
        yAxis.setGranularityEnabled(true);
        yAxis.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main));
        yAxis.setDrawLabels(true);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return String.format("%.0f", Math.pow(10, value));
            }
        });
    }

    private List<String> getBrainWavesLabels() {
        List<String> labels = new ArrayList<>();
        for (BrainWave brainWave : BrainWave.values()) {
            String label = brainWave.name()
                    .replace("ALPHA", "α")
                    .replace("BETA", "β")
                    .replace("GAMMA", "γ")
                    .replace("DELTA", "δ")
                    .replace("THETA", "θ")
                    .replace("LOW", "low")
                    .replace("MID", "mid")
                    .replace("HIGH", "high")
                    .replace("_", " ") + " ";
            labels.add(label);
        }
        return labels;
    }

    @Override
    protected void onDestroy() {
        bluetoothNeuroSky.feedRawSignal(false);
        super.onDestroy();
        bluetoothNeuroSky.disconnect();
    }
}
