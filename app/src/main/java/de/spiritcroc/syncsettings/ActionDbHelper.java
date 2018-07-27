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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class ActionDbHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = ActionDbHelper.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static final int VERSION = 1;
    public static final String NAME = "action.db";

    private static final String TEXT = " TEXT";
    private static final String INTEGER = " INTEGER";

    private static final String CREATE_TABLE = "CREATE TABLE ";

    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String UNIQUE = " UNIQUE";
    private static final String NOT_NULL = " NOT NULL";
    private static final String REFERENCES = " REFERENCES ";
    private static final String ON_DELETE = " ON DELETE";
    private static final String ON_CONFLICT = " ON CONFLICT";
    private static final String CASCADE = " CASCADE";
    private static final String IGNORE = " IGNORE";

    private static final String COMMA_SEP = ",";

    private static class DbContract {
        private static class Action {
            private static final String TABLE = "action0";
            private static final String COLUMN_ID = "id";
            private static final String COLUMN_ACTION = "action1";
        }
        private static class ActionAccounts {
            private static final String TABLE = "action_accounts";
            private static final String COLUMN_ACTION_ID = "action_id";
            private static final String COLUMN_ACCOUNT = "account";
            private static final String COLUMN_AUTHORITY = "authority";
        }
    }

    private static final String SQL_CREATE_ACTION =
            CREATE_TABLE + DbContract.Action.TABLE + " (" +
                    DbContract.Action.COLUMN_ID + INTEGER + PRIMARY_KEY + COMMA_SEP +
                    DbContract.Action.COLUMN_ACTION + TEXT + NOT_NULL + ")";

    private static final String SQL_CREATE_ACTION_ACCOUNTS =
            CREATE_TABLE + DbContract.ActionAccounts.TABLE + " (" +
                    DbContract.ActionAccounts.COLUMN_ACTION_ID + INTEGER + REFERENCES +
                            DbContract.Action.TABLE + "(" + DbContract.Action.COLUMN_ID + ")" +
                                    ON_DELETE + CASCADE + COMMA_SEP +
                    DbContract.ActionAccounts.COLUMN_ACCOUNT + TEXT + NOT_NULL + COMMA_SEP +
                    DbContract.ActionAccounts.COLUMN_AUTHORITY + TEXT + NOT_NULL + COMMA_SEP +
                    UNIQUE + "(" + DbContract.ActionAccounts.COLUMN_ACTION_ID + COMMA_SEP +
                            DbContract.ActionAccounts.COLUMN_ACCOUNT + COMMA_SEP +
                            DbContract.ActionAccounts.COLUMN_AUTHORITY + ")" +
                    ON_CONFLICT + IGNORE + ")";

    public ActionDbHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ACTION);
        db.execSQL(SQL_CREATE_ACTION_ACCOUNTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
            throws UnsupportedUpgradeException{
        throw new UnsupportedUpgradeException(oldVersion, newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign key on update/delete functionality
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    }

    public static class UnsupportedUpgradeException extends RuntimeException {
        public final int oldVersion;
        public final int newVersion;

        public UnsupportedUpgradeException(int oldVersion, int newVersion) {
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }
    }

    public static class Action {
        public String action;
        public String[] accounts;
        public String[] authorities;
        public Action(String action, String[] accounts, String[] authorities) {
            this(action);
            this.accounts = accounts;
            this.authorities = authorities;
        }
        public Action(String action) {
            this.action = action;
        }
        public long getActionId(SQLiteDatabase db) {
            long id = -1;
            if (action == null) {
                Log.wtf(LOG_TAG, "Action missing action string");
            } else if ((accounts == null) != (authorities == null)) {
                Log.wtf(LOG_TAG, "Inconsistent account and authority length");
            } else {
                // Try to find existing id for same action
                ArrayList<String> restrictions = new ArrayList<>();
                restrictions.add(DbContract.Action.COLUMN_ACTION + " = '" + action + "'");
                if (accounts == null) {
                    // We have already checked whether accounts and authorities are fine together
                    accounts = new String[0];
                    authorities = new String[0];
                }
                if (accounts.length > 0) {
                    for (int i = 0; i < accounts.length; i++) {
                        restrictions.add("EXISTS (SELECT * FROM " +
                                DbContract.ActionAccounts.TABLE + " WHERE " +
                                DbContract.ActionAccounts.TABLE + "." +
                                        DbContract.ActionAccounts.COLUMN_ACTION_ID + " = " +
                                DbContract.Action.TABLE + "." +
                                        DbContract.Action.COLUMN_ID +
                                " AND " + DbContract.ActionAccounts.COLUMN_ACCOUNT + " = '" +
                                accounts[i] +
                                "' AND " + DbContract.ActionAccounts.COLUMN_AUTHORITY + " = '" +
                                authorities[i] + "')");
                    }
                    // Make sure no additional accounts in the action id
                    String lastRestriction = "NOT EXISTS (SELECT * FROM " +
                            DbContract.ActionAccounts.TABLE + " WHERE " +
                            DbContract.ActionAccounts.TABLE + "." +
                                    DbContract.ActionAccounts.COLUMN_ACTION_ID + " = " +
                            DbContract.Action.TABLE + "." +
                                    DbContract.Action.COLUMN_ID + " AND (";
                    // Not equals
                    lastRestriction += "(" +
                            DbContract.ActionAccounts.COLUMN_ACCOUNT + " <> '" +
                                    accounts[0] +
                            "' OR " + DbContract.ActionAccounts.COLUMN_AUTHORITY + " <> '" +
                                    authorities[0] + "')";
                    for (int i = 1; i < accounts.length; i++) {
                        // Not equals
                        lastRestriction += " AND (" +
                                DbContract.ActionAccounts.COLUMN_ACCOUNT + " <> '" +
                                accounts[i] +
                                "' OR " + DbContract.ActionAccounts.COLUMN_AUTHORITY + " <> '" +
                                authorities[i] + "')";
                    }
                    lastRestriction += "))";
                    restrictions.add(lastRestriction);
                } else {
                    restrictions.add("NOT EXISTS (SELECT * FROM " +
                            DbContract.ActionAccounts.TABLE + " WHERE " +
                            DbContract.ActionAccounts.TABLE + "." +
                                    DbContract.ActionAccounts.COLUMN_ACTION_ID + " = " +
                            DbContract.Action.TABLE + "." +
                                    DbContract.Action.COLUMN_ID + ")");
                }
                String completeRestriction = "(" + restrictions.get(0) + ")";
                for (int i = 1; i < restrictions.size(); i++) {
                    completeRestriction += " AND (" + restrictions.get(i) + ")";
                }

                String[] projection = new String[] {
                        DbContract.Action.COLUMN_ID,
                };
                // Get action id from db if already exists
                Cursor cursor = db.query(DbContract.Action.TABLE, projection,
                        completeRestriction, null, null, null, null);
                if (cursor.moveToFirst()) {
                    id = cursor.getLong(cursor.getColumnIndex(DbContract.Action.COLUMN_ID));
                    if (DEBUG) {
                        Log.d(LOG_TAG, "Found existing id for action: " + id);
                    }
                    if (cursor.moveToNext()) {
                        Log.w(LOG_TAG, "Multiple identical actions found, using first found");
                    }
                }
                cursor.close();
                if (id == -1) {
                    // New action
                    id = System.currentTimeMillis();
                    if (DEBUG) {
                        Log.d(LOG_TAG, "Created new id for action: " + id);
                    }
                    ContentValues values = new ContentValues();
                    values.put(DbContract.Action.COLUMN_ID, id);
                    values.put(DbContract.Action.COLUMN_ACTION, action);
                    db.insert(DbContract.Action.TABLE, "null", values);
                    for (int i = 0; i < accounts.length; i++) {
                        values = new ContentValues();
                        values.put(DbContract.ActionAccounts.COLUMN_ACTION_ID, id);
                        values.put(DbContract.ActionAccounts.COLUMN_ACCOUNT, accounts[i]);
                        values.put(DbContract.ActionAccounts.COLUMN_AUTHORITY, authorities[i]);
                        db.insert(DbContract.ActionAccounts.TABLE, "null", values);
                    }
                }
            }
            return id;
        }
        public static Action createFromActionId(SQLiteDatabase db, long id) {
            String[] projection = new String[] {
                     DbContract.Action.COLUMN_ACTION,
            };
            String selection = DbContract.Action.COLUMN_ID + " = " + id;
            Cursor cursor = db.query(DbContract.Action.TABLE, projection, selection,
                    null, null, null, null);
            if (!cursor.moveToFirst()) {
                Log.e(LOG_TAG, "Action for id " + id +
                        " not found, action shortcut might be outdated");
                return null;
            }
            int actionIndex = cursor.getColumnIndex(DbContract.Action.COLUMN_ACTION);
            Action result = new Action(cursor.getString(actionIndex));
            if (cursor.moveToNext()) {
                Log.wtf(LOG_TAG, cursor.getCount() +
                        "different actions produced by SQL join statement");
            }
            cursor.close();
            projection = new String[] {
                    DbContract.ActionAccounts.COLUMN_ACCOUNT,
                    DbContract.ActionAccounts.COLUMN_AUTHORITY,
            };
            selection = DbContract.ActionAccounts.COLUMN_ACTION_ID + " = " + id;
            cursor = db.query(DbContract.ActionAccounts.TABLE, projection, selection,
                    null, null, null, null);
            int accountIndex = cursor.getColumnIndex(DbContract.ActionAccounts.COLUMN_ACCOUNT);
            int authorityIndex = cursor.getColumnIndex(DbContract.ActionAccounts.COLUMN_AUTHORITY);
            result.accounts = new String[cursor.getCount()];
            result.authorities = new String[result.accounts.length];
            if (cursor.moveToFirst()) {
                do {
                    int pos = cursor.getPosition();
                    result.accounts[pos] = cursor.getString(accountIndex);
                    result.authorities[pos] = cursor.getString(authorityIndex);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
    }
}
