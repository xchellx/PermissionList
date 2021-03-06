package xjonx.permlist.activity;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.color.MaterialColors;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.l4digital.fastscroll.FastScrollView;
import com.nambimobile.widgets.efab.ExpandableFab;
import com.nambimobile.widgets.efab.ExpandableFabLayout;
import com.nambimobile.widgets.efab.FabOption;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;

import xjonx.permlist.PermissionListAdapter;
import xjonx.permlist.PermissionListTask;
import xjonx.permlist.R;
import xjonx.permlist.util.DebugUtil;
import xjonx.permlist.util.FileUtil;
import xjonx.permlist.util.FlagUtil;
import xjonx.permlist.util.ThemeUtil;
import xjonx.permlist.util.ApplicationUtil;
import xjonx.permlist.util.ColorUtil;
import xjonx.permlist.util.PermissionUtil;
import xjonx.permlist.util.ToastUtil;
import xjonx.permlist.view.ThemableSwipeRefreshLayout;

public class PermissionListActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        SearchView.OnQueryTextListener, PermissionListAdapter.OnFilterOptionsUpdatedListener, PermissionListAdapter.OnListItemClickListener {
    public static final @NonNull String TAG = PermissionListActivity.class.getName();
    public static final int REQUESTCODE_EXPORTJSON = 200;

    // Layout fields
    private FrameLayout fl_main;
    public volatile ThemableSwipeRefreshLayout tsrl_permlist_container;
    private volatile FastScrollView fsv_permlist;
    private LinearLayout ll_permprog_container;
    public volatile ProgressBar pb_permprog;
    public volatile ExpandableFabLayout fab_filter_layout;
    public volatile ExpandableFab fab_filter;
    public volatile FabOption fab_filter_permissionname;
    public volatile FabOption fab_filter_packagename;
    public volatile FabOption fab_filter_isrevocable;

    // Class fields
    public final @NonNull ArrayList<PermissionListAdapter.ListItem> permList = new ArrayList<>();
    public final @NonNull PermissionListAdapter permListAdp = new PermissionListAdapter(permList,
            EnumSet.of(PermissionListAdapter.FilterOptions.PERMISSION), this, this);
    private LinearLayoutManager fsv_permlist_manager;
    public volatile Animator permprogShowAnim;
    public volatile Animator permprogHideAnim;
    private volatile boolean refreshIsRunning = false;
    private @NonNull String queryCache = "";
    private volatile @Nullable PermissionListTask permListTask = null;
    private volatile boolean isSharing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_permlist);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to initialize layout", e);
            ToastUtil.showToast(this,  R.string.layout_failed, true, ColorUtil.Color.RED);
            return;
        }
        initialize(savedInstanceState);
        try {
            if (PermissionUtil.checkPermissions(this, getPackageName())) {
                initializeLogic();
            }
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to get current permissions", e);
            ToastUtil.showToast(this,  R.string.permissions_failed, true, ColorUtil.Color.RED);
        }
    }

    @Override
    protected void onStop() {
        deinitialize(true);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        deinitialize(false);

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtil.REQUESTCODE_REQUESTPERMISSIONS) {
            if (PermissionUtil.hasAllPermissionsGranted(grantResults)) {
                Log.v(TAG, "Permissions were granted by user; showing UI.");
                initializeLogic();
            } else {
                Log.e(TAG, "Permissions were not granted by user; hiding UI.");
                ToastUtil.showToast(this,  R.string.permissions_revoked, true, ColorUtil.Color.RED);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (FlagUtil.isFlagSet(requestCode, (FileUtil.REQUESTCODE_CREATESAFFILE | REQUESTCODE_EXPORTJSON))) {
            if (resultCode == RESULT_OK && data != null) {
                ToastUtil.showToast(PermissionListActivity.this, getString(R.string.export_started), false, ColorUtil.Color.BLUE);
                // Save asynchronously
                new Thread(() -> {
                    try (final ParcelFileDescriptor fd = PermissionListActivity.this.getContentResolver().openFileDescriptor(data.getData(), "w")) {
                        try (final FileWriter writer = new FileWriter(fd.getFileDescriptor())) {
                            new GsonBuilder()
                                    .excludeFieldsWithoutExposeAnnotation()
                                    .registerTypeAdapter(PermissionListAdapter.ListItem.class, new PermissionListAdapter.ListItem.Serializer())
                                    .setPrettyPrinting()
                                    .create()
                                    .toJson(PermissionListActivity.this.permList, writer);
                            PermissionListActivity.this.runOnUiThread(() -> ToastUtil.showToast(PermissionListActivity.this,  R.string.export_success,
                                    false, ColorUtil.Color.BLUE));
                        } catch (final Exception e) {
                            PermissionListActivity.this.runOnUiThread(() -> {
                                Log.e(TAG, "Failed to export permission list.", e);
                                ToastUtil.showToast(PermissionListActivity.this,  String.format(getString(R.string.export_failed),
                                        e.getMessage()), true, ColorUtil.Color.RED);
                            });
                        }
                    } catch (IOException | JsonIOException e) {
                        Log.e(TAG, "Failed to export permission list.", e);
                        ToastUtil.showToast(PermissionListActivity.this,  String.format(getString(R.string.export_failed), e.getMessage()),
                                true, ColorUtil.Color.RED);
                    }
                }).start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.options_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.options_theme).setEnabled(!refreshIsRunning);
        menu.findItem(R.id.options_export).setEnabled(!refreshIsRunning);
        if (DebugUtil.isDebug(this)) {
            menu.findItem(R.id.options_bugreport).setVisible(true).setEnabled(true);
        }
        final MenuItem options_theme_auto = menu.findItem(R.id.options_theme_auto);
        final MenuItem options_theme_light = menu.findItem(R.id.options_theme_light);
        final MenuItem options_theme_dark = menu.findItem(R.id.options_theme_dark);
        options_theme_auto.setEnabled(!refreshIsRunning);
        options_theme_light.setEnabled(!refreshIsRunning);
        options_theme_dark.setEnabled(!refreshIsRunning);
        SharedPreferences preferences = ApplicationUtil.getApplicationPreferences(this);
        switch (preferences.getString(ThemeUtil.KEY_THEME, ThemeUtil.THEME_AUTO)) {
            case ThemeUtil.THEME_AUTO:
                options_theme_auto.setChecked(true);
                break;
            case ThemeUtil.THEME_LIGHT:
                options_theme_light.setChecked(true);
                break;
            case ThemeUtil.THEME_DARK:
                options_theme_dark.setChecked(true);
                break;
        }

        final MenuItem options_search = menu.findItem(R.id.options_search);
        final SearchView options_search_action = (SearchView) options_search.getActionView();
        options_search.setEnabled(!refreshIsRunning);
        options_search.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Force searchItem not to be listed under overflow menu
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                if (!refreshIsRunning) {
                    // We must notify ActionView has collapsed for setQuery/getQuery to stay
                    options_search_action.onActionViewExpanded();
                    options_search_action.setQuery(queryCache, false);
                    options_search_action.setOnQueryTextListener(PermissionListActivity.this);
                    permListAdp.filter(permList, queryCache);
                    fsv_permlist_manager.scrollToPositionWithOffset(0, 0);
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                permListAdp.filter(permList);
                fsv_permlist_manager.scrollToPositionWithOffset(0, 0);
                options_search_action.setOnQueryTextListener(null);
                queryCache = (options_search_action.getQuery() == null) ? queryCache : options_search_action.getQuery().toString();
                // We must notify ActionView has collapsed for setQuery/getQuery to stay
                options_search_action.onActionViewCollapsed();
                //Call menu to be redrawn
                supportInvalidateOptionsMenu();
                return true;
            }
        });
        if (refreshIsRunning) {
            options_search.collapseActionView();
        }

        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.options_search:
                return true;
            case R.id.options_about:
                // Setup and show dialog
                AlertDialog aboutDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.about_dialog_title)
                        .setMessage(
                                HtmlCompat.fromHtml(
                                        getString(R.string.about_dialog_message).replace(
                                                getResources().getResourceEntryName(R.attr.colorError),
                                                String.format("#%06X", 0xFFFFFF & MaterialColors.getColor(this,
                                                        R.attr.colorError, ColorUtil.Color.RED))
                                        ),
                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                )
                        )
                        .setPositiveButton(android.R.string.ok, null)
                        .setCancelable(true)
                        .show();
                // Make HTML URLs clickable
                ((TextView) Objects.requireNonNull(aboutDialog.findViewById(android.R.id.message)))
                        .setMovementMethod(LinkMovementMethod.getInstance());
                return true;
            case R.id.options_export:
                FileUtil.createSAFFile(this, REQUESTCODE_EXPORTJSON, FileUtil.getDateFileName(PermissionListActivity.this,
                        "permissionlist", ".json"), "application/json");
                return true;
            case R.id.options_bugreport:
                ToastUtil.showToast(PermissionListActivity.this, getString(R.string.bugreport_started), false, ColorUtil.Color.BLUE);
                new Thread(() -> {
                    try {
                        final String logcat = DebugUtil.getLogcat();
                        PermissionListActivity.this.runOnUiThread(() -> ToastUtil.showToast(PermissionListActivity.this, getString(R.string.bugreport_success),
                                false, ColorUtil.Color.BLUE));
                        FileUtil.shareCacheFileText(PermissionListActivity.this, PermissionListActivity.this.getString(R.string.bugreport_sharetitle),
                                FileUtil.getDateFileName(PermissionListActivity.this, "bugreport", ".txt"), logcat);
                        PermissionListActivity.this.runOnUiThread(() -> PermissionListActivity.this.isSharing = true);
                    } catch (final IOException e) {
                        PermissionListActivity.this.runOnUiThread(() -> {
                            Log.e(TAG, "Failed to send logcat.", e);
                            ToastUtil.showToast(PermissionListActivity.this,  String.format(
                                    PermissionListActivity.this.getString(R.string.bugreport_failed),
                                    e.getMessage()), true, ColorUtil.Color.RED);
                        });
                    }
                }).start();
                return true;
            case R.id.options_theme_auto:
            case R.id.options_theme_light:
            case R.id.options_theme_dark:
                item.setChecked(true);
                SharedPreferences preferences = ApplicationUtil.getApplicationPreferences(this);
                if (item.getItemId() == R.id.options_theme_auto) {
                    preferences.edit().putString(ThemeUtil.KEY_THEME, ThemeUtil.THEME_AUTO).apply();
                    ThemeUtil.applyTheme(ThemeUtil.THEME_AUTO);
                } else if (item.getItemId() == R.id.options_theme_light) {
                    preferences.edit().putString(ThemeUtil.KEY_THEME, ThemeUtil.THEME_LIGHT).apply();
                    ThemeUtil.applyTheme(ThemeUtil.THEME_LIGHT);
                } else if (item.getItemId() == R.id.options_theme_dark) {
                    preferences.edit().putString(ThemeUtil.KEY_THEME, ThemeUtil.THEME_DARK).apply();
                    ThemeUtil.applyTheme(ThemeUtil.THEME_DARK);
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (!refreshIsRunning) {
            queryCache = (query == null) ? "" : query;
            permListAdp.filter(permList, queryCache);
            fsv_permlist_manager.scrollToPositionWithOffset(0, 0);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onFilterOptionsUpdated() {
        permListAdp.filter(permList, queryCache);
        fsv_permlist_manager.scrollToPositionWithOffset(0, 0);
    }

    @SuppressLint("NonConstantResourceId")
    public void onFilterOptionClicked(View v) {
        switch (v.getId()) {
            case R.id.fab_filter_permissionname:
                if (permListAdp.getFilterOption(PermissionListAdapter.FilterOptions.PACKAGE)) {
                    permListAdp.setFilterOption(PermissionListAdapter.FilterOptions.PACKAGE, false);
                    fab_filter_packagename.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_unchecked));
                }
                permListAdp.setFilterOption(PermissionListAdapter.FilterOptions.PERMISSION, true);
                fab_filter_permissionname.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_checked));
                break;
            case R.id.fab_filter_packagename:
                if (permListAdp.getFilterOption(PermissionListAdapter.FilterOptions.PERMISSION)) {
                    permListAdp.setFilterOption(PermissionListAdapter.FilterOptions.PERMISSION, false);
                    fab_filter_permissionname.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_unchecked));
                }
                permListAdp.setFilterOption(PermissionListAdapter.FilterOptions.PACKAGE, true);
                fab_filter_packagename.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_checked));
                break;
            case R.id.fab_filter_isrevocable:
                permListAdp.setFilterOption(PermissionListAdapter.FilterOptions.REVOCABLE, !permListAdp.getFilterOption(PermissionListAdapter.FilterOptions.REVOCABLE));
                fab_filter_isrevocable.setFabOptionIcon(ContextCompat.getDrawable(this, permListAdp.getFilterOption(
                        PermissionListAdapter.FilterOptions.REVOCABLE) ? R.drawable.ic_check_box : R.drawable.ic_check_box_outline));
                break;
        }
    }

    @Override
    public void onListItemClick(PermissionListAdapter.ListItem listItem) {
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(
                "permission_data",
                new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .registerTypeAdapter(PermissionListAdapter.ListItem.class, new PermissionListAdapter.ListItem.Serializer())
                        .setPrettyPrinting()
                        .create()
                        .toJson(listItem)
        ));
        ToastUtil.showToast(this,  R.string.permission_copied, false, ColorUtil.Color.BLUE);
    }

    private void initialize(Bundle savedInstanceState) {
        // Initialize layout fields
        fl_main = findViewById(R.id.fl_main);
        tsrl_permlist_container = findViewById(R.id.tsrl_permlist_container);
        fsv_permlist = findViewById(R.id.fsv_permlist);
        ll_permprog_container = findViewById(R.id.ll_permprog_container);
        pb_permprog = findViewById(R.id.pb_permprog);
        fab_filter_layout = findViewById(R.id.fab_filter_layout);
        fab_filter = findViewById(R.id.fab_filter);
        fab_filter_permissionname = findViewById(R.id.fab_filter_permissionname);
        fab_filter_packagename = findViewById(R.id.fab_filter_packagename);
        fab_filter_isrevocable = findViewById(R.id.fab_filter_isrevocable);

        // Initialize activity fields
        fsv_permlist_manager = new LinearLayoutManager(this);
        permprogShowAnim = AnimatorInflater.loadAnimator(this, R.animator.permprog_show);
        permprogHideAnim = AnimatorInflater.loadAnimator(this, R.animator.permprog_hide);
        queryCache = "";
        refreshIsRunning = false;

        // Setup layout fields
        tsrl_permlist_container.setOnRefreshListener(() -> permListTask = (PermissionListTask) new PermissionListTask(this).execute());
        fsv_permlist.setLayoutManager(fsv_permlist_manager);
        fsv_permlist.setAdapter(permListAdp);
        fab_filter_packagename.setOnClickListener(this::onFilterOptionClicked);
        fab_filter_permissionname.setOnClickListener(this::onFilterOptionClicked);
        fab_filter_isrevocable.setOnClickListener(this::onFilterOptionClicked);
        fixTheme();

        // Setup activity fields
        permprogShowAnim.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) { ll_permprog_container.setVisibility(View.VISIBLE); }
            @Override public void onAnimationEnd(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
        permprogHideAnim.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) { ll_permprog_container.setVisibility(View.GONE); }
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
        permprogShowAnim.setTarget(ll_permprog_container);
        permprogHideAnim.setTarget(ll_permprog_container);
    }

    private void initializeLogic() {
        // Show everything; no errors happened on initialization
        fl_main.setVisibility(View.VISIBLE);
        // Post to view to ensure task is ran when view is ready and the posted runnable is correctly disposed
        pb_permprog.post(new Runnable() {
            @Override
            public void run() {
                permListTask = (PermissionListTask) new PermissionListTask(PermissionListActivity.this).execute();
                pb_permprog.removeCallbacks(this);
            }
        });
    }

    private void deinitialize(boolean isDestroyed) {
        if (isSharing) {
            isSharing = false;
        } else {
            // Delete cache on app exit
            new Thread(() -> {
                try {
                    if (getCacheDir().exists()) {
                        FileUtil.deleteDirectory(getCacheDir());
                    }
                } catch (SecurityException e) {
                    PermissionListActivity.this.runOnUiThread(() -> Log.e(TAG, "Failed to clear cache directory.", e));
                }
            }).start();
        }
        // Cancel refresh task if it is running
        if (permListTask != null && refreshIsRunning) {
            if (!permListTask.isCancelled()) {
                setRefreshIsRunning(false);
                permListTask.setSearchUIDisabledStatus(false);
                permListTask.cancel(true);
                permListTask = null;
            }
        }
    }

    public void setRefreshIsRunning(boolean runningRefresh) {
        refreshIsRunning = runningRefresh;
        supportInvalidateOptionsMenu();
    }

    public void fixTheme() {
        // Sometimes the colors from the theme are incorrectly applied by TypedArray.
        // Re-applying them with MaterialColors and ContextCompat seems to fix this.
        fsv_permlist.getFastScroller().setHandleColor(MaterialColors.getColor(fsv_permlist, R.attr.colorSecondary));
        fsv_permlist.getFastScroller().setBubbleColor(MaterialColors.getColor(fsv_permlist, R.attr.colorSecondary));
        fsv_permlist.getFastScroller().setTrackColor(MaterialColors.getColor(fsv_permlist, R.attr.colorSecondaryVariant));
        fsv_permlist.getFastScroller().setBubbleTextColor(MaterialColors.getColor(fsv_permlist, R.attr.colorOnSurface));
        fab_filter.setEfabIcon(ContextCompat.getDrawable(this, R.drawable.ic_filter));
        if (permListAdp.getFilterOption(PermissionListAdapter.FilterOptions.PERMISSION)) {
            fab_filter_permissionname.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_checked));
        } else {
            fab_filter_permissionname.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_unchecked));
        }
        if (permListAdp.getFilterOption(PermissionListAdapter.FilterOptions.PACKAGE)) {
            fab_filter_packagename.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_checked));
        } else {
            fab_filter_packagename.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_radio_button_unchecked));
        }
        if (permListAdp.getFilterOption(PermissionListAdapter.FilterOptions.REVOCABLE)) {
            fab_filter_isrevocable.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_box));
        } else {
            fab_filter_isrevocable.setFabOptionIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_box_outline));
        }
    }
}