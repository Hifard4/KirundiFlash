package com.kirundiflash.vip;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * PLACEHOLDER -- full VIP upgrade screen (pricing cards, CTA, billing
 * integration) comes in a later build step.
 */
public class UpgradeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Go VIP — coming in a later step!");
        textView.setPadding(48, 96, 48, 48);
        textView.setTextSize(18);
        setContentView(textView);
    }
}
