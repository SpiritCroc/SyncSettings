/*
 * Copyright (C) 2016
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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleCheckableExpandableListAdapter extends SimpleExpandableListAdapter {
    private static final String LOG_TAG =
            SimpleCheckableExpandableListAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    private OnChildCheckboxClickListener onChildCheckboxClickListener;

    private volatile ArrayList<Integer> expandedGroups = new ArrayList<>();

    public SimpleCheckableExpandableListAdapter(Context context, OnChildCheckboxClickListener checkboxListener,
                                                List<? extends Map<String, ?>> groupData,int groupLayout,
                                                String[] groupFrom, int[] groupTo,
                                                List<? extends List<? extends Map<String, ?>>> childData,
                                                int childLayout, String[] childFrom, int[] childTo) {
        super(context, groupData, groupLayout, groupFrom, groupTo,
                childData, childLayout, childFrom, childTo);
        this.onChildCheckboxClickListener = checkboxListener;
    }

    public SimpleCheckableExpandableListAdapter(Context context, OnChildCheckboxClickListener checkboxListener,
                                       List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                       int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                       List<? extends List<? extends Map<String, ?>>> childData,
                                       int childLayout, String[] childFrom, int[] childTo) {
        super(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom, groupTo,
                childData, childLayout, childFrom, childTo);
        this.onChildCheckboxClickListener = checkboxListener;
    }

    public SimpleCheckableExpandableListAdapter(Context context, OnChildCheckboxClickListener checkboxListener,
                                       List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                       int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                       List<? extends List<? extends Map<String, ?>>> childData,
                                       int childLayout, int lastChildLayout, String[] childFrom,
                                       int[] childTo) {
        super(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom, groupTo,
                childData, childLayout, lastChildLayout, childFrom, childTo);
        this.onChildCheckboxClickListener = checkboxListener;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        View v = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
        View cb = v.findViewById(android.R.id.checkbox);
        if (cb instanceof CheckBox) {
            cb.setOnClickListener(checkboxOnClickListener);
            cb.setTag(new Position(groupPosition, childPosition));
            ((CheckBox) cb).setChecked(onChildCheckboxClickListener.shouldBeChecked(groupPosition, childPosition));
        }
        return v;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
        View cb = v.findViewById(android.R.id.checkbox);
        if (cb instanceof CheckBox) {
            cb.setOnClickListener(checkboxOnClickListener);
        }
        return v;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
        if (DEBUG) Log.v(LOG_TAG, "Expanded " + groupPosition);
        expandedGroups.add(groupPosition);
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
        if (DEBUG) Log.v(LOG_TAG, "Collapsed " + groupPosition);
        expandedGroups.remove((Integer) groupPosition);
    }

    public void restoreExpandedGroups(ExpandableListView ls, ArrayList<Integer> expandedGroups,
                                      int offset) {
        if (DEBUG) Log.v(LOG_TAG, "Restore expanded groups: " + expandedGroups.size());
        for (int i = 0; i < expandedGroups.size(); i++) {
            int group = expandedGroups.get(i) + offset;
            if (group >= 0) {
                if (DEBUG) Log.v(LOG_TAG, "\t\texpand " + group);
                ls.expandGroup(group);
            }
        }
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
            onChildCheckboxClickListener.onCheckboxClick(cb, p.groupPosition, p.childPosition);
        }
    };

    public class Position {
        int groupPosition;
        int childPosition;
        public Position(int groupPosition, int childPosition) {
            this.groupPosition = groupPosition;
            this.childPosition = childPosition;
        }
    }

    public interface OnChildCheckboxClickListener {
        void onCheckboxClick(CheckBox cb, int groupPosition, int childPosition);
        boolean shouldBeChecked(int groupPosition, int childPosition);
    }
}
