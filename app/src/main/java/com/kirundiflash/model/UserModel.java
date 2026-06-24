package com.kirundiflash.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Mirrors a row in the Supabase "public.users" table.
 *
 * Postgres columns (snake_case, see supabase_schema.sql):
 *   id                (uuid, references auth.users)
 *   username          (text)
 *   email             (text)
 *   is_vip            (boolean)
 *   trial_start_date  (bigint)  - epoch millis, set at signup via trigger.
 *                                 Drives the 14-day freemium countdown.
 *   xp_seconds        (bigint)  - total seconds spent in-app, used as XP for
 *                                 the leaderboard (1 XP per second).
 *   country           (text)    - for the leaderboard flag icon.
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

    public UserModel() {
    }

    public UserModel(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.isVIP = false;
        this.trialStartDate = System.currentTimeMillis();
        this.xpSeconds = 0L;
        this.country = "BI";
    }

    /** Parses a single user row as returned by Supabase's PostgREST API. */
    public static UserModel fromJson(JSONObject json) throws JSONException {
        UserModel user = new UserModel();
        user.id = json.getString("id");
        user.username = json.optString("username", "");
        user.email = json.optString("email", "");
        user.isVIP = json.optBoolean("is_vip", false);
        user.trialStartDate = json.optLong("trial_start_date", System.currentTimeMillis());
        user.xpSeconds = json.optLong("xp_seconds", 0L);
        user.country = json.optString("country", "BI");
        return user;
    }

    /** Serializes fields that are safe/expected to be updated by the client (RLS-permitted). */
    public JSONObject toUpdateJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("is_vip", isVIP);
        json.put("xp_seconds", xpSeconds);
        json.put("country", country);
        return json;
    }

    // --- Derived helpers ---

    public boolean isTrialActive() {
        return System.currentTimeMillis() - trialStartDate < TRIAL_DURATION_MILLIS;
    }

    public boolean hasPremiumAccess() {
        return isVIP || isTrialActive();
    }

    public long trialDaysRemaining() {
        if (isVIP) return 0;
        long remainingMillis = TRIAL_DURATION_MILLIS - (System.currentTimeMillis() - trialStartDate);
        if (remainingMillis <= 0) return 0;
        return (remainingMillis / (24L * 60 * 60 * 1000)) + 1;
    }

    public long getXP() {
        return xpSeconds;
    }

    // --- Getters / setters ---

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
