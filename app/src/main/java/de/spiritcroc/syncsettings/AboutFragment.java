/*
 * Copyright (C) 2017-2018 SpiritCroc
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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.about, container, false);

        WebView aboutWebView = (WebView) v.findViewById(R.id.about_web_view);

        aboutWebView.setBackgroundColor(Color.TRANSPARENT);
        aboutWebView.loadDataWithBaseURL("http://de.spiritcroc.syncsettings.phantomfile",
                styleHtml(getActivity(), R.string.about_html), "text/html", "UTF-8", null);

        return v;
    }

    public static String styleHtml(Context context, @StringRes int resourceId) {
        TypedArray ta = context.obtainStyledAttributes(new int[] {
                android.R.attr.textColorPrimary,
                android.R.attr.textColorSecondary,
                R.attr.colorAccent,}
        );
        String textColorPrimary = String.format("#%06X", (0xFFFFFF & ta.getColor(0, Color.GRAY)));
        String textColorSecondary = String.format("#%06X", (0xFFFFFF & ta.getColor(1, Color.GRAY)));
        String accentColor = String.format("#%06X", (0xFFFFFF & ta.getColor(2, Color.GRAY)));
        ta.recycle();
        String html = context.getString(resourceId);
        html = html.replaceAll("\\?android:attr/textColorPrimary", textColorPrimary);
        html = html.replaceAll("\\?android:attr/textColorSecondary", textColorSecondary);
        html = html.replaceAll("\\?android:attr/colorAccent", accentColor);
        return html;
    }
}
