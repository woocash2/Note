package com.note;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void loginPress(View view) {
        // TODO
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
    }

    public void registerPress(View view) {
        // TODO
    }
}