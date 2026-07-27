package com.example.quickstock.activities;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import com.google.android.material.button.MaterialButton;
import com.example.quickstock.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        Animation fade = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        findViewById(R.id.logoContainer).startAnimation(fade);
        findViewById(R.id.emailLayout).startAnimation(fade);
        findViewById(R.id.passwordLayout).startAnimation(fade);
        findViewById(R.id.btnLogin).startAnimation(fade);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Prevent going back to Login with the Back button
        });
    }
}