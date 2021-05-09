package com.note;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase root = FirebaseDatabase.getInstance();

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
        Log.d("DB", "registerPress siema");
        DatabaseReference reference = root.getReference("message");
        reference.push().setValue("Hello");
    }
}