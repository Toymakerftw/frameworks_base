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

import static android.os.UserHandle.USER_SYSTEM;

import android.app.UiModeManager;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.os.RemoteException;
import android.util.Log;

public class ThemesUtils {

    public static final String TAG = "ThemesUtils";

    private static final String[] SWITCH_THEMES = {
        "com.android.system.switch.stock", // 0
        "com.android.system.switch.oneplus", // 1
	"com.android.system.switch.narrow", // 2
        "com.android.system.switch.contained", // 3
	"com.android.system.switch.telegram", // 4
    };

        "com.android.systemui.qstile.default", // 0
    private static final String[] QS_TILE_THEMES = {
        "com.android.systemui.qstile.circlegradient", // 1
        "com.android.systemui.qstile.circletrim", // 2
        "com.android.systemui.qstile.dottedcircle", // 3
        "com.android.systemui.qstile.dualtonecircle", // 4
        "com.android.systemui.qstile.dualtonecircletrim", // 5
        "com.android.systemui.qstile.ink", // 6
        "com.android.systemui.qstile.inkdrop", // 7
        "com.android.systemui.qstile.mountain", // 8
        "com.android.systemui.qstile.ninja", // 9
        "com.android.systemui.qstile.oreo", // 10
        "com.android.systemui.qstile.oreocircletrim", // 11
        "com.android.systemui.qstile.oreosquircletrim", // 12
        "com.android.systemui.qstile.pokesign", // 13
        "com.android.systemui.qstile.squaremedo", // 14
        "com.android.systemui.qstile.squircle", // 15
        "com.android.systemui.qstile.squircletrim", // 16
        "com.android.systemui.qstile.teardrop", // 17
        "com.android.systemui.qstile.wavey", // 18
        "com.android.systemui.qstile.cookie", //19
        "com.android.systemui.qstile.cosmos", //20
        "com.android.systemui.qstile.dividedcircle", //21
        "com.android.systemui.qstile.justicons", //22
        "com.android.systemui.qstile.neonlike", //23
        "com.android.systemui.qstile.triangle", //24
        "com.android.systemui.qstile.oos", //25
    };

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

    public static void updateSwitchStyle(IOverlayManager om, int userId, int switchStyle) {
        if (switchStyle == 0) {
            stockSwitchStyle(om, userId);
        } else {
            try {
                om.setEnabled(SWITCH_THEMES[switchStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change switch theme", e);
            }
        }
    }

    public static void stockSwitchStyle(IOverlayManager om, int userId) {
        for (int i = 0; i < SWITCH_THEMES.length; i++) {
            String switchtheme = SWITCH_THEMES[i];
            try {
                om.setEnabled(switchtheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
