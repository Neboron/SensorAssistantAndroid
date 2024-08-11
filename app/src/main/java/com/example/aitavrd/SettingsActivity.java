package com.example.aitavrd;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "aitaPrefsDroid";
    public static final String SETTINGS_UPDATE_FLAG = "settingsUpdateFlag";
    public static final String NEUROSKY_SWITCH_KEY = "neuroSkySwitch";
    public static final String AITAVRT_SWITCH_KEY = "aitaVrtSwitch";
    public static final String OSC_SWITCH_KEY = "oscSwitch";
    public static final String OSC_IP_KEY = "oscIpAddress";
    public static final String OSC_RX_PORT_KEY = "oscRxPort";
    public static final String OSC_TX_PORT_KEY = "oscTxPort";
    public static final String OSC_STREAM1_INPUT_KEY = "oscStreamInput1";
    public static final String OSC_STREAM2_INPUT_KEY = "oscStreamInput2";
    public static final String OSC_STREAM3_INPUT_KEY = "oscStreamInput3";
    public static final String OSC_STREAM4_INPUT_KEY = "oscStreamInput4";
    public static final String OSC_STREAM1_ADDRESS_KEY = "oscStreamAddress1";
    public static final String OSC_STREAM2_ADDRESS_KEY = "oscStreamAddress2";
    public static final String OSC_STREAM3_ADDRESS_KEY = "oscStreamAddress3";
    public static final String OSC_STREAM4_ADDRESS_KEY = "oscStreamAddress4";


    private SwitchMaterial neuroSkySwitch;
    private SwitchMaterial aitaVrtSwitch;
    private SwitchMaterial oscSwitch;
    private EditText oscIpEditText;
    private EditText oscRxPortEditText;
    private EditText oscTxPortEditText;
    private TextView oscMessagesSentCount;
    private TextView oscMessagesReceivedCount;
    private Button oscReconnectButton;
    private LinearLayout oscTestInputsDropdownLayout;
    private ImageView oscTestInputsDropdownIcon;
    private LinearLayout oscTestInputsLayout;
    private SeekBar oscTestSeekBar;
    private LinearLayout oscStreamersDropdownLayout;
    private ImageView oscStreamersDropdownIcon;
    private LinearLayout oscStreamersLayout;

    // Stream 1
    private Spinner oscStreamDropdownSpinner1;
    private EditText oscStreamAddressEditText1;
    private ImageButton oscStreamListenAddressButton1;

    // Stream 2
    private Spinner oscStreamDropdownSpinner2;
    private EditText oscStreamAddressEditText2;
    private ImageButton oscStreamListenAddressButton2;

    // Stream 3
    private Spinner oscStreamDropdownSpinner3;
    private EditText oscStreamAddressEditText3;
    private ImageButton oscStreamListenAddressButton3;

    // Stream 4
    private Spinner oscStreamDropdownSpinner4;
    private EditText oscStreamAddressEditText4;
    private ImageButton oscStreamListenAddressButton4;


    private ImageButton returnButton;

    private OSC oscClient;
    private boolean oscTestInputsVisible = false;
    private boolean oscStreamersVisible = false;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        returnButton                    = findViewById(R.id.returnButton);
        neuroSkySwitch                  = findViewById(R.id.neuroSkySwitch);
        aitaVrtSwitch                   = findViewById(R.id.aitaVrtSwitch);
        oscSwitch                       = findViewById(R.id.oscSwitch);
        oscIpEditText                   = findViewById(R.id.oscIpEditText);
        oscRxPortEditText               = findViewById(R.id.oscRxPortEditText);
        oscTxPortEditText               = findViewById(R.id.oscTxPortEditText);
        oscMessagesSentCount            = findViewById(R.id.oscMessagesSentCount);
        oscMessagesReceivedCount        = findViewById(R.id.oscMessagesReceivedCount);
        oscReconnectButton              = findViewById(R.id.oscReconnectButton);
        oscTestInputsDropdownLayout     = findViewById(R.id.oscTestInputsDropdownLayout);
        oscTestInputsDropdownIcon       = findViewById(R.id.oscTestInputsDropdownIcon);
        oscTestInputsLayout             = findViewById(R.id.oscTestInputsLayout);
        oscTestSeekBar                  = findViewById(R.id.oscTestSeekBar);
        oscStreamersDropdownLayout      = findViewById(R.id.oscStreamersDropdownLayout);
        oscStreamersDropdownIcon        = findViewById(R.id.oscStreamersDropdownIcon);
        oscStreamersLayout              = findViewById(R.id.oscStreamersLayout);
        oscStreamDropdownSpinner1       = findViewById(R.id.oscStreamDropdownSpinner1);
        oscStreamAddressEditText1       = findViewById(R.id.oscStreamAddressEditText1);
        oscStreamListenAddressButton1   = findViewById(R.id.oscStreamListenAddressButton1);
        oscStreamDropdownSpinner2       = findViewById(R.id.oscStreamDropdownSpinner2);
        oscStreamAddressEditText2       = findViewById(R.id.oscStreamAddressEditText2);
        oscStreamListenAddressButton2   = findViewById(R.id.oscStreamListenAddressButton2);
        oscStreamDropdownSpinner3       = findViewById(R.id.oscStreamDropdownSpinner3);
        oscStreamAddressEditText3       = findViewById(R.id.oscStreamAddressEditText3);
        oscStreamListenAddressButton3   = findViewById(R.id.oscStreamListenAddressButton3);
        oscStreamDropdownSpinner4       = findViewById(R.id.oscStreamDropdownSpinner4);
        oscStreamAddressEditText4       = findViewById(R.id.oscStreamAddressEditText4);
        oscStreamListenAddressButton4   = findViewById(R.id.oscStreamListenAddressButton4);


        // Load saved state
        SharedPreferences preferences  = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean neuroSkySwitchState    = preferences.getBoolean(NEUROSKY_SWITCH_KEY, true);
        boolean aitaVrtSwitchState     = preferences.getBoolean(AITAVRT_SWITCH_KEY, true);
        boolean oscSwitchState         = preferences.getBoolean(OSC_SWITCH_KEY, true);
        String  savedOscIpAddress      = preferences.getString(OSC_IP_KEY, "192.168.50.3"); // Never mind
        String  savedOscRxPort         = preferences.getString(OSC_RX_PORT_KEY, "9000"); // Default to 9000 (VRChat RX)
        String  savedOscTxPort         = preferences.getString(OSC_TX_PORT_KEY, "9001"); // Default to 9001 (VRChat TX)
        String  savedOscStreamInput1   = preferences.getString(OSC_STREAM1_INPUT_KEY, "DISABLE");
        String  savedOscStreamInput2   = preferences.getString(OSC_STREAM2_INPUT_KEY, "DISABLE");
        String  savedOscStreamInput3   = preferences.getString(OSC_STREAM3_INPUT_KEY, "DISABLE");
        String  savedOscStreamInput4   = preferences.getString(OSC_STREAM4_INPUT_KEY, "DISABLE");
        String  savedOscStreamAddress1 = preferences.getString(OSC_STREAM1_ADDRESS_KEY, "/Your/Avatar/Parameter/Address");
        String  savedOscStreamAddress2 = preferences.getString(OSC_STREAM2_ADDRESS_KEY, "/Your/Avatar/Parameter/Address");
        String  savedOscStreamAddress3 = preferences.getString(OSC_STREAM3_ADDRESS_KEY, "/Your/Avatar/Parameter/Address");
        String  savedOscStreamAddress4 = preferences.getString(OSC_STREAM4_ADDRESS_KEY, "/Your/Avatar/Parameter/Address");

        neuroSkySwitch.setChecked(neuroSkySwitchState);
        aitaVrtSwitch.setChecked(aitaVrtSwitchState);
        oscSwitch.setChecked(oscSwitchState);
        oscIpEditText.setText(savedOscIpAddress);
        oscRxPortEditText.setText(savedOscRxPort);
        oscTxPortEditText.setText(savedOscTxPort);

        oscStreamAddressEditText1.setText(savedOscStreamAddress1);
        oscStreamAddressEditText2.setText(savedOscStreamAddress2);
        oscStreamAddressEditText3.setText(savedOscStreamAddress3);
        oscStreamAddressEditText4.setText(savedOscStreamAddress4);

        // Set up Spinners
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.osc_stream_input_items, R.layout.spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        oscStreamDropdownSpinner1.setAdapter(adapter);
        oscStreamDropdownSpinner2.setAdapter(adapter);
        oscStreamDropdownSpinner3.setAdapter(adapter);
        oscStreamDropdownSpinner4.setAdapter(adapter);

        // Set initial Spinners selection based on saved state
        setSpinnerSelection(oscStreamDropdownSpinner1, savedOscStreamInput1);
        setSpinnerSelection(oscStreamDropdownSpinner2, savedOscStreamInput2);
        setSpinnerSelection(oscStreamDropdownSpinner3, savedOscStreamInput3);
        setSpinnerSelection(oscStreamDropdownSpinner4, savedOscStreamInput4);


        // Set initial track color
        updateTrackColor(neuroSkySwitch, neuroSkySwitchState);
        updateTrackColor(aitaVrtSwitch,  aitaVrtSwitchState);
        updateTrackColor(oscSwitch,      oscSwitchState);


        oscClient = oscClient.getInstance(this);

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        neuroSkySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(NEUROSKY_SWITCH_KEY, isChecked);
            editor.apply();

            // Update the track color
            updateTrackColor(neuroSkySwitch, isChecked);
        });

        aitaVrtSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(AITAVRT_SWITCH_KEY, isChecked);
            editor.apply();

            // Update the track color
            updateTrackColor(aitaVrtSwitch, isChecked);
        });

        oscSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(OSC_SWITCH_KEY, isChecked);
            editor.apply();

            // Update the track color
            updateTrackColor(oscSwitch, isChecked);
        });

        // Save IP and Port fields when they lose focus
        oscIpEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_IP_KEY, oscIpEditText.getText().toString());
                editor.apply();
            }
        });

        oscRxPortEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_RX_PORT_KEY, oscRxPortEditText.getText().toString());
                editor.apply();
            }
        });

        oscTxPortEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_TX_PORT_KEY, oscTxPortEditText.getText().toString());
                editor.apply();
            }
        });

        oscReconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipAddress = oscIpEditText.getText().toString();
                int rxPort = Integer.parseInt(oscRxPortEditText.getText().toString());
                int txPort = Integer.parseInt(oscTxPortEditText.getText().toString());

                //oscClient.initialize(ipAddress, rxPort, txPort);

                try {
                    oscClient.initialize(ipAddress, rxPort, txPort);
                    List<Object> args = Arrays.asList(0.01f);
                    oscClient.sendMessage("/avatar/parameters/RibbonPoseY", args);

                    // Update message count display
                    oscMessagesSentCount.setText(String.valueOf(oscClient.getTransmittedMessageCount()));
                    oscMessagesReceivedCount.setText(String.valueOf(oscClient.getReceivedMessageCount()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        oscTestInputsDropdownLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (oscTestInputsVisible) {
                    oscTestInputsLayout.setVisibility(View.GONE);
                    oscTestInputsDropdownIcon.setRotation(0);
                } else {
                    oscTestInputsLayout.setVisibility(View.VISIBLE);
                    oscTestInputsDropdownIcon.setRotation(180);
                }
                oscTestInputsVisible = !oscTestInputsVisible;
            }
        });

        // TSB1F Input Source (0.0f - 1.0f)
        oscTestSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100.0f; // Convert progress to a float between 0.0f and 1.0f as common value for VRChat parameters
                List<Object> arg = Arrays.asList(value);

                // Check each spinner and send a message if "TSB1F" is selected
                checkInputAndSendMessage(oscStreamDropdownSpinner1, oscStreamAddressEditText1, arg, "TSB1F");
                checkInputAndSendMessage(oscStreamDropdownSpinner2, oscStreamAddressEditText2, arg, "TSB1F");
                checkInputAndSendMessage(oscStreamDropdownSpinner3, oscStreamAddressEditText3, arg, "TSB1F");
                checkInputAndSendMessage(oscStreamDropdownSpinner4, oscStreamAddressEditText4, arg, "TSB1F");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        oscStreamersDropdownLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (oscStreamersVisible) {
                    oscStreamersLayout.setVisibility(View.GONE);
                    oscStreamersDropdownIcon.setRotation(0);
                } else {
                    oscStreamersLayout.setVisibility(View.VISIBLE);
                    oscStreamersDropdownIcon.setRotation(180);
                }
                oscStreamersVisible = !oscStreamersVisible;
            }
        });

        // Stream 1
        oscStreamDropdownSpinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM1_INPUT_KEY, selectedItem);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        oscStreamAddressEditText1.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM1_ADDRESS_KEY, oscStreamAddressEditText1.getText().toString());
                editor.apply();
            }
        });

        oscStreamListenAddressButton1.setOnClickListener(new View.OnClickListener() {
            private boolean isListeningForOscMessage = false;
            private Handler oscTimeoutHandler = new Handler();
            private Runnable oscTimeoutRunnable;

            public void onClick(View v) {
                if (isListeningForOscMessage) {
                    oscClient.cancelReceiveSingleMessage();
                    isListeningForOscMessage = false;
                    oscStreamAddressEditText1.setText("");
                    oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                } else {
                    isListeningForOscMessage = true;
                    oscStreamAddressEditText1.setText("Listening For OSC Message...");
                    oscClient.receiveSingleMessage(new OSC.OSCMessageListener() {
                        @Override
                        public void onMessageReceived(String address, List<Object> arguments) {
                            runOnUiThread(() -> {
                                oscStreamAddressEditText1.setText(address);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString(OSC_STREAM1_ADDRESS_KEY, oscStreamAddressEditText1.getText().toString());
                                editor.apply();
                                isListeningForOscMessage = false;
                                oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                            });
                        }
                    });

                    // Define the timeout runnable
                    oscTimeoutRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isListeningForOscMessage) {
                                oscClient.cancelReceiveSingleMessage();
                                isListeningForOscMessage = false;
                                oscStreamAddressEditText1.setText("");
                            }
                        }
                    };

                    // Post the timeout runnable
                    oscTimeoutHandler.postDelayed(oscTimeoutRunnable, 10000); // 10 seconds
                }
            }
        });


        // Stream 2
        oscStreamDropdownSpinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM2_INPUT_KEY, selectedItem);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        oscStreamAddressEditText2.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM2_ADDRESS_KEY, oscStreamAddressEditText2.getText().toString());
                editor.apply();
            }
        });

        oscStreamListenAddressButton2.setOnClickListener(new View.OnClickListener() {
            private boolean isListeningForOscMessage = false;
            private Handler oscTimeoutHandler = new Handler();
            private Runnable oscTimeoutRunnable;

            public void onClick(View v) {
                if (isListeningForOscMessage) {
                    oscClient.cancelReceiveSingleMessage();
                    isListeningForOscMessage = false;
                    oscStreamAddressEditText2.setText("");
                    oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                } else {
                    isListeningForOscMessage = true;
                    oscStreamAddressEditText2.setText("Listening For OSC Message...");
                    oscClient.receiveSingleMessage(new OSC.OSCMessageListener() {
                        @Override
                        public void onMessageReceived(String address, List<Object> arguments) {
                            runOnUiThread(() -> {
                                oscStreamAddressEditText2.setText(address);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString(OSC_STREAM2_ADDRESS_KEY, oscStreamAddressEditText2.getText().toString());
                                editor.apply();
                                isListeningForOscMessage = false;
                                oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                            });
                        }
                    });

                    // Define the timeout runnable
                    oscTimeoutRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isListeningForOscMessage) {
                                oscClient.cancelReceiveSingleMessage();
                                isListeningForOscMessage = false;
                                oscStreamAddressEditText2.setText("");
                            }
                        }
                    };

                    // Post the timeout runnable
                    oscTimeoutHandler.postDelayed(oscTimeoutRunnable, 10000); // 10 seconds
                }
            }
        });

        // Stream 3
        oscStreamDropdownSpinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM3_INPUT_KEY, selectedItem);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        oscStreamAddressEditText3.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM3_ADDRESS_KEY, oscStreamAddressEditText3.getText().toString());
                editor.apply();
            }
        });

        oscStreamListenAddressButton3.setOnClickListener(new View.OnClickListener() {
            private boolean isListeningForOscMessage = false;
            private Handler oscTimeoutHandler = new Handler();
            private Runnable oscTimeoutRunnable;

            public void onClick(View v) {
                if (isListeningForOscMessage) {
                    oscClient.cancelReceiveSingleMessage();
                    isListeningForOscMessage = false;
                    oscStreamAddressEditText3.setText("");
                    oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                } else {
                    isListeningForOscMessage = true;
                    oscStreamAddressEditText3.setText("Listening For OSC Message...");
                    oscClient.receiveSingleMessage(new OSC.OSCMessageListener() {
                        @Override
                        public void onMessageReceived(String address, List<Object> arguments) {
                            runOnUiThread(() -> {
                                oscStreamAddressEditText3.setText(address);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString(OSC_STREAM3_ADDRESS_KEY, oscStreamAddressEditText3.getText().toString());
                                editor.apply();
                                isListeningForOscMessage = false;
                                oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                            });
                        }
                    });

                    // Define the timeout runnable
                    oscTimeoutRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isListeningForOscMessage) {
                                oscClient.cancelReceiveSingleMessage();
                                isListeningForOscMessage = false;
                                oscStreamAddressEditText3.setText("");
                            }
                        }
                    };

                    // Post the timeout runnable
                    oscTimeoutHandler.postDelayed(oscTimeoutRunnable, 10000); // 10 seconds
                }
            }
        });

        // Stream 4
        oscStreamDropdownSpinner4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM4_INPUT_KEY, selectedItem);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        oscStreamAddressEditText4.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(OSC_STREAM4_ADDRESS_KEY, oscStreamAddressEditText4.getText().toString());
                editor.apply();
            }
        });

        oscStreamListenAddressButton4.setOnClickListener(new View.OnClickListener() {
            private boolean isListeningForOscMessage = false;
            private Handler oscTimeoutHandler = new Handler();
            private Runnable oscTimeoutRunnable;

            public void onClick(View v) {
                if (isListeningForOscMessage) {
                    oscClient.cancelReceiveSingleMessage();
                    isListeningForOscMessage = false;
                    oscStreamAddressEditText4.setText("");
                    oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                } else {
                    isListeningForOscMessage = true;
                    oscStreamAddressEditText4.setText("Listening For OSC Message...");
                    oscClient.receiveSingleMessage(new OSC.OSCMessageListener() {
                        @Override
                        public void onMessageReceived(String address, List<Object> arguments) {
                            runOnUiThread(() -> {
                                oscStreamAddressEditText4.setText(address);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString(OSC_STREAM4_ADDRESS_KEY, oscStreamAddressEditText4.getText().toString());
                                editor.apply();
                                isListeningForOscMessage = false;
                                oscTimeoutHandler.removeCallbacks(oscTimeoutRunnable);
                            });
                        }
                    });

                    // Define the timeout runnable
                    oscTimeoutRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isListeningForOscMessage) {
                                oscClient.cancelReceiveSingleMessage();
                                isListeningForOscMessage = false;
                                oscStreamAddressEditText4.setText("");
                            }
                        }
                    };

                    // Post the timeout runnable
                    oscTimeoutHandler.postDelayed(oscTimeoutRunnable, 10000); // 10 seconds
                }
            }
        });



        // Start the IP reachability check
        handler.post(checkOscRunnable);
    }


    private void updateTrackColor(SwitchMaterial switchMat, boolean isChecked) {
        if (isChecked) {
            switchMat.setTrackTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_term_main_st)));
        } else {
            switchMat.setTrackTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.aperture_term_main_dark)));
        }
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

    private void checkInputAndSendMessage(Spinner spinner, EditText addressEditText, List<Object> args, String targetValue) {
        if (spinner.getSelectedItem().toString().equals(targetValue)) {
            String address = addressEditText.getText().toString();
            oscClient.sendMessage(address, args);
        }
    }

    private final Runnable checkOscRunnable = new Runnable() {
        @Override
        public void run() {
            checkOscReachability();
            handler.postDelayed(this, 2000); // Repeat every 2 seconds
        }
    };

    private void checkOscReachability() {
        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(oscIpEditText.getText().toString());
                int rxPort = Integer.parseInt(oscRxPortEditText.getText().toString());
                int txPort = Integer.parseInt(oscTxPortEditText.getText().toString());
                boolean reachable = address.isReachable(2000); // 2 seconds timeout
                runOnUiThread(() -> {
                    // Update message count display
                    oscMessagesSentCount.setText(String.valueOf(oscClient.getTransmittedMessageCount()));
                    oscMessagesReceivedCount.setText(String.valueOf(oscClient.getReceivedMessageCount()));

                    if (reachable) {
                        oscIpEditText.setTextColor(ContextCompat.getColor(this, R.color.green_term_main_st));
                    } else {
                        oscIpEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main_dark));
                    }
                });
            } catch (UnknownHostException e) {
                runOnUiThread(() -> oscIpEditText.setTextColor(ContextCompat.getColor(this, R.color.aperture_term_main_dark)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkOscRunnable);
    }
}
