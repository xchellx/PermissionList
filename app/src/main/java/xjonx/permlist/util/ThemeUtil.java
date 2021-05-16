package xjonx.permlist.util;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

@SuppressWarnings({"unused", "JavadocReference"})
public class ThemeUtil {
    public static final @NonNull String TAG = ThemeUtil.class.getName();

    /**
     * SharedPreferences constant for theme key
     */
    public static final @NonNull String KEY_THEME = "theme";
    /**
     * SharedPreferences constant for theme key auto theme value
     */
    public static final @NonNull String THEME_AUTO = "auto";
    /**
     * SharedPreferences constant for theme key light theme value
     */
    public static final @NonNull String THEME_LIGHT = "light";
    /**
     * SharedPreferences constant for theme key dark theme value
     */
    public static final @NonNull String THEME_DARK = "dark";

    /**
     * Apply the theme specified by the parameter <code>theme</code> according to {@link ThemeUtil#THEME_AUTO},
     * {@link ThemeUtil#THEME_LIGHT} and {@link ThemeUtil#THEME_DARK}. This should be called before super call of
     * {@link android.app.Activity#onCreate(Bundle)} or at {@link android.app.Application#onCreate()}
     * @param theme The theme to apply according to {@link ThemeUtil#THEME_AUTO}, {@link ThemeUtil#THEME_LIGHT} and {@link ThemeUtil#THEME_DARK}.
     */
    public static void applyTheme(final @NonNull String theme) {
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
