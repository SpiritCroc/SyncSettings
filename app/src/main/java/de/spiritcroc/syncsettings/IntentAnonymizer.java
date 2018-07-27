/*
 * Copyright (C) 2018 SpiritCroc
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * For better privacy, we want to hide account information from external applications.
 * To achieve this, we anonymize intents by returning an action id that we're storing internal
 * for specific account/action combinations.
 */
public class IntentAnonymizer {
    private static final String LOG_TAG = IntentAnonymizer.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static Intent anonymizeIntent(Context context, Intent intent) {
        Bundle b = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (b == null) {
            if (DEBUG) {
                Log.d(LOG_TAG, "No data found to anonymize");
            }
            return intent;
        }
        String accounts[] = null;
        String authorities[] = null;
        String action = null;

        // Read and remove account information
        if (b.containsKey(Constants.EXTRA_ACCOUNT_STRING)) {
            accounts = new String[]{b.getString(Constants.EXTRA_ACCOUNT_STRING)};
            b.remove(Constants.EXTRA_ACCOUNT_STRING);
        }
        if (b.containsKey(Constants.EXTRA_ACCOUNT_STRING_ARRAY)) {
            if (accounts != null) {
                Log.e(LOG_TAG, "anonymizeIntent: ambiguous account extra");
            }
            accounts = b.getStringArray(Constants.EXTRA_ACCOUNT_STRING_ARRAY);
            b.remove(Constants.EXTRA_ACCOUNT_STRING_ARRAY);
        }

        // Read and remove authority information
        if (b.containsKey(Constants.EXTRA_AUTHORITY)) {
            authorities = new String[]{b.getString(Constants.EXTRA_AUTHORITY)};
            b.remove(Constants.EXTRA_AUTHORITY);
        }
        if (b.containsKey(Constants.EXTRA_AUTHORITY_ARRAY)) {
            if (authorities != null) {
                Log.e(LOG_TAG, "anonymizeIntent: ambiguous authority extra");
            }
            authorities = b.getStringArray(Constants.EXTRA_AUTHORITY_ARRAY);
            b.remove(Constants.EXTRA_AUTHORITY_ARRAY);
        }

        // Read and remove action information
        if (b.containsKey(Constants.EXTRA_ACTION)) {
            action = b.getString(Constants.EXTRA_ACTION);
            b.remove(Constants.EXTRA_ACTION);
        }

        // Ensure consistency
        if ((accounts == null) != (authorities == null)) {
            Log.wtf(LOG_TAG, "anonymizeIntent: inconsistent account and authorities extras");
        } else if (accounts != null) {
            if (action == null) {
                Log.wtf(LOG_TAG, "anonymizeIntent: no action found for accounts");
            } else if (accounts.length != authorities.length) {
                Log.wtf(LOG_TAG, "anonymizeIntent: inconsistent account and authorities lengths");
            } else {
                // Get anonymized action ID
                ActionDbHelper dbHelper = new ActionDbHelper(context);
                b.putLong(Constants.EXTRA_ANONYMIZED_ACTION,
                        new ActionDbHelper.Action(action, accounts, authorities).getActionId(
                                dbHelper.getWritableDatabase()
                        ));
                dbHelper.close();
            }
        } else {
            // Get anonymized action ID for master sync action
            ActionDbHelper dbHelper = new ActionDbHelper(context);
            b.putLong(Constants.EXTRA_ANONYMIZED_ACTION,
                    new ActionDbHelper.Action(action).getActionId(
                            dbHelper.getWritableDatabase()
                    ));
            dbHelper.close();
        }
        intent.removeExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, b);
        return intent;
    }

    public static Intent deanonymizeIntent(Context context, Intent intent) {
        ActionDbHelper dbHelper = new ActionDbHelper(context);
        Bundle b = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (b == null) {
            if (DEBUG) {
                Log.d(LOG_TAG, "No data found to deanonymize");
            }
            return intent;
        }
        if (DEBUG) {
            Log.d(LOG_TAG, "Anonymized intent contains: " + bundleContentDebug(b));
        }
        // Convert anonymized extra to better readable extras for processing
        if (b.containsKey(Constants.EXTRA_ANONYMIZED_ACTION)) {
            long id = b.getLong(Constants.EXTRA_ANONYMIZED_ACTION, -1);
            b.remove(Constants.EXTRA_ANONYMIZED_ACTION);
            ActionDbHelper.Action action = ActionDbHelper.Action.createFromActionId(
                    dbHelper.getReadableDatabase(), id);
            dbHelper.close();
            if (action == null) {
                Log.e(LOG_TAG, "Deanonymizing intent failed");
            } else {
                b.putString(Constants.EXTRA_ACTION, action.action);
                if (action.accounts.length > 0) {
                    b.putStringArray(Constants.EXTRA_ACCOUNT_STRING_ARRAY, action.accounts);
                }
                if (action.authorities.length > 0) {
                    b.putStringArray(Constants.EXTRA_AUTHORITY_ARRAY, action.authorities);
                }
            }
        } else {
            // legacy intent
            if (DEBUG) {
                Log.d(LOG_TAG, "Unanonymized legacy intent");
            }
        }
        if (DEBUG) {
            Log.d(LOG_TAG, "Deanonymized intent contains: " + bundleContentDebug(b));
        }
        intent.removeExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, b);
        return intent;
    }

    private static String bundleContentDebug(Bundle b) {
        String result = "";
        String[] extras = {
                Constants.EXTRA_ANONYMIZED_ACTION,
                Constants.EXTRA_ACTION,
                Constants.EXTRA_ACCOUNT_STRING,
                Constants.EXTRA_ACCOUNT_STRING_ARRAY,
                Constants.EXTRA_AUTHORITY,
                Constants.EXTRA_AUTHORITY_ARRAY,
        };
        for (String e: extras) {
            result += "\n" + e + (b.containsKey(e) ? " included" : " not included");
        }
        return result;
    }
}
