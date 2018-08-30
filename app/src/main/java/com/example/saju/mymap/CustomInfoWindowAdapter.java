package com.example.saju.mymap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Saju on 8/29/2018.
 */

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View myWindowView;
    Context context;

    public CustomInfoWindowAdapter(Context context) {
        this.context = context;
        myWindowView = LayoutInflater.from(context).inflate(R.layout.custom_info, null);
    }

    private void rendWindowText(Marker m, View v) {

        String title = m.getTitle();
        if (!title.equals("")) {
            TextView tvTitle = v.findViewById(R.id.infoTitle);
            tvTitle.setText(title);
        }

        String snippted = m.getSnippet();
        if (!snippted.equals("")) {
            TextView tvSnippted = v.findViewById(R.id.infoSnippted);
            tvSnippted.setText(snippted);
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        rendWindowText(marker, myWindowView);
        return myWindowView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        rendWindowText(marker, myWindowView);
        return myWindowView;
    }
}
