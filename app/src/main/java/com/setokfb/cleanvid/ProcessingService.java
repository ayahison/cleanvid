package com.setokfb.cleanvid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

/**
 * Foreground Service supaya encode ffmpeg tidak dibunuh Android
 * saat app di-minimize / layar mati.
 */
public class ProcessingService extends Service {

    private static final String CH_ID = "cleanvid_proc";
    public static final String EXTRA_URI = "uri";
    public static final String ACTION_DONE = "com.setokfb.cleanvid.DONE";
    public static final String ACTION_FAIL = "com.setokfb.cleanvid.FAIL";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH_ID, "Processing",
                NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(ch);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String uriStr = intent != null ? intent.getStringExtra(EXTRA_URI) : null;
        if (uriStr == null) { stopSelf(); return START_NOT_STICKY; }

        Notification n = null;
        if (Build.VERSION.SDK_INT >= 29) {
            n = new Notification.Builder(this, CH_ID)
                    .setContentTitle("CleanVid")
                    .setContentText("Memproses video...")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .build();
        } else {
            n = new Notification.Builder(this)
                    .setContentTitle("CleanVid")
                    .setContentText("Memproses video...")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .build();
        }
        startForeground(1, n);

        Uri uri = Uri.parse(uriStr);
        new Thread(() -> {
            try {
                String inPath = copyToCache(uri);
                notify("Re-encode bersih...");
                String outPath = getCacheDir().getAbsolutePath()
                        + "/clean_" + System.currentTimeMillis() + ".mp4";

                Cleaner.clean(inPath, outPath,
                    (msg) -> notify(msg));

                notify("Menyimpan ke Galeri...");
                Uri saved = Saver.saveToGallery(this, outPath);

                Intent done = new Intent(saved != null ? ACTION_DONE : ACTION_FAIL);
                done.putExtra("path", saved != null ? saved.toString() : "gagal simpan");
                sendBroadcast(done);
            } catch (Exception e) {
                Intent fail = new Intent(ACTION_FAIL);
                fail.putExtra("path", e.getMessage());
                sendBroadcast(fail);
            } finally {
                stopSelf();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void notify(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        Notification n;
        if (Build.VERSION.SDK_INT >= 29) {
            n = new Notification.Builder(this, CH_ID)
                    .setContentTitle("CleanVid")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .build();
        } else {
            n = new Notification.Builder(this)
                    .setContentTitle("CleanVid")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .build();
        }
        nm.notify(1, n);
    }

    private String copyToCache(Uri src) throws Exception {
        String name = "input_" + System.currentTimeMillis() + ".mp4";
        java.io.File dst = new java.io.File(getCacheDir(), name);
        try (java.io.InputStream in = getContentResolver().openInputStream(src);
             java.io.OutputStream out = new java.io.FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        return dst.getAbsolutePath();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
