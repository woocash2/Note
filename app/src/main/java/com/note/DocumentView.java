package com.note;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Stack;

public class DocumentView extends View {
    
    float x, y;
    public FingerPath path;
    public int strokeWidth = 10;
    public int strokeMultiplier = 1;
    public int alpha = 255;
    public int color = Color.BLACK;
    private Paint paint;

    public ArrayList<FingerPath> paths = new ArrayList<>();

    private Bitmap bitmap;
    private Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    private Canvas canvas;

    private int preferredDistToEdge = 1200;
    private int maxPointX = 0;
    private int maxPointY = 0;
    private int initialWidth = 1800;
    private int initialHeight = 3000;

    private TextFieldManager textFieldManager;
    private DocumentCoverView coverView;
    public Stack<Object> recent = new Stack<>();

    public DocumentView(Context context) {
        this(context, null);
    }

    public DocumentView(Context context, AttributeSet attrSet) {
        super(context, attrSet);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void init(DisplayMetrics metrics, TextFieldManager manager, DocumentCoverView cover) {
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

        textFieldManager = manager;
        coverView = cover;
    }

    public void pathStart(float a, float b) {
        path = new FingerPath(color, strokeWidth, new Path());
        path.alpha = alpha;
        path.strokeMultiplier = strokeMultiplier;
        paths.add(path);
        recent.push(path);
        path.path.reset();
        path.path.moveTo(a, b);
        x = a;
        y = b;
        path.points.add(new Pair<>(x, y));
        maxPointX = Math.max(maxPointX, (int)x);
        maxPointY = Math.max(maxPointY, (int)y);
    }

    public void pathMove(float a, float b) {
        path.path.quadTo(x, y, (x + a) / 2, (y + b) / 2);
        x = a;
        y = b;
        path.points.add(new Pair<>(x, y));
        maxPointX = Math.max(maxPointX, (int)x);
        maxPointY = Math.max(maxPointY, (int)y);
    }

    public void pathUp() {
        path.path.lineTo(x, y);
        path.points.add(new Pair<>(x, y));
        reshapeDocument();
    }

    public void strokeEraserTouch(float a, float b) {
        ArrayList<FingerPath> toRemove = new ArrayList<>();

        for (FingerPath fp : paths) {
            if (fp.closeEnough(a, b)) {
                toRemove.add(fp);
            }
        }

        for (FingerPath fp : toRemove)
            paths.remove(fp);
    }

    public void reshapeDocument() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.width = Math.max(maxPointX + preferredDistToEdge, initialWidth);
        params.height = Math.max(maxPointY + preferredDistToEdge, initialHeight);
        setLayoutParams(params);
    }

    public void recalculateMaxXY() {
        maxPointX = 0;
        maxPointY = 0;
        Pair<Integer, Integer> textsMaxCoords = textFieldManager.maxCoords();
        maxPointX = Math.max(maxPointX, textsMaxCoords.first);
        maxPointY = Math.max(maxPointY, textsMaxCoords.second);

        for (FingerPath fp : paths) {
            Pair<Integer, Integer> pathsMaxCoords = fp.maxCoords();
            maxPointX = Math.max(maxPointX, pathsMaxCoords.first);
            maxPointY = Math.max(maxPointY, pathsMaxCoords.second);
        }

        reshapeDocument();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (NoteActivity.state == NoteActivity.State.PEN) {
            float a = event.getX();
            float b = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                pathStart(a, b);
            else if (event.getAction() == MotionEvent.ACTION_MOVE)
                pathMove(a, b);
            else if (event.getAction() == MotionEvent.ACTION_UP)
                pathUp();
            invalidate();
            draw(canvas);
        }
        else if (NoteActivity.state == NoteActivity.State.TEXT) {
            float a = event.getRawX();
            float b = event.getRawY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                maxPointX = Math.max(maxPointX, (int)event.getX());
                maxPointY = Math.max(maxPointY, (int)event.getY());
                textFieldManager.addTextField(a, b, event.getX(), event.getY());
                reshapeDocument();
                invalidate();
                requestLayout();
                forceLayout();
                draw(canvas);

                coverView.srcX = a;
                coverView.srcY = b;
                coverView.displacementX = -a + 60;
                coverView.displacementY = -b + 240;
                coverView.displaceDocument();
            }
            invalidate();
            draw(canvas);
        }
        else if (NoteActivity.state == NoteActivity.State.ERASE) {
            float a = event.getX();
            float b = event.getY();
            strokeEraserTouch(a, b);

            if (event.getAction() == MotionEvent.ACTION_UP) {
                recalculateMaxXY();
            }
            invalidate();
            draw(canvas);
        }
        return true;
    }

    public void removeRecent() {
        if (recent.empty())
            return;

        Object view;

        while (true) {
            view = recent.pop();
            if (recent.empty())
                return;
            if (paths.contains((FingerPath) view) || textFieldManager.texts.contains((TextWithRealCoords)view))
                break;
        }

        if (view.getClass().equals(FingerPath.class)) {
            paths.remove(view);
            invalidate();
            draw(canvas);
        }
        else {
            textFieldManager.removeText((TextWithRealCoords) view);
        }
        recalculateMaxXY();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.drawColor(Color.WHITE);

        for (FingerPath fp : paths) {
            paint.setColor(fp.color);
            paint.setStrokeWidth(fp.strokeWidth * fp.strokeMultiplier);
            paint.setAlpha(fp.alpha);
            paint.setMaskFilter(null);
            canvas.drawPath(fp.path, paint);
        }

        canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        canvas.restore();
    }
}
