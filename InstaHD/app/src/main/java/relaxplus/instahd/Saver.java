package relaxplus.instahd;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

public final class Saver {
    public static void save(Context ctx, final String file, final String link, Activity activity) {
        try {
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(link);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(file);
            request.setDescription("Saving...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file + ".jpg");
            dm.enqueue(request);

            SnackbarManager.show(
                    Snackbar.with(ctx)
                            .text("Saving...")
                            .textColor(Color.rgb(0xFA, 0xFA, 0xFA))
                            .textTypeface(MainActivity.tfRobotoCondensed)
                            .color(Color.rgb(0x33, 0x33, 0x33)), activity);

        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }
}
