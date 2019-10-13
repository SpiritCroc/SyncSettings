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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Good guide for using SimpleExpandableListAdapter:
 * http://blog.denevell.org/android-SimpleExpandableListAdapter-example.html
 */

public class SelectSyncActivity extends AppCompatActivity {
    private static final String LOG_TAG = SelectSyncActivity.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG && false;

    private static final int REQUEST_SELECT_ACTION = 1;
    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 2;
    private static final int REQUEST_ACCOUNT = 3;

    private ExpandableListView listView;
    private SimpleCheckableExpandableListAdapter listAdapter;
    private ArrayList<Account> groups;
    private ArrayList<Sync> syncs;

    private boolean multiSelectMode = false;
    private boolean detailedInformation = false;
    private ArrayList<Sync> multiSelectSyncs = new ArrayList<>();
    private ArrayList<Sync> initSelectedSyncs = new ArrayList<>();
    private ArrayList<SyncListPos> initSelectedSyncPositions = new ArrayList<>();

    private boolean masterSyncGroupWasExpanded = false;
    private int addAccountGroupPosition = -1;

    private boolean hasAccountsPermission = true;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_sync);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_select_sync);
        }

        listView = (ExpandableListView) findViewById(R.id.list_view);
        groups = new ArrayList<>();
        syncs = new ArrayList<>();

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                if (DEBUG) Log.d(LOG_TAG, "Click " + groupPosition + "/" + childPosition);

                CheckBox cb;
                if (multiSelectMode) {
                    cb = (CheckBox) v.findViewById(android.R.id.checkbox);
                    cb.toggle();
                } else {
                    cb = null;
                }

                return onSyncClick(groupPosition, childPosition, cb);
            }
        });
        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition,
                                        long id) {
                return SelectSyncActivity.this.onGroupClick(groupPosition);
            }
        });

        boolean expandFirst = false;

        Intent intent = IntentAnonymizer.deanonymizeIntent(this, getIntent());
        if (intent.hasExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE)) {
            // Create pre-selection from previous configuration
            Bundle localeBundle =
                    intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (localeBundle == null) {
                Log.w(LOG_TAG, "Intent has locale bundle which is null");
            } else {
                if (localeBundle.containsKey(Constants.EXTRA_ACCOUNT_STRING)) {
                    addInitSync(localeBundle.getString(Constants.EXTRA_ACCOUNT_STRING),
                            localeBundle.getString(Constants.EXTRA_AUTHORITY));
                } else if (localeBundle.containsKey(Constants.EXTRA_ACCOUNT_STRING_ARRAY)) {
                    String[] accountStringArray =
                            localeBundle.getStringArray(Constants.EXTRA_ACCOUNT_STRING_ARRAY);
                    String[] authorityArray =
                            localeBundle.getStringArray(Constants.EXTRA_AUTHORITY_ARRAY);
                    if (accountStringArray != null && authorityArray != null &&
                            accountStringArray.length == authorityArray.length) {
                        for (int i = 0; i < accountStringArray.length; i++) {
                            addInitSync(accountStringArray[i], authorityArray[i]);
                        }
                    } else {
                        Log.w(LOG_TAG, "Invalid sync array extras");
                    }
                } else {
                    // Probably master action used
                    expandFirst = true;
                }
            }
            if (initSelectedSyncs.size() > 1) {
                multiSelectMode = true;
            }
            multiSelectSyncs = (ArrayList<Sync>) initSelectedSyncs.clone();
        }

        updateHasAccountsPermission();
        loadSyncs(null);
        if (!hasAccountsPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS},
                    REQUEST_PERMISSION_GET_ACCOUNTS
            );
        }

        if (expandFirst) {
            listView.expandGroup(0);
        }

        // Missing account permission information view click listener
        findViewById(R.id.missing_account_permission_text).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (updateHasAccountsPermission()) {
            loadSyncs(0);
        }
    }

    private boolean updateHasAccountsPermission() {
        boolean hasAccountsPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED;
        if (hasAccountsPermission != this.hasAccountsPermission) {
            this.hasAccountsPermission = hasAccountsPermission;
            return true;
        } else {
            return false;
        }
    }

    private void addInitSync(String accountString, String authority) {
        if (accountString == null || authority == null) {
            return;
        }
        Account account = Util.getAccount(this, accountString);
        if (account == null) {
            return;
        }
        initSelectedSyncs.add(new Sync(account, authority));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_sync, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.next).setVisible(multiSelectMode && !multiSelectSyncs.isEmpty());
        MenuItem multiSelect = menu.findItem(R.id.multi_select);
        multiSelect.setChecked(multiSelectMode);

        menu.findItem(R.id.select_all).setVisible(multiSelectMode &&
                multiSelectSyncs.size() < syncs.size());
        menu.findItem(R.id.undo_selection).setVisible(multiSelectMode &&
                !multiSelectSyncs.isEmpty());

        menu.findItem(R.id.expand_all).setVisible(listAdapter != null &&
                !listAdapter.allGroupsExpanded());
        menu.findItem(R.id.collapse_all).setVisible(listAdapter != null &&
                !listAdapter.allGroupsCollapsed());

        MenuItem detailed = menu.findItem(R.id.detailed_information);
        detailed.setChecked(detailedInformation);

        // Some options only make sense combined with account access
        if (hasAccountsPermission) {
            detailed.setVisible(true);
            multiSelect.setVisible(true);
        } else {
            // If somehow default values were changed before account access was denied,
            // still allow to return to default value
            detailed.setVisible(detailedInformation);
            multiSelect.setVisible(multiSelectMode);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.next:
                selectMultiSyncAction();
                return true;
            case R.id.multi_select:
                multiSelectMode = !multiSelectMode;
                item.setChecked(multiSelectMode);
                int groupOffset = multiSelectMode ? -1 : 1;
                loadSyncs(groupOffset);
                invalidateOptionsMenu();
                return true;
            case R.id.select_all:
                selectAll();
                return true;
            case R.id.undo_selection:
                undoSelection();
                return true;
            case R.id.expand_all:
                listAdapter.expandAll(listView);
                return true;
            case R.id.collapse_all:
                listAdapter.collapseAll(listView);
                return true;
            case R.id.detailed_information:
                detailedInformation = !detailedInformation;
                item.setChecked(detailedInformation);
                loadSyncs(0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_ACTION:
                if (resultCode == RESULT_OK) {
                    finishWithResult(data);
                }
                break;
            case REQUEST_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    updateHasAccountsPermission();
                    loadSyncs(null);
                }
                break;
            default:
                Log.w(LOG_TAG, "onActivityResult: unhandled requestCode " + requestCode);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                          @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_GET_ACCOUNTS) {
            updateHasAccountsPermission();
            loadSyncs(null);
        }
    }

    private void finishWithResult(Intent data) {
        data = IntentAnonymizer.anonymizeIntent(this, data);
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * @param groupOffsetToPrevious
     * Null if no previous state or new state not describable using offset to previous
     */
    private void loadSyncs(Integer groupOffsetToPrevious) {

        ArrayList<Integer> expandedGroups;
        int listPosition = 0, listPositionOffset = 0;
        if (listAdapter == null || groupOffsetToPrevious == null) {
            expandedGroups = null;
        } else {
            expandedGroups = listAdapter.getExpandedGroups();
            listPosition = listView.getFirstVisiblePosition();
            View v = listView.getChildAt(0);
            listPositionOffset = (v == null ? 0 : v.getTop());
            if (groupOffsetToPrevious == -1) {
                masterSyncGroupWasExpanded = expandedGroups.contains(0);
            }
        }

        syncs.clear();
        /* Remember selected syncs
        if (groupOffsetToPrevious != null && groupOffsetToPrevious != 0) {
            // We changed the list content, so reset selected syncs
            multiSelectSyncs = (ArrayList<Sync>) initSelectedSyncs.clone();
        }
        */
        initSelectedSyncPositions.clear();

        if (!multiSelectMode) {
            // Syncs with no account will be master sync setting
            syncs.add(new Sync(null, getString(R.string.sync_master_on)));
            syncs.add(new Sync(null, getString(R.string.sync_master_off)));
            syncs.add(new Sync(null, getString(R.string.sync_master_toggle)));

            // Create pre-selection from previous configuration
            Intent intent = getIntent();
            Bundle localeBundle =
                    intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (localeBundle != null) {
                String action = localeBundle.getString(Constants.EXTRA_ACTION);
                if (action != null) {
                    int pos;
                    switch (action) {
                        case Constants.ACTION_MASTER_SYNC_ON:
                            pos = 0;
                            break;
                        case Constants.ACTION_MASTER_SYNC_OFF:
                            pos = 1;
                            break;
                        case Constants.ACTION_MASTER_SYNC_TOGGLE:
                            pos = 2;
                            break;
                        default:
                            pos = -1;
                            break;
                    }
                    if (pos >= 0) {
                        initSelectedSyncPositions.add(new SyncListPos(0, pos));
                    }
                }
            }
        }

        Account[] accounts;
        ArrayList<String> authorities;
        if (hasAccountsPermission) {
            // Get user accounts
            AccountManager accountManager = AccountManager.get(getApplicationContext());
            accounts = accountManager.getAccounts();

            // Get authorities
            authorities = Util.getAuthorities();
        } else {
            // Assume we can't see any accounts, because even if we do (because individual access
            // was granted), we cannot operate on them
            accounts = new Account[0];
            authorities = new ArrayList<>();
        }


        // Get available account/authority combinations
        if (DEBUG) Log.d(LOG_TAG, "Found accounts: " + accounts.length);
        for (Account account: accounts) {
            if (DEBUG) Log.d(LOG_TAG, "Found account " + account.name);
            for (int i = 0; i < authorities.size(); i++) {
                if (Util.shouldShowSync(account, authorities.get(i))) {
                    syncs.add(new Sync(account, authorities.get(i)));
                    if (DEBUG) Log.d(LOG_TAG, "Added authority " + authorities.get(i));
                } else {
                    if (DEBUG) Log.d(LOG_TAG, "Ignored authority " + authorities.get(i));
                }
            }
        }

        // Build expandable list
        groups.clear();
        for (int i = 0; i < syncs.size(); i++) {
            if (syncs.get(i).account != null && !groups.contains(syncs.get(i).account)) {
                groups.add(syncs.get(i).account);
            }
        }

        final String ROOT = "ROOT_NAME";
        final String CHILD = "CHILD_NAME";

        List<Map<String, String>> groupData = new ArrayList<Map<String, String>>() {{
            // Count addIndex to track which position some groups are for direct actions
            int addIndex = 0;
            if (!multiSelectMode) {
                // Add master sync group:
                add(new HashMap<String, String>() {{
                    put(ROOT, getString(R.string.sync_master));
                }});
                addIndex++;
            }
            // Add accounts:
            for (int i = 0; i < groups.size(); i++) {
                final int j = i;
                add(new HashMap<String, String>() {{
                    put(ROOT, Util.accountToReadableString(SelectSyncActivity.this, groups.get(j),
                            detailedInformation));
                }});
            }
            // Accounts don't need to track addIndex, so once when done is enough
            addIndex += groups.size();
            // Add entry to add new accounts on devices starting with Oreo
            if (hasAccountsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Add account only makes sense when we have permission to read accounts
                add(new HashMap<String, String>() {{
                    put(ROOT, getString(R.string.sync_add_account));
                }});
                addAccountGroupPosition = addIndex++;
            } else {
                addAccountGroupPosition = -1;
            }
        }};

        final List<List<Map<String, String>>> listOfChildGroups = new ArrayList<>();



        if (!multiSelectMode) {
            // Add master sync items:
            List<Map<String, String>> masterChildGroup = new ArrayList<Map<String, String>>() {{
                for (int j = 0; j < syncs.size(); j++) {
                    final Sync sync = syncs.get(j);
                    if (sync.account == null) {
                        add(new HashMap<String, String>() {{
                            put(CHILD, sync.authority);
                        }});
                    } else {
                        break;
                    }
                }
            }};
            // Add account data:
            listOfChildGroups.add(masterChildGroup);
        }

        final ArrayList<Integer> initExpandedGroups = new ArrayList<>();

        // Add items
        for (int i = 0; i < groups.size(); i++) {
            final int currentGroup = i + (multiSelectMode ? 0 : 1);

            final int x = i;
            List<Map<String, String>> childGroup = new ArrayList<Map<String, String>>(){{
                int childCount = 0;
                for (int j = 0; j < syncs.size(); j++) {
                    final Sync sync = syncs.get(j);
                    if (sync.account != null && sync.account.equals(groups.get(x))) {
                        final String entry = Util.authorityToReadableString(SelectSyncActivity.this,
                                sync.authority, detailedInformation);
                        add(new HashMap<String, String>() {{
                            put(CHILD, entry);
                        }});
                        if (initSelectedSyncs.contains(sync)) {
                            initSelectedSyncPositions.add(new SyncListPos(currentGroup, childCount));
                            if (!initExpandedGroups.contains(currentGroup)) {
                                initExpandedGroups.add(currentGroup);
                            }
                        }
                        childCount++;
                    }
                }
            }};
            listOfChildGroups.add(childGroup);
        }

        if (addAccountGroupPosition != -1) {
            // Add an empty group: we want to handle clicks on that group directly
            listOfChildGroups.add(new ArrayList<Map<String, String>>());
        }

        int itemLayoutId = multiSelectMode ? R.layout.checkable_expandable_list_item :
                android.R.layout.simple_expandable_list_item_1;
        listAdapter = new SimpleCheckableExpandableListAdapter(
                this,
                new SimpleCheckableExpandableListAdapter.OnAdapterUpdateListener() {
                    @Override
                    public void onCheckboxClick(CheckBox cb, int groupPosition, int childPosition) {
                        if (SimpleCheckableExpandableListAdapter.Position.isGroup(childPosition)) {
                            onGroupCheckboxClick(groupPosition, cb);
                        } else {
                            onSyncClick(groupPosition, childPosition, cb);
                        }
                    }
                    @Override
                    public Boolean getCheckedStateFor(int groupPosition, int childPosition) {
                        if (SimpleCheckableExpandableListAdapter.Position.isGroup(childPosition)) {
                            return getGroupState(groupPosition);
                        } else {
                            return multiSelectSyncs
                                    .contains(getSyncForPosition(groupPosition, childPosition));
                        }
                    }
                    @Override
                    public void onGroupExpandOrCollapse() {
                        invalidateOptionsMenu();
                    }
                    @Override
                    public int getTextColorForPosition(int groupPosition, int childPosition) {
                        if (initSelectedSyncPositions.contains(
                                new SyncListPos(groupPosition, childPosition))) {
                            return getResources().getColor(R.color.init_select_text_color);
                        }
                        return getResources().getColor(R.color.default_text_color);
                    }
                    @Override
                    public int getTextColorForGroup(int groupPosition) {
                        if (groupPosition == addAccountGroupPosition) {
                            return getResources().getColor(R.color.action_group_text_color);
                        }
                        return getResources().getColor(R.color.group_text_color);
                    }
                },

                groupData,
                itemLayoutId,
                new String[] {ROOT},
                new int[] {android.R.id.text1},

                listOfChildGroups,
                itemLayoutId,
                new String[] {CHILD},
                new int[] {android.R.id.text1}
        );
        listView.setAdapter(listAdapter);
        if (expandedGroups != null) {
            if (!multiSelectMode && (masterSyncGroupWasExpanded ||
                    expandedGroups.size() == listAdapter.getGroupCount() - 1)) {
                // Expand all groups. Negative group number because of offset
                expandedGroups.add(-1);
            }
            listAdapter.restoreExpandedGroups(listView, expandedGroups, groupOffsetToPrevious);
            int newPosition = listPosition + groupOffsetToPrevious;
            if (groupOffsetToPrevious == 1 && expandedGroups.contains(0)) {
                // Count of master sync settings
                newPosition += 3;
            } else if (masterSyncGroupWasExpanded) {
                // Count of master sync settings
                newPosition -= 3;
            }
            if (!(listPosition == 0 && listPositionOffset == 0 || newPosition < 0)) {
                listView.setSelectionFromTop(newPosition, listPositionOffset);
            }
        } else if (!initExpandedGroups.isEmpty()) {
            listAdapter.restoreExpandedGroups(listView, initExpandedGroups, 0);
            listView.setSelection(initExpandedGroups.get(0));
        } else {
            // restoreExpandedGroups also handles implied expanded state for empty groups
            // see expand_indicator.xml for why this is needed
            listAdapter.restoreExpandedGroups(listView, initExpandedGroups, 0);
        }

        // Show message if account access was denied
        findViewById(R.id.missing_account_permission_text).setVisibility(hasAccountsPermission
                ? View.GONE
                : View.VISIBLE);
    }

    private boolean onSyncClick(int groupPosition, int childPosition, CheckBox cb) {
        if (!multiSelectMode) {
            if (groupPosition == 0) {
                // Master sync settings
                String action = syncs.get(childPosition).authority;
                Intent result = new Intent();
                Bundle localeBundle = new Bundle();
                if (getString(R.string.sync_master_on).equals(action)) {
                    Util.maybeRequestPermissions(SelectSyncActivity.this,
                            new String[]{Manifest.permission.WRITE_SYNC_SETTINGS}
                    );
                    localeBundle.putString(Constants.EXTRA_ACTION, Constants.ACTION_MASTER_SYNC_ON);
                    result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                            getString(R.string.shortcut_sync_master_on)
                    );
                } else if (getString(R.string.sync_master_off).equals(action)) {
                    Util.maybeRequestPermissions(SelectSyncActivity.this,
                            new String[]{Manifest.permission.WRITE_SYNC_SETTINGS}
                    );
                    localeBundle.putString(Constants.EXTRA_ACTION, Constants.ACTION_MASTER_SYNC_OFF);
                    result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                            getString(R.string.shortcut_sync_master_off)
                    );
                } else if (getString(R.string.sync_master_toggle).equals(action)) {
                    Util.maybeRequestPermissions(SelectSyncActivity.this,
                            new String[]{Manifest.permission.READ_SYNC_SETTINGS,
                                    Manifest.permission.WRITE_SYNC_SETTINGS}
                    );
                    localeBundle.putString(Constants.EXTRA_ACTION,
                            Constants.ACTION_MASTER_SYNC_TOGGLE);
                    result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                            getString(R.string.shortcut_sync_master_toggle)
                    );
                } else {
                    Log.w(LOG_TAG, "Could not find master action " + action);
                    return false;
                }
                result.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, localeBundle);
                finishWithResult(result);
                return true;
            }

            // Fix offset because of master sync settings
            groupPosition--;
        }

        Sync clickedSync = getSyncForPosition(groupPosition, childPosition);

        if (clickedSync != null) {
            if (DEBUG) Log.d(LOG_TAG, "Clicked " + clickedSync.account.name +
                    "/" + clickedSync.authority);

            if (clickedSync.account == null) {
                // Master sync setting
            } else if (multiSelectMode) {
                Boolean previousGroupSelected = getGroupState(groupPosition);
                UpdateSelectionChecker updateChecker = new UpdateSelectionChecker();

                if (cb.isChecked()) {
                    multiSelectSyncs.add(clickedSync);
                } else {
                    multiSelectSyncs.remove(clickedSync);
                }

                updateChecker.update();
                if (previousGroupSelected != getGroupState(groupPosition)) {
                    listAdapter.notifyDataSetInvalidated();
                }

                if (DEBUG) {
                    Log.v(LOG_TAG, "Selected syncs:");
                    for (int j = 0; j < multiSelectSyncs.size(); j++) {
                        Log.v(LOG_TAG, "\t\t" + multiSelectSyncs.get(j));
                    }
                }
            } else {
                selectAction(clickedSync);
            }
        }
        return true;
    }

    private void onGroupCheckboxClick(int groupPosition, CheckBox cb) {
        UpdateSelectionChecker updateChecker = new UpdateSelectionChecker();

        ArrayList<Sync> syncs = getSyncsForGroup(groupPosition);
        if (cb.isChecked()) {
            for (int i = 0; i < syncs.size(); i++) {
                if (!multiSelectSyncs.contains(syncs.get(i))) {
                    multiSelectSyncs.add(syncs.get(i));
                }
            }
        } else {
            for (int i = 0; i < syncs.size(); i++) {
                multiSelectSyncs.remove(syncs.get(i));
            }
        }
        listAdapter.notifyDataSetInvalidated();

        updateChecker.update();
    }

    private boolean onGroupClick(int groupPosition) {
        if (groupPosition == addAccountGroupPosition) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.sync_add_account_title)
                    .setMessage(R.string.sync_add_account_summary)
                    .setPositiveButton(R.string.sync_add_account_continue,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                                        startActivityForResult(
                                                AccountManager.newChooseAccountIntent(null, null,
                                                        null, null, null, null, null),
                                                REQUEST_ACCOUNT);
                                    } else {
                                        Log.e(LOG_TAG, "addAccountGroupPosition shown API "
                                                + Build.VERSION.SDK_INT);
                                    }
                                }
                            })
                    .setNegativeButton(R.string.sync_add_account_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Only close dialog
                                }
                            })
                    .show();
            return true;
        }
        return false;
    }

    private void selectAction(Sync sync) {
        Intent intent =
                new Intent(SelectSyncActivity.this, SelectActionActivity.class);
        intent.putExtra(Constants.EXTRA_ACCOUNT_STRING,
                sync.account.toString());
        intent.putExtra(Constants.EXTRA_AUTHORITY, sync.authority);
        selectAction(intent);
    }

    private void selectMultiSyncAction() {
        if (multiSelectSyncs.size() == 1) {
            selectAction(multiSelectSyncs.get(0));
        } else {
            String[] accountStrings = new String[multiSelectSyncs.size()];
            String[] authorityStrings = new String[multiSelectSyncs.size()];
            for (int i = 0; i < multiSelectSyncs.size(); i++) {
                accountStrings[i] = multiSelectSyncs.get(i).account.toString();
                authorityStrings[i] = multiSelectSyncs.get(i).authority;
            }
            Intent intent =
                    new Intent(SelectSyncActivity.this, SelectActionActivity.class);
            intent.putExtra(Constants.EXTRA_ACCOUNT_STRING_ARRAY, accountStrings);
            intent.putExtra(Constants.EXTRA_AUTHORITY_ARRAY, authorityStrings);
            selectAction(intent);
        }
    }

    private void selectAction(Intent intent) {
        Bundle localeBundle = IntentAnonymizer.deanonymizeIntent(this, getIntent())
                .getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        String action =
                localeBundle == null ? null : localeBundle.getString(Constants.EXTRA_ACTION);
        intent.putExtra(Constants.EXTRA_PREVIOUS_ACTION, action);
        startActivityForResult(intent, REQUEST_SELECT_ACTION);
    }

    private void selectAll() {
        multiSelectSyncs = (ArrayList<Sync>) syncs.clone();
        listAdapter.notifyDataSetInvalidated();
        invalidateOptionsMenu();
        listAdapter.expandAll(listView);
    }

    private void undoSelection() {
        multiSelectSyncs.clear();
        listAdapter.notifyDataSetInvalidated();
        invalidateOptionsMenu();
    }

    private Sync getSyncForPosition(int groupPosition, int childPosition) {
        int index = 0;
        for (int i = 0; i < syncs.size(); i++) {
            if (syncs.get(i).account == null) {
                // Irrelevant, master sync settings already checked
                continue;
            }
            if (syncs.get(i).account.equals(groups.get(groupPosition))) {
                index++;
            }
            if (index == childPosition + 1) {
                return syncs.get(i);
            }
        }
        return null;
    }

    private ArrayList<Sync> getSyncsForGroup(int groupPosition) {
        ArrayList<Sync> list = new ArrayList<>();
        if (groupPosition >= groups.size()) {
            // Irrelevant final entries: return empty list
            return list;
        }
        for (int i = 0; i < syncs.size(); i++) {
            if (syncs.get(i).account == null) {
                // Irrelevant, master sync settings already checked
                continue;
            }
            if (syncs.get(i).account.equals(groups.get(groupPosition))) {
                list.add(syncs.get(i));
            }
        }
        return list;
    }

    /**
     * @return
     * False - no item in the group is checked
     * True - all items in the group are checked
     * Null - some, but not all items in the group are checked
     */
    private Boolean getGroupState(int groupPosition) {
        ArrayList<Sync> syncs = getSyncsForGroup(groupPosition);
        boolean someIn = false;
        boolean allIn = true;
        for (int i = 0; i < syncs.size(); i++) {
            if (multiSelectSyncs.contains(syncs.get(i))) {
                someIn = true;
            } else {
                allIn = false;
            }
        }
        if (!someIn) {
            return false;
        }
        if (allIn) {
            return true;
        }
        return null;
    }

    class Sync {
        private Account account;
        private String authority;
        public Sync(Account account, String authority) {
            this.account = account;
            this.authority = authority;
        }
        @Override
        public String toString() {
            return "Sync {account = " + account + " authority = " + authority + " }";
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof Sync) {
                Sync s = (Sync) o;
                if ((this.account == null) != (s.account == null)) {
                    return false;
                }
                if ((this.authority == null) != (s.authority == null)) {
                    return false;
                }
                return (this.account == null || this.account.toString().equals(s.account.toString())) &&
                        (this.authority == null || this.authority.equals(s.authority));
            }
            return false;
        }
    }

    private class UpdateSelectionChecker {
        boolean previousEmpty;
        boolean previousFull;
        UpdateSelectionChecker() {
            previousEmpty = multiSelectSyncs.isEmpty();
            previousFull = multiSelectSyncs.size() == syncs.size();
        }
        void update() {
            if (previousEmpty != multiSelectSyncs.isEmpty() ||
                    previousFull != (multiSelectSyncs.size() == syncs.size())) {
                invalidateOptionsMenu();
            }
        }
    }
}
