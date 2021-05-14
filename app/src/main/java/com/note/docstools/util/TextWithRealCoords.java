package com.note.docstools.util;

import android.widget.EditText;

import java.io.Serializable;

public class TextWithRealCoords implements Serializable {
    public EditText text;
    public float realX;
    public float realY;
    public float size;

    public TextWithRealCoords(EditText et, float x, float y, float s) {
        text = et;
        realX = x;
        realY = y;
        size = s;
    }
}
