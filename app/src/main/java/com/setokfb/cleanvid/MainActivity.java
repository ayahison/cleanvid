package com.setokfb.cleanvid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * CleanVid — pilih video -> re-encode bersih (strip metadata ala Telegram)
 * -> simpan ke Galeri/Movies/CleanVid.
 */
public class MainActivity extends Activity {

    private static final int PICK_VIDEO = 42;

    private TextView log;
    private Button pickBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log = findViewById(R.id.log);
        pickBtn = findViewById(R.id.pick);
        pickBtn.setOnClickListener(v -> openPicker());
        appendLog("CleanVid siap. Pilih video untuk dibersihkan.");
    }

    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("video/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Pilih video"), PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_VIDEO && res == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            appendLog("Video dipilih: " + uri.getLastPathSegment());
            process(uri);
        }
    }

    private void process(Uri src) {
        pickBtn.setEnabled(false);
        appendLog("Menyalin video ke cache...");
        new Thread(() -> {
            try {
                String inPath = copyToCache(src);
                String outPath = getCacheDir().getAbsolutePath() + "/clean_" + System.currentTimeMillis() + ".mp4";

                appendLog("Re-encode bersih (strip metadata + kompresi)...");
                Cleaner.clean(inPath, outPath, (msg) -> runOnUiThread(() -> appendLog(msg)));

                appendLog("Menyimpan ke Galeri...");
                Uri saved = Saver.saveToGallery(this, outPath);
                if (saved != null) {
                    appendLog("✅ SELESAI: " + saved.getLastPathSegment());
                    appendLog("Video bersih ada di Galeri → Movies → CleanVid");
                } else {
                    appendLog("⚠️ Gagal simpan ke galeri. Cek logcat.");
                }
            } catch (Exception e) {
                appendLog("❌ ERROR: " + e.getMessage());
            } finally {
                runOnUiThread(() -> pickBtn.setEnabled(true));
            }
        }).start();
    }

    private String copyToCache(Uri src) throws Exception {
        String name = "input_" + System.currentTimeMillis() + ".mp4";
        java.io.File dst = new java.io.File(getCacheDir(), name);
        try (InputStream in = getContentResolver().openInputStream(src);
             OutputStream out = new java.io.FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                total += n;
            }
            appendLog(String.format("Disalin: %.1f MB", total / 1048576.0));
        }
        return dst.getAbsolutePath();
    }

    private void appendLog(String s) {
        runOnUiThread(() -> log.append(s + "\n"));
    }
}
