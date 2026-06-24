package com.kirundiflash.flashcards;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * PLACEHOLDER -- full flashcard swipe/flip/CSV-move logic comes in the next
 * build step. This exists now only so DashboardActivity's category card
 * navigation compiles and runs end-to-end.
 *
 * Expects an Intent extra EXTRA_TYPE = "VERB" or "WORD" (see
 * FlashcardItem.Type) telling it which deck to open.
 */
public class FlashcardActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "extra_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String type = getIntent().getStringExtra(EXTRA_TYPE);

        TextView textView = new TextView(this);
        textView.setText("Flashcards (" + type + ") — coming in the next step!");
        textView.setPadding(48, 96, 48, 48);
        textView.setTextSize(18);
        setContentView(textView);
    }
}
