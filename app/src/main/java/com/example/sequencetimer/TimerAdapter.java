package com.example.sequencetimer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.TimerViewHolder> {

    private List<TimerItem> timerList;
    private OnItemRemoveListener removeListener;
    private int currentRunningTimerIndex = -1; // To highlight the currently running timer

    public interface OnItemRemoveListener {
        void onItemRemove(int position);
    }

    public TimerAdapter(List<TimerItem> timerList, OnItemRemoveListener removeListener) {
        this.timerList = timerList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public TimerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.linear_layout, parent, false);
        return new TimerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TimerViewHolder holder, int position) {
        TimerItem currentItem = timerList.get(position);
        holder.textViewDuration.setText("Timer " + (position + 1) + ": " + currentItem.getDurationInSecondsString());

        holder.buttonRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                // Ensure position is valid before trying to remove
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    removeListener.onItemRemove(adapterPos);
                }
            }
        });

        // Highlight based on status
        if (position == currentRunningTimerIndex) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF9C4")); // Light Yellow for running
        } else if (currentItem.isCompleted()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#C8E6C9")); // Light Green for completed
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Default
        }
    }

    @Override
    public int getItemCount() {
        return timerList.size();
    }

    // Method to update the currently running timer for highlighting
    public void setCurrentRunningTimerIndex(int index) {
        int oldIndex = this.currentRunningTimerIndex;
        this.currentRunningTimerIndex = index;
        if (oldIndex != -1 && oldIndex < getItemCount()) { // Check bounds for oldIndex
            notifyItemChanged(oldIndex); // Refresh old item
        }
        if (index != -1 && index < getItemCount()) { // Check bounds for index
            notifyItemChanged(index); // Refresh new current item
        }
    }
    // Method to clear all highlights
    public void clearHighlights() {
        int oldIndex = this.currentRunningTimerIndex;
        this.currentRunningTimerIndex = -1;
        if (oldIndex != -1 && oldIndex < getItemCount()) { // Check bounds
            notifyItemChanged(oldIndex);
        }
    }


    static class TimerViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDuration;
        ImageButton buttonRemove;

        TimerViewHolder(View itemView) {
            super(itemView);
            textViewDuration = itemView.findViewById(R.id.textViewTimerItemDuration);
            buttonRemove = itemView.findViewById(R.id.buttonRemoveTimer);
        }
    }
}