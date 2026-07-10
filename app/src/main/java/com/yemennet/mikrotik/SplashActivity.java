package com.yemennet.mikrotik;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        ImageView splashLogo = findViewById(R.id.splashLogo);
        ProgressBar splashProgress = findViewById(R.id.splashProgress);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(splashLogo, "alpha", 0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.start();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(splashLogo, "scaleX", 0.5f, 1f);
        scaleX.setDuration(800);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(splashLogo, "scaleY", 0.5f, 1f);
        scaleY.setDuration(800);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.start();

        ObjectAnimator progressAnim = ObjectAnimator.ofInt(splashProgress, "progress", 0, 100);
        progressAnim.setDuration(SPLASH_DURATION);
        progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnim.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, ShowWebView.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }
}
