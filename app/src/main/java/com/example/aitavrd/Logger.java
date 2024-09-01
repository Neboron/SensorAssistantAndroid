package com.example.aitavrd;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.LinkedList;

public class Logger {
    private static final int MAX_LOG_COUNT = 100;
    private TextView logTextView;
    private ScrollView scrollViewLog;
    private LinkedList<SpannableString> logMessages = new LinkedList<>();
    private boolean isLoading = false;
    private String currentLoadMessage = "";
    private int animationStep = 0;
    private final Handler loadAnimationHandler = new Handler();

    private final Runnable loadAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isLoading) {
                // Update animation character
                char[] animationChars = {'|', '/', '-', '\\'};
                String animatedMessage = currentLoadMessage + " " + animationChars[animationStep];
                animationStep = (animationStep + 1) % 4;

                // Remove the last message and add the new one
                logMessages.removeLast();
                logMessages.addLast(createSpannableString(animatedMessage));

                // Update the log display
                updateLogDisplay();

                loadAnimationHandler.postDelayed(this, 100); // Update every 100ms
            }
        }
    };

    public Logger(TextView logTextView, ScrollView scrollViewLog) {
        this.logTextView = logTextView;
        this.scrollViewLog = scrollViewLog;
    }

    public void log(String message) {
        if (message.startsWith("[LOAD]")) {
            isLoading = true;
            currentLoadMessage = message.substring(6); // Remove the "[LOAD]" prefix
            logMessages.addLast(createSpannableString(currentLoadMessage));
            loadAnimationHandler.post(loadAnimationRunnable);
        } else if (message.startsWith("[LOAD OK]") || message.startsWith("[LOAD FAIL]")) {
            stopLoadAnimation(message);
        } else {
            if (isLoading) {
                stopLoadAnimation(" ");
            }
            logMessages.addLast(createSpannableString(message));
        }

        // Ensure we don't exceed the max log count
        if (logMessages.size() > MAX_LOG_COUNT) {
            logMessages.removeFirst();
        }

        // Update the log display
        updateLogDisplay();
    }


    private void stopLoadAnimation(String endMessage) {
        if (isLoading) {
            isLoading = false;
            loadAnimationHandler.removeCallbacks(loadAnimationRunnable);

            // Determine how to replace the animation
            String replacement = " ";
            if (endMessage.startsWith("[LOAD OK]")) {
                replacement = "[OK]";
            } else if (endMessage.startsWith("[LOAD FAIL]")) {
                replacement = "[FAIL]";
            }

            // Replace the last log with the end message
            logMessages.removeLast();
            logMessages.addLast(createSpannableString(currentLoadMessage + " " + replacement));
            updateLogDisplay();
        }
    }

    private SpannableString createSpannableString(String message) {
        // If message starts with "!!", change the color and remove "!!"
        SpannableString spannableString;
        if (message.startsWith("[!!]")) {
            String cleanMessage = message.substring(4);
            spannableString = new SpannableString(cleanMessage);
            spannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, cleanMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannableString = new SpannableString(message);
        }
        return spannableString;
    }

    private void updateLogDisplay() {
        logTextView.setText("");
        for (SpannableString logMessage : logMessages) {
            logTextView.append(logMessage);
            logTextView.append("\n");
        }
        scrollViewLog.post(() -> scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN));
    }

    public void showLogo() {
        String[] logoLines = new String[]{
                " _    ______  ___________ ___ ",
                "| |  / / __ \\/ ____/ ___//   |",
                "| | / / /_/ / /    \\__ \\/ /| |",
                "| |/ / _, _/ /___ ___/ / ___ |",
                "|___/_/ |_|\\____//____/_/  |_|",
                "                              ",
                "VRChat Sensor Assistant",
                "Version: 0.24.08.01",
                " "
        };

        for (String line : logoLines) {
            log(line);
        }
    }
}