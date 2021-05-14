package com.note.docstools.util;

import android.content.Context;
import android.graphics.Path;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.EditText;

import com.note.MenuActivity;
import com.note.NoteActivity;
import com.note.docstools.DocumentCoverView;
import com.note.docstools.DocumentInfo;
import com.note.docstools.DocumentView;
import com.note.docstools.TextFieldManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public interface Serializer {

    public static PathInfo makePathInfo(FingerPath fp) {
        PathInfo info = new PathInfo(fp.color, fp.strokeWidth, fp.points, fp.alpha, fp.strokeMultiplier);
        return info;
    }

    public static TextInfo makeTextInfo(TextWithRealCoords txt) {
        TextInfo info = new TextInfo(txt.text.getX(), txt.text.getY(), txt.realX, txt.realY, txt.text.getCurrentTextColor(), txt.size, txt.text.getText().toString());
        return info;
    }

    public static FingerPath makeFingerPath(PathInfo info) {
        Path path = new Path();
        FingerPath fp = new FingerPath(info.color, info.strokeWidth, path);

        Pair<Float, Float> prev = new Pair<>(0f, 0f);
        for (int i = 0; i < info.pointsX.size(); i++) {
            float x = info.pointsX.get(i);
            float y = info.pointsY.get(i);
            fp.points.add(new Pair<>(x, y));

            if (i == 0) {
                path.moveTo(x, y);
                prev = new Pair<>(x, y);
            }
            else if (i == info.pointsX.size() - 1) {
                path.lineTo(x, y);
            }
            else {
                path.quadTo(prev.first, prev.second, (x + prev.first) / 2f, (y + prev.second) / 2f);
                prev = new Pair<>(x, y);
            }
        }

        fp.alpha = info.alpha;
        fp.strokeMultiplier = info.strokeMultiplier;

        return fp;
    }

    public static TextWithRealCoords makeTextWithRealCoords(TextInfo info, Context context) {
        EditText editText = new EditText(context);
        editText.setTextSize(info.size);
        editText.setText(info.text);
        editText.setTextColor(info.color);
        editText.setX(info.x);
        editText.setY(info.y);

        return new TextWithRealCoords(editText, info.realX, info.realY, info.size);
    }

    public static String serializeDocument(String name, DocumentView documentView, DocumentCoverView coverView, TextFieldManager textFieldManager, boolean move) {
        if (move) {
            coverView.displacementX = -documentView.getX();
            coverView.displacementY = -documentView.getY() + NoteActivity.barHeight;
            Log.d("HEIGHT", Integer.toString(NoteActivity.barHeight));
            coverView.displaceDocument();
        }

        ArrayList<PathInfo> pathInfos = new ArrayList<>();
        ArrayList<TextInfo> textInfos = new ArrayList<>();

        for (FingerPath fp : documentView.paths) {
            pathInfos.add(makePathInfo(fp));
        }
        for (TextWithRealCoords twrc : textFieldManager.texts) {
            textInfos.add(makeTextInfo(twrc));
        }

        DocumentInfo documentInfo = new DocumentInfo(name, pathInfos, textInfos, documentView.getHeight(), documentView.getWidth());
        String serialized = "";

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(documentInfo);
            so.flush();
            serialized = new String(Base64.encode(bo.toByteArray(), 0));

            Log.d("SERIALIZATION", "serialized document: " + serialized);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return serialized;
    }

    public static DocumentInfo deserializeDocument(String serialized) {

        try {
            byte b[] = Base64.decode(serialized.getBytes(), 0);
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            DocumentInfo doc = (DocumentInfo) si.readObject();
            Log.d("DESERIALIZE SUCCESS", "SUCCESS");
            return doc;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<FingerPath> infoToFingerPaths(ArrayList<PathInfo> infos) {
        ArrayList<FingerPath> fpaths = new ArrayList<>();
        for (PathInfo info : infos) {
            fpaths.add(makeFingerPath(info));
            Log.d("POINTS ON PATH", "X: " + info.pointsX.toString() + " Y: " + info.pointsY);
        }
        return fpaths;
    }

    public static ArrayList<TextWithRealCoords> infoToTextWithRealCoords(ArrayList<TextInfo> infos, Context context) {
        ArrayList<TextWithRealCoords> texts = new ArrayList<>();
        for (TextInfo info : infos) {
            texts.add(makeTextWithRealCoords(info, context));
        }
        return texts;
    }

    public static void injectDataToDocument(DocumentView documentView, TextFieldManager textFieldManager, DocumentInfo documentInfo, Context context) {
        ArrayList<FingerPath> fpaths = new ArrayList<>();
        ArrayList<TextWithRealCoords> texts = new ArrayList<>();

        documentView.paths = infoToFingerPaths(documentInfo.pathInfos);
        textFieldManager.texts = infoToTextWithRealCoords(documentInfo.textInfos, context);

        documentView.recalculateMaxXY();
        documentView.invalidate();
        documentView.draw(documentView.canvas);
        textFieldManager.refresh();
    }
}
