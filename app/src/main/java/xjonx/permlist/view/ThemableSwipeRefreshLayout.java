package xjonx.permlist.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import xjonx.permlist.R;
import xjonx.permlist.util.ColorUtil;

/**
 * This is a {@link SwipeRefreshLayout} but themable by xml.
 * {@inheritDoc}
 */
public class ThemableSwipeRefreshLayout extends SwipeRefreshLayout {
    /**
     * {@inheritDoc}
     */
    public ThemableSwipeRefreshLayout(@NonNull Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    public ThemableSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs, new int[]{
                R.attr.progressBackgroundColor, R.attr.progressColor1,
                R.attr.progressColor2, R.attr.progressColor3,
                R.attr.progressColor4
        });
        setProgressBackgroundColorSchemeColor(a.getColor(0, ColorUtil.Color.WHITE));
        setColorSchemeColors(
                a.getColor(1, ColorUtil.Color.BLACK), a.getColor(2, ColorUtil.Color.BLACK),
                a.getColor(3, ColorUtil.Color.BLACK), a.getColor(4, ColorUtil.Color.BLACK));
        a.recycle();
    }
}
