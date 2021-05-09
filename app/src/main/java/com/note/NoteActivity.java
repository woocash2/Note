package com.note;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import java.util.Map;
import java.util.TreeMap;

public class NoteActivity extends AppCompatActivity {

    public enum State {TEXT, PEN, ERASE, MOVE};
    public int[] strokeMultipliers = {1, 2, 4};
    public int[] penAlphas = {255, 153, 63};
    public static State state = State.MOVE;

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
}