package com.example.sequencetimer;

public class TimerItem {
    private long durationMillis; // Duration in milliseconds
    private boolean isCompleted;

    public TimerItem(long durationMillis) {
        this.durationMillis = durationMillis;
        this.isCompleted = false;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    // Helper to display duration in seconds for the UI
    public String getDurationInSecondsString() {
        return (durationMillis / 1000) + "s";
    }
}