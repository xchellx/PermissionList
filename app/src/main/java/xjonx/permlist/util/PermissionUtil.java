package xjonx.permlist.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class PermissionUtil {
    public static final @NonNull String TAG = PermissionUtil.class.getName();

    /**
     * A request code used by {@link PermissionUtil#checkPermissions(Activity, String)} and will be
     * passed to {@link ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}
     * if {@link PermissionUtil#checkPermissions(Activity, String)} is used.
     */
    public static final int REQUESTCODE_REQUESTPERMISSIONS = 210;

    /**
     * Get all used permissions of a specified package name. Note that this gets <strong>used</strong>
     * permissions which means any permissions defined by a &lt;uses-permission&gt; tag NOT a &lt;permission&gt;
     * This is used by {@link PermissionUtil#checkPermissions(Activity, String)}.
     * @param ctx Context, required for {@link Context#getPackageManager}
     * @param packageName A package name to get the permissions from
     * @return An array of {@link String} permission names for all the used permissions of the specified package name
     * @throws PackageManager.NameNotFoundException If the package name is not found
     */
    public static @NonNull String[] getRequestedPermissions(Context ctx, String packageName) throws PackageManager.NameNotFoundException {
        if (ctx != null && packageName != null) {
            final String[] reqPerms = ctx.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            // Android 11's "QUERY_ALL_PACKAGES" permission has a bug on earlier versions where it
            // acts as like the permission is a "revocable" permission which has automatically been
            // denied despite no dialog appearing for it to be accepted. Blacklist this permission
            // on versions lower than Android 11.
            for (int r = 0; r < reqPerms.length; r++)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && reqPerms[r].equals(Manifest.permission.QUERY_ALL_PACKAGES))
                    reqPerms[r] = "";
            return reqPerms;
        } else {
            return new String[0];
        }
    }

    /**
     * Get a {@link PermissionInfo} object for all defined permissions of a specified package name. Note that
     * this gets <strong>defined</strong> permissions which means any permissions defined by a &lt;permission&gt;
     * tag NOT a &lt;uses-permission&gt;
     * tag.
     * @param ctx Context, required for {@link Context#getPackageManager}
     * @param packageName A package name to get the permissions from
     * @return An array of {@link PermissionInfo} objects for all the defined permissions of the specified package name
     * @throws PackageManager.NameNotFoundException If the package name is not found
     */
    public static @NonNull PermissionInfo[] getPermissions(Context ctx, String packageName) throws PackageManager.NameNotFoundException {
        if (ctx != null && packageName != null) {
            PermissionInfo[] perms = ctx.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS).permissions;
            return (perms == null) ? new PermissionInfo[0] : perms;
        } else {
            return new PermissionInfo[0];
        }
    }

    /**
     * Get a {@link PermissionInfo} object for all used permissions of a specified package name. Note that
     * this gets <strong>used</strong> permissions which means any permissions defined by a &lt;uses-permission&gt;
     * tag NOT a &lt;permission&gt;
     * tag.
     * @param ctx Context, required for {@link Context#getPackageManager}
     * @param packageName A package name to get the permissions from
     * @return An array of {@link PermissionInfo} objects for all the used permissions of the specified package name
     * @throws PackageManager.NameNotFoundException If the package name is not found
     */
    public static @NonNull PermissionInfo[] getUsedPermissions(Context ctx, String packageName) throws PackageManager.NameNotFoundException {
        if (ctx != null && packageName != null) {
            String[] reqPerms = ctx.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (reqPerms != null) {
                ArrayList<PermissionInfo> reqPermsInfs = new ArrayList<>();
                for (String reqPerm : reqPerms) {
                    PermissionInfo reqPermInf = getPermissionInfo(ctx, reqPerm);
                    if (reqPermInf != null) {
                        reqPermsInfs.add(reqPermInf);
                    }
                }
                return (PermissionInfo[])reqPermsInfs.toArray();
            } else {
                return new PermissionInfo[0];
            }
        } else {
            return new PermissionInfo[0];
        }
    }

    /**
     * Get a {@link PermissionInfo} object for all defined AND used permissions of a specified package name.
     * Note that this gets <strong>defined</strong> AND <strong>used</strong> permissions which means any
     * permissions defined by a &lt;permission&gt; tag OR a &lt;uses-permission&gt; tag.
     * @param ctx Context, required for {@link Context#getPackageManager}
     * @param packageName A package name to get the permissions from
     * @return An array of {@link PermissionInfo} objects for all the defined AND used permissions of the specified package name
     * @throws PackageManager.NameNotFoundException If the package name is not found
     */
    public static @NonNull PermissionInfo[] getAllPermissions(Context ctx, String packageName) throws PackageManager.NameNotFoundException {
        if (ctx != null && packageName != null) {
            PermissionInfo[] perms = getPermissions(ctx, packageName);
            PermissionInfo[] usdPerms = getUsedPermissions(ctx, packageName);
            PermissionInfo[] allPerms = new PermissionInfo[perms.length + usdPerms.length];
            System.arraycopy(perms, 0, allPerms, 0, perms.length);
            System.arraycopy(usdPerms, 0, allPerms, perms.length, usdPerms.length);
            return allPerms;
        } else {
            return new PermissionInfo[0];
        }
    }

    /**
     * Get a {@link PermissionInfo} object from a permission name
     * @param ctx Context, required for {@link Context#getPackageManager}
     * @param permissionName A permission name
     * @return The {@link PermissionInfo} object for the specified permission name
     * @throws PackageManager.NameNotFoundException If the specified permission name is not found
     */
    public static @Nullable PermissionInfo getPermissionInfo(Context ctx, String permissionName) throws PackageManager.NameNotFoundException {
        if (ctx != null && permissionName != null) {
            return ctx.getPackageManager().getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
        } else {
            return null;
        }
    }

    /**
     * Check if all permissions defined by &lt;uses-permission&gt; tags inside the AndroidManifest have
     * been granted (accepted by the user). This is to be used with {@link PermissionUtil#getRequestedPermissions(Context, String)}.
     * @param ctx Context, required for {@link ActivityCompat#checkSelfPermission(Context, String)}
     * @param permissions Permissions returned by {@link PermissionUtil#getRequestedPermissions(Context, String)}
     * @return True if all permissions were accepted, else false
     */
    public static boolean hasPermissions(Context ctx, String[] permissions) {
        if (ctx != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_DENIED && !permission.equals("")) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if all permissions defined by &lt;uses-permission&gt; tags inside the AndroidManifest have
     * been granted (accepted by the user). This is to be used with {@link ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}.
     * @param grantResults Permission grant results returned by {@link ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}
     * @return True if all permissions were accepted, else false
     */
    public static boolean hasAllPermissionsGranted(int[] grantResults) {
        if (grantResults != null) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if any permissions defined by &lt;uses-permission&gt; tags inside the AndroidManifest exist
     * and request these permissions if some are found. The request code used is {@link PermissionUtil#REQUESTCODE_REQUESTPERMISSIONS}
     * which will be passed to {@link ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}.
     * @param activity Activity reference, needed for {@link ActivityCompat#requestPermissions(Activity, String[], int)}
     * @param packageName Package name, needed for getting the permissions for the specified package name
     * @return true if no permissions are requested or the permissions have already been accepted, false if the permissions have been denied.
     * @throws PackageManager.NameNotFoundException if the specified package name is not found
     */
    public static boolean checkPermissions(Activity activity, String packageName) throws PackageManager.NameNotFoundException {
        if (activity != null && packageName != null) {
            String[] permissions = getRequestedPermissions(activity, packageName);
            if (permissions.length == 0) {
                return true;
            } else {
                if (!hasPermissions(activity, permissions)) {
                    ActivityCompat.requestPermissions(activity, permissions, REQUESTCODE_REQUESTPERMISSIONS);
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            return false;
        }
    }
}
