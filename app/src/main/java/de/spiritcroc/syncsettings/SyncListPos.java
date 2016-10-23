package de.spiritcroc.syncsettings;

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

public class SyncListPos {
    public int groupPosition;
    public int childPosition;

    public SyncListPos(int groupPosition, int childPosition) {
        this.groupPosition = groupPosition;
        this.childPosition = childPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SyncListPos) {
            SyncListPos slp = (SyncListPos) o;
            return this.groupPosition == slp.groupPosition &&
                    this.childPosition == slp.childPosition;
        }
        return false;
    }
}
