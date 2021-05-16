package xjonx.permlist.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import xjonx.permlist.BuildConfig;

@SuppressWarnings("unused")
public class DebugUtil {
    public static final @NonNull String TAG = DebugUtil.class.getName();

    /**
     * Get full stack trace from an exception as a string.
     *
     * @param th A throwable exception, error, or any other throwable object
     * @return The full stack trace from the throwable exception, error, or any other throwable object
     */
    public static @NonNull String getStackTrace(@Nullable Throwable th) {
        if (th != null) {
            final Writer result = new StringWriter();

            final PrintWriter printWriter = new PrintWriter(result);
            Throwable cause = th;

            int failSafe = 0, failSafeMax = 20;
            while (cause != null) {
                if (failSafe > failSafeMax) {
                    break;
                } else {
                    cause.printStackTrace(printWriter);
                    cause = cause.getCause();
                }
                failSafe++;
            }
            final String stacktraceAsString = result.toString();
            printWriter.close();

            return stacktraceAsString;
        } else {
            return "";
        }
    }

    /**
     * Check whether the application is a debug build.<br>
     * This method checks for {@link BuildConfig#DEBUG}.
     *
     * @return True if application is a debug build, false if not.
     */
    public static boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }

    /**
     * Check whether the application is debuggable.<br>
     * This method checks for {@link ApplicationInfo#FLAG_DEBUGGABLE}.
     *
     * @return True if application is debuggable, false if not.
     */
    public static boolean isDebuggable(Context ctx) {
        return (0 != (ctx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }

    /**
     * Check whether the application is a debug build and is debuggable.<br>
     * This method checks for {@link BuildConfig#DEBUG} and {@link ApplicationInfo#FLAG_DEBUGGABLE}.
     *
     * @return True if application is a debug build and is debuggable, false if not.
     */
    public static boolean isDebug(Context ctx) {
        return isDebugBuild() && isDebuggable(ctx);
    }

    /**
     * Get the logcat of the current application. This runs the "logcat" utility internally by using
     * {@link Runtime#getRuntime} and {@link Runtime#exec(String)} which may lead to undefined behavior
     * in unaccounted for situations.
     * @return The logcat of the current application, represented as a {@link String}.
     */
    public static @NonNull String getLogcat() {
        StringBuilder log = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v threadtime");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(Integer.toString(android.os.Process.myPid()))) {
                    log.append(line);
                    log.append(System.lineSeparator());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get logcat.", e);
        }
        return log.toString();
    }
}
