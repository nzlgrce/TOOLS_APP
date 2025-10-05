package com.example.uttoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class LapAdapter extends RecyclerView.Adapter<LapAdapter.LapViewHolder> {

    private final ArrayList<String> lapList;

    public LapAdapter(ArrayList<String> lapList) {
        this.lapList = lapList;
    }

    @NonNull
    @Override
    public LapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lap, parent, false);
        return new LapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LapViewHolder holder, int position) {
        // Calculate lap number properly
        int lapNumber = lapList.size() - position; // Makes newest lap #1 if added at top
        holder.lapNumber.setText(String.format(Locale.getDefault(), "%02d", lapNumber));

        // Set lap time (actual stopwatch value)
        String lapTime = lapList.get(position);
        holder.lapTime.setText(lapTime);
    }

    @Override
    public int getItemCount() {
        return lapList.size();
    }

    static class LapViewHolder extends RecyclerView.ViewHolder {
        TextView lapNumber, lapTime;

        LapViewHolder(@NonNull View itemView) {
            super(itemView);
            lapNumber = itemView.findViewById(R.id.lapNumber);
            lapTime = itemView.findViewById(R.id.lapTime);
        }
    }
}
