package xjonx.permlist.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings("unused")
public class FileUtil {
    public static final @NonNull String TAG = FileUtil.class.getName();

    /**
     * TODO: Document this.
     */
    public static final int REQUESTCODE_CREATESAFFILE = 211;

    /**
     * TODO: Document this.
     */
    public static void createSAFFile(Activity activity) { createSAFFile(activity, -1); }

    /**
     * TODO: Document this.
     */
    public static void createSAFFile(Activity activity, int optReqCode) { createSAFFile(activity, optReqCode, "", "*/*"); }

    /**
     * TODO: Document this.
     */
    public static void createSAFFile(Activity activity, String fileName) { createSAFFile(activity, -1, fileName); }

    /**
     * TODO: Document this.
     */
    public static void createSAFFile(Activity activity, int optReqCode, String fileName) { createSAFFile(activity, optReqCode, fileName, "*/*"); }

    /**
     * TODO: Document this.
     */
    public static void createSAFFile(Activity activity, String fileName, String fileType) { createSAFFile(activity, -1, fileName, fileType); }

    /**
     * TODO: Document this.
     */
    public static void createSAFFile(Activity activity, int optReqCode, String fileName, String fileType) {
        if (activity != null && fileName != null && fileType != null) {
            Intent intent = new Intent()
                    .setAction(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(fileType)
                    .putExtra(Intent.EXTRA_TITLE, fileName)
                    .putExtra(Intent.EXTRA_MIME_TYPES, new String[] { fileType });

            // Ensure "Show internal storage" shows up, so file creation is NOT restricted to JUST the Downloads folder
            String DocumentsContract_EXTRA_SHOW_ADVANCED;
            try {
                // There is a constant for this extra but it is hidden. However, it is on the whitelist so it is safe to access directly.
                // See: https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces#list-names
                @SuppressWarnings("JavaReflectionMemberAccess")
                Field EXTRA_SHOW_ADVANCED = DocumentsContract.class.getDeclaredField("EXTRA_SHOW_ADVANCED");
                EXTRA_SHOW_ADVANCED.setAccessible(true);
                DocumentsContract_EXTRA_SHOW_ADVANCED = (String) EXTRA_SHOW_ADVANCED.get(null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                // This firmware does not include this constant, fallback to arbitrary string and hope it works.
                Log.w(FileUtil.class.getName(), "'EXTRA_SHOW_ADVANCED' not found in 'DocumentsContract', falling back to arbitrary string.", e);
                DocumentsContract_EXTRA_SHOW_ADVANCED = "android.content.extra.SHOW_ADVANCED";
            }
            intent.putExtra(DocumentsContract_EXTRA_SHOW_ADVANCED, true);

            activity.startActivityForResult(intent, (optReqCode >= 0) ? (REQUESTCODE_CREATESAFFILE | optReqCode) : REQUESTCODE_CREATESAFFILE);
        }
    }

    /**
     * TODO: Document this.
     */
    public static void shareCacheFileText(Context ctx, String shareTitle, String fileName,
                                          final String data) throws IOException {
        shareCacheFileText(ctx, shareTitle, new File(fileName), data);
    }

    /**
     * TODO: Document this.
     */
    public static void shareCacheFileText(Context ctx, String shareTitle, File file,
                                          final String data) throws IOException {
        File shareDir = ctx.getCacheDir();
        File shareFile = new File(shareDir, file.getName());
        if (shareFile.createNewFile() || shareFile.exists()) {
            try (OutputStreamWriter shareStream = new OutputStreamWriter(new FileOutputStream(shareFile, false), StandardCharsets.UTF_8)) {
                shareStream.write(data);
            }
            Uri shareUri = FileProvider.getUriForFile(ctx, "xjonx.permlist.FileProvider", shareFile);
            if (shareUri != null) {
                ctx.startActivity(Intent.createChooser(new Intent()
                                .setAction(Intent.ACTION_SEND)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .setDataAndType(shareUri, ctx.getContentResolver().getType(shareUri))
                                .putExtra(Intent.EXTRA_STREAM, shareUri)
                                .putExtra(Intent.EXTRA_MIME_TYPES, new String[] { ctx.getContentResolver().getType(shareUri) }),
                        shareTitle));
            }
        } else {
            throw new IOException("Failed to create cache file '" + shareFile.getPath() + "'.");
        }
    }

    /**
     * TODO: Document this.
     */
    @SuppressWarnings("deprecation")
    public static @NonNull String getDateFileName(Context ctx, final String fileName, final String extension) {
        if (fileName != null && extension != null) {
            Locale locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locale = ctx.getResources().getConfiguration().getLocales().get(0);
            } else {
                locale = ctx.getResources().getConfiguration().locale;
            }
            return fileName + new SimpleDateFormat("yyyyMMddHHmmss.SSS",
                    locale
            ).format(System.currentTimeMillis()) + extension;
        } else {
            return "";
        }
    }

    public static void deleteDirectory(File dir) throws SecurityException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) { deleteDirectory(f); }
            f.delete();
        }
    }
}
