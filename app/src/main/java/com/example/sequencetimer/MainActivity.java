package com.example.sequencetimer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText editTextTimerDuration;
    private Button buttonAddTimer;
    private Button buttonStartTimers;
    private RecyclerView recyclerViewTimers;
    private TextView textViewCurrentTimerDisplay;

    private List<TimerItem> timerItemsList;
    private TimerAdapter timerAdapter;

    private CountDownTimer currentCountDownTimer;
    private int currentTimerIndex = -1;
    private MediaPlayer alarmPlayer;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private boolean areTimersRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relative_layout);

        TextView copyright = findViewById(R.id.copyright_text);
        copyright.setText(String.format(Locale.getDefault(),
                "© %d Vinit More",
                Calendar.getInstance().get(Calendar.YEAR)));

        editTextTimerDuration = findViewById(R.id.editTextTimerDuration);
        buttonAddTimer = findViewById(R.id.buttonAddTimer);
        buttonStartTimers = findViewById(R.id.buttonStartTimers);
        recyclerViewTimers = findViewById(R.id.recyclerViewTimers);
        textViewCurrentTimerDisplay = findViewById(R.id.textViewCurrentTimer);

        Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink);
        textViewCurrentTimerDisplay.setAnimation(blinkAnimation);

        timerItemsList = new ArrayList<>();
        timerAdapter = new TimerAdapter(timerItemsList, position -> {
            if (!areTimersRunning) {
                if (position >= 0 && position < timerItemsList.size()) {
                    timerItemsList.remove(position);
                    timerAdapter.notifyItemRemoved(position);
                    // The following line is important to update subsequent item indices
                    timerAdapter.notifyItemRangeChanged(position, timerItemsList.size() - position);
                    updateStartButtonState();
                }
            } else {
                Toast.makeText(this, "Cannot remove timers while running.", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerViewTimers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTimers.setAdapter(timerAdapter);

        buttonAddTimer.setOnClickListener(v -> addTimer());
        buttonStartTimers.setOnClickListener(v -> {
            if (areTimersRunning) {
                stopAllTimers();
            } else {
                if (!timerItemsList.isEmpty()) {
                    // Reset completion status before starting
                    for(TimerItem item : timerItemsList) {
                        item.setCompleted(false);
                    }
                    timerAdapter.notifyDataSetChanged(); // Refresh UI for completion status
                    currentTimerIndex = -1; // Reset index to start from the beginning
                    startNextTimer();
                } else {
                    Toast.makeText(this, "Please add timers first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        initializeAlarmPlayer();
    }

    private void initializeAlarmPlayer() {
        try {
            // Try to release existing player if any, to avoid resource leaks on re-initialization
            if (alarmPlayer != null) {
                alarmPlayer.release();
                alarmPlayer = null;
            }
            alarmPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI);
            if (alarmPlayer == null) { // Fallback if default alarm URI is null or fails
                alarmPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI);
            }

            if (alarmPlayer != null) {
                alarmPlayer.setOnErrorListener((mp, what, extra) -> {
                    Toast.makeText(MainActivity.this, "MediaPlayer error occurred.", Toast.LENGTH_SHORT).show();
                    // Attempt to reset or release the player on error
                    mp.reset(); // or mp.release() and set alarmPlayer to null
                    return true; // True if the error has been handled
                });
                alarmPlayer.setOnCompletionListener(mp -> {
                    // Optional: actions to take when sound finishes playing naturally
                    // (though we mostly control it with pause/seekTo(0))
                });
            } else {
                Toast.makeText(this, "Could not load alarm sound. Please check system sounds.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing alarm player: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace(); // Log the actual error
        }
    }


    private void addTimer() {
        String durationStr = editTextTimerDuration.getText().toString().trim();
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "Please enter a duration.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long durationSeconds = Long.parseLong(durationStr);
            if (durationSeconds <= 0) {
                Toast.makeText(this, "Duration must be positive.", Toast.LENGTH_SHORT).show();
                return;
            }
            timerItemsList.add(new TimerItem(durationSeconds * 1000));
            timerAdapter.notifyItemInserted(timerItemsList.size() - 1);
            editTextTimerDuration.setText("");
            updateStartButtonState();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid duration format.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStartButtonState() {
        if (areTimersRunning) {
            buttonStartTimers.setText("Stop All Timers");
            buttonStartTimers.setEnabled(true); // Stop button should always be enabled if running
        } else {
            buttonStartTimers.setText("Start All Timers");
            buttonStartTimers.setEnabled(!timerItemsList.isEmpty());
        }
        buttonAddTimer.setEnabled(!areTimersRunning);
    }

    private void startNextTimer() {
        currentTimerIndex++; // Increment to get the next timer in the list

        if (currentTimerIndex < timerItemsList.size()) { // Check if there's a timer at this new index
            areTimersRunning = true;
            updateStartButtonState(); // Update button states (e.g., disable "Add Timer")
            textViewCurrentTimerDisplay.setVisibility(View.VISIBLE); // Show the current timer display
            timerAdapter.setCurrentRunningTimerIndex(currentTimerIndex); // Highlight current timer in RecyclerView

            TimerItem timerToStart = timerItemsList.get(currentTimerIndex);
            long durationMillis = timerToStart.getDurationMillis();

            currentCountDownTimer = new CountDownTimer(durationMillis, 1000) { // 1000ms = 1 second interval
                @Override
                public void onTick(long millisUntilFinished) {
                    long secondsLeft = millisUntilFinished / 1000;
                    // Update the display with the current timer's number, total duration, and time remaining
                    textViewCurrentTimerDisplay.setText(String.format(Locale.getDefault(),
                            "⏱ Timer %d (%s): %02d:%02d",
                            currentTimerIndex + 1, // User-friendly 1-based index
                            timerItemsList.get(currentTimerIndex).getDurationInSecondsString(), // Original duration
                            secondsLeft / 60, // Minutes remaining
                            secondsLeft % 60)); // Seconds remaining
                }

                @Override
                public void onFinish() {
                    // This block executes when the current CountDownTimer finishes
                    if (currentTimerIndex < timerItemsList.size()) { // Double-check index bounds
                        timerItemsList.get(currentTimerIndex).setCompleted(true); // Mark as completed
                        timerAdapter.notifyItemChanged(currentTimerIndex); // Update RecyclerView item (e.g., for highlighting)
                    }

                    textViewCurrentTimerDisplay.setText(String.format(Locale.getDefault(),
                            "⏱ Timer %d: Finished!", currentTimerIndex + 1));
                    textViewCurrentTimerDisplay.setTextSize(72f);
                    playAlarmSound(); // Play the alarm
                    timerAdapter.setCurrentRunningTimerIndex(-1); // Clear current running highlight from adapter

                    // Use a Handler to post a delayed action, allowing the alarm to play for a bit
                    uiHandler.postDelayed(() -> {
                        if (alarmPlayer != null && alarmPlayer.isPlaying()) {
                            alarmPlayer.pause(); // Pause the sound
                            alarmPlayer.seekTo(0); // Rewind for next use
                        }
                        startNextTimer(); // Attempt to start the *next* timer in the sequence
                    }, 2000); // 2-second delay
                }
            }.start(); // Start the CountDownTimer
        } else {
            // This block executes if currentTimerIndex is out of bounds (all timers have been processed)
            Toast.makeText(this, "All timers completed!", Toast.LENGTH_LONG).show();
            textViewCurrentTimerDisplay.setText("All timers finished!");
            resetTimersState(true); // Reset state, indicating all timers completed naturally
        }
    }

    private void stopAllTimers() {
        if (currentCountDownTimer != null) {
            currentCountDownTimer.cancel(); // Stop the active CountDownTimer
            currentCountDownTimer = null;
        }
        if (alarmPlayer != null && alarmPlayer.isPlaying()) {
            alarmPlayer.stop(); // Stop the alarm sound if it's playing
            // It's good practice to prepare or reset the MediaPlayer after stopping
            try {
                alarmPlayer.prepare(); // Prepare for next play, or use alarmPlayer.seekTo(0);
            } catch (Exception e) {
                // If prepare fails, it might be in an error state. Consider re-initializing.
                initializeAlarmPlayer();
            }
        }
        resetTimersState(false); // Reset state, indicating timers were stopped manually
        Toast.makeText(this, "Timers stopped.", Toast.LENGTH_SHORT).show();
    }

    private void resetTimersState(boolean allCompletedNaturally) {
        areTimersRunning = false; // Mark timers as not running

        timerAdapter.clearHighlights(); // Clear any visual highlight on the running timer item

        if (allCompletedNaturally) {
            textViewCurrentTimerDisplay.setText("All timers finished!");
            // Optionally, reset currentTimerIndex if you want "Start All" to always start from the first timer
            // currentTimerIndex = -1;
        } else {
            // If stopped manually, provide feedback about where it stopped
            if (currentTimerIndex >= 0 && currentTimerIndex < timerItemsList.size()) {
                TimerItem lastActive = timerItemsList.get(currentTimerIndex);
                textViewCurrentTimerDisplay.setText(String.format(Locale.getDefault(),
                        "Stopped at Timer %d (%s)", currentTimerIndex + 1, lastActive.getDurationInSecondsString()));
            } else {
                textViewCurrentTimerDisplay.setText("Timers stopped. Ready.");
            }
        }
        // Do NOT reset currentTimerIndex here if you want "Start All Timers" to potentially resume.
        // If "Start All Timers" should always start from the beginning, then set currentTimerIndex = -1 here
        // and in the onClickListener for buttonStartTimers.
        // The current logic in buttonStartTimers already resets currentTimerIndex = -1 before calling startNextTimer.

        updateStartButtonState(); // Update UI elements like button text and enabled state
    }


    private void playAlarmSound() {
        if (alarmPlayer != null) {
            try {
                if (alarmPlayer.isPlaying()) {
                    alarmPlayer.stop(); // Stop current playback
                    // MediaPlayer needs to be prepared again after stop() before start()
                    alarmPlayer.prepare();
                }
                alarmPlayer.seekTo(0); // Rewind to the beginning of the sound
                alarmPlayer.start(); // Start playing the alarm
            } catch (IllegalStateException e) {
                // This can happen if player is in a wrong state (e.g. released, not prepared)
                Toast.makeText(this, "MediaPlayer state error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                initializeAlarmPlayer(); // Try to re-initialize
                // Optionally, try playing again after re-initialization
                if(alarmPlayer != null) {
                    try {
                        alarmPlayer.seekTo(0);
                        alarmPlayer.start();
                    } catch (Exception ex) {
                        // Log this second attempt failure
                    }
                }
            } catch (Exception e) { // Catch other exceptions like IOException from prepare()
                Toast.makeText(this, "Error playing alarm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                initializeAlarmPlayer(); // Try to re-initialize
            }
        } else {
            Toast.makeText(this, "Alarm player not ready. Initializing...", Toast.LENGTH_SHORT).show();
            initializeAlarmPlayer(); // Try to initialize if null
            // Optionally, try playing again after re-initialization
            if(alarmPlayer != null) {
                uiHandler.postDelayed(this::playAlarmSound, 500); // Try again shortly
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources to prevent leaks
        if (currentCountDownTimer != null) {
            currentCountDownTimer.cancel();
        }
        if (alarmPlayer != null) {
            if (alarmPlayer.isPlaying()) {
                alarmPlayer.stop();
            }
            alarmPlayer.release(); // Release the MediaPlayer resources
            alarmPlayer = null;
        }
        uiHandler.removeCallbacksAndMessages(null); // Remove any pending delayed posts
    }

    @Override
    protected void onStop() {
        super.onStop();
        // If timers are running and app is stopped, consider implications.
        // For this simple version, CountDownTimer might pause or be killed.
        // A ForegroundService would be needed for true background operation.
        // If alarmPlayer is playing, you might want to stop it here too.
        if (alarmPlayer != null && alarmPlayer.isPlaying()) {
            // Decide if you want to stop alarm on app stop or let it finish
            // alarmPlayer.pause(); // or alarmPlayer.stop();
        }
    }
}