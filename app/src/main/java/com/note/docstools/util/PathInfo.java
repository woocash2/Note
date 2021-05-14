package com.note.docstools.util;

import android.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;

public class PathInfo implements Serializable {
    public int color;
    public int strokeWidth;
    public ArrayList<Float> pointsX = new ArrayList<>();
    public ArrayList<Float> pointsY = new ArrayList<>();
    public int alpha;
    public int strokeMultiplier;

    public PathInfo(int c, int s, ArrayList<Pair<Float, Float>> p, int a, int sm) {
        color = c;
        strokeWidth = s;
        for (Pair<Float, Float> pt : p) {
            pointsX.add(pt.first);
            pointsY.add(pt.second);
        }
        alpha = a;
        strokeMultiplier = sm;
    }
}
