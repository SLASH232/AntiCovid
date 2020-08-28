package com.ashtech.anticovid.Users;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ashtech.anticovid.Adapters.IOnLoadLocationListner;
import com.ashtech.anticovid.Adapters.MyLatLng;
import com.ashtech.anticovid.MainActivity;
import com.ashtech.anticovid.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SocialTrial extends FragmentActivity implements OnMapReadyCallback, IOnLoadLocationListner, GeoQueryEventListener {

    private GoogleMap mMap;
    LocationCallback locationCallback;
    LocationRequest mLocationRequest;

    Marker currentUser;
    ArrayList<LatLng> dangerousArea;
    Location lastLocation;
    private GeoQuery geoQuery;

    private FusedLocationProviderClient mFusedLocationClient;
    DatabaseReference myref;
    GeoFire geoFire;
    IOnLoadLocationListner iOnLoadLocationListner;

   private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_trial);

        Dexter.withActivity(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        buildLocationRequest();
                        buildLoationCallback();
                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(SocialTrial.this);



                        initArea();
                        settingGeoFire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(SocialTrial.this, "You must Enable permission", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();









    }

    private void initArea()
    {
        myref= FirebaseDatabase.getInstance().getReference("DangerousArea").child("MyCity");
            iOnLoadLocationListner= this;

        myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Update dangerous List
                List<MyLatLng> latlnglist = new ArrayList<>();
                for(DataSnapshot locaitonSnapshot:snapshot.getChildren())
                {
                    MyLatLng latLng= locaitonSnapshot.getValue(MyLatLng.class);
                    latlnglist.add(latLng);
                }

                iOnLoadLocationListner.onLoadLocationSuccess(latlnglist);



            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void addUserMarker() {
        geoFire.setLocation("You", new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });
        if(currentUser!=null) currentUser.remove();
        currentUser= mMap.addMarker(new MarkerOptions().position(new LatLng(lastLocation.getLatitude(),
                lastLocation.getLongitude())).title("You"));
        CameraPosition position= new  CameraPosition.Builder().
                target(currentUser.getPosition()).zoom(17).bearing(19).tilt(30).build();
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));


    }


    private void settingGeoFire() {

        myref=FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire=new GeoFire(myref);
    }

    private void buildLoationCallback() {
    locationCallback=new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if(mMap!=null)
            {
                lastLocation= locationResult.getLastLocation();
                addUserMarker();

            }
             }
    };
    }

    private void buildLocationRequest() {
    mLocationRequest=new LocationRequest();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    mLocationRequest.setInterval(5000);
    mLocationRequest.setFastestInterval(3000);
    mLocationRequest.setSmallestDisplacement(10f);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if(mFusedLocationClient!=null) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }

        //Add Circle for dangerous areaa

       addCircleArea();

    }

    private void addCircleArea() {
        if(geoQuery!=null) {

            geoQuery.removeGeoQueryEventListener(this);
            geoQuery.removeAllListeners();
        }
        for(LatLng latLng: dangerousArea)
        {
            mMap.addCircle(new CircleOptions().center(latLng)
                    .radius(500)        //500m
                    .strokeColor(Color.RED)
                    .fillColor(0x220000ff)
                    .strokeWidth(5.0f));
            mMap.addMarker(new MarkerOptions().position(latLng).title("COVID ENCOUNTERED"));
            //Create GeoQuery when user in danger Location

            geoQuery=geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude),0.5f);  //500m

            geoQuery.addGeoQueryEventListener(SocialTrial.this);

        }
    }

    @Override
    protected void onStop() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }


    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        dangerousArea=new ArrayList<>();
        for(MyLatLng myLatLng:latLngs)
        {
            LatLng convert =new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            dangerousArea.add(convert);
        }
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(SocialTrial.this);

        if(mMap!=null)
        {
            mMap.clear();
            //Add USer Marker
            addUserMarker();

            //Add CIrcle of dangerous area
            addCircleArea();
        }

    }

    @Override
    public void onLoadLocationFailed(String msg) {
        Toast.makeText(this, ""+msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("ASHTECH",String.format("%s Entered in dangerous area!!",key));
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("ASHTECH",String.format("%s Is no longer in dangerous area!!",key));

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Log.d("Move",String.format("%s moved within the dangerous area[%f/%f]",key,location.latitude,location.longitude));

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Log.e("ERROR",""+error);
    }


    private void sendNotification(String title, String content) {

        String NOTIFICATION_CHANNEL_ID= "ASHTECT_Multiple_Channel";
        NotificationManager manager=(NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel=new NotificationChannel(NOTIFICATION_CHANNEL_ID,"My Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);

            //Config
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);
            manager.createNotificationChannel(notificationChannel);

        }
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

       Notification notification=builder.build();
       manager.notify(new Random().nextInt(),notification);
    }
}





