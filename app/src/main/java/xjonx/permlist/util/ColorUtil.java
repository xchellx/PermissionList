package xjonx.permlist.util;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

@SuppressWarnings("unused")
public class ColorUtil {
    public static final @NonNull String TAG = ColorUtil.class.getName();

    /**
     * Get the opposite contrasted color (either black or white) from the specified color using the W3C Luminance
     * formula.<br><br>
     *
     * <b>W3C Luminance formula:</b><br>
     * <code>lightness = (0.299 * red + 0.587 * green + 0.114 * blue) / 1000</code><br><br>
     *
     * <b>For reference:</b><br>
     * <code>
     * yellow = white<br>
     * red = white
     * </code>
     *
     * @param clr The color to get the contrast of.
     * @return The contrast of the color, either in {@link Color#BLACK} or {@link Color#WHITE}
     */
    public static int getContrastColorW3C(int clr) {
        final int red = android.graphics.Color.red(clr);
        final int green = android.graphics.Color.green(clr);
        final int blue = android.graphics.Color.blue(clr);
        final int y = (299 * red + 587 * green + 114 * blue) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Get the opposite contrasted color (either black or white) from the specified color using the sRGB Luminance
     * formula.<br><br>
     *
     * <b>sRGB Luminance formula:</b><br>
     * <code>lightness = (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 1000</code><br><br>
     *
     *
     * <b>For reference:</b><br>
     * <code>
     * yellow = black<br>
     * red = black
     * </code>
     *
     * @param clr The color to get the contrast of.
     * @return The contrast of the color, either in {@link Color#BLACK} or {@link Color#WHITE}
     */
    public static int getContrastColorsRGB(final int clr) {
        final int red = android.graphics.Color.red(clr);
        final int green = android.graphics.Color.green(clr);
        final int blue = android.graphics.Color.blue(clr);
        //noinspection OctalInteger
        final int y = (2126 * red + 7152 * green + 0722 * blue) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    /**
     * A wrapper for {@link android.graphics.Color}'s {@link androidx.annotation.ColorInt} constants
     */
    public static final class Color {
        @ColorInt public static final int BLACK       = android.graphics.Color.BLACK;
        @ColorInt public static final int DKGRAY      = android.graphics.Color.DKGRAY;
        @ColorInt public static final int GRAY        = android.graphics.Color.GRAY;
        @ColorInt public static final int LTGRAY      = android.graphics.Color.LTGRAY;
        @ColorInt public static final int WHITE       = android.graphics.Color.WHITE;
        @ColorInt public static final int RED         = android.graphics.Color.RED;
        @ColorInt public static final int GREEN       = android.graphics.Color.GREEN;
        @ColorInt public static final int BLUE        = android.graphics.Color.BLUE;
        @ColorInt public static final int YELLOW      = android.graphics.Color.YELLOW;
        @ColorInt public static final int ORANGE      = 0xFFFFA500;
        @ColorInt public static final int CYAN        = android.graphics.Color.CYAN;
        @ColorInt public static final int MAGENTA     = android.graphics.Color.MAGENTA;
        @ColorInt public static final int TRANSPARENT = android.graphics.Color.TRANSPARENT;
    }
}
