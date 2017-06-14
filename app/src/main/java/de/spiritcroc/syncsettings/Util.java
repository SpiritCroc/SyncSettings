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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
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
        String action;
        Account account;
        String authority;
        Bundle localeBundle =
                intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (localeBundle != null) {
            Log.v(LOG_TAG, "Using locale bundle to get instructions");

            action = localeBundle.getString(Constants.EXTRA_ACTION);
            if (action == null) {
                Log.e(LOG_TAG, "handleAction: EXTRA_BUNDLE missing action");
                return;
            }

            if (localeBundle.containsKey(Constants.EXTRA_ACCOUNT_STRING_ARRAY)) {
                String[] accountStringArray =
                        localeBundle.getStringArray(Constants.EXTRA_ACCOUNT_STRING_ARRAY);
                String[] authorityArray =
                        localeBundle.getStringArray(Constants.EXTRA_AUTHORITY_ARRAY);

                handleAction(context, action, accountStringArray, authorityArray);

                return;
            } else {
                account = getAccountFromBundle(context, localeBundle);
                authority = localeBundle.getString(Constants.EXTRA_AUTHORITY);
            }
        } else {
            Log.v(LOG_TAG, "Locale bundle not found, using direct intent extras for instructions");

            action = intent.getStringExtra(Constants.EXTRA_ACTION);
            if (action == null) {
                Log.e(LOG_TAG, "handleAction: intent missing action");
                return;
            }

            if (intent.hasExtra(Constants.EXTRA_ACCOUNT_STRING_ARRAY)) {
                String[] accountStringArray =
                        intent.getStringArrayExtra(Constants.EXTRA_ACCOUNT_STRING_ARRAY);
                String[] authorityArray =
                        intent.getStringArrayExtra(Constants.EXTRA_AUTHORITY_ARRAY);

                handleAction(context, action, accountStringArray, authorityArray);

                return;
            } else {
                account = getAccountFromIntent(context, intent);
                authority = intent.getStringExtra(Constants.EXTRA_AUTHORITY);
            }
        }

        if (Constants.ACTION_REQUEST_PERMISSIONS.equals(action)) {
            if (!(context instanceof Activity)) {
                Log.w(LOG_TAG, "handleAction request permission: context is no activity");
                return;
            }
            String[] permissions = intent.getStringArrayExtra(Constants.EXTRA_REQUIRED_PERMISSIONS);
            maybeRequestPermissions((Activity) context, permissions);
            return;
        }

        handleAction(context, action, account, authority);
    }

    private static void handleAction(Context context, String action,
                                     Account account, String authority) {
        switch (action) {
            case Constants.ACTION_MASTER_SYNC_ON:
                autoMasterSyncOn();
                break;
            case Constants.ACTION_MASTER_SYNC_OFF:
                autoMasterSyncOff();
                break;
            case Constants.ACTION_MASTER_SYNC_TOGGLE:
                autoMasterSyncToggle();
                break;
            case Constants.ACTION_SYNC_NOW:
                if (accountAndAuthorityValid(context, account, authority)) {
                    syncNow(account, authority);
                }
                break;
            case Constants.ACTION_FORCE_SYNC_NOW:
                if (accountAndAuthorityValid(context, account, authority)) {
                    forceSyncNow(account, authority);
                }
                break;
            case Constants.ACTION_AUTO_SYNC_ON:
                if (accountAndAuthorityValid(context, account, authority)) {
                    autoSyncOn(account, authority);
                }
                break;
            case Constants.ACTION_AUTO_SYNC_OFF:
                if (accountAndAuthorityValid(context, account, authority)) {
                    autoSyncOff(account, authority);
                }
                break;
            case Constants.ACTION_AUTO_SYNC_TOGGLE:
                if (accountAndAuthorityValid(context, account, authority)) {
                    autoSyncToggle(account, authority);
                }
                break;
            default:
                Log.w(LOG_TAG, "handleAction: Unknown action: " + action);
                break;
        }
    }

    private static void handleAction(Context context, String action,
                                     String[] accountStringArray, String[] authorityArray) {
        if (accountStringArray == null) {
            Log.e(LOG_TAG, "accountStringArray = null");
            return;
        }
        if (authorityArray == null) {
            Log.e(LOG_TAG, "accountStringArray = null");
            return;
        }
        if (accountStringArray.length != authorityArray.length) {
            Log.e(LOG_TAG, "accountStringArray.length != authorityArray.length");
            return;
        }
        for (int i = 0; i < accountStringArray.length; i++) {
            handleAction(context, action, getAccount(context, accountStringArray[i]),
                    authorityArray[i]);
        }
    }

    public static Account getAccountFromIntent(Context context, Intent intent) {
        if (intent.hasExtra(Constants.EXTRA_ACCOUNT_STRING)) {
            String accountString = intent.getStringExtra(Constants.EXTRA_ACCOUNT_STRING);
            return getAccount(context, accountString);
        } else if (intent.hasExtra(Constants.EXTRA_ACCOUNT)) {
            // Compatibility with intents from app with versionCode <= 2
            Log.d(LOG_TAG, "getAccountFromIntent: Use old way to get account from intent");
            return intent.getParcelableExtra(Constants.EXTRA_ACCOUNT);
        } else {
            Log.d(LOG_TAG, "getAccountFromIntent: No account extra found in intent");
            return null;
        }
    }

    public static Account getAccountFromBundle(Context context, Bundle bundle) {
        if (bundle.containsKey(Constants.EXTRA_ACCOUNT_STRING)) {
            String accountString = bundle.getString(Constants.EXTRA_ACCOUNT_STRING);
            return getAccount(context, accountString);
        } else {
            Log.d(LOG_TAG, "getAccountFromIntent: No account extra found in intent");
            return null;
        }
    }

    public static Account getAccount(Context context, String accountString) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) ==
                PackageManager.PERMISSION_DENIED) {
            context.startActivity(new Intent(context, ShortcutActivity.class)
                    .putExtra(Constants.EXTRA_ACTION, Constants.ACTION_REQUEST_PERMISSIONS)
                    .putExtra(Constants.EXTRA_REQUIRED_PERMISSIONS,
                            new String[]{Manifest.permission.GET_ACCOUNTS})
            );
            return null;
        }
        Account[] accounts = AccountManager.get(context.getApplicationContext()).getAccounts();
        for (Account account: accounts) {
            if (BuildConfig.DEBUG) Log.v(LOG_TAG, "Found account " + account.toString());
            if (account.toString().equals(accountString)) {
                if (BuildConfig.DEBUG) Log.v(LOG_TAG, "Found account to use");
                return account;
            }
        }
        Log.e(LOG_TAG, "getAccount: could not recover account " + accountString);
        return null;
    }

    private static boolean accountAndAuthorityValid(Context context, Account account,
                                                    String authority) {
        if (account == null) {
            Log.w(LOG_TAG, "account == null");
            Toast.makeText(context.getApplicationContext(),
                    R.string.toast_account_not_found, Toast.LENGTH_LONG
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

    public static String accountToReadableString(Context context, Account account, boolean detailed) {
        PackageManager pm = context.getPackageManager();
        String result = null;
        for (AuthenticatorDescription d: AccountManager.get(context).getAuthenticatorTypes()) {
            if (account.type.equals(d.type)) {
                try {
                    result = pm.getResourcesForApplication(d.packageName).getString(d.labelId);
                } catch (Exception e) {
                    if (DEBUG) {
                        Log.i(LOG_TAG, "accountToReadableString() got exception: "
                                + e.getMessage());
                    }
                }
                break;
            }
        }
        if (TextUtils.isEmpty(result)) {
            result = account.type;
        } else if (detailed) {
            result = context.getString(R.string.sync_account_detailed, result, account.type);
        }
        return context.getString(R.string.sync_account, account.name, result);
    }

    public static String authorityToReadableString(Context context, String authority, boolean detailed) {
        PackageManager pm = context.getPackageManager();
        ProviderInfo pi = pm.resolveContentProvider(authority, 0);
        String result = null;
        if (pi != null) {
            result = (String) pi.loadLabel(pm);
        }
        if (TextUtils.isEmpty(result)) {
            result = authority;
        } else if (detailed) {
            result = context.getString(R.string.sync_authority_detailed, result, authority);
        }
        return result;
    }

    /**
     * In comparison to authorityToReadableString(PackageManager pm, String authority),
     * treats null-authorities as multiple authorities
     */
    public static String authorityToReadableString(Context context, @Nullable String authority) {
        String result = authorityToReadableString(context, authority, false);
        if (result == null) {
            return context.getString(R.string.shortcut_sync_mutliple_authorities);
        }
        return result;
    }

    public static boolean shouldShowSync(Account account, String authority) {
        return ContentResolver.getIsSyncable(account, authority) > 0
                && (!DemonstrationHelper.ENABLED || DemonstrationHelper.isSyncWhiteListed(account, authority));
    }
}
