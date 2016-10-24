/*
 * Copyright (C) 2015 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.syncsettings;

public abstract class Constants {

    // According to the AOSP browser code, there is no public string defining this intent so if Home
    // changes the value, this string has to get updated:
    public static final String INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    public static final String EXTRA_ACTION =
            "de.spiritcroc.syncsettings.extra.ACTION";
    public static final String EXTRA_PREVIOUS_ACTION =
            "de.spiritcroc.syncsettings.extra.PREVIOUS_ACTION";
    @Deprecated // Use EXTRA_ACCOUNT_STRING instead
    public static final String EXTRA_ACCOUNT =
            "de.spiritcroc.syncsettings.extra.ACCOUNT";
    public static final String EXTRA_ACCOUNT_STRING =
            "de.spiritcroc.syncsettings.extra.ACCOUNT_STRING";
    public static final String EXTRA_AUTHORITY =
            "de.spiritcroc.syncsettings.extra.AUTHORITY";
    public static final String EXTRA_ACCOUNT_STRING_ARRAY =
            "de.spiritcroc.syncsettings.extra.ACCOUNT_STRING_ARRAY";
    public static final String EXTRA_AUTHORITY_ARRAY =
            "de.spiritcroc.syncsettings.extra.AUTHORITY_ARRAY";

    public static final String ACTION_MASTER_SYNC_ON =
            "de.spiritcroc.syncsettings.action.MASTER_SYNC_ON";
    public static final String ACTION_MASTER_SYNC_OFF =
            "de.spiritcroc.syncsettings.action.MASTER_SYNC_OFF";
    public static final String ACTION_MASTER_SYNC_TOGGLE =
            "de.spiritcroc.syncsettings.action.MASTER_SYNC_TOGGLE";
    public static final String ACTION_SYNC_NOW =
            "de.spiritcroc.syncsettings.action.SYNC_NOW";
    public static final String ACTION_FORCE_SYNC_NOW =
            "de.spiritcroc.syncsettings.action.FORCE_SYNC_NOW";
    public static final String ACTION_AUTO_SYNC_ON =
            "de.spiritcroc.syncsettings.action.AUTO_SYNC_ON";
    public static final String ACTION_AUTO_SYNC_OFF =
            "de.spiritcroc.syncsettings.action.AUTO_SYNC_OFF";
    public static final String ACTION_AUTO_SYNC_TOGGLE =
            "de.spiritcroc.syncsettings.action.AUTO_SYNC_TOGGLE";

    public static final String ACTION_REQUEST_PERMISSIONS =
            "de.spiritcroc.syncsettings.action.REQUEST_PERMISSIONS";
    public static final String EXTRA_REQUIRED_PERMISSIONS =
            "de.spiritcroc.syncsettings.extra.REQUIRED_PERMISSIONS";

}
