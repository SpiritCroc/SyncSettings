/*
 * Copyright (C) 2016-2018
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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.buildware.widget.indeterm.IndeterminateCheckBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleCheckableExpandableListAdapter extends SimpleExpandableListAdapter {
    private static final String LOG_TAG =
            SimpleCheckableExpandableListAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    private OnAdapterUpdateListener updateListener;

    private volatile ArrayList<Integer> expandedGroups = new ArrayList<>();

    public SimpleCheckableExpandableListAdapter(Context context, OnAdapterUpdateListener updateListener,
                                                List<? extends Map<String, ?>> groupData,int groupLayout,
                                                String[] groupFrom, int[] groupTo,
                                                List<? extends List<? extends Map<String, ?>>> childData,
                                                int childLayout, String[] childFrom, int[] childTo) {
        super(context, groupData, groupLayout, groupFrom, groupTo,
                childData, childLayout, childFrom, childTo);
        this.updateListener = updateListener;
    }

    public SimpleCheckableExpandableListAdapter(Context context, OnAdapterUpdateListener updateListener,
                                       List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                       int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                       List<? extends List<? extends Map<String, ?>>> childData,
                                       int childLayout, String[] childFrom, int[] childTo) {
        super(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom, groupTo,
                childData, childLayout, childFrom, childTo);
        this.updateListener = updateListener;
    }

    public SimpleCheckableExpandableListAdapter(Context context, OnAdapterUpdateListener updateListener,
                                       List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                       int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                       List<? extends List<? extends Map<String, ?>>> childData,
                                       int childLayout, int lastChildLayout, String[] childFrom,
                                       int[] childTo) {
        super(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom, groupTo,
                childData, childLayout, lastChildLayout, childFrom, childTo);
        this.updateListener = updateListener;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        View v = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
        View cb = v.findViewById(android.R.id.checkbox);
        if (cb instanceof IndeterminateCheckBox) {
            cb.setOnClickListener(checkboxOnClickListener);
            cb.setTag(new Position(groupPosition, childPosition));
            ((IndeterminateCheckBox) cb).setState(updateListener.getCheckedStateFor(groupPosition, childPosition));
        }
        ((TextView) v.findViewById(android.R.id.text1)).setTextColor(
                updateListener.getTextColorForPosition(groupPosition, childPosition)
        );
        return v;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
        View cb = v.findViewById(android.R.id.checkbox);
        if (cb instanceof IndeterminateCheckBox) {
            cb.setOnClickListener(checkboxOnClickListener);
            cb.setTag(new Position(groupPosition, Position.IS_GROUP_CHILD_POSITION));
            ((IndeterminateCheckBox) cb).setState(updateListener.getCheckedStateFor(groupPosition,
                    Position.IS_GROUP_CHILD_POSITION));

            if (getChildrenCount(groupPosition) == 0) {
                // No children -> disable checkbox
                cb.setVisibility(View.GONE);
            } else {
                cb.setVisibility(View.VISIBLE);
            }
        }
        ((TextView) v.findViewById(android.R.id.text1)).setTextColor(
                updateListener.getTextColorForGroup(groupPosition)
        );
        return v;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
        if (getChildrenCount(groupPosition) == 0) {
            if (DEBUG) Log.v(LOG_TAG, "Ignore expand of empty group " + groupPosition);
            return;
        }
        if (DEBUG) Log.v(LOG_TAG, "Expanded " + groupPosition);
        if (!expandedGroups.contains(groupPosition)) {
            expandedGroups.add(groupPosition);
        }
        updateListener.onGroupExpandOrCollapse();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
        if (DEBUG) Log.v(LOG_TAG, "Collapsed " + groupPosition);
        expandedGroups.remove((Integer) groupPosition);
        updateListener.onGroupExpandOrCollapse();
    }

    public void restoreExpandedGroups(ExpandableListView ls, ArrayList<Integer> expandedGroups,
                                      int offset) {
        if (DEBUG) Log.v(LOG_TAG, "Restore expanded groups: " + expandedGroups.size());
        for (int group = 0; group < getGroupCount(); group++) {
            if (getChildrenCount(group) == 0) {
                // Keep empty groups "expanded" for expand indicator workaround, see
                // drawable/expand_indicator.xml
                ls.expandGroup(group);
                if (DEBUG) Log.v(LOG_TAG, "\t\texpand empty " + group);
            } else {
                int originalGroup = group - offset;
                if (expandedGroups.contains(originalGroup)) {
                    ls.expandGroup(group);
                    if (DEBUG) Log.v(LOG_TAG, "\t\texpand " + group);
                } else {
                    if (DEBUG) Log.v(LOG_TAG, "\t\tdon't expand" + group);
                }
            }
        }
    }

    public boolean allGroupsCollapsed() {
        for (Integer group: expandedGroups) {
            // We don't care about empty groups
            if (getChildrenCount(group) > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean allGroupsExpanded() {
        for (int group = 0; group < getGroupCount(); group++) {
            // We don't care about empty groups
            if (getChildrenCount(group) > 0 && !expandedGroups.contains(group)) {
                return false;
            }
        }
        return true;
    }

    public void expandAll(ExpandableListView ls) {
        for (int i = 0; i < getGroupCount(); i++) {
            ls.expandGroup(i);
        }
        ls.setSelection(0);
    }

    public void collapseAll(ExpandableListView ls) {
        for (int i = 0; i < getGroupCount(); i++) {
            // Keep empty groups "expanded" for expand indicator workaround, see
            // drawable/expand_indicator.xml
            if (getChildrenCount(i) > 0) {
                ls.collapseGroup(i);
            }
        }
        ls.setSelection(0);
    }

    public ArrayList<Integer> getExpandedGroups() {
        return expandedGroups;
    }

    private View.OnClickListener checkboxOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CheckBox cb = (CheckBox) v;
            if (DEBUG) Log.d(LOG_TAG, "Checkbox toggled to " + cb.isChecked());
            Position p = (Position) cb.getTag();
            updateListener.onCheckboxClick(cb, p.groupPosition, p.childPosition);
        }
    };

    public static class Position {
        final static int IS_GROUP_CHILD_POSITION = -1;
        int groupPosition;
        int childPosition;
        public Position(int groupPosition, int childPosition) {
            this.groupPosition = groupPosition;
            this.childPosition = childPosition;
        }
        public static boolean isGroup(int childPosition) {
            return childPosition == IS_GROUP_CHILD_POSITION;
        }
    }

    public interface OnAdapterUpdateListener {
        void onCheckboxClick(CheckBox cb, int groupPosition, int childPosition);
        @Nullable Boolean getCheckedStateFor(int groupPosition, int childPosition);
        void onGroupExpandOrCollapse();
        int getTextColorForPosition(int groupPosition, int childPosition);
        int getTextColorForGroup(int groupPosition);
    }
}
