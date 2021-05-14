package com.note.docstools.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.text.TextPaint;
import android.widget.EditText;

import com.note.NoteActivity;
import com.note.docstools.DocumentInfo;
import com.note.docstools.DocumentView;
import com.note.docstools.TextFieldManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public interface JPEGConverter {

    public static void saveAsJPEG(DocumentInfo documentInfo, String fileName, Context context) {

        int height = documentInfo.height;
        int width = documentInfo.width;

        ArrayList<FingerPath> fps = Serializer.infoToFingerPaths(documentInfo.pathInfos);
        ArrayList<TextWithRealCoords> txts = Serializer.infoToTextWithRealCoords(documentInfo.textInfos, context);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.save();
        canvas.drawColor(Color.WHITE);

        for (FingerPath fp : fps) {
            paint.setColor(fp.color);
            paint.setStrokeWidth(fp.strokeWidth * fp.strokeMultiplier);
            paint.setAlpha(fp.alpha);
            paint.setMaskFilter(null);
            canvas.drawPath(fp.path, paint);
        }

        for (TextWithRealCoords twrc : txts) {
            EditText text = twrc.text;

            TextPaint textPaint = new TextPaint();
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize(text.getTextSize());
            textPaint.setColor(text.getCurrentTextColor());
            canvas.drawText(text.getText().toString(), twrc.realX, twrc.realY + NoteActivity.barHeight, textPaint);
        }

        canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        canvas.restore();

        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, fileName + ".jpeg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
