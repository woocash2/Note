package com.note;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.note.docstools.DocumentInfo;
import com.note.docstools.util.JPEGConverter;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveUploader {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private Drive drive;
    private Context context;


    public DriveUploader(Drive d, Context ctx) {
        drive = d;
        context = ctx;
    }

    public Task<String> createFile(String fileName, DocumentInfo documentInfo) {
        return Tasks.call(executor, () -> {
            File file = JPEGConverter.saveAsJPEG(documentInfo, fileName, context);

            com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File();
            metaData.setName(fileName);
            FileContent content = new FileContent("image/jpeg", file);

            com.google.api.services.drive.model.File upFile = null;

            try {
                upFile = drive.files().create(metaData, content).execute();
            }
            catch (Exception e) {
                Log.e("DRIVE", "createFile: FAILED");
                e.printStackTrace();
            }

            if (upFile == null)
                Log.e("DRIVE", "createFile: FILE IS NULL");

            return upFile.getId();
        });
    }
}
