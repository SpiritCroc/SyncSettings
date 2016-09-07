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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public abstract class Util {
    private static final String LOG_TAG = Util.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static ArrayList<String> getAuthorities() {
        SyncAdapterType[] syncAdapterTypes = ContentResolver.getSyncAdapterTypes();
        ArrayList<String> syncAuthorities = new ArrayList<>();
        for (SyncAdapterType type: syncAdapterTypes) {
            if (DEBUG) Log.d(LOG_TAG, "Found sync type " + type);
            if (!syncAuthorities.contains(type.authority)) {
                syncAuthorities.add(type.authority);
                if (DEBUG) Log.d(LOG_TAG, "Added sync authority " + type.authority);
            }
        }
        return syncAuthorities;
    }

    public static void handleAction(Context context, Intent intent) {
        String action = intent.getStringExtra(Constants.EXTRA_ACTION);
        if (action == null) {
            Log.w(LOG_TAG, "handleAction: intent has no action set");
            return;
        }
        Account account = getAccountFromIntent(context, intent);
        String authority = intent.getStringExtra(Constants.EXTRA_AUTHORITY);
        switch (action) {
            case Constants.ACTION_MASTER_SYNC_ON:
                Util.autoMasterSyncOn();
                break;
            case Constants.ACTION_MASTER_SYNC_OFF:
                Util.autoMasterSyncOff();
                break;
            case Constants.ACTION_MASTER_SYNC_TOGGLE:
                Util.autoMasterSyncToggle();
                break;
            case Constants.ACTION_SYNC_NOW:
                if (accountAndAuthorityValid(context, account, authority)) {
                    Util.syncNow(account, authority);
                }
                break;
            case Constants.ACTION_FORCE_SYNC_NOW:
                if (accountAndAuthorityValid(context, account, authority)) {
                    Util.forceSyncNow(account, authority);
                }
                break;
            case Constants.ACTION_AUTO_SYNC_ON:
                if (accountAndAuthorityValid(context, account, authority)) {
                    Util.autoSyncOn(account, authority);
                }
                break;
            case Constants.ACTION_AUTO_SYNC_OFF:
                if (accountAndAuthorityValid(context, account, authority)) {
                    Util.autoSyncOff(account, authority);
                }
                break;
            case Constants.ACTION_AUTO_SYNC_TOGGLE:
                if (accountAndAuthorityValid(context, account, authority)) {
                    Util.autoSyncToggle(account, authority);
                }
                break;
            default:
                Log.w(LOG_TAG, "handleAction: Unknown action: " + intent.getAction());
                break;
        }
    }

    public static Account getAccountFromIntent(Context context, Intent intent) {
        if (intent.hasExtra(Constants.EXTRA_ACCOUNT_STRING)) {
            String accountString = intent.getStringExtra(Constants.EXTRA_ACCOUNT_STRING);
            Account[] accounts = AccountManager.get(context.getApplicationContext()).getAccounts();
            for (Account account: accounts) {
                if (account.toString().equals(accountString)) {
                    return account;
                }
            }
            Log.e(LOG_TAG, "getAccountFromIntent: could not recover account " + accountString);
            return null;
        } else if (intent.hasExtra(Constants.EXTRA_ACCOUNT)) {
            // Compatibility with intents from app with versionCode <= 2
            Log.d(LOG_TAG, "getAccountFromIntent: Use old way to get account from intent");
            return intent.getParcelableExtra(Constants.EXTRA_ACCOUNT);
        } else {
            Log.d(LOG_TAG, "getAccountFromIntent: No account extra found in intent");
            return null;
        }
    }

    private static boolean accountAndAuthorityValid(Context context, Account account,
                                                    String authority) {
        if (account == null) {
            Log.w(LOG_TAG, "account == null");
            Toast.makeText(context.getApplicationContext(),
                    R.string.toast_an_error_occurred, Toast.LENGTH_SHORT
            ).show();
            return false;
        } else if (authority == null) {
            Log.w(LOG_TAG, "authority == null");
            Toast.makeText(context.getApplicationContext(),
                    R.string.toast_an_error_occurred, Toast.LENGTH_SHORT
            ).show();
            return false;
        } else {
            return true;
        }
    }

    public static void syncNow(Account account, String authority) {
        Bundle extras = new Bundle();
        ContentResolver.requestSync(account, authority, extras);
    }

    public static void forceSyncNow(Account account, String authority) {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true);
        ContentResolver.requestSync(account, authority, extras);
    }

    public static void autoSyncOn(Account account, String authority) {
        if (DEBUG) {
            Log.d(LOG_TAG, "sync on " + account + ", " + authority);
            Log.d(LOG_TAG, "previous " +
                    ContentResolver.getSyncAutomatically(account, authority));
        }
        ContentResolver.setSyncAutomatically(account, authority, true);
        if (DEBUG) {
            Log.d("syncsettings", "now " +
                    ContentResolver.getSyncAutomatically(account, authority));
        }
    }

    public static void autoSyncOff(Account account, String authority) {
        if (DEBUG) {
            Log.d(LOG_TAG, "sync off " + account + ", " + authority);
            Log.d(LOG_TAG, "previous " +
                    ContentResolver.getSyncAutomatically(account, authority));
        }
        ContentResolver.setSyncAutomatically(account, authority, false);
        if (DEBUG) {
            Log.d("syncsettings", "now " +
                    ContentResolver.getSyncAutomatically(account, authority));
        }
    }

    public static void autoSyncToggle(Account account, String authority) {
        if (DEBUG) {
            Log.d(LOG_TAG, "sync toggle " + account + ", " + authority);
            Log.d(LOG_TAG, "previous " +
                    ContentResolver.getSyncAutomatically(account, authority));
        }
        boolean autoSync = ContentResolver.getSyncAutomatically(account, authority);
        ContentResolver.setSyncAutomatically(account, authority, !autoSync);
        if (DEBUG) {
            Log.d("syncsettings", "now " +
                    ContentResolver.getSyncAutomatically(account, authority));
        }
    }

    public static void autoMasterSyncOn() {
        ContentResolver.setMasterSyncAutomatically(true);
    }

    public static void autoMasterSyncOff() {
        ContentResolver.setMasterSyncAutomatically(false);
    }

    public static void autoMasterSyncToggle() {
        boolean autoSync = ContentResolver.getMasterSyncAutomatically();
        ContentResolver.setMasterSyncAutomatically(!autoSync);
    }

    public static void maybeRequestPermissions(Activity activity, String[] permissions) {
        ArrayList<String> denied = new ArrayList<>();
        for (String permission: permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) ==
                    PackageManager.PERMISSION_DENIED) {
                denied.add(permission);
            }
        }
        if (!denied.isEmpty()) {
            String[] require = denied.toArray(new String[denied.size()]);
            if (DEBUG) {
                for (String s: require) {
                    Log.d(LOG_TAG, "Require permission " + s);
                }
            }
            ActivityCompat.requestPermissions(activity, require, 0);
        }
    }
}
