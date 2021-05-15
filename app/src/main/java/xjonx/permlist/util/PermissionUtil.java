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
     * TODO: Document this.
     */
    public static final int REQUESTCODE_REQUESTPERMISSIONS = 210;

    /**
     * TODO: Document this.
     */
    public static @NonNull String[] getRequestedPermissions(Context ctx, String packageName) throws PackageManager.NameNotFoundException {
        if (ctx != null && packageName != null) {
            final String[] reqPerms = ctx.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            // Android 11's "QUERY_ALL_PACKAGES" permission has a bug on earlier versions where it
            // acts as like the permission is a "revocable" permission which has automatically been
            // denied despite no dialog appearing for it to be accepted. Blacklist against this exception
            // on versions lower than Android 11/
            for (int r = 0; r < reqPerms.length; r++)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && reqPerms[r].equals(Manifest.permission.QUERY_ALL_PACKAGES))
                    reqPerms[r] = "";
            return reqPerms;
        } else {
            return new String[0];
        }
    }

    /**
     * TODO: Document this.
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
     * TODO: Document this.
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
     * TODO: Document this.
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
     * TODO: Document this.
     */
    public static @Nullable PermissionInfo getPermissionInfo(Context ctx, String permissionName) throws PackageManager.NameNotFoundException {
        if (ctx != null && permissionName != null) {
            return ctx.getPackageManager().getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
        } else {
            return null;
        }
    }

    /**
     * TODO: Document this.
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
     * TODO: Document this.
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
     * TODO: Document this.
     */
    public static boolean checkPermissions(Activity activity, String packageName) throws PackageManager.NameNotFoundException, IllegalArgumentException {
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
