package xjonx.permlist.util;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

@SuppressWarnings("unused")
public class ThemeUtil {
    public static final @NonNull String TAG = ThemeUtil.class.getName();

    public static final @NonNull String KEY_THEME = "theme";
    public static final @NonNull String THEME_AUTO = "auto";
    public static final @NonNull String THEME_LIGHT = "light";
    public static final @NonNull String THEME_DARK = "dark";

    public static void applyTheme(@NonNull String theme) {
        switch (theme) {
            case THEME_LIGHT: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            }
            case THEME_DARK: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            }
            case THEME_AUTO:
            default: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
            }
        }
    }
}
