package com.setokfb.cleanvid;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

/**
 * Re-encode "ala Telegram":
 *  - strip SEMUA metadata (C2PA, encoder info, GPS, timestamps)
 *  - H.264 baseline-ish, bitrate rendah-menengah, GOP standar
 *  - AAC audio re-encode
 *  - tambah sedikit noise halus (mengikis artefak generatif)
 *  - ubah timestamp + sedikit crop 1px agar hash konten bergeser
 */
public class Cleaner {

    public interface LogFn { void log(String s); }

    public static void clean(String in, String out, LogFn logFn) {
        // -map_metadata -1          : buang semua metadata kontainer
        // -fflags +bitexact         : buang encoder version string
        // -vf scale+crop+noise      : geser piksel & noise halus
        // -g 48 -keyint_min 48      : GOP pola umum HP
        // -movflags +faststart      : moov di depan (pola upload normal)
        String cmd =
            "-y -i \"" + in + "\" " +
            "-map_metadata -1 " +
            "-map 0:v:0 -map 0:a:0? " +
            "-c:v libx264 -preset medium -crf 23 " +
            "-vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2,crop=trunc(iw/2)*2-2:trunc(ih/2)*2-2,noise=alls=2:allf=t+u\" " +
            "-pix_fmt yuv420p " +
            "-c:a aac -b:a 128k -ar 44100 " +
            "-g 48 -keyint_min 48 -sc_threshold 0 " +
            "-movflags +faststart " +
            "-fflags +bitexact -flags:v +bitexact -flags:a +bitexact " +
            "\"" + out + "\"";

        logFn.log("FFmpeg: mulai...");
        FFmpegSession session = FFmpegKit.execute(cmd);

        if (ReturnCode.isSuccess(session.getReturnCode())) {
            logFn.log("FFmpeg: sukses");
        } else {
            String fail = session.getFailStackTrace() != null
                ? session.getFailStackTrace() : "code=" + session.getReturnCode();
            logFn.log("FFmpeg GAGAL: " + fail);
            throw new RuntimeException("FFmpeg failed: " + fail);
        }
    }
}
