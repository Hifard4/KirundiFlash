package com.kirundiflash;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.kirundiflash.auth.LoginActivity;
import com.kirundiflash.dashboard.DashboardActivity;
import com.kirundiflash.data.CsvManager;
import com.kirundiflash.data.SupabaseClient;

/**
 * Entry point. No UI of its own -- just decides whether to route to
 * LoginActivity or DashboardActivity based on whether a Supabase session
 * is already stored locally, then finishes itself.
 *
 * Also runs CsvManager.initializeIfNeeded() once here so the four local
 * CSV files (known/unknown verbs/words) are guaranteed to exist before
 * any other screen tries to read them.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new CsvManager(this).initializeIfNeeded();

        Intent intent = SupabaseClient.isLoggedIn(this)
                ? new Intent(this, DashboardActivity.class)
                : new Intent(this, LoginActivity.class);

        startActivity(intent);
        finish();
    }
}
