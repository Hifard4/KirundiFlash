package com.kirundiflash.auth;

import android.content.Context;

import com.kirundiflash.data.SupabaseClient;
import com.kirundiflash.data.SupabaseConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Wraps Supabase Auth REST endpoints for email/password signup and login.
 *
 * Reference: https://supabase.com/docs/reference/api/introduction
 * Endpoints used:
 *   POST {SUPABASE_URL}/auth/v1/signup
 *   POST {SUPABASE_URL}/auth/v1/token?grant_type=password
 *   POST {SUPABASE_URL}/auth/v1/logout
 *
 * All callbacks fire on a background thread (OkHttp's callback thread) --
 * if you need to touch UI (e.g. show a Toast, navigate), wrap the body in
 * runOnUiThread() in the calling Activity.
 */
public class AuthManager {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface AuthCallback {
        void onSuccess(String userId);
        void onError(String message);
    }

    private final Context context;

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Registers a new user. The `username` is passed as user metadata --
     * our SQL trigger (handle_new_user) reads it when creating the row in
     * public.users.
     */
    public void register(String email, String password, String username, AuthCallback callback) {
        try {
            JSONObject metadata = new JSONObject();
            metadata.put("username", username);

            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            body.put("data", metadata);

            Request request = new Request.Builder()
                    .url(SupabaseConfig.AUTH_URL + "/signup")
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    handleAuthResponse(response, callback);
                }
            });
        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }

    /** Logs in an existing user with email + password. */
    public void login(String email, String password, AuthCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            Request request = new Request.Builder()
                    .url(SupabaseConfig.AUTH_URL + "/token?grant_type=password")
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    handleAuthResponse(response, callback);
                }
            });
        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }

    /**
     * Logs out the current user. Calls Supabase's logout endpoint (revokes
     * the refresh token server-side) and clears the locally stored session.
     */
    public void logout(AuthCallback callback) {
        String accessToken = SupabaseClient.getAccessToken(context);
        if (accessToken == null) {
            SupabaseClient.clearSession(context);
            callback.onSuccess(null);
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/logout")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create("", null))
                .build();

        SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Even if the network call fails, clear local session so the
                // user isn't stuck logged in on this device.
                SupabaseClient.clearSession(context);
                callback.onSuccess(null);
            }

            @Override
            public void onResponse(Call call, Response response) {
                SupabaseClient.clearSession(context);
                callback.onSuccess(null);
            }
        });
    }

    // --- shared response parsing for signup + login ---

    private void handleAuthResponse(Response response, AuthCallback callback) throws IOException {
        String responseBody = response.body() != null ? response.body().string() : "";

        if (!response.isSuccessful()) {
            String message = "Authentication failed";
            try {
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("msg")) {
                    message = errorJson.getString("msg");
                } else if (errorJson.has("error_description")) {
                    message = errorJson.getString("error_description");
                }
            } catch (JSONException ignored) {
                // fall back to generic message above
            }
            callback.onError(message);
            return;
        }

        try {
            JSONObject json = new JSONObject(responseBody);
            String accessToken = json.optString("access_token", null);
            String refreshToken = json.optString("refresh_token", null);

            // On signup with email confirmation enabled, Supabase may return
            // a user object WITHOUT a session/access_token until the user
            // confirms their email. Handle that case explicitly.
            if (accessToken == null) {
                JSONObject user = json.optJSONObject("user");
                if (user != null) {
                    callback.onError("Account created. Please check your email to confirm before logging in.");
                } else {
                    callback.onError("Unexpected response from server.");
                }
                return;
            }

            JSONObject user = json.getJSONObject("user");
            String userId = user.getString("id");

            SupabaseClient.saveSession(context, accessToken, refreshToken, userId);
            callback.onSuccess(userId);
        } catch (JSONException e) {
            callback.onError("Failed to parse server response: " + e.getMessage());
        }
    }
}
