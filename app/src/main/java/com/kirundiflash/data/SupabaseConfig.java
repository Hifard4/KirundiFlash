package com.kirundiflash.data;

/**
 * Central place for Supabase credentials.
 *
 * REPLACE THE TWO VALUES BELOW with your actual Project URL and anon public
 * key from Supabase Dashboard -> Project Settings -> API.
 *
 * The anon key is safe to ship inside the app -- it only grants whatever
 * your Row Level Security (RLS) policies allow (see supabase_schema.sql).
 * Never put your service_role key here or anywhere in the app.
 */
public class SupabaseConfig {

    public static final String SUPABASE_URL = "https://hjethybcemjmtoiusexa.supabase.co";

    // Supabase's newer "publishable key" format (sb_publishable_...) -- this
    // is the direct equivalent of the older anon/JWT key: safe to embed in
    // the app, RLS policies still govern what it can actually do.
    public static final String SUPABASE_ANON_KEY = "sb_publishable_Zz5WHGdk0C8-56yKLeG7AQ_Jzz1-fIH";

    // Derived endpoints -- no need to edit these
    public static final String AUTH_URL = SUPABASE_URL + "/auth/v1";
    public static final String REST_URL = SUPABASE_URL + "/rest/v1";

    private SupabaseConfig() {
        // no instances
    }
}
