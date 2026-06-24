package com.kirundiflash.leaderboard;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * PLACEHOLDER -- full leaderboard (podium, RecyclerView ranking list,
 * sticky user row) comes in a later build step.
 */
public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Leaderboard — coming in a later step!");
        textView.setPadding(48, 96, 48, 48);
        textView.setTextSize(18);
        setContentView(textView);
    }
}
