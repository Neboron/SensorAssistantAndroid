package com.example.aitavrd;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XValueMarker;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mohammedalaa.seekbar.DoubleValueSeekBarView;
import com.mohammedalaa.seekbar.OnDoubleValueSeekBarChangeListener;
import com.mohammedalaa.seekbar.OnRangeSeekBarChangeListener;
import com.mohammedalaa.seekbar.RangeSeekBarView;

import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NeuroSkyDetailsActivity extends AppCompatActivity
        implements BluetoothNeuroSky.RawDataListener, BluetoothNeuroSky.BrainWavesDataListener,
        BluetoothNeuroSky.EyeBlinkDataListener, BluetoothNeuroSky.AttentionDataListener, BluetoothNeuroSky.MeditationDataListener {


    public static final String PREFS_NAME = "aitaPrefsDroid";
    public static final String SETTINGS_UPDATE_FLAG = "NeuroSkySettingsUpdateFlag";
    public static final String BLINK_TRIGGER_INTENSITY_MIN_KEY = "blinkTriggerIntensityMin";
    public static final String BLINK_TRIGGER_INTENSITY_MAX_KEY = "blinkTriggerIntensityMax";
    public static final String BLINK_RELEASE_TIME_KEY = "blinkReleaseTime";
    public static final String BLINK_USE_INTEGERS_KEY = "blinkUseIntegers";
    public static final String BLINK_USE_INTEGERS_TRUE_KEY = "blinkUseIntegersTrue";
    public static final String BLINK_USE_INTEGERS_FALSE_KEY = "blinkUseIntegersFalse";
    public static final String ATTENTION_FILTER_KEY = "attentionFilter";
    public static final String MEDITATION_FILTER_KEY = "meditationFilter";
    public static final String ATTENTION_INTERPOLATION_POINT_KEY = "attention_interpolation_point_";
    public static final String MEDITATION_INTERPOLATION_POINT_KEY = "meditation_interpolation_point_";
    public static final String ATTENTION_TRIGGER_ENABLE_KEY = "attentionTriggerEnable";
    public static final String ATTENTION_TRIGGER_VALUE_KEY = "attentionTriggerValue";
    public static final String ATTENTION_TRIGGER_EMOTION_ID_KEY = "attentionTriggerEmotionId";
    public static final String ATTENTION_TRIGGER_STRENGTH_KEY = "attentionTriggerStrength";
    public static final String MEDITATION_TRIGGER_ENABLE_KEY = "meditationTriggerEnable";
    public static final String MEDITATION_TRIGGER_VALUE_KEY = "meditationTriggerValue";
    public static final String MEDITATION_TRIGGER_EMOTION_ID_KEY = "meditationTriggerEmotionId";
    public static final String MEDITATION_TRIGGER_STRENGTH_KEY = "meditationTriggerStrength";

    private ImageButton returnButton;
    private ImageView connectionStatusIcon;
    private XYPlot rawPlot;
    private XYPlot recognitionPlot;
    private SimpleXYSeries seriesRaw;
    private SimpleXYSeries seriesAttention;
    private SimpleXYSeries seriesMeditation;
    private RadarChart brainWavesRadarChart;
    private TextView lowAlphaVariable, highAlphaVariable, lowBetaVariable, highBetaVariable;
    private TextView lowGamaVariable, midGamaVariable, deltaVariable, thetaVariable;
    private TextView blinkCountVariable, blinkIntensityVariable;
    private DoubleValueSeekBarView blinkRangeSeekBar;
    private RangeSeekBarView blinkReleaseTimeSeekBar;
    private SwitchMaterial blinkUseIntegersSwitch;
    private EditText blinkUseIntegersTrueEditText;
    private EditText blinkUseIntegersFalseEditText;
    private Spinner attentionFilterDropdownSpinner;
    private Spinner meditationFilterDropdownSpinner;
    private ImageView interpolationDropdownIcon;
    private LinearLayout interpolationDropdownLayout;
    private XYPlot attentionInterpolationPlot;
    private XYPlot meditationInterpolationPlot;
    private LinearLayout triggerSettingsDropdownLayout;
    private LinearLayout triggerSettingsLayout;
    private ImageView triggerSettingsDropdownIcon;
    private SwitchMaterial attentionTriggerSwitch;
    private NumberPicker attentionTriggerValuePicker;
    private NumberPicker attentionTriggerEmotionIdPicker;
    private NumberPicker attentionTriggerStrengthPicker;
    private SwitchMaterial meditationTriggerSwitch;
    private NumberPicker meditationTriggerValuePicker;
    private NumberPicker meditationTriggerEmotionIdPicker;
    private NumberPicker meditationTriggerStrengthPicker;

    private final Handler handler = new Handler();
    private static final int X_RANGE_RAW = 600;  // Number of data points displayed in the plot
    private static final int X_RANGE_AM = 100;  // Number of data points displayed in the recognition plot (Attention, Meditation)
    private BluetoothNeuroSky bluetoothNeuroSky;
    private boolean isRawPlotVisible = false;
    private boolean isInterpolationPlotsVisible = false;
    private LinkedList<RadarDataSet> brainWavesRadarDataSets = new LinkedList<>();

    // Variables for attention interpolation
    private boolean attentionPointBeingMoved = false; // Flag to track if a point is being moved
    private int attentionMovingPointIndex = -1;  // Tracks which point is being moved
    public static final int ATTENTION_FIXED_X_POINTS = 5;  // Number of fixed x points
    private Number[] attentionFixedValuesX = {1, 25, 50, 75, 99};  // Fixed x values
    private Number[] attentionPointValues = {0, 25, 50, 75, 100};  // Initial y values
    private SimpleXYSeries attentionMovableSeries;  // Series for movable points
    private XValueMarker attentionMarker;

    // Variables for meditation interpolation
    private boolean meditationPointBeingMoved = false; // Flag to track if a point is being moved
    private int meditationMovingPointIndex = -1;  // Tracks which point is being moved
    public static final int MEDITATION_FIXED_X_POINTS = 5;  // Number of fixed x points for meditation
    private Number[] meditationFixedValuesX = {1, 25, 50, 75, 99};  // Fixed x values for meditation
    private Number[] meditationPointValues = {0, 25, 50, 75, 100};  // Initial y values for meditation
    private SimpleXYSeries meditationMovableSeries;  // Series for movable meditation points
    private XValueMarker meditationMarker;




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
        blinkUseIntegersSwitch           = findViewById(R.id.blinkUseIntegersSwitch);
        blinkUseIntegersTrueEditText     = findViewById(R.id.blinkUseIntegersTrueEditText);
        blinkUseIntegersFalseEditText    = findViewById(R.id.blinkUseIntegersFalseEditText);
        attentionFilterDropdownSpinner   = findViewById(R.id.attentionFilterDropdownSpinner);
        meditationFilterDropdownSpinner  = findViewById(R.id.meditationFilterDropdownSpinner);
        interpolationDropdownIcon        = findViewById(R.id.interpolationPlotDropdownIcon);
        interpolationDropdownLayout      = findViewById(R.id.interpolationPlotDropdownLayout);
        attentionInterpolationPlot       = findViewById(R.id.attentionInterpolationPlot);
        meditationInterpolationPlot      = findViewById(R.id.meditationInterpolationPlot);
        triggerSettingsDropdownLayout    = findViewById(R.id.triggerSettingsDropdownLayout);
        triggerSettingsLayout            = findViewById(R.id.triggerSettingsLayout);
        triggerSettingsDropdownIcon      = findViewById(R.id.triggerSettingsDropdownIcon);
        attentionTriggerSwitch           = findViewById(R.id.attentionTriggerSwitch);
        attentionTriggerValuePicker      = findViewById(R.id.attentionTriggerValuePicker);
        attentionTriggerEmotionIdPicker  = findViewById(R.id.attentionTriggerEmotionIdPicker);
        attentionTriggerStrengthPicker   = findViewById(R.id.attentionTriggerStrengthPicker);
        meditationTriggerSwitch          = findViewById(R.id.meditationTriggerSwitch);
        meditationTriggerValuePicker     = findViewById(R.id.meditationTriggerValuePicker);
        meditationTriggerEmotionIdPicker = findViewById(R.id.meditationTriggerEmotionIdPicker);
        meditationTriggerStrengthPicker  = findViewById(R.id.meditationTriggerStrengthPicker);





        // Load saved state
        SharedPreferences preferences        = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Boolean settingsUpdateFlag           = preferences.getBoolean(SETTINGS_UPDATE_FLAG, false);
        int savedBlinkTriggerIntensityMin    = preferences.getInt(BLINK_TRIGGER_INTENSITY_MIN_KEY, 40);
        int savedBlinkTriggerIntensityMax    = preferences.getInt(BLINK_TRIGGER_INTENSITY_MAX_KEY, 80);
        int savedBlinkReleaseTime            = preferences.getInt(BLINK_RELEASE_TIME_KEY, 10);
        boolean savedBlinkUseIntegers        = preferences.getBoolean(BLINK_USE_INTEGERS_KEY, false);
        int savedBlinkUseIntegersTrue        = preferences.getInt(BLINK_USE_INTEGERS_TRUE_KEY, 1);
        int savedBlinkUseIntegersFalse       = preferences.getInt(BLINK_USE_INTEGERS_FALSE_KEY, 0);
        String savedAttentionFilter          = preferences.getString(ATTENTION_FILTER_KEY, "DISABLE");
        String savedMeditationFilter         = preferences.getString(MEDITATION_FILTER_KEY, "DISABLE");
        boolean savedAttentionTrigger        = preferences.getBoolean(ATTENTION_TRIGGER_ENABLE_KEY, false);
        int savedAttentionTriggerValue       = preferences.getInt(ATTENTION_TRIGGER_VALUE_KEY, 90);
        int savedAttentionTriggerEmotionId   = preferences.getInt(ATTENTION_TRIGGER_EMOTION_ID_KEY, 0);
        int savedAttentionTriggerStrength    = preferences.getInt(ATTENTION_TRIGGER_STRENGTH_KEY, 2);
        boolean savedMeditationTrigger       = preferences.getBoolean(MEDITATION_TRIGGER_ENABLE_KEY, false);
        int savedMeditationTriggerValue      = preferences.getInt(MEDITATION_TRIGGER_VALUE_KEY, 90);
        int savedMeditationTriggerEmotionId  = preferences.getInt(MEDITATION_TRIGGER_EMOTION_ID_KEY, 0);
        int savedMeditationTriggerStrength   = preferences.getInt(MEDITATION_TRIGGER_STRENGTH_KEY, 2);

        for (int i = 0; i < ATTENTION_FIXED_X_POINTS; i++) {
            float savedValue = preferences.getFloat(ATTENTION_INTERPOLATION_POINT_KEY + i, attentionPointValues[i].floatValue());
            attentionPointValues[i] = savedValue;
        }

        for (int i = 0; i < MEDITATION_FIXED_X_POINTS; i++) {
            float savedValue = preferences.getFloat(MEDITATION_INTERPOLATION_POINT_KEY + i, meditationPointValues[i].floatValue());
            meditationPointValues[i] = savedValue;
        }



        blinkRangeSeekBar.setCurrentMinValue(savedBlinkTriggerIntensityMin);
        blinkRangeSeekBar.setCurrentMaxValue(savedBlinkTriggerIntensityMax);
        blinkReleaseTimeSeekBar.setCurrentValue(savedBlinkReleaseTime/10);
        blinkUseIntegersSwitch.setChecked(savedBlinkUseIntegers);
        blinkUseIntegersTrueEditText.setText(String.valueOf(savedBlinkUseIntegersTrue));
        blinkUseIntegersFalseEditText.setText(String.valueOf(savedBlinkUseIntegersFalse));

        // Set up Spinners
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.neurosky_filter_items, R.layout.spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        attentionFilterDropdownSpinner.setAdapter(adapter);
        meditationFilterDropdownSpinner.setAdapter(adapter);

        // Set initial Spinner selection
        setSpinnerSelection(attentionFilterDropdownSpinner, savedAttentionFilter);
        setSpinnerSelection(meditationFilterDropdownSpinner, savedMeditationFilter);

        // Set initial track color
        updateTrackColor(blinkUseIntegersSwitch, savedBlinkUseIntegers);

        // Set initial integers color
        if (savedBlinkUseIntegers) {
            blinkUseIntegersTrueEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main));
            blinkUseIntegersFalseEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main));
        } else {
            blinkUseIntegersTrueEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main_dark));
            blinkUseIntegersFalseEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main_dark));
        }


        deviceNameTextView.setText(deviceName);
        deviceAddressTextView.setText(deviceAddress);


        // Set up the plots
        configureRawPlot(rawPlot, "Raw Data");
        configureRecognitionPlot(recognitionPlot, "Algo Plot");
        configureAttentionInterpolationPlot(attentionInterpolationPlot, "Attention Interpolation");
        configureMeditationInterpolationPlot(meditationInterpolationPlot, "Meditation Interpolation");
        configureBrainWavesRadarChart(brainWavesRadarChart);


        // Initialize the series
        seriesRaw = new SimpleXYSeries("Raw Data");
        seriesAttention = new SimpleXYSeries("Attention");
        seriesMeditation = new SimpleXYSeries("Meditation");
        attentionMovableSeries = new SimpleXYSeries(Arrays.asList(attentionFixedValuesX), Arrays.asList(attentionPointValues), "Attention Points");
        meditationMovableSeries = new SimpleXYSeries(Arrays.asList(meditationFixedValuesX), Arrays.asList(meditationPointValues), "Meditation Points");
        seriesRaw.useImplicitXVals();
        seriesAttention.useImplicitXVals();
        seriesMeditation.useImplicitXVals();

        // Set up the series format
        LineAndPointFormatter seriesRawFormat = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf1);
        LineAndPointFormatter seriesAttentionFormat = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf3);
        LineAndPointFormatter seriesMeditationFormat = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf2);
        LineAndPointFormatter attentionMovableSeriesFormatter = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf5);
        LineAndPointFormatter meditationMovableSeriesFormatter = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_plf5);


        // Configure interpolation on the formatter for Interpolation Curves
        attentionMovableSeriesFormatter.setInterpolationParams(
                new CatmullRomInterpolator.Params(20, CatmullRomInterpolator.Type.Uniform));
        meditationMovableSeriesFormatter.setInterpolationParams(
                new CatmullRomInterpolator.Params(20, CatmullRomInterpolator.Type.Uniform));

        // Add the series to the plots
        rawPlot.addSeries(seriesRaw, seriesRawFormat);
        recognitionPlot.addSeries(seriesAttention, seriesAttentionFormat);
        recognitionPlot.addSeries(seriesMeditation, seriesMeditationFormat);
        attentionInterpolationPlot.addSeries(attentionMovableSeries, attentionMovableSeriesFormatter);
        meditationInterpolationPlot.addSeries(meditationMovableSeries, meditationMovableSeriesFormatter);



        // Set up the dropdown layout
        LinearLayout rawDropdownLayout = findViewById(R.id.rawPlotDropdownLayout);
        ImageView dropdownIcon = findViewById(R.id.rawPlotDropdownIcon);


        attentionTriggerSwitch.setChecked(savedAttentionTrigger);
        // Set initial track color
        updateTrackColor(attentionTriggerSwitch, savedAttentionTrigger);

        meditationTriggerSwitch.setChecked(savedMeditationTrigger);
        // Set initial track color
        updateTrackColor(meditationTriggerSwitch, savedMeditationTrigger);

        // Configure the range of values for the pickers
        attentionTriggerValuePicker.setMinValue(50);
        attentionTriggerValuePicker.setMaxValue(100);
        attentionTriggerValuePicker.setValue(savedAttentionTriggerValue);

        attentionTriggerEmotionIdPicker.setMinValue(0);
        attentionTriggerEmotionIdPicker.setMaxValue(255);
        attentionTriggerEmotionIdPicker.setValue(savedAttentionTriggerEmotionId);

        attentionTriggerStrengthPicker.setMinValue(1);
        attentionTriggerStrengthPicker.setMaxValue(10);
        attentionTriggerStrengthPicker.setValue(savedAttentionTriggerStrength);

        meditationTriggerValuePicker.setMinValue(50);
        meditationTriggerValuePicker.setMaxValue(100);
        meditationTriggerValuePicker.setValue(savedMeditationTriggerValue);

        meditationTriggerEmotionIdPicker.setMinValue(0);
        meditationTriggerEmotionIdPicker.setMaxValue(255);
        meditationTriggerEmotionIdPicker.setValue(savedMeditationTriggerEmotionId);

        meditationTriggerStrengthPicker.setMinValue(1);
        meditationTriggerStrengthPicker.setMaxValue(10);
        meditationTriggerStrengthPicker.setValue(savedMeditationTriggerStrength);



        SharedPreferences.Editor editor = preferences.edit();

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
                SharedPreferences preferences   = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                editor.putInt(BLINK_TRIGGER_INTENSITY_MIN_KEY, min);
                editor.putInt(BLINK_TRIGGER_INTENSITY_MAX_KEY, max);
                editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
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

                editor.putInt(BLINK_RELEASE_TIME_KEY, (time*10));
                editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
                editor.apply();
                bluetoothNeuroSky.setEyeBlinkFilterParams(preferences.getInt(BLINK_TRIGGER_INTENSITY_MIN_KEY, 0),
                                                          preferences.getInt(BLINK_TRIGGER_INTENSITY_MAX_KEY, 100), (time*10)+20);
            }
        });

        blinkUseIntegersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state
            editor.putBoolean(BLINK_USE_INTEGERS_KEY, isChecked);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();

            // Update the track color
            updateTrackColor(blinkUseIntegersSwitch, isChecked);

            if (isChecked) {
                blinkUseIntegersTrueEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main));
                blinkUseIntegersFalseEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main));
            } else {
                blinkUseIntegersTrueEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main_dark));
                blinkUseIntegersFalseEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main_dark));
            }
        });

        blinkUseIntegersTrueEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int intValue = Integer.parseInt(blinkUseIntegersTrueEditText.getText().toString());
                    editor.putInt(BLINK_USE_INTEGERS_TRUE_KEY, intValue);
                } catch (NumberFormatException e) {
                    editor.putInt(BLINK_USE_INTEGERS_TRUE_KEY, 1);
                }
                editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
                editor.apply();
            }
        });

        blinkUseIntegersFalseEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int intValue = Integer.parseInt(blinkUseIntegersFalseEditText.getText().toString());
                    editor.putInt(BLINK_USE_INTEGERS_FALSE_KEY, intValue);
                } catch (NumberFormatException e) {
                    editor.putInt(BLINK_USE_INTEGERS_FALSE_KEY, 0);
                }
                editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
                editor.apply();
            }
        });

        attentionFilterDropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                editor.putString(ATTENTION_FILTER_KEY, selectedItem);
                editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        meditationFilterDropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                editor.putString(MEDITATION_FILTER_KEY, selectedItem);
                editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        interpolationDropdownLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInterpolationPlotsVisible) {
                    attentionInterpolationPlot.setVisibility(View.GONE);
                    meditationInterpolationPlot.setVisibility(View.GONE);
                    interpolationDropdownIcon.setRotation(0);
                } else {
                    attentionInterpolationPlot.setVisibility(View.VISIBLE);
                    meditationInterpolationPlot.setVisibility(View.VISIBLE);
                    interpolationDropdownIcon.setRotation(180);
                }
                isInterpolationPlotsVisible = !isInterpolationPlotsVisible;
            }
        });

        attentionInterpolationPlot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleAttentionTouch(event);
            }
        });

        meditationInterpolationPlot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleMeditationTouch(event);
            }
        });


        triggerSettingsDropdownLayout.setOnClickListener(new View.OnClickListener() {
            private boolean isTriggerSettingsVisible = false;

            @Override
            public void onClick(View v) {
                if (isTriggerSettingsVisible) {
                    triggerSettingsLayout.setVisibility(View.GONE);
                    triggerSettingsDropdownIcon.setRotation(0);
                } else {
                    triggerSettingsLayout.setVisibility(View.VISIBLE);
                    triggerSettingsDropdownIcon.setRotation(180);
                }
                isTriggerSettingsVisible = !isTriggerSettingsVisible;
            }
        });

        attentionTriggerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state
            editor.putBoolean(ATTENTION_TRIGGER_ENABLE_KEY, isChecked);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();

            // Update the track color
            updateTrackColor(attentionTriggerSwitch, isChecked);
        });

        attentionTriggerValuePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            editor.putInt(ATTENTION_TRIGGER_VALUE_KEY, newVal);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();
        });

        attentionTriggerEmotionIdPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            editor.putInt(ATTENTION_TRIGGER_EMOTION_ID_KEY, newVal);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();
        });

        attentionTriggerStrengthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            editor.putInt(ATTENTION_TRIGGER_STRENGTH_KEY, newVal);
            editor.apply();
        });

        meditationTriggerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state
            editor.putBoolean(MEDITATION_TRIGGER_ENABLE_KEY, isChecked);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();

            // Update the track color
            updateTrackColor(meditationTriggerSwitch, isChecked);
        });

        meditationTriggerValuePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            editor.putInt(MEDITATION_TRIGGER_VALUE_KEY, newVal);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();
        });

        meditationTriggerEmotionIdPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            editor.putInt(MEDITATION_TRIGGER_EMOTION_ID_KEY, newVal);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();
        });

        meditationTriggerStrengthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            editor.putInt(MEDITATION_TRIGGER_STRENGTH_KEY, newVal);
            editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
            editor.apply();
        });

        checkConnectionHandler.postDelayed(checkConnectionRunnable, 1000);

    }

    private void updateTrackColor(SwitchMaterial switchMat, boolean isChecked) {
        if (isChecked) {
            switchMat.setTrackTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_term_main_st)));
        } else {
            switchMat.setTrackTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.aperture_term_main_dark)));
        }
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
            // Update the attention marker position with the actual value
            if (isInterpolationPlotsVisible) {
                attentionMarker.setValue(value);
                attentionInterpolationPlot.redraw();
            }

            // Add the value to the attention series
            if (seriesAttention.size() >= X_RANGE_AM) {
                seriesAttention.removeFirst();
            }
            seriesAttention.addLast(null, value);

            // Redraw the plot to update it with the new data point and marker

        });
    }

    @Override
    public void onMeditationDataReceived(int value) {
        runOnUiThread(() -> {
            // Update the meditation marker position with the actual value
            if (isInterpolationPlotsVisible) {
                meditationMarker.setValue(value);
                meditationInterpolationPlot.redraw();
            }

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

    private void configureAttentionInterpolationPlot(XYPlot plot, String title) {
        plot.setTitle(title);
        plot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 100, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 10);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 20);

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(obj.toString());
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(obj.toString());
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

        XYGraphWidget.Edge bottomEdge = XYGraphWidget.Edge.BOTTOM;
        plot.getGraph().getLineLabelStyle(bottomEdge).getPaint().setTextSize(PixelUtils.spToPix(12));

        plot.getGraph().getGridBackgroundPaint().setColor(ContextCompat.getColor(this, R.color.aperture_term_background));
        plot.getGraph().getDomainGridLinePaint().setColor(ContextCompat.getColor(this, R.color.aperture_term_main_st));
        plot.getGraph().getRangeGridLinePaint().setColor(ContextCompat.getColor(this, R.color.aperture_term_main_st));
        plot.getGraph().getDomainGridLinePaint().setStrokeWidth(1f);
        plot.getGraph().getRangeGridLinePaint().setStrokeWidth(1f);

        // Initialize the attention marker
        attentionMarker = new XValueMarker(0, "");
        attentionMarker.setLinePaint(new Paint());
        attentionMarker.getLinePaint().setColor(Color.parseColor("#FF00D5")); // Line color
        attentionMarker.getLinePaint().setStrokeWidth(2f); // Line thickness
        attentionMarker.getTextPaint().setColor(Color.RED); // Text color
        attentionMarker.getTextPaint().setTextSize(PixelUtils.spToPix(12)); // Text size

        // Add the marker to the plot
        plot.addMarker(attentionMarker);
    }

    private void configureMeditationInterpolationPlot(XYPlot plot, String title) {
        plot.setTitle(title);
        plot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 100, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 10);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 20);

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(obj.toString());
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(obj.toString());
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

        plot.getGraph().getGridBackgroundPaint().setColor(ContextCompat.getColor(this, R.color.aperture_term_background));
        plot.getGraph().getDomainGridLinePaint().setColor(ContextCompat.getColor(this, R.color.aperture_term_main_st));
        plot.getGraph().getRangeGridLinePaint().setColor(ContextCompat.getColor(this, R.color.aperture_term_main_st));
        plot.getGraph().getDomainGridLinePaint().setStrokeWidth(1f);
        plot.getGraph().getRangeGridLinePaint().setStrokeWidth(1f);

        XYGraphWidget.Edge bottomEdge = XYGraphWidget.Edge.BOTTOM;
        plot.getGraph().getLineLabelStyle(bottomEdge).getPaint().setTextSize(PixelUtils.spToPix(12));

        // Initialize the meditation marker
        meditationMarker = new XValueMarker(0, "");
        meditationMarker.setLinePaint(new Paint());
        meditationMarker.getLinePaint().setColor(Color.parseColor("#17F5B6")); // Line color
        meditationMarker.getLinePaint().setStrokeWidth(2f); // Line thickness
        meditationMarker.getTextPaint().setColor(Color.RED); // Text color
        meditationMarker.getTextPaint().setTextSize(PixelUtils.spToPix(12)); // Text size

        // Add the marker to the plot
        plot.addMarker(meditationMarker);
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


    private boolean handleAttentionTouch(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if the touch is near any existing point
                attentionMovingPointIndex = findNearestAttentionPointIndex(touchX, touchY, 100);
                if (attentionMovingPointIndex != -1) {
                    // If a point is detected within the threshold, disallow parent view to intercept touch events
                    attentionPointBeingMoved = true;
                    attentionInterpolationPlot.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (attentionMovingPointIndex != -1) {
                    // Convert the touch y-coordinate to plot y-coordinate
                    Number newY = attentionInterpolationPlot.screenToSeriesY(touchY);

                    // Clamp the new y-value to be within 0 and 100
                    newY = Math.max(0, Math.min(100, newY.floatValue()));

                    // Update the series with the clamped y-value
                    attentionMovableSeries.setY(newY, attentionMovingPointIndex);
                    attentionInterpolationPlot.redraw();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                if (attentionMovingPointIndex != -1) {
                    // Save the updated values to shared preferences
                    for (int i = 0; i < ATTENTION_FIXED_X_POINTS; i++) {
                        editor.putFloat(ATTENTION_INTERPOLATION_POINT_KEY + i, attentionMovableSeries.getY(i).floatValue());
                    }
                    editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
                    editor.apply();
                }

                attentionMovingPointIndex = -1;
                attentionPointBeingMoved = false;
                attentionInterpolationPlot.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    private boolean handleMeditationTouch(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if the touch is near any existing point
                meditationMovingPointIndex = findNearestMeditationPointIndex(touchX, touchY, 100);
                if (meditationMovingPointIndex != -1) {
                    // If a point is detected within the threshold, disallow parent view to intercept touch events
                    meditationPointBeingMoved = true;
                    meditationInterpolationPlot.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (meditationMovingPointIndex != -1) {
                    // Convert the touch y-coordinate to plot y-coordinate
                    Number newY = meditationInterpolationPlot.screenToSeriesY(touchY);

                    // Clamp the new y-value to be within 0 and 100
                    newY = Math.max(0, Math.min(100, newY.floatValue()));

                    // Update the series with the clamped y-value
                    meditationMovableSeries.setY(newY, meditationMovingPointIndex);
                    meditationInterpolationPlot.redraw();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                if (meditationMovingPointIndex != -1) {
                    // Save the updated values to shared preferences
                    for (int i = 0; i < MEDITATION_FIXED_X_POINTS; i++) {
                        editor.putFloat(MEDITATION_INTERPOLATION_POINT_KEY + i, meditationMovableSeries.getY(i).floatValue());
                    }
                    editor.putBoolean(SETTINGS_UPDATE_FLAG, true);
                    editor.apply();
                }

                meditationMovingPointIndex = -1;
                meditationPointBeingMoved = false;
                meditationInterpolationPlot.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }


    private int findNearestAttentionPointIndex(float touchX, float touchY, double threshold) {
        double minDist = Double.MAX_VALUE;
        int nearestIndex = -1;

        for (int i = 0; i < ATTENTION_FIXED_X_POINTS; i++) {
            // Convert series coordinates to screen coordinates for each point
            float pointX = attentionInterpolationPlot.seriesToScreenX(attentionMovableSeries.getX(i));
            float pointY = attentionInterpolationPlot.seriesToScreenY(attentionMovableSeries.getY(i));

            // Invert Y coordinate system (Plot coordinates and display is inversed)
            float invertedTouchY = attentionInterpolationPlot.getHeight() - touchY;

            // Calculate the distance from the touch point to the series point
            double dist = Math.sqrt(Math.pow(touchX - pointX, 2) + Math.pow(invertedTouchY - pointY, 2));

            // Check if this point is within the threshold distance and is the nearest
            if (dist < threshold && dist < minDist) {
                minDist = dist;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    private int findNearestMeditationPointIndex(float touchX, float touchY, double threshold) {
        double minDist = Double.MAX_VALUE;
        int nearestIndex = -1;

        for (int i = 0; i < MEDITATION_FIXED_X_POINTS; i++) {
            // Convert series coordinates to screen coordinates for each point
            float pointX = meditationInterpolationPlot.seriesToScreenX(meditationMovableSeries.getX(i));
            float pointY = meditationInterpolationPlot.seriesToScreenY(meditationMovableSeries.getY(i));

            // Invert Y coordinate system (Plot coordinates and display is inversed)
            float invertedTouchY = meditationInterpolationPlot.getHeight() - touchY;

            // Calculate the distance from the touch point to the series point
            double dist = Math.sqrt(Math.pow(touchX - pointX, 2) + Math.pow(invertedTouchY - pointY, 2));

            // Check if this point is within the threshold distance and is the nearest
            if (dist < threshold && dist < minDist) {
                minDist = dist;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }


    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }







    @Override
    protected void onDestroy() {
        bluetoothNeuroSky.feedRawSignal(false);
        super.onDestroy();
        bluetoothNeuroSky.disconnect();
    }
}
