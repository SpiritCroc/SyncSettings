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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class ShortcutActivity extends AppCompatActivity {

    private static final int REQUEST_SHORTCUT = 2;

    private Intent resultIntent;

    private EditText editShortcutText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();

        boolean createShortcut = !intent.hasExtra(Constants.EXTRA_ACTION) &&
                !intent.hasExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

        if (createShortcut) {
            // Set theme before super.onCreate()
            // Default theme is TransparentTheme so the activity is invisible when launching
            // shortcuts
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);

        if (createShortcut) {
            // Create shortcut
            setContentView(R.layout.activity_shortcut);
            startActivityForResult(
                    new Intent(getApplicationContext(), SelectSyncActivity.class),
                    REQUEST_SHORTCUT
            );

            editShortcutText = (EditText) findViewById(R.id.edit_shortcut_name);
        } else {
            // Handle shortcut
            if (!Constants.ACTION_REQUEST_PERMISSIONS.equals(
                    intent.getStringExtra(Constants.EXTRA_ACTION))) {
                finish();
            }
            Util.handleAction(this, intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_shortcut, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.next:
                if (resultIntent != null) {
                    String shortcutText = editShortcutText.getText().toString();
                    resultIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutText);
                    setResult(RESULT_OK, resultIntent);
                }
            case R.id.cancel:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Edit the shortcut name
        editShortcutText.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(editShortcutText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SHORTCUT && resultCode == RESULT_OK) {
            data.setClass(getApplicationContext(), ShortcutActivity.class);
            resultIntent = new Intent(Constants.INSTALL_SHORTCUT);
            resultIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, data);
            String shortcutName =
                    data.getStringExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB);
            editShortcutText.setText(shortcutName);
            resultIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource
                            .fromContext(getApplicationContext(), R.mipmap.ic_launcher)
            );
        } else {
            // Abort
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        finish();
    }

}
