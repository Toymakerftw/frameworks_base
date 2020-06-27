/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.bootleggers;

import android.app.ActivityManager;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.SearchManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ActivityNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import android.os.UserHandle;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

import com.android.internal.statusbar.IStatusBarService;

import java.util.Locale;

public class BootlegUtils {

    private static int sDeviceType = -1;
    private static final int DEVICE_PHONE = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;
    public static final String INTENT_SCREENSHOT = "action_take_screenshot";
    public static final String INTENT_REGION_SCREENSHOT = "action_take_region_screenshot";
    public static final String SYSTEMUI = "com.android.systemui";
    private static IStatusBarService mStatusBarService = null;

    private static final String[] QS_TILE_THEMES = {
        "com.android.systemui.qstile.default", // 0
        "com.android.systemui.qstile.circletrim", // 1
        "com.android.systemui.qstile.dualtonecircletrim", // 2
        "com.android.systemui.qstile.squircletrim", // 3
        "com.android.systemui.qstile.circlegradient", // 4
        "com.android.systemui.qstile.wavey", // 5
        "com.android.systemui.qstile.circledualtone", // 6
        "com.android.systemui.qstile.pokesign", // 7
        "com.android.systemui.qstile.ninja", // 8
        "com.android.systemui.qstile.dottedcircle", // 9
        "com.android.systemui.qstile.attemptmountain", // 10
	"com.android.systemui.qstile.squaremedo", // 11
        "com.android.systemui.qstile.hexagon", // 12
        "com.android.systemui.qstile.star", // 13
        "com.android.systemui.qstile.gear", // 14
        "com.android.systemui.qstile.diamond", // 15
        "com.android.systemui.qstile.cookie", // 16
        "com.android.systemui.qstile.circleoutline", //17
        "com.android.systemui.qstile.roundedrect", //18
        "com.android.systemui.qstile.square", //19
        "com.android.systemui.qstile.squircle", //20
        "com.android.systemui.qstile.teardrop", //21
        "com.android.systemui.qstile.badge", //22
        "com.android.systemui.qstile.badgetwo", //23
        "com.android.systemui.qstile.triangle", //24
        "com.android.systemui.qstile.oos", //25
    };

    public static boolean isChineseLanguage() {
       return Resources.getSystem().getConfiguration().locale.getLanguage().startsWith(
               Locale.CHINESE.getLanguage());
    }

    public static boolean deviceSupportsFlashLight(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null
                        && flashAvailable
                        && lensFacing != null
                        && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true;
                }
            }
        } catch (CameraAccessException e) {
            // Ignore
        }
        return false;
    }

    public static boolean killProcess(Context context, int userId) {
        final int mUserId = ActivityManager.getCurrentUser();
        try {
            return killForegroundAppInternal(context, mUserId);
        } catch (RemoteException e) {
        }
        return false;
    }

    public static boolean killForegroundAppInternal(Context context, int userId)
            throws RemoteException {
        final String packageName = getForegroundTaskPackageName(context, userId);

        if (packageName == null) {
            return false;
        }

        final IActivityManager am = ActivityManagerNative.getDefault();
        am.forceStopPackage(packageName, UserHandle.USER_CURRENT);

        return true;
    }

    public static String getForegroundTaskPackageName(Context context, int userId)
            throws RemoteException {
        final String defaultHomePackage = resolveCurrentLauncherPackage(context, userId);
        final IActivityManager am = ActivityManager.getService();
        final StackInfo focusedStack = am.getFocusedStackInfo();

        if (focusedStack == null || focusedStack.topActivity == null) {
            return null;
        }

        final String packageName = focusedStack.topActivity.getPackageName();
        if (!packageName.equals(defaultHomePackage)
                && !packageName.equals(SYSTEMUI)) {
            return packageName;
        }

        return null;
    }

    public static String resolveCurrentLauncherPackage(Context context, int userId) {
        final Intent launcherIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME);
        final PackageManager pm = context.getPackageManager();
        final ResolveInfo launcherInfo = pm.resolveActivityAsUser(launcherIntent, 0, userId);

        if (launcherInfo.activityInfo != null &&
                !launcherInfo.activityInfo.packageName.equals("android")) {
            return launcherInfo.activityInfo.packageName;
        }

        return null;
    }

    public static boolean isPackageAvailable(String packageName, Context context) {
        Context mContext = context;
        final PackageManager pm = mContext.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            int enabled = pm.getApplicationEnabledSetting(packageName);
            return enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void switchScreenOff(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm!= null && pm.isScreenOn()) {
            pm.goToSleep(SystemClock.uptimeMillis());
        }
    }

    // Screen on
    public static void switchScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
	pm.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
    }

    // Method to detect navigation bar is in use
    public static boolean hasNavigationBar(Context context) {
        boolean hasNavbar = false;
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            hasNavbar = wm.hasNavigationBar(context.getDisplayId());
        } catch (RemoteException ex) {
        }
        return hasNavbar;
    }

    public static void setPartialScreenshot(boolean active) {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.setPartialScreenshot(active);
            } catch (RemoteException e) {}
        }
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    public static void takeScreenshot(boolean full) {
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            wm.sendCustomAction(new Intent(full? INTENT_SCREENSHOT : INTENT_REGION_SCREENSHOT));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        return isPackageInstalled(context, pkg, true);
    }

    public static boolean deviceHasFlashlight(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public static void toggleCameraFlash() {
        FireActions.toggleCameraFlash();
    }

    private static final class FireActions {
        private static IStatusBarService mStatusBarService = null;

        private static IStatusBarService getStatusBarService() {
            synchronized (FireActions.class) {
                if (mStatusBarService == null) {
                    mStatusBarService = IStatusBarService.Stub.asInterface(
                            ServiceManager.getService("statusbar"));
                }
                return mStatusBarService;
            }
        }

        public static void toggleCameraFlash() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.toggleCameraFlash();
                } catch (RemoteException e) {
                    // do nothing.
                }
            }
        }
    }

    private static int getScreenType(Context context) {
        if (sDeviceType == -1) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayInfo outDisplayInfo = new DisplayInfo();
            wm.getDefaultDisplay().getDisplayInfo(outDisplayInfo);
            int shortSize = Math.min(outDisplayInfo.logicalHeight, outDisplayInfo.logicalWidth);
            int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT
                    / outDisplayInfo.logicalDensityDpi;
            if (shortSizeDp < 600) {
                // 0-599dp: "phone" UI with a separate status & navigation bar
                sDeviceType =  DEVICE_PHONE;
            } else if (shortSizeDp < 720) {
                // 600-719dp: "phone" UI with modifications for larger screens
                sDeviceType = DEVICE_HYBRID;
            } else {
                // 720dp: "tablet" UI with a single combined status & navigation bar
                sDeviceType = DEVICE_TABLET;
            }
        }
        return sDeviceType;
    }

    public static boolean isPhone(Context context) {
        return getScreenType(context) == DEVICE_PHONE;
    }

    public static boolean isHybrid(Context context) {
        return getScreenType(context) == DEVICE_HYBRID;
    }

    public static boolean isTablet(Context context) {
        return getScreenType(context) == DEVICE_TABLET;
    }

    private static IStatusBarService getStatusBarService() {
        synchronized (BootlegUtils.class) {
            if (mStatusBarService == null) {
                mStatusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService("statusbar"));
            }
            return mStatusBarService;
        }
    }

    // Omni Switch Constants

    /**
     * Package name of the omnniswitch app
     */
    public static final String APP_PACKAGE_NAME = "org.omnirom.omniswitch";

    /**
     * Intent broadcast action for showing the omniswitch overlay
     */
    public static final String ACTION_SHOW_OVERLAY = APP_PACKAGE_NAME + ".ACTION_SHOW_OVERLAY";

    /**
     * Intent broadcast action for hiding the omniswitch overlay
     */
    public static final String ACTION_HIDE_OVERLAY = APP_PACKAGE_NAME + ".ACTION_HIDE_OVERLAY";

    /**
     * Intent broadcast action for toogle the omniswitch overlay
     */
    public static final String ACTION_TOGGLE_OVERLAY = APP_PACKAGE_NAME + ".ACTION_TOGGLE_OVERLAY";

    /**
     * Intent broadcast action for restoring the home stack
     */
    public static final String ACTION_RESTORE_HOME_STACK = APP_PACKAGE_NAME + ".ACTION_RESTORE_HOME_STACK";

    /**
     * Intent for launching the omniswitch settings actvity
     */
    public static Intent INTENT_LAUNCH_APP = new Intent(Intent.ACTION_MAIN)
            .setClassName(APP_PACKAGE_NAME, APP_PACKAGE_NAME + ".SettingsActivity");

    // Switches qs tile style to user selected.
    public static void updateTileStyle(IOverlayManager om, int userId, int qsTileStyle) {
        if (qsTileStyle == 0) {
            stockTileStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_TILE_THEMES[qsTileStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs tile icon", e);
            }
        }
    }

    // Switches qs tile style back to stock.
    public static void stockTileStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_TILE_THEMES.length; i++) {
            String qstiletheme = QS_TILE_THEMES[i];
            try {
                om.setEnabled(qstiletheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
