package com.ashtech.anticovid.Adapters;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface IOnLoadLocationListner {
    void onLoadLocationSuccess(List<MyLatLng> latLngs);
    void onLoadLocationFailed(String msg);
}
