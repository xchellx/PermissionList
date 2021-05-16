package xjonx.permlist.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings({"unused", "JavadocReference"})
public class FileUtil {
    public static final @NonNull String TAG = FileUtil.class.getName();

    /**
     * This is a request code that is used by {@link FileUtil#createSAFFile}.
     */
    public static final int REQUESTCODE_CREATESAFFILE = 211;

    /**
     * Create a file using the Storage Access Framework (SAF). This will create an intent that will open a file chooser.
     * When the user creates the file from that file chooser, {@link Activity#onActivityResult(int, int, Intent)} will be
     * called with a request code of {@link FileUtil#REQUESTCODE_CREATESAFFILE}.
     * @param activity An activity instance, required for calling the create file intent
     */
    public static void createSAFFile(Activity activity) { createSAFFile(activity, -1); }

    /**
     * Create a file using the Storage Access Framework (SAF). This will create an intent that will open a file chooser.
     * When the user creates the file from that file chooser, {@link Activity#onActivityResult(int, int, Intent)} will be
     * called with a request code of {@link FileUtil#REQUESTCODE_CREATESAFFILE} optionally XOR'd with the <code>optReqCode</code> parameter.
     * @param activity An activity instance, required for calling the create file intent
     * @param optReqCode An optional request code that will be XOR'd with {@link FileUtil#REQUESTCODE_CREATESAFFILE}
     */
    public static void createSAFFile(Activity activity, int optReqCode) { createSAFFile(activity, optReqCode, "", null); }

    /**
     * Create a file using the Storage Access Framework (SAF). This will create an intent that will open a file chooser.
     * When the user creates the file from that file chooser, {@link Activity#onActivityResult(int, int, Intent)} will be
     * called with a request code of {@link FileUtil#REQUESTCODE_CREATESAFFILE}.
     * @param activity An activity instance, required for calling the create file intent
     * @param fileName A file name. Note that the path will be ignored, only the name will be used. This will set the initial filename presented to the user.
     */
    public static void createSAFFile(Activity activity, String fileName) { createSAFFile(activity, -1, fileName); }

    /**
     * Create a file using the Storage Access Framework (SAF). This will create an intent that will open a file chooser.
     * When the user creates the file from that file chooser, {@link Activity#onActivityResult(int, int, Intent)} will be
     * called with a request code of {@link FileUtil#REQUESTCODE_CREATESAFFILE} optionally XOR'd with the <code>optReqCode</code> parameter.
     * @param activity An activity instance, required for calling the create file intent
     * @param optReqCode An optional request code that will be XOR'd with {@link FileUtil#REQUESTCODE_CREATESAFFILE}
     * @param fileName A file name. Note that the path will be ignored, only the name will be used. This will set the initial filename presented to the user.
     */
    public static void createSAFFile(Activity activity, int optReqCode, String fileName) { createSAFFile(activity, optReqCode, fileName, null); }

    /**
     * Create a file using the Storage Access Framework (SAF). This will create an intent that will open a file chooser.
     * When the user creates the file from that file chooser, {@link Activity#onActivityResult(int, int, Intent)} will be
     * called with a request code of {@link FileUtil#REQUESTCODE_CREATESAFFILE}.
     * @param activity An activity instance, required for calling the create file intent
     * @param fileName A file name. Note that the path will be ignored, only the name will be used. This will set the initial filename presented to the user.
     * @param fileType A file MIME type. Defaults to "*&#47;*".
     */
    public static void createSAFFile(Activity activity, String fileName, String fileType) { createSAFFile(activity, -1, fileName, fileType); }

    /**
     * Create a file using the Storage Access Framework (SAF). This will create an intent that will open a file chooser.
     * When the user creates the file from that file chooser, {@link Activity#onActivityResult(int, int, Intent)} will be
     * called with a request code of {@link FileUtil#REQUESTCODE_CREATESAFFILE} optionally XOR'd with the <code>optReqCode</code> parameter.
     * @param activity An activity instance, required for calling the create file intent
     * @param optReqCode An optional request code that will be XOR'd with {@link FileUtil#REQUESTCODE_CREATESAFFILE}
     * @param fileName A file name. Note that the path will be ignored, only the name will be used. This will set the initial filename presented to the user.
     * @param fileType A file MIME type. Defaults to "*&#47;*".
     */
    public static void createSAFFile(Activity activity, int optReqCode, String fileName, @Nullable String fileType) {
        if (activity != null && fileName != null) {
            if (fileType == null || fileType.trim().equals("")) {
                fileType = "*/*";
            }
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
     * Create a file containing the text in parameter <code>data</code> to a temporary file in cache
     * then show menu to share it to other apps. Note that the handling of the activity life cycle
     * and whether the cached file is available is not handled by this method.
     * @param ctx Context, required for getting the cache directory and starting the share intent
     * @param shareTitle Optional title to show on the share panel
     * @param fileName File to create in cache directory and to share. Note that the path is ignored, only the name is accounted for.
     * @param data Data to share, as a string. This should NOT be binary data!
     * @throws IOException when creation of the cache file fails.
     */
    public static void shareCacheFileText(Context ctx, String shareTitle, String fileName,
                                          final String data) throws IOException {
        shareCacheFileText(ctx, shareTitle, new File((fileName == null) ? "" : fileName), data);
    }

    /**
     * Create a file containing the text in parameter <code>data</code> to a temporary file in cache
     * then show menu to share it to other apps. Note that the handling of the activity life cycle
     * and whether the cached file is available is not handled by this method.
     * @param ctx Context, required for getting the cache directory and starting the share intent
     * @param shareTitle Optional title to show on the share panel
     * @param file File to create in cache directory and to share. Note that the path is ignored, only the name is accounted for.
     * @param data Data to share, as a string. This should NOT be binary data!
     * @throws IOException when creation of the cache file fails.
     */
    public static void shareCacheFileText(Context ctx, String shareTitle, File file,
                                          final String data) throws IOException {
        if (ctx != null && shareTitle != null && file != null && data != null) {
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
    }

    /**
     * Format a file name string with a date suffix. The date format will be yyyyMMddHHmmss.SSS
     * @param ctx Context, required for {@link Context#getResources}
     * @param fileName A file name string
     * @param extension A file extension string
     * @return A formatted file name string with a date suffix
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
            return fileName + new SimpleDateFormat("yyyyMMddHHmmss.SSS", locale)
                    .format(System.currentTimeMillis()) + extension;
        } else {
            return "";
        }
    }

    /**
     * Delete a directory. This operation is recursive but is only one level deep. Subfolders are not
     * accounted for.
     * @param dir The directory to delete
     * @throws SecurityException If access to the folder is denied
     */
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
