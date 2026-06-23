package com.kirundiflash.app.model;

/**
 * Mirrors a document in the Firestore "users" collection.
 *
 * Firestore fields:
 *   id              (String)  - Firebase Auth UID, also used as document ID
 *   username        (String)
 *   email           (String)
 *   isVIP           (boolean)
 *   trialStartDate  (long)    - epoch millis, set at registration. Drives the
 *                               14-day freemium countdown.
 *   xpSeconds       (long)    - total seconds spent in-app, used as XP for
 *                               the leaderboard (1 XP per second, see
 *                               SessionTimer / FirestoreManager for the
 *                               exact conversion).
 *   country         (String)  - optional, for the leaderboard flag icon.
 */
public class UserModel {

    public static final long TRIAL_DURATION_MILLIS = 14L * 24 * 60 * 60 * 1000; // 2 weeks

    private String id;
    private String username;
    private String email;
    private boolean isVIP;
    private long trialStartDate;
    private long xpSeconds;
    private String country;

    // Required no-arg constructor for Firestore deserialization
    public UserModel() {
    }

    public UserModel(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.isVIP = false;
        this.trialStartDate = System.currentTimeMillis();
        this.xpSeconds = 0L;
        this.country = "BI"; // default Burundi, user can change in profile
    }

    // --- Derived helpers (not stored directly in Firestore) ---

    /** True if the user is still within their 14-day free trial window. */
    public boolean isTrialActive() {
        return System.currentTimeMillis() - trialStartDate < TRIAL_DURATION_MILLIS;
    }

    /** True if the user has full premium access (either VIP or still trialing). */
    public boolean hasPremiumAccess() {
        return isVIP || isTrialActive();
    }

    /** Days remaining in the free trial, 0 if expired or already VIP. */
    public long trialDaysRemaining() {
        if (isVIP) return 0;
        long remainingMillis = TRIAL_DURATION_MILLIS - (System.currentTimeMillis() - trialStartDate);
        if (remainingMillis <= 0) return 0;
        return (remainingMillis / (24L * 60 * 60 * 1000)) + 1; // round up
    }

    /** XP for leaderboard display = seconds spent in app. 1 second = 1 XP. */
    public long getXP() {
        return xpSeconds;
    }

    // --- Getters / setters (required by Firestore) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVIP() { return isVIP; }
    public void setVIP(boolean VIP) { isVIP = VIP; }

    public long getTrialStartDate() { return trialStartDate; }
    public void setTrialStartDate(long trialStartDate) { this.trialStartDate = trialStartDate; }

    public long getXpSeconds() { return xpSeconds; }
    public void setXpSeconds(long xpSeconds) { this.xpSeconds = xpSeconds; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
