package com.setokfb.cleanvid;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/** Simpan video hasil ke Galeri via MediaStore (tanpa permission di API 29+). */
public class Saver {

    public static Uri saveToGallery(Context ctx, String filePath) {
        try {
            File src = new File(filePath);
            String name = "CLEANVID_" + System.currentTimeMillis() + ".mp4";

            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Video.Media.DISPLAY_NAME, name);
            cv.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            cv.put(MediaStore.Video.Media.RELATIVE_PATH,
                   Environment.DIRECTORY_MOVIES + "/CleanVid");

            Uri uri = ctx.getContentResolver().insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) return null;

            try (FileInputStream in = new FileInputStream(src);
                 OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            src.delete(); // bersihkan cache
            return uri;
        } catch (Exception e) {
            return null;
        }
    }
}
