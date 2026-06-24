package com.kirundiflash.profile;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * PLACEHOLDER -- full profile (stats grid, badges, settings list, logout)
 * comes in a later build step.
 */
public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Profile — coming in a later step!");
        textView.setPadding(48, 96, 48, 48);
        textView.setTextSize(18);
        setContentView(textView);
    }
}
