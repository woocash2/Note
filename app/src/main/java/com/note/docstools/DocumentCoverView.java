package com.note.docstools;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.note.NoteActivity;

import java.io.Serializable;

public class DocumentCoverView extends View implements Serializable {

    public float displacementX = 0f;
    public float displacementY = 0f;
    public float srcX = 0f;
    public float srcY = 0f;

    int maxAcceptableX = 300;
    int maxAcceptableY = 300;
    int beginOfDoc = 180;
    int minAcceptableX;
    int minAcceptableY;

    public Point windowSize = new Point();
    public DocumentView documentView;
    private TextFieldManager textFieldManager;

    public DocumentCoverView(Context context) {
        super(context);
    }

    public DocumentCoverView(Context context, AttributeSet attrset) {
        super(context, attrset);
    }

    public void init(Point winsize, TextFieldManager manager) {
        windowSize.x = winsize.x;
        windowSize.y = winsize.y - 60;
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.height = winsize.y - 60;
        setLayoutParams(params);

        textFieldManager = manager;
    }

    public void pathStart(float a, float b) {
        srcX = a;
        srcY = b;
    }

    public void pathMove(float a, float b) {
        displacementX = a - srcX;
        displacementY = b - srcY;
        srcX = a;
        srcY = b;
        displaceDocument();
    }

    public void displaceDocument() {

        minAcceptableX = -documentView.getWidth() - maxAcceptableX + windowSize.x;
        minAcceptableY = -documentView.getHeight() - maxAcceptableY + windowSize.y;

        float newX = documentView.getX() + displacementX;
        newX = Math.max(newX, minAcceptableX);
        newX = Math.min(newX, maxAcceptableX);

        float newY = documentView.getY() + displacementY;
        newY = Math.max(newY, minAcceptableY);
        newY = Math.min(newY, maxAcceptableY + beginOfDoc);

        float finalDisplacementX = newX - documentView.getX();
        float finalDisplacementY = newY - documentView.getY();
        documentView.setY(newY);
        documentView.setX(newX);
        textFieldManager.displaceTexts(finalDisplacementX, finalDisplacementY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (NoteActivity.state == NoteActivity.State.MOVE) {
            float a = event.getX();
            float b = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                pathStart(a, b);
            else if (event.getAction() == MotionEvent.ACTION_MOVE)
                pathMove(a, b);
            invalidate();
        }
        return true;
    }
}
