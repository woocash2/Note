package com.note;

import android.graphics.Path;
import android.graphics.Point;
import android.util.Pair;

import java.util.ArrayList;

public class FingerPath {
    public int color;
    public int strokeWidth;
    public Path path;
    public float touchDist = 5f;
    public ArrayList<Pair<Float, Float>> points = new ArrayList<>();

    public int alpha = 255;
    public int strokeMultiplier = 1;

    public FingerPath(int col, int width, Path pth) {
        color = col;
        strokeWidth = width;
        path = pth;
    }

    public boolean closeEnough(float a, float b) {
        double mindist = touchDist + 1f;
        for (int i = 0; i < points.size() - 1; i++) {
            Pair<Float, Float> pt1 = points.get(i);
            Pair<Float, Float> pt2 = points.get(i + 1);

            float A = a - pt1.first; // position of point rel one end of line
            float B = b - pt1.second;
            float C = pt2.first - pt1.first; // vector along line
            float D = pt2.second - pt1.second;
            float E = -D; // orthogonal vector
            float F = C;

            if (pt1.equals(pt2)) {
                mindist = Math.min(Math.sqrt(A*A + B*B), mindist);
                System.out.println(Math.sqrt(A*A + B*B));
                continue;
            }

            // check if 'between' pt1 and pt2
            float G = a - pt2.first;
            float H = b - pt2.second;
            if ((E*B-F*A)*(E*H-F*G) > 0)
                continue;

            float dot = A * E + B * F;
            float len_sq = E * E + F * F;

            System.out.println((float) Math.abs(dot) / Math.sqrt(len_sq));

            mindist = Math.min((float) Math.abs(dot) / Math.sqrt(len_sq), mindist);
        }
        return mindist < touchDist;
    }

    public Pair<Integer, Integer> maxCoords() {
        int maxX = 0;
        int maxY = 0;

        for (Pair<Float, Float> pt : points) {
            maxX = Math.max(pt.first.intValue(), maxX);
            maxY = Math.max(pt.second.intValue(), maxY);
        }

        return new Pair<>(maxX, maxY);
    }
}
