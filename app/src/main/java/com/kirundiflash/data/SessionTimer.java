package com.kirundiflash.data;

import android.content.Context;

import com.kirundiflash.model.UserModel;

/**
 * Tracks how long the user spends in the app and converts that into XP
 * (1 second in-app = 1 XP, per the leaderboard spec: "XP measured by time
 * spent using the app").
 *
 * Usage pattern (see DashboardActivity, FlashcardActivity, etc.):
 *   onResume() -> sessionTimer.start()
 *   onPause()  -> sessionTimer.stopAndSync()
 *
 * Call start()/stopAndSync() from EVERY Activity's onResume/onPause if you
 * want total across-app time tracked accurately, not just time on the
 * Dashboard. Right now only DashboardActivity uses it -- extend to other
 * screens as you build them out, otherwise XP will only accrue while the
 * user is sitting on the dashboard itself, undercounting real usage.
 */
public class SessionTimer {

    private final Context context;
    private final UserRepository userRepository;
    private long sessionStartMillis = 0L;

    public SessionTimer(Context context, UserRepository userRepository) {
        this.context = context.getApplicationContext();
        this.userRepository = userRepository;
    }

    /** Call from onResume(). Marks the start of an active session. */
    public void start() {
        sessionStartMillis = System.currentTimeMillis();
    }

    /**
     * Call from onPause(). Computes elapsed seconds since start() and
     * pushes the increment to Supabase via UserRepository.incrementXp().
     *
     * Fire-and-forget: we don't block the UI thread or show errors here,
     * since losing a few seconds of XP on a rare network blip isn't worth
     * interrupting the user's flow.
     */
    public void stopAndSync() {
        if (sessionStartMillis == 0L) return;

        long elapsedSeconds = (System.currentTimeMillis() - sessionStartMillis) / 1000;
        sessionStartMillis = 0L;

        if (elapsedSeconds <= 0) return;

        userRepository.incrementXp(elapsedSeconds, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(UserModel user) {
                // No-op -- next screen load will reflect the new total.
            }

            @Override
            public void onError(String message) {
                // Silently ignore. Worst case: a few seconds of XP lost on
                // a transient network failure. Not worth surfacing to the
                // user mid-navigation.
            }
        });
    }
}
