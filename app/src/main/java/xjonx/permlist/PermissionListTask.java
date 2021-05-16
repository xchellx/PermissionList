package xjonx.permlist;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import xjonx.permlist.activity.PermissionListActivity;
import xjonx.permlist.util.PermissionUtil;

@SuppressWarnings("deprecation")
public class PermissionListTask extends AsyncTask<Void, Integer, ArrayList<PermissionListAdapter.ListItem>> {
    private final @NonNull WeakReference<PermissionListActivity> _this;

    public PermissionListTask(@Nullable PermissionListActivity p_this) { _this = new WeakReference<>(p_this); }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (_this.get() != null && !isCancelled()) {
            // Enable search status and disable UI
            setSearchUIDisabledStatus(true);
        } else {
            if (isCancelled()) {
                Log.e(PermissionListTask.class.getName(), "(onPreExecute) Task will not continue, AsyncTask was canceled");
                // Clear references explicitly
                _this.clear();
            } else {
                Log.e(PermissionListTask.class.getName(), "(onPreExecute) Failed to run AsyncTask, _this is null");
            }
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
            if (_this.get() != null) {
                // Clear references explicitly
                _this.clear();
            }
            return new ArrayList<>();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        if (_this.get() != null && !isCancelled()) {
            // Set max progress for progress bar
            _this.get().pb_permprog.setMax(values[1]);
            // Increase progress bar progress
            _this.get().pb_permprog.setProgress(values[0]);
        } else {
            if (isCancelled()) {
                Log.e(PermissionListTask.class.getName(), "(onProgressUpdate) Task will not continue, AsyncTask was canceled");
                // Clear references explicitly
                _this.clear();
            } else {
                Log.e(PermissionListTask.class.getName(), "(onProgressUpdate) Failed to run AsyncTask, _this is null");
            }
        }
    }

    @Override
    protected void onPostExecute(ArrayList<PermissionListAdapter.ListItem> result) {
        super.onPostExecute(result);

        if (_this.get() != null && !isCancelled()) {
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
            if (isCancelled()) {
                Log.e(PermissionListTask.class.getName(), "(onPostExecute) Task will not continue, AsyncTask was canceled");
                // Clear references explicitly
                _this.clear();
            } else {
                Log.e(PermissionListTask.class.getName(), "(onPostExecute) Failed to run AsyncTask, _this is null");
            }
        }
    }

    public void setSearchUIDisabledStatus(boolean enabledStatus) {
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
