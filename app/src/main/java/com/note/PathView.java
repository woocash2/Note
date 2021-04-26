package com.note;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public interface PathView  {

    public void pathStart(float a, float b);
    public void pathMove(float a, float b);
    public void pathUp();
}
