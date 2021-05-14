package com.note;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class MainActivity extends AppCompatActivity {

    String appId = "noteapp-mqare";
    public static App app;
    public static Realm realm;
    public static String userName;

    EditText loginText;
    EditText passwordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Realm.init(this);

        app = new App(new AppConfiguration.Builder(appId).build());
        realm = Realm.getDefaultInstance();

        setContentView(R.layout.activity_main);
    }

    public void loginPress(View view) {
        // TODO
        loginText = findViewById(R.id.loginText);
        passwordText = findViewById(R.id.passwordText);
        userName = loginText.getText().toString();

        String emailStr = loginText.getText().toString();
        String passwdStr = passwordText.getText().toString();

        Credentials emailPasswordCredentials = Credentials.emailPassword(emailStr, passwdStr);
        AtomicReference<User> user = new AtomicReference<User>();

        app.loginAsync(emailPasswordCredentials, it -> {
            if (it.isSuccess()) {
                Log.v("AUTH", "Logged in as " + emailStr);
                user.set(app.currentUser());
                Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, MenuActivity.class);
                startActivity(intent);
            } else {
                Log.e("AUTH", it.getError().toString());
                Toast.makeText(this, "Wrong email/password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void registerPress(View view) {
        // TODO

        loginText = findViewById(R.id.loginText);
        passwordText = findViewById(R.id.passwordText);

        String emailStr = loginText.getText().toString();
        String passwdStr = passwordText.getText().toString();
        Log.d("REGISTER DATA", emailStr + " " + passwdStr);

        Credentials emailPasswordCredentials = Credentials.emailPassword(emailStr, passwdStr);
        AtomicReference<User> user = new AtomicReference<User>();

        app.getEmailPassword().registerUserAsync(emailStr, passwdStr, e -> {
            if (e.isSuccess()) {
                Log.d("AUTH", "Registered");
                Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
            }
            else {
                Log.d("AUTH", "FAILED to register");
                Toast.makeText(this, "Already exists or password too short", Toast.LENGTH_SHORT).show();
            }
        });
    }
}