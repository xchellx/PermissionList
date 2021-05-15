package xjonx.permlist.activity;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import xjonx.permlist.PermissionListAdapter;
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

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        SearchView.OnQueryTextListener, PermissionListAdapter.OnFilterOptionsUpdatedListener, PermissionListAdapter.OnListItemClickListener {
    public static final @NonNull String TAG = MainActivity.class.getName();
    public static final int REQUESTCODE_EXPORTJSON = 200;

    // Layout fields
    private FrameLayout fl_main;
    private ThemableSwipeRefreshLayout tsrl_permlist_container;
    private FastScrollView fsv_permlist;
    private LinearLayout ll_permprog_container;
    private ProgressBar pb_permprog;
    private ExpandableFabLayout fab_filter_layout;
    private ExpandableFab fab_filter;
    private FabOption fab_filter_permissionname;
    private FabOption fab_filter_packagename;
    private FabOption fab_filter_isrevocable;

    // Class fields
    private final @NonNull ArrayList<PermissionListAdapter.ListItem> permList = new ArrayList<>();
    private final @NonNull PermissionListAdapter permListAdp = new PermissionListAdapter(permList,
            EnumSet.of(PermissionListAdapter.FilterOptions.PERMISSION), this, this);
    private LinearLayoutManager fsv_permlist_manager;
    private Animator permprogShowAnim;
    private Animator permprogHideAnim;
    private boolean refreshIsRunning = false;
    private @NonNull String queryCache = "";
    private @Nullable PermissionListTask permListTask = null;
    private boolean isSharing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
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
            return;
        }
    }

    @Override
    protected void onStop() {
        deinitialize();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        deinitialize();

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
                ToastUtil.showToast(MainActivity.this, getString(R.string.export_started), false, ColorUtil.Color.BLUE);
                // Save asynchronously
                new Thread(() -> {
                    try (final ParcelFileDescriptor fd = MainActivity.this.getContentResolver().openFileDescriptor(data.getData(), "w")) {
                        try (final FileWriter writer = new FileWriter(fd.getFileDescriptor())) {
                            new GsonBuilder()
                                    .excludeFieldsWithoutExposeAnnotation()
                                    .registerTypeAdapter(PermissionListAdapter.ListItem.class, new PermissionListAdapter.ListItem.Serializer())
                                    .setPrettyPrinting()
                                    .create()
                                    .toJson(MainActivity.this.permList, writer);
                            MainActivity.this.runOnUiThread(() -> ToastUtil.showToast(MainActivity.this,  R.string.export_success,
                                    false, ColorUtil.Color.BLUE));
                        } catch (final Exception e) {
                            MainActivity.this.runOnUiThread(() -> {
                                Log.e(TAG, "Failed to export permission list.", e);
                                ToastUtil.showToast(MainActivity.this,  String.format(getString(R.string.export_failed),
                                        e.getMessage()), true, ColorUtil.Color.RED);
                            });
                        }
                    } catch (IOException | JsonIOException e) {
                        Log.e(TAG, "Failed to export permission list.", e);
                        ToastUtil.showToast(MainActivity.this,  String.format(getString(R.string.export_failed), e.getMessage()),
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
                    options_search_action.setOnQueryTextListener(MainActivity.this);
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
                FileUtil.createSAFFile(this, REQUESTCODE_EXPORTJSON, FileUtil.getDateFileName(MainActivity.this,
                        "permissionlist", ".json"), "application/json");
                return true;
            case R.id.options_bugreport:
                ToastUtil.showToast(MainActivity.this, getString(R.string.bugreport_started), false, ColorUtil.Color.BLUE);
                new Thread(() -> {
                    try {
                        final String logcat = DebugUtil.getLogcat();
                        MainActivity.this.runOnUiThread(() -> ToastUtil.showToast(MainActivity.this, getString(R.string.bugreport_success),
                                false, ColorUtil.Color.BLUE));
                        FileUtil.shareCacheFileText(MainActivity.this, MainActivity.this.getString(R.string.bugreport_sharetitle),
                                FileUtil.getDateFileName(MainActivity.this, "bugreport", ".txt"), logcat);
                        MainActivity.this.runOnUiThread(() -> MainActivity.this.isSharing = true);
                    } catch (final IOException e) {
                        MainActivity.this.runOnUiThread(() -> {
                            Log.e(TAG, "Failed to send logcat.", e);
                            ToastUtil.showToast(MainActivity.this,  String.format(
                                    MainActivity.this.getString(R.string.bugreport_failed),
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
                permListTask = (PermissionListTask) new PermissionListTask(MainActivity.this).execute();
                pb_permprog.removeCallbacks(this);
            }
        });
    }

    private void deinitialize() {
        if (isSharing) {
            isSharing = false;
        } else {
            new Thread(() -> {
                try {
                    if (getCacheDir().exists()) {
                        FileUtil.deleteDirectory(getCacheDir());
                    }
                } catch (SecurityException e) {
                    MainActivity.this.runOnUiThread(() -> Log.e(TAG, "Failed to clear cache directory.", e));
                }
            }).start();
        }
        if (permListTask != null) {
            permListTask.cancel(true);
            permListTask = null;
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

    @SuppressWarnings("deprecation")
    private static class PermissionListTask extends AsyncTask<Void, Integer, ArrayList<PermissionListAdapter.ListItem>> {
        private final @NonNull WeakReference<MainActivity> _this;

        public PermissionListTask(@Nullable MainActivity p_this) { _this = new WeakReference<>(p_this); }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (_this.get() != null) {
                // Enable search status and disable UI
                setSearchUIDisabledStatus(true);
            } else {
                Log.e(PermissionListTask.class.getName(), "Failed to run AsyncTask, _this is null");
            }
        }

        @Override
        protected ArrayList<PermissionListAdapter.ListItem> doInBackground(Void... params) {
            if (_this.get() != null && !isCancelled()) {
                final ArrayList<PermissionListAdapter.ListItem> permRslt = new ArrayList<>();
                // Get all installed packages
                final List<PackageInfo> pckgs = _this.get().getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);
                final int pckgsSize = pckgs.size();
                int pckgsProgress = 0;
                for (PackageInfo pckgInf : pckgs) {
                    // Get all permissions that each package declares.
                    PermissionInfo[] perms;
                    try {
                        perms = PermissionUtil.getPermissions(_this.get(), pckgInf.packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        // Would log exception here but it doesn't matter in the long run. Also, logging is UI operation
                        // so to avoid slowing down task, there is no logging)
                        perms = new PermissionInfo[0];
                    }
                    // Go through each package permission and add it (along whether it is revocable)
                    for (PermissionInfo perm : perms) {
                        permRslt.add(new PermissionListAdapter.ListItem(perm.name,
                                ((perm.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS),
                                pckgInf.packageName, pckgInf.applicationInfo.loadIcon(_this.get().getPackageManager())));
                    }
                    // Update progress
                    publishProgress(++pckgsProgress, pckgsSize);
                }
                return permRslt;
            } else {
                return new ArrayList<>();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            if (_this.get() != null) {
                // Set max progress for progress bar
                _this.get().pb_permprog.setMax(values[1]);
                // Increase progress bar progress
                _this.get().pb_permprog.setProgress(values[0]);
            } else {
                Log.e(PermissionListTask.class.getName(), "Failed to run AsyncTask, _this is null");
            }
        }

        @Override
        protected void onPostExecute(ArrayList<PermissionListAdapter.ListItem> result) {
            super.onPostExecute(result);

            if (_this.get() != null) {
                // Update list
                _this.get().permList.clear();
                _this.get().permList.addAll(result);
                _this.get().permListAdp.replaceAll(_this.get().permList);
                _this.get().permListAdp.filter(_this.get().permList);
                // Disable search status and enable UI
                setSearchUIDisabledStatus(false);

                // Clear references explicitly
                _this.clear();
            } else {
                Log.e(PermissionListTask.class.getName(), "Failed to run AsyncTask, _this is null");
            }
        }

        private void setSearchUIDisabledStatus(boolean enabledStatus) {
            if (enabledStatus) {
                // Reset progress bar progress
                _this.get().pb_permprog.setProgress(0);
                _this.get().pb_permprog.setMax(1);
                // Start loading anim
                _this.get().tsrl_permlist_container.setRefreshing(true);
                // Disable filter fabs
                if (_this.get().fab_filter_layout.isOpen()) {
                    _this.get().fab_filter_layout.close();
                }
                _this.get().fab_filter_permissionname.setFabOptionEnabled(false);
                _this.get().fab_filter_packagename.setFabOptionEnabled(false);
                _this.get().fab_filter_isrevocable.setFabOptionEnabled(false);
                _this.get().fab_filter.setEfabEnabled(false);
                // Show progress bar
                _this.get().permprogShowAnim.start();
                // Disable menu options and stop any current searches
                _this.get().setRefreshIsRunning(true);
            } else {
                // Enable menu options and stop any current searches
                _this.get().setRefreshIsRunning(false);
                // Stop loading anim
                _this.get().tsrl_permlist_container.setRefreshing(false);
                // Enable filter fabs
                _this.get().fab_filter_permissionname.setFabOptionEnabled(true);
                _this.get().fab_filter_packagename.setFabOptionEnabled(true);
                _this.get().fab_filter_isrevocable.setFabOptionEnabled(true);
                _this.get().fab_filter.setEfabEnabled(true);
                // Fix filter fab icon theme being reset
                _this.get().fixTheme();
                // Hide progress bar
                _this.get().permprogHideAnim.start();
            }
        }
    }
}