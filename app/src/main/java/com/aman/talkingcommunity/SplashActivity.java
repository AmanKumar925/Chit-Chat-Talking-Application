package com.aman.talkingcommunity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_IMAGE_DELAY = 3000L;
    private static final long SPLASH_DISPLAY_LENGTH = 8000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable showSecondSplashImage = new Runnable() {
        @Override
        public void run() {
            ImageView firstImage = findViewById(R.id.spsh);
            ImageView secondImage = findViewById(R.id.spshq);

            if (firstImage != null) {
                firstImage.setVisibility(View.GONE);
            }
            if (secondImage != null) {
                secondImage.setVisibility(View.VISIBLE);
            }
        }
    };

    private final Runnable openMainActivity = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }

            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }

        setContentView(R.layout.activity_splash);

        handler.postDelayed(showSecondSplashImage, SPLASH_IMAGE_DELAY);
        handler.postDelayed(openMainActivity, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(showSecondSplashImage);
        handler.removeCallbacks(openMainActivity);
        super.onDestroy();
    }
}
