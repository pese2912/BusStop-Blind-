package com.bus.busstopblind;

import android.content.Intent;
import android.os.Looper;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;



/**
 * Created by Sangsu on 2016-10-21.
 */
public class SplashActivity extends AppCompatActivity {

    Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 2000);

    }
}
