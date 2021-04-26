package com.note;

import android.widget.EditText;

public class TextWithRealCoords {
    public EditText text;
    public float realX;
    public float realY;

    public TextWithRealCoords(EditText et, float x, float y) {
        text = et;
        realX = x;
        realY = y;
    }
}
