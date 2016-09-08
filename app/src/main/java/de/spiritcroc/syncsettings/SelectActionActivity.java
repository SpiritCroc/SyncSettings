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
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SelectActionActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG = SelectActionActivity.class.getSimpleName();

    private Account account;
    private String authority;

    private Button syncNowButton;
    private Button forceSyncNowButton;
    private Button autoSyncOnButton;
    private Button autoSyncOffButton;
    private Button autoSyncToggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_action);

        Intent creationIntent = getIntent();
        account= Util.getAccountFromIntent(this, creationIntent);
        authority = creationIntent.getStringExtra(Constants.EXTRA_AUTHORITY);

        if (account == null) {
            Log.w(LOG_TAG, "account == null");
            Toast.makeText(getApplicationContext(),
                    R.string.toast_an_error_occurred, Toast.LENGTH_SHORT
            ).show();
            finish();
        } else if (authority == null) {
            Log.w(LOG_TAG, "authority == null");
            Toast.makeText(getApplicationContext(),
                    R.string.toast_an_error_occurred, Toast.LENGTH_SHORT
            ).show();
            finish();
        }

        syncNowButton = (Button) findViewById(R.id.button_sync_now);
        forceSyncNowButton = (Button) findViewById(R.id.button_force_sync_now);
        autoSyncOnButton = (Button) findViewById(R.id.button_auto_sync_on);
        autoSyncOffButton = (Button) findViewById(R.id.button_auto_sync_off);
        autoSyncToggleButton = (Button) findViewById(R.id.button_auto_sync_toggle);

        syncNowButton.setOnClickListener(this);
        forceSyncNowButton.setOnClickListener(this);
        autoSyncOnButton.setOnClickListener(this);
        autoSyncOffButton.setOnClickListener(this);
        autoSyncToggleButton.setOnClickListener(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(account.name);
        }
    }

    @Override
    public void onClick(View view) {
        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_ACCOUNT_STRING, account.toString());
        if (BuildConfig.DEBUG) Log.v(LOG_TAG, "put extra for intent: EXTRA_ACCOUNT_STRING=" + account.toString());
        result.putExtra(Constants.EXTRA_AUTHORITY, authority);
        if (BuildConfig.DEBUG) Log.v(LOG_TAG, "put extra for intent: EXTRA_AUTHORITY=" + authority);

        if (view.equals(syncNowButton)) {
            result.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_SYNC_NOW);
            result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    getString(R.string.shortcut_sync_now, account.name, authority)
            );
        } else if (view.equals(forceSyncNowButton)) {
            result.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_FORCE_SYNC_NOW);
            result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    getString(R.string.shortcut_sync_now, account.name, authority)
            );
        } else if (view.equals(autoSyncOnButton)) {
            Util.maybeRequestPermissions(this,
                    new String[]{Manifest.permission.WRITE_SYNC_SETTINGS}
            );
            result.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_AUTO_SYNC_ON);
            result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    getString(R.string.shortcut_auto_sync_on, account.name, authority)
            );
        } else if (view.equals(autoSyncOffButton)) {
            Util.maybeRequestPermissions(this,
                    new String[]{Manifest.permission.WRITE_SYNC_SETTINGS}
            );
            result.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_AUTO_SYNC_OFF);
            result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    getString(R.string.shortcut_auto_sync_off, account.name, authority)
            );
        } else if (view.equals(autoSyncToggleButton)) {
            Util.maybeRequestPermissions(this,
                    new String[]{Manifest.permission.READ_SYNC_SETTINGS,
                            Manifest.permission.WRITE_SYNC_SETTINGS}
            );
            result.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_AUTO_SYNC_TOGGLE);
            result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    getString(R.string.shortcut_auto_sync_toggle, account.name, authority)
            );
        } else {
            Log.w(LOG_TAG, "onClick for unknown view");
            return;// Don't finish
        }

        finishWithResult(result);
    }

    private void finishWithResult(Intent data) {
        setResult(RESULT_OK, data);
        finish();
    }
}
