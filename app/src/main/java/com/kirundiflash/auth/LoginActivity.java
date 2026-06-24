package com.kirundiflash.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.kirundiflash.R;
import com.kirundiflash.dashboard.DashboardActivity;
import com.kirundiflash.data.SupabaseClient;

/**
 * Combined Sign In / Register screen, matching the KirundiFlash mockup.
 *
 * Also doubles as the receiver for the Google OAuth redirect -- see the
 * intent-filter on this Activity in AndroidManifest.xml. When Supabase
 * redirects back to com.kirundiflash://login-callback#access_token=...,
 * Android routes that URI into onNewIntent() here.
 */
public class LoginActivity extends AppCompatActivity {

    private enum Mode { SIGN_IN, REGISTER }

    private Mode currentMode = Mode.SIGN_IN;
    private boolean passwordVisible = false;

    private TextView tabSignIn, tabRegister;
    private EditText inputUsername, inputEmail, inputPassword;
    private ImageView togglePasswordVisibility;
    private Button btnSubmit;
    private LinearLayout btnGoogleSignIn;
    private TextView textError, textForgotPassword;
    private ProgressBar progressBar;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already logged in (and not arriving from a fresh OAuth redirect),
        // skip straight to the dashboard.
        if (SupabaseClient.isLoggedIn(this) && getIntent().getData() == null) {
            goToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);
        authManager = new AuthManager(this);

        bindViews();
        setupTabToggle();
        setupPasswordToggle();
        setupSubmitButton();
        setupGoogleSignIn();

        // Handle the case where the Activity is freshly created BY the
        // OAuth redirect intent (not onNewIntent, since singleTask only
        // triggers onNewIntent if the Activity instance already exists).
        handlePotentialOAuthRedirect(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handlePotentialOAuthRedirect(intent);
    }

    private void handlePotentialOAuthRedirect(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data == null) return;
        if (!"com.kirundiflash".equals(data.getScheme())) return;

        showLoading(true);
        GoogleAuthHelper.handleRedirect(this, data, new GoogleAuthHelper.GoogleAuthCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    showLoading(false);
                    goToDashboard();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void bindViews() {
        tabSignIn = findViewById(R.id.tabSignIn);
        tabRegister = findViewById(R.id.tabRegister);
        inputUsername = findViewById(R.id.inputUsername);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        textError = findViewById(R.id.textError);
        textForgotPassword = findViewById(R.id.textForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupTabToggle() {
        tabSignIn.setOnClickListener(v -> switchMode(Mode.SIGN_IN));
        tabRegister.setOnClickListener(v -> switchMode(Mode.REGISTER));
    }

    private void switchMode(Mode mode) {
        currentMode = mode;
        hideError();

        boolean isRegister = (mode == Mode.REGISTER);

        tabSignIn.setBackgroundResource(isRegister ? 0 : R.drawable.bg_tab_active);
        tabSignIn.setTextColor(getColorRes(isRegister ? R.color.text_gray : R.color.text_white));

        tabRegister.setBackgroundResource(isRegister ? R.drawable.bg_tab_active : 0);
        tabRegister.setTextColor(getColorRes(isRegister ? R.color.text_white : R.color.text_gray));

        inputUsername.setVisibility(isRegister ? View.VISIBLE : View.GONE);
        textForgotPassword.setVisibility(isRegister ? View.GONE : View.VISIBLE);
        btnSubmit.setText(isRegister ? "Create Account" : "Sign In");
    }

    private int getColorRes(int colorRes) {
        return getResources().getColor(colorRes, getTheme());
    }

    private void setupPasswordToggle() {
        togglePasswordVisibility.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            int selectionStart = inputPassword.getSelectionStart();
            inputPassword.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            inputPassword.setSelection(selectionStart);
        });
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            hideError();
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }

            if (currentMode == Mode.REGISTER) {
                String username = inputUsername.getText().toString().trim();
                if (username.isEmpty()) {
                    showError("Please choose a username.");
                    return;
                }
                if (password.length() < 6) {
                    showError("Password must be at least 6 characters.");
                    return;
                }
                doRegister(email, password, username);
            } else {
                doLogin(email, password);
            }
        });
    }

    private void doRegister(String email, String password, String username) {
        showLoading(true);
        authManager.register(email, password, username, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    showLoading(false);
                    goToDashboard();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void doLogin(String email, String password) {
        showLoading(true);
        authManager.login(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    showLoading(false);
                    goToDashboard();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void setupGoogleSignIn() {
        btnGoogleSignIn.setOnClickListener(v -> GoogleAuthHelper.launchGoogleSignIn(this));
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        btnGoogleSignIn.setEnabled(!loading);
    }

    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        textError.setVisibility(View.GONE);
    }
}
