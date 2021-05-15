package xjonx.permlist;

import android.app.Application;

import xjonx.permlist.util.ApplicationUtil;
import xjonx.permlist.util.ThemeUtil;

public class PermissionListApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        ThemeUtil.applyTheme(ApplicationUtil.getApplicationPreferences(this).getString(ThemeUtil.KEY_THEME, ThemeUtil.THEME_AUTO));
    }
}
