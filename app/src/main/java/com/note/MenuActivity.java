package com.note;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.note.docstools.DocumentInfo;
import com.note.docstools.util.JPEGConverter;
import com.note.docstools.util.Serializer;

import org.bson.Document;

import java.util.ArrayList;

import io.realm.mongodb.App;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class MenuActivity extends AppCompatActivity {

    ListView listView;
    public static String workDocName = "";
    public static String serialized = "";
    public ArrayList<Document> documents = new ArrayList<>();
    public ArrayList<String> docNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        listView = findViewById(R.id.listview);
        serialized = "";
        workDocName = "siema";
    }

    @Override
    protected void onResume() {
        super.onResume();
        readDocuments(MainActivity.app);
    }

    public void createNote(View view) {
        EditText newDocName = findViewById(R.id.newnotetext);
        String docName = newDocName.getText().toString();
        if (!docName.equals("") && !docNames.contains(docName)) {
            workDocName = docName;
            serialized =  "";
            openNote();
        }
        else {
            Toast.makeText(getApplicationContext(), "Already exists", Toast.LENGTH_SHORT).show();
        }
    }

    public void openNote() {
        Intent intent = new Intent(this, NoteActivity.class);
        startActivity(intent);
    }

    public void displayDocuments(MongoCursor<Document> docs) {
        documents.clear();
        docNames.clear();
        listView.clearChoices();

        while (docs.hasNext()) {
            Document doc = docs.next();
            Log.d("DOC NAMES", doc.get("name").toString());
            docNames.add(doc.get("name").toString());
            documents.add(doc);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.text_item, docNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                workDocName = docNames.get(position);
                serialized = documents.get(position).get("document").toString();
                showPopupMenu();
                //openNote();
            }
        });
    }

    public void showPopupMenu() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View contactPopupView = getLayoutInflater().inflate(R.layout.popup, null);
        AppCompatButton open = contactPopupView.findViewById(R.id.openButton);
        AppCompatButton download = contactPopupView.findViewById(R.id.downloadButton);
        AppCompatButton delete = contactPopupView.findViewById(R.id.deleteButton);
        AppCompatButton back = contactPopupView.findViewById(R.id.backButton);

        dialogBuilder.setView(contactPopupView);
        Dialog dialog = dialogBuilder.create();

        dialog.show();

        open.setOnClickListener(e -> {
            openNote();
            dialog.dismiss();
        });
        download.setOnClickListener(e -> {
            DocumentInfo documentInfo = Serializer.deserializeDocument(serialized);
            JPEGConverter.saveAsJPEG(documentInfo, workDocName, getApplicationContext());
            dialog.dismiss();
        });
        delete.setOnClickListener(e -> {
            deleteDocument(MainActivity.app, workDocName);
            dialog.dismiss();
        });
        back.setOnClickListener(e -> {
            dialog.dismiss();
        });
    }

    public void readDocuments(App app) {
        User appUser = app.currentUser();
        MongoClient mongoClient = appUser.getMongoClient("mongodb-atlas");
        MongoDatabase db = mongoClient.getDatabase("NoteDatabase");
        MongoCollection<Document> collection = db.getCollection("NoteCollection");

        Document filter = new Document().append("userid", appUser.getId());
        RealmResultTask<MongoCursor<Document>> docs = collection.find(filter).iterator();

        docs.getAsync(task -> {
            if (task.isSuccess()) {
                MongoCursor<Document> results = task.get();
                Log.d("DOCUMENT", "displaying");
                displayDocuments(results);
            }
            else {
                Log.d("DOCUMENT", task.getError().toString());
            }
        });
    }

    public void deleteDocument(App app, String name) {
        User appUser = app.currentUser();
        MongoClient mongoClient = appUser.getMongoClient("mongodb-atlas");
        MongoDatabase db = mongoClient.getDatabase("NoteDatabase");
        MongoCollection<Document> collection = db.getCollection("NoteCollection");

        Document filter = new Document().append("userid", appUser.getId());
        filter.append("name", name);

        collection.deleteOne(filter).getAsync(task -> {
            if (task.isSuccess()) {
                Log.d("DELETE DOCUMENT", appUser.getId() + " " + name);
            } else {
                Log.d("DELETE DOCUMENT", task.getError().toString());
            }
            readDocuments(app);
        });
    }
}