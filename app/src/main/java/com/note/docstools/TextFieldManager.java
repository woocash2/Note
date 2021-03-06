package com.note.docstools;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.note.docstools.util.TextWithRealCoords;

import java.io.Serializable;
import java.nio.channels.FileLock;
import java.util.ArrayList;

public class TextFieldManager implements Serializable {

    public float textSize = 20f;
    public int color = Color.BLACK;

    private ConstraintLayout mainLayout;
    public ArrayList<TextWithRealCoords> texts = new ArrayList<>();

    private float initialTextTranslationX = 20f;
    private float initialTextTranslationY = 50f;
    private DocumentView documentView;

    public TextFieldManager(ConstraintLayout layout, DocumentView view) {
        mainLayout = layout;
        documentView = view;
    }

    public void addTextField(float a, float b, float realA, float realB) {
        EditText text = new EditText(mainLayout.getContext());
        text.setBackground(null);
        mainLayout.addView(text, 2);
        text.setX(a - initialTextTranslationX);
        text.setY(b - initialTextTranslationY);
        text.setTextSize(textSize);
        Log.d("TRUE TEXT SIZE", Float.toString(textSize) + " " + Float.toString( text.getTextSize()));
        text.setTextColor(color);

        TextWithRealCoords twrc = new TextWithRealCoords(text, realA, realB, textSize);
        texts.add(twrc);
        documentView.recent.push(twrc);

        text.requestFocus();
        ((InputMethodManager) text.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).
                showSoftInput(text, InputMethodManager.SHOW_IMPLICIT);
    }

    public void displaceTexts(float dx, float dy) {
        for (TextWithRealCoords twrc : texts) {
            twrc.text.setX(twrc.text.getX() + dx);
            twrc.text.setY(twrc.text.getY() + dy);
        }
    }

    public void removeEmpty() {
        ArrayList<TextWithRealCoords> toRemove = new ArrayList<>();
        for (TextWithRealCoords twrc : texts) {
            if (twrc.text.getText().toString().equals(""))
                toRemove.add(twrc);
        }
        for (TextWithRealCoords twrc : toRemove)
            texts.remove(twrc);
        documentView.recalculateMaxXY();
    }

    public Pair<Integer, Integer> maxCoords() {
        int maxX = 0;
        int maxY = 0;
        for (TextWithRealCoords twrc : texts) {
            maxX = Math.max((int)twrc.realX, maxX);
            maxY = Math.max((int)twrc.realY, maxY);
        }
        return new Pair<>(maxX, maxY);
    }

    public void removeText(TextWithRealCoords text) {
        texts.remove(text);
        mainLayout.removeView(text.text);
    }

    public void refresh() {
        for (TextWithRealCoords twrc : texts) {
            mainLayout.removeView(twrc.text);
            mainLayout.addView(twrc.text, 2);
        }
    }
}
