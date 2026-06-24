package com.kirundiflash.data;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Shared OkHttp client + helpers for talking to Supabase's REST and Auth
 * APIs directly (no Kotlin SDK -- see project notes on why we went this
 * route from a Java codebase).
 *
 * Handles attaching the required headers:
 *   - apikey: the anon public key, required on every request
 *   - Authorization: Bearer <token> -- the anon key by default, or the
 *     user's own access token once they're logged in (required for RLS
 *     policies like "auth.uid() = id" to resolve correctly)
 */
public class SupabaseClient {

    private static final String PREFS_NAME = "supabase_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";

    private static OkHttpClient httpClient;

    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder().build();
        }
        return httpClient;
    }

    /** Builds a request with the apikey header + Authorization header pre-attached. */
    public static Request.Builder authedRequestBuilder(Context context, String url) {
        String accessToken = getAccessToken(context);
        String bearer = (accessToken != null) ? accessToken : SupabaseConfig.SUPABASE_ANON_KEY;

        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + bearer)
                .addHeader("Content-Type", "application/json");
    }

    // --- Session persistence (SharedPreferences -- simple and sufficient here) ---

    public static void saveSession(Context context, String accessToken, String refreshToken, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public static String getAccessToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACCESS_TOKEN, null);
    }

    public static String getRefreshToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_REFRESH_TOKEN, null);
    }

    public static String getCurrentUserId(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, null);
    }

    public static boolean isLoggedIn(Context context) {
        return getAccessToken(context) != null;
    }

    public static void clearSession(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
