package com.kirundiflash.auth;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.kirundiflash.data.SupabaseClient;
import com.kirundiflash.data.SupabaseConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Handles Google Sign-In via Supabase's OAuth redirect flow.
 *
 * Unlike Firebase, Supabase doesn't have a native Google Sign-In SDK
 * exchange. Instead, the flow is:
 *
 *   1. App opens a browser/Custom Tab to Supabase's authorize URL.
 *   2. User signs in with Google in the browser.
 *   3. Google redirects back to Supabase, which redirects to OUR custom
 *      redirect URI (e.g. "com.kirundiflash://login-callback") with
 *      the access_token and refresh_token in the URL fragment.
 *   4. Android intercepts that redirect via an intent-filter (see
 *      AndroidManifest.xml) and we parse the tokens out of the URI.
 *
 * Setup required:
 *   - Supabase Dashboard -> Authentication -> Providers -> Google: enabled,
 *     with your Google OAuth Client ID/Secret.
 *   - Supabase Dashboard -> Authentication -> URL Configuration: add
 *     "com.kirundiflash://login-callback" as a Redirect URL.
 *   - AndroidManifest.xml: add an intent-filter on LoginActivity (or a
 *     dedicated AuthCallbackActivity) for that custom scheme.
 */
public class GoogleAuthHelper {

    private static final String REDIRECT_URI = "com.kirundiflash://login-callback";

    /**
     * Call this from a button click to start the Google OAuth flow.
     * Launches the system browser (or Custom Tab) pointed at Supabase's
     * authorize endpoint.
     */
    public static void launchGoogleSignIn(Context context) {
        String authorizeUrl = SupabaseConfig.AUTH_URL
                + "/authorize?provider=google&redirect_to=" + Uri.encode(REDIRECT_URI);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
        context.startActivity(intent);
    }

    public interface GoogleAuthCallback {
        void onSuccess(String userId);
        void onError(String message);
    }

    /**
     * Call this from onCreate()/onNewIntent() of the Activity that receives
     * the redirect (the one with the intent-filter for our custom scheme).
     *
     * Supabase returns the tokens in the URL FRAGMENT (after #), not as
     * normal query parameters, e.g.:
     *   com.kirundiflash://login-callback#access_token=xxx&refresh_token=yyy&...
     *
     * Uri.getFragment() retrieves everything after '#' so we parse it manually.
     */
    public static void handleRedirect(Context context, Uri redirectUri, GoogleAuthCallback callback) {
        if (redirectUri == null) {
            callback.onError("No redirect data received.");
            return;
        }

        String fragment = redirectUri.getFragment();
        if (fragment == null) {
            callback.onError("Redirect did not contain auth tokens.");
            return;
        }

        String accessToken = null;
        String refreshToken = null;

        for (String param : fragment.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length != 2) continue;
            if (pair[0].equals("access_token")) {
                accessToken = pair[1];
            } else if (pair[0].equals("refresh_token")) {
                refreshToken = pair[1];
            }
        }

        if (accessToken == null) {
            callback.onError("No access token in redirect.");
            return;
        }

        fetchUserAndSaveSession(context, accessToken, refreshToken, callback);
    }

    /** After getting the access_token, call /auth/v1/user to find out who this is. */
    private static void fetchUserAndSaveSession(Context context, String accessToken,
                                                  String refreshToken, GoogleAuthCallback callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/user")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to fetch user info.");
                    return;
                }
                try {
                    JSONObject user = new JSONObject(response.body().string());
                    String userId = user.getString("id");
                    SupabaseClient.saveSession(context, accessToken, refreshToken, userId);
                    callback.onSuccess(userId);
                } catch (JSONException e) {
                    callback.onError("Failed to parse user info: " + e.getMessage());
                }
            }
        });
    }
}
