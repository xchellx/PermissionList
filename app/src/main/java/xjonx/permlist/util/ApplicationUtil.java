package xjonx.permlist.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import xjonx.permlist.R;

@SuppressWarnings("unused")
public class ApplicationUtil {
    public static final @NonNull String TAG = ApplicationUtil.class.getName();

    /**
     * Get the {@link SharedPreferences} object for this current application.
     * @param ctx Context, required for {@link Context#getSharedPreferences(String, int)}
     * @return The {@link SharedPreferences} object for this current application.
     */
    public static SharedPreferences getApplicationPreferences(Context ctx) {
        return ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
    }
}
