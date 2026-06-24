package com.kirundiflash.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.kirundiflash.R;
import com.kirundiflash.auth.AuthManager;
import com.kirundiflash.auth.LoginActivity;
import com.kirundiflash.data.CsvManager;
import com.kirundiflash.data.SessionTimer;
import com.kirundiflash.data.UserRepository;
import com.kirundiflash.flashcards.FlashcardActivity;
import com.kirundiflash.flashcards.model.FlashcardItem.Type;
import com.kirundiflash.leaderboard.LeaderboardActivity;
import com.kirundiflash.model.UserModel;
import com.kirundiflash.profile.ProfileActivity;
import com.kirundiflash.vip.UpgradeActivity;

/**
 * The central hub, matching the KirundiFlash dashboard mockup: greeting +
 * streak + XP top bar, level progress bar, Verbs/Words category cards
 * showing mastery counts from the local CSVs, and bottom navigation.
 *
 * XP and streak/level numbers come from Supabase (UserRepository); mastery
 * counts ("120/450 Mastered") come from the local CSV files (CsvManager)
 * since that's where per-card progress actually lives on this device.
 */
public class DashboardActivity extends AppCompatActivity {

    // Same XP-per-level curve assumption used to render the progress bar.
    // Adjust freely -- this is just a placeholder leveling curve until you
    // decide on real level thresholds.
    private static final long XP_PER_LEVEL = 500;

    private ImageView imgAvatar;
    private TextView textGreeting, textStreak, textXP, textXPToNext;
    private ProgressBar progressLevel;
    private LinearLayout cardVerbs, cardWords;
    private TextView textVerbsProgress, textWordsProgress;

    private UserRepository userRepository;
    private CsvManager csvManager;
    private SessionTimer sessionTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        userRepository = new UserRepository(this);
        csvManager = new CsvManager(this);
        sessionTimer = new SessionTimer(this, userRepository);

        bindViews();
        setupCategoryCards();
        setupBottomNav();

        loadUserData();
        loadCsvProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionTimer.start();
        // Refresh in case the user just finished a flashcard session and
        // came back here -- counts and XP may have changed.
        loadUserData();
        loadCsvProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sessionTimer.stopAndSync();
    }

    private void bindViews() {
        imgAvatar = findViewById(R.id.imgAvatar);
        textGreeting = findViewById(R.id.textGreeting);
        textStreak = findViewById(R.id.textStreak);
        textXP = findViewById(R.id.textXP);
        progressLevel = findViewById(R.id.progressLevel);
        textXPToNext = findViewById(R.id.textXPToNext);
        cardVerbs = findViewById(R.id.cardVerbs);
        cardWords = findViewById(R.id.cardWords);
        textVerbsProgress = findViewById(R.id.textVerbsProgress);
        textWordsProgress = findViewById(R.id.textWordsProgress);
    }

    private void setupCategoryCards() {
        cardVerbs.setOnClickListener(v -> openFlashcards(Type.VERB));
        cardWords.setOnClickListener(v -> openFlashcards(Type.WORD));
    }

    private void openFlashcards(Type type) {
        Intent intent = new Intent(this, FlashcardActivity.class);
        intent.putExtra(FlashcardActivity.EXTRA_TYPE, type.name());
        startActivity(intent);
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            // Already here -- no-op.
        });
        findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.navVIP).setOnClickListener(v ->
                startActivity(new Intent(this, UpgradeActivity.class)));
    }

    /** Pulls username, XP, streak-relevant data from Supabase. */
    private void loadUserData() {
        userRepository.getCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(UserModel user) {
                runOnUiThread(() -> renderUser(user));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    // If the session is invalid/expired, send the user back
                    // to Login rather than showing a broken dashboard.
                    if (message != null && message.contains("Not logged in")) {
                        new AuthManager(DashboardActivity.this).logout(new AuthManager.AuthCallback() {
                            @Override
                            public void onSuccess(String userId) {
                                goToLogin();
                            }

                            @Override
                            public void onError(String error) {
                                goToLogin();
                            }
                        });
                    }
                    // Otherwise (e.g. transient network error), just leave
                    // the last-rendered values on screen rather than
                    // interrupting the user with an error dialog here.
                });
            }
        });
    }

    private void renderUser(UserModel user) {
        textGreeting.setText("Bwakeye, " + user.getUsername() + "!");
        textXP.setText("⭐ " + formatXp(user.getXP()));

        long xpIntoLevel = user.getXP() % XP_PER_LEVEL;
        int progressPercent = (int) ((xpIntoLevel * 100) / XP_PER_LEVEL);
        progressLevel.setProgress(progressPercent);
        textXPToNext.setText(xpIntoLevel + " / " + XP_PER_LEVEL + " XP to next level");

        // Streak tracking isn't modeled in UserModel yet (would need a
        // "last_active_date" + "streak_count" column and daily-login logic
        // server-side). Showing a static placeholder for now -- flagging
        // this as a follow-up rather than inventing fake data silently.
        textStreak.setText("🔥 --");
    }

    private String formatXp(long xp) {
        if (xp >= 1000) {
            return String.format("%,d", xp);
        }
        return String.valueOf(xp);
    }

    /** Reads local CSV mastery counts for the two category cards. */
    private void loadCsvProgress() {
        int verbsKnown = csvManager.countKnown(Type.VERB);
        int verbsTotal = csvManager.countTotal(Type.VERB);
        int wordsKnown = csvManager.countKnown(Type.WORD);
        int wordsTotal = csvManager.countTotal(Type.WORD);

        textVerbsProgress.setText(verbsKnown + "/" + verbsTotal + " Mastered");
        textWordsProgress.setText(wordsKnown + "/" + wordsTotal + " Mastered");
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
