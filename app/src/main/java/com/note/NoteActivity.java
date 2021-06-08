package com.note;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.note.docstools.DocumentInfo;
import com.note.docstools.DocumentCoverView;
import com.note.docstools.DocumentView;
import com.note.docstools.TextFieldManager;
import com.note.docstools.util.Serializer;

import org.bson.Document;

import java.util.TreeMap;

import io.realm.mongodb.App;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class NoteActivity extends AppCompatActivity {

    public enum State {TEXT, PEN, ERASE, MOVE};
    public int[] strokeMultipliers = {1, 2, 4};
    public int[] penAlphas = {255, 153, 63};
    public static State state = State.MOVE;
    public static int barHeight;

    private ConstraintLayout mainLayout;

    private ConstraintLayout colorPalette;
    private ConstraintLayout sizePalette;
    private ConstraintLayout penPalette;
    private ConstraintLayout menuBar;

    TreeMap<String, View> mainButtons = new TreeMap<>();

    private DocumentView documentView;
    private DocumentCoverView documentCover;
    private TextFieldManager textFieldManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        mainLayout = findViewById(R.id.mainLayout);
        documentView = findViewById(R.id.documentView);
        textFieldManager = new TextFieldManager(mainLayout, documentView);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        documentCover = findViewById(R.id.documentCover);
        documentCover.documentView = documentView;
        documentView.init(displayMetrics, textFieldManager, documentCover);

        Point winsize = new Point();
        getWindowManager().getDefaultDisplay().getSize(winsize);
        documentCover.init(winsize, textFieldManager);

        menuBar = findViewById(R.id.menuBar);
        barHeight = menuBar.getLayoutParams().height;
        colorPalette = findViewById(R.id.colorPalette);
        sizePalette = findViewById(R.id.sizePallete);
        penPalette = findViewById(R.id.penPalette);

        mainButtons.put("colorButton", (View) findViewById(R.id.colorButton));
        mainButtons.put("textButton", (View) findViewById(R.id.textButton));
        mainButtons.put("sizeButton", (View) findViewById(R.id.sizeButton));
        mainButtons.put("penButton", (View) findViewById(R.id.penButton));
        mainButtons.put("eraserButton", (View) findViewById(R.id.eraserButton));
        mainButtons.put("moveButton", (View) findViewById(R.id.moveButton));

        state = State.MOVE;
        mainButtons.get("moveButton").setScaleX(1.2f);
        mainButtons.get("moveButton").setScaleY(1.2f);

        if (MenuActivity.newlyCreated) {
            String serialized = Serializer.serializeDocument(MenuActivity.workDocName, documentView, documentCover, textFieldManager, false);
            saveDocument(MainActivity.app, MenuActivity.workDocName, serialized, true);
        }
        if (!MenuActivity.serialized.equals("")) {
            DocumentInfo dinfo = Serializer.deserializeDocument(MenuActivity.serialized);
            Serializer.injectDataToDocument(documentView, textFieldManager, dinfo, this);
        }
    }

    public void showHideColorPalette(View view) {
        int visibility = colorPalette.getVisibility();
        hidePalettes(view);
        visibility = visibility == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        colorPalette.setVisibility(visibility);
    }

    public void showHideSizePalette(View view) {
        int visibility = sizePalette.getVisibility();
        hidePalettes(view);
        visibility = visibility == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        sizePalette.setVisibility(visibility);
    }

    public void showHidePenPalette(View view) {
        int visibility = penPalette.getVisibility();
        hidePalettes(view);
        visibility = visibility == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        penPalette.setVisibility(visibility);
    }

    public void hidePalettes(View view) {
        colorPalette.setVisibility(View.INVISIBLE);
        sizePalette.setVisibility(View.INVISIBLE);
        penPalette.setVisibility(View.INVISIBLE);
        textFieldManager.removeEmpty();
    }

    public void restoreButtonSizes() {
        for (View button : mainButtons.values()) {
            button.setScaleX(1f);
            button.setScaleY(1f);
        }
    }

    public void textSelect(View view) {
        hidePalettes(view);
        state = State.TEXT;
        documentCover.setVisibility(View.INVISIBLE);
        documentView.color = mainButtons.get("colorButton").getBackgroundTintList().getDefaultColor();
        restoreButtonSizes();
        mainButtons.get("textButton").setScaleX(1.2f);
        mainButtons.get("textButton").setScaleY(1.2f);
    }

    public void moveSelect(View view) {
        hidePalettes(view);
        documentCover.setVisibility(View.VISIBLE);
        state = State.MOVE;
        restoreButtonSizes();
        mainButtons.get("moveButton").setScaleX(1.2f);
        mainButtons.get("moveButton").setScaleY(1.2f);
    }

    public void penSelect(View view) {
        AppCompatImageButton button = (AppCompatImageButton) view;
        ((AppCompatImageButton) mainButtons.get("penButton")).setImageDrawable(button.getDrawable());
        hidePalettes(view);
        documentCover.setVisibility(View.INVISIBLE);
        state = State.PEN;
        documentView.color = mainButtons.get("colorButton").getBackgroundTintList().getDefaultColor();

        int toolId;
        switch (button.getId()) {
            case R.id.pen:
                toolId = 0;
                break;
            case R.id.marker:
                toolId = 1;
                break;
            default:
                toolId = 2;
                break;
        }

        documentView.strokeMultiplier = strokeMultipliers[toolId];
        documentView.alpha = penAlphas[toolId];

        restoreButtonSizes();
        mainButtons.get("penButton").setScaleX(1.2f);
        mainButtons.get("penButton").setScaleY(1.2f);
    }

    public void eraserSelect(View view) {
        AppCompatImageButton button = (AppCompatImageButton) view;
        hidePalettes(view);
        documentCover.setVisibility(View.INVISIBLE);
        state = State.ERASE;

        restoreButtonSizes();
        mainButtons.get("eraserButton").setScaleX(1.2f);
        mainButtons.get("eraserButton").setScaleY(1.2f);
    }

    public void colorSelect(View view) {
        mainButtons.get("colorButton").setBackgroundTintList(view.getBackgroundTintList());
        documentView.color = mainButtons.get("colorButton").getBackgroundTintList().getDefaultColor();
        textFieldManager.color = documentView.color;
        hidePalettes(view);
    }

    public void sizeSelect(View view) {
        TextView textView = (TextView) view;
        ((AppCompatButton) mainButtons.get("sizeButton")).setText(textView.getText());
        documentView.strokeWidth = Integer.parseInt((String) textView.getText());
        textFieldManager.textSize = documentView.strokeWidth * 2f;
        hidePalettes(view);
    }

    public void undoMove(View view) {
        documentView.removeRecent();
    }

    @Override
    public void onBackPressed() {
        String serialized = Serializer.serializeDocument(MenuActivity.workDocName, documentView, documentCover, textFieldManager, true);
        saveDocument(MainActivity.app, MenuActivity.workDocName, serialized, false);
    }

    public void quitActivity() {
        super.onBackPressed();
    }

    public void saveDocument(App app, String documentName, String serialized, boolean createNew) {
        User appUser = app.currentUser();
        MongoClient mongoClient = appUser.getMongoClient("mongodb-atlas");
        MongoDatabase db = mongoClient.getDatabase("NoteDatabase");
        MongoCollection<Document> collection = db.getCollection("NoteCollection");

        Document userDoc = new Document("userid", appUser.getId());
        userDoc.append("name", documentName);
        userDoc.append("document", serialized);

        if (!createNew) {
            Document filter = new Document("userid", appUser.getId());
            filter.append("name", documentName);

            collection.findOneAndReplace(filter, userDoc).getAsync(task -> {
                if (task.isSuccess()) {
                    Log.d("INSERT DOCUMENT", appUser.getId() + " " + documentName);
                } else {
                    Log.d("INSERT DOCUMENT", task.getError().toString());
                }
                if (!createNew)
                    quitActivity();
            });
        }
        else {
            collection.insertOne(userDoc).getAsync(task -> {
                if (task.isSuccess()) {
                    Log.d("INSERT DOCUMENT", appUser.getId() + " " + documentName);
                } else {
                    Log.d("INSERT DOCUMENT", task.getError().toString());
                }
            });
        }
    }
}