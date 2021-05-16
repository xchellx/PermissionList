package xjonx.permlist.util;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;

import xjonx.permlist.R;

@SuppressWarnings("unused")
public class ToastUtil {
    public static final @NonNull String TAG = ToastUtil.class.getName();

    /**
     * Show a Toast for a short amount of time with the specified message with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param msg The message to show within the Toast.
     */
    public static void showToast(@NonNull Context ctx, @Nullable String msg) {
        showToast(ctx, msg, false, ColorUtil.Color.TRANSPARENT);
    }

    /**
     * Show a Toast for a short amount of time with the specified message with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param id A resource ID for a string
     */
    public static void showToast(@NonNull Context ctx, int id) {
        showToast(ctx, id, false, ColorUtil.Color.TRANSPARENT);
    }

    /**
     * Show a Toast for a short amount of time with the specified message in the specified color with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param msg The message to show within the Toast.
     * @param clr The color to show the Toast in. Color resource int is not supported here, use {@link Context#getColor} instead.
     */
    public static void showToast(@NonNull Context ctx, @Nullable String msg, @ColorInt int clr) {
        showToast(ctx, msg, false, clr);
    }

    /**
     * Show a Toast for a short amount of time with the specified message in the specified color with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param id A resource ID for a string
     * @param clr The color to show the Toast in. Color resource int is not supported here, use {@link Context#getColor} instead.
     */
    public static void showToast(@NonNull Context ctx, int id, @ColorInt int clr) {
        showToast(ctx, id, false, clr);
    }

    /**
     * Show a Toast for a short or long amount of time with the specified message with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param msg The message to show within the Toast.
     * @param lng The length to show the Toast for. True for long a amount of time, false for a short amount of time
     */
    public static void showToast(@NonNull Context ctx, @Nullable String msg, boolean lng) {
        showToast(ctx, msg, lng, ColorUtil.Color.TRANSPARENT);
    }

    /**
     * Show a Toast for a short or long amount of time with the specified message with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param id A resource ID for a string
     * @param lng The length to show the Toast for. True for long a amount of time, false for a short amount of time
     */
    public static void showToast(@NonNull Context ctx, int id, boolean lng) {
        showToast(ctx, id, lng, ColorUtil.Color.TRANSPARENT);
    }

    /**
     * Show a Toast for a short or long amount of time with the specified message in the specified color with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param id A resource ID for a string
     * @param lng The length to show the Toast for. True for long a amount of time, false for a short amount of time
     * @param clr The color to show the Toast in. Color resource int is not supported here, use {@link Context#getColor} instead.
     */
    public static void showToast(@NonNull Context ctx, int id, boolean lng, @ColorInt int clr) {
        showToast(ctx, ctx.getString(id), lng, clr);
    }

    /**
     * Show a Toast for a short or long amount of time with the specified message in the specified color with Context.
     *
     * @param ctx The Context to show the Toast from
     * @param msg The message to show within the Toast.
     * @param lng The length to show the Toast for. True for long a amount of time, false for a short amount of time
     * @param clr The color to show the Toast in. Color resource int is not supported here, use {@link Context#getColor} instead.
     */
    @SuppressWarnings("deprecation")
    public static void showToast(@NonNull Context ctx, @Nullable String msg, boolean lng, @ColorInt int clr) {
        Toast toast;
        View toastView;
        TextView toastTextView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 disallows toast customization; fallback to custom layout
            toast = new Toast(ctx);
            LayoutInflater inflate = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //noinspection InflateParams
            toastView = inflate.inflate(R.layout.transient_notification, null);
            toastTextView = toastView.findViewById(R.id.message);
        } else {
            // Use getView
            //noinspection ShowToast
            toast = Toast.makeText(ctx, "", Toast.LENGTH_SHORT);
            toastView = toast.getView();
            toastTextView = toastView.findViewById(android.R.id.message);
        }
        if (clr != ColorUtil.Color.TRANSPARENT) {
            toastView.getBackground().setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(clr, BlendModeCompat.SRC_IN));
            toastTextView.setTextColor(ColorUtil.getContrastColorW3C(clr));
        }
        toastTextView.setText((msg == null) ? "null" : msg);
        toast.setView(toastView);
        toast.setDuration(lng ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }
}
