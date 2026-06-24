package com.kirundiflash.data;

import android.content.Context;

import com.kirundiflash.model.UserModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Reads/writes rows in the Supabase "public.users" table via the
 * auto-generated PostgREST API (the {SUPABASE_URL}/rest/v1/users endpoint).
 *
 * RLS policies (see supabase_schema.sql) restrict what each call can
 * actually do:
 *   - getCurrentUser / getLeaderboard: any authenticated user can SELECT
 *   - updateUser: only allowed where auth.uid() = id (enforced server-side)
 */
public class UserRepository {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface UserCallback {
        void onSuccess(UserModel user);
        void onError(String message);
    }

    public interface UserListCallback {
        void onSuccess(List<UserModel> users);
        void onError(String message);
    }

    private final Context context;

    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Fetches the row for the currently logged-in user. */
    public void getCurrentUser(UserCallback callback) {
        String userId = SupabaseClient.getCurrentUserId(context);
        if (userId == null) {
            callback.onError("Not logged in.");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/users?id=eq." + userId + "&select=*";
        Request request = SupabaseClient.authedRequestBuilder(context, url).get().build();

        SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to fetch user.");
                    return;
                }
                try {
                    JSONArray rows = new JSONArray(response.body().string());
                    if (rows.length() == 0) {
                        callback.onError("User row not found.");
                        return;
                    }
                    callback.onSuccess(UserModel.fromJson(rows.getJSONObject(0)));
                } catch (JSONException e) {
                    callback.onError("Failed to parse user: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Updates the current user's row (username, is_vip, xp_seconds, country).
     * Only fields included in toUpdateJson() are sent; RLS ensures a user
     * can only ever update their own row regardless of what id is implied.
     */
    public void updateUser(UserModel user, UserCallback callback) {
        String userId = SupabaseClient.getCurrentUserId(context);
        if (userId == null) {
            callback.onError("Not logged in.");
            return;
        }

        try {
            String url = SupabaseConfig.REST_URL + "/users?id=eq." + userId;
            RequestBody body = RequestBody.create(user.toUpdateJson().toString(), JSON);

            Request request = SupabaseClient.authedRequestBuilder(context, url)
                    .patch(body)
                    .addHeader("Prefer", "return=representation")
                    .build();

            SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError("Failed to update user.");
                        return;
                    }
                    try {
                        JSONArray rows = new JSONArray(response.body().string());
                        if (rows.length() == 0) {
                            callback.onError("Update returned no rows.");
                            return;
                        }
                        callback.onSuccess(UserModel.fromJson(rows.getJSONObject(0)));
                    } catch (JSONException e) {
                        callback.onError("Failed to parse update response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError("Failed to build update body: " + e.getMessage());
        }
    }

    /**
     * Adds `deltaSeconds` to the user's xp_seconds. Call this periodically
     * (e.g. every time the app goes to background) from SessionTimer rather
     * than overwriting the whole row, to avoid clobbering concurrent writes.
     *
     * NOTE: PostgREST doesn't support atomic "increment by" out of the box
     * via simple PATCH -- this does a read-then-write. For a single-device
     * personal-use case this is fine; for true concurrency safety you'd
     * want a Postgres RPC function (e.g. `increment_xp(seconds int)`) that
     * does the increment atomically in SQL. Flagging this in case you later
     * support a user being logged in on multiple devices at once.
     */
    public void incrementXp(long deltaSeconds, UserCallback callback) {
        getCurrentUser(new UserCallback() {
            @Override
            public void onSuccess(UserModel user) {
                user.setXpSeconds(user.getXpSeconds() + deltaSeconds);
                updateUser(user, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Fetches the top N users by xp_seconds, descending -- for the leaderboard. */
    public void getLeaderboard(int limit, UserListCallback callback) {
        String url = SupabaseConfig.REST_URL + "/users?select=*&order=xp_seconds.desc&limit=" + limit;
        Request request = SupabaseClient.authedRequestBuilder(context, url).get().build();

        SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to fetch leaderboard.");
                    return;
                }
                try {
                    JSONArray rows = new JSONArray(response.body().string());
                    List<UserModel> users = new ArrayList<>();
                    for (int i = 0; i < rows.length(); i++) {
                        users.add(UserModel.fromJson(rows.getJSONObject(i)));
                    }
                    callback.onSuccess(users);
                } catch (JSONException e) {
                    callback.onError("Failed to parse leaderboard: " + e.getMessage());
                }
            }
        });
    }
}
