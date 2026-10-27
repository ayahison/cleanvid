package com.setokfb.cleanvid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int PICK_VIDEO = 42;

    private TextView log;
    private Button pickBtn;
    private Uri pendingUri;

    private final BroadcastReceiver recv = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (ProcessingService.ACTION_DONE.equals(i.getAction())) {
                appendLog("✅ SELESAI: " + i.getStringExtra("path"));
                appendLog("Hasil di Galeri → Movies → CleanVid");
                pickBtn.setEnabled(true);
            } else if (ProcessingService.ACTION_FAIL.equals(i.getAction())) {
                appendLog("❌ GAGAL: " + i.getStringExtra("path"));
                pickBtn.setEnabled(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log = findViewById(R.id.log);
        pickBtn = findViewById(R.id.pick);
        pickBtn.setOnClickListener(v -> openPicker());

        IntentFilter f = new IntentFilter();
        f.addAction(ProcessingService.ACTION_DONE);
        f.addAction(ProcessingService.ACTION_FAIL);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(recv, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(recv, f);
        }

        appendLog("CleanVid siap. Pilih video — proses aman walau app diminimize.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(recv); } catch (Exception ignored) {}
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
            pendingUri = data.getData();
            appendLog("Video dipilih: " + pendingUri.getLastPathSegment());
            startProcessing();
        }
    }

    private void startProcessing() {
        pickBtn.setEnabled(false);
        appendLog("Memulai Foreground Service (anti-kill)...");

        Intent svc = new Intent(this, ProcessingService.class);
        svc.putExtra(ProcessingService.EXTRA_URI, pendingUri.toString());
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        appendLog("Proses jalan di background — app boleh di-minimize.");
    }

    private void appendLog(String s) {
        runOnUiThread(() -> log.append(s + "\n"));
    }
}
