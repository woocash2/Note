package com.note.docstools.util;

import java.io.Serializable;

public class TextInfo implements Serializable {
    public float x;
    public float y;
    public float realX;
    public float realY;
    public int color;
    public float size;
    public String text;

    public TextInfo(float xx, float yy, float rx, float ry, int c, float s, String t) {
        x = xx;
        y = yy;
        realX = rx;
        realY = ry;
        color = c;
        size = s;
        text = t;
    }
}
