package com.note.docstools;

import com.note.docstools.util.PathInfo;
import com.note.docstools.util.TextInfo;

import java.io.Serializable;
import java.util.ArrayList;

public class DocumentInfo implements Serializable {

    // a container for all document info

    public String name;
    public ArrayList<PathInfo> pathInfos;
    public ArrayList<TextInfo> textInfos;
    public int height;
    public int width;

    public DocumentInfo(String n, ArrayList<PathInfo> pin, ArrayList<TextInfo> tin, int h, int w) {
        name = n;
        pathInfos = pin;
        textInfos = tin;
        height = h;
        width = w;
    }
}
