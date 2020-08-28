package com.ashtech.anticovid.Users;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import com.ashtech.anticovid.LgnSgn.SocialLoginActivity;
import com.ashtech.anticovid.MainActivity;
import com.ashtech.anticovid.R;
import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.collection.LLRBNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SocialMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    LatLng myPosition;
    GeoFire geoFire;
    DatabaseReference ref;
    private List<LatLng> dangerousArea;

    private DatabaseReference countref;

    int count;

    List<Address> addresses;
    private FusedLocationProviderClient mFusedLocationClient;
    private Button mLogout,mSetLocationBtn, mRemoveMarkerBtn;
    private Switch mWorkingSwitch;
    private int status = 0;
    private String customerId = "", destination;
    private LatLng  setupLatLng;
    int t=1,removeC;
    int toggle=0;           // toggle           t=0 setlocation   t=1 confirm

    private Boolean isLoggingOut = false;
    private SupportMapFragment mapFragment;
    private LinearLayout mMarkerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mMarkerAdrress, mMarkedCity, mCustomerDestination;

    private Marker dangerMarker,latestMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_maps);



        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogout = (Button) findViewById(R.id.logout);
        mSetLocationBtn = (Button) findViewById(R.id.setMarkerBtn);
        mRemoveMarkerBtn = (Button) findViewById(R.id.removeMarkerBtn);


        //hooks
        mMarkerInfo = (LinearLayout) findViewById(R.id.MarkerInfo);
//
//        ref=FirebaseDatabase.getInstance().getReference("MyLocation");
//        geoFire=new GeoFire(ref);


        mMarkerAdrress = (TextView) findViewById(R.id.MarkedAddress);
        mMarkedCity = (TextView) findViewById(R.id.MarkedCity);

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    connectUser();
                } else {
                    disconnectUser();
                }
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                isLoggingOut = true;

                disconnectUser();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(SocialMapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });



        countref=FirebaseDatabase.getInstance().getReference().child("DangerousArea");
               countref.child("MyCity").addValueEventListener(new ValueEventListener() {
                   @Override
                   public void onDataChange(@NonNull DataSnapshot snapshot) {
                       if(snapshot.exists())
                       {
                           count=(int)snapshot.getChildrenCount();
                       }
                       else
                       {
                           count=0;
                       }
                   }

                   @Override
                   public void onCancelled(@NonNull DatabaseError error) {

                   }
               });





    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }


//
//        //Create Dangerous are
//        LatLng dangerous_area=new LatLng(37.7533,-122.4056);
//        mMap.addCircle(new CircleOptions().center(dangerous_area).radius(500).strokeColor(Color.RED).fillColor(0X220000FF).strokeWidth(5.0f));
//
//        //Add GeoQuery Here
//        //0.5f==500m
//        GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(dangerous_area.latitude,dangerous_area.longitude),0.5f);



        //DRAGGABLE EVENT

        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                mMarkerInfo.setVisibility(View.GONE);
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                setupLatLng =marker.getPosition();
                Geocoder geocoder=new Geocoder(getApplicationContext(),Locale.getDefault());

                try {
                    addresses  = geocoder.getFromLocation(setupLatLng.latitude,setupLatLng.longitude,1);
                    String address = addresses.get(0).getAddressLine(0);
                    String city = addresses.get(0).getLocality();

                    mMarkerInfo.setVisibility(View.VISIBLE);
                    mMarkerAdrress.setText(address);
                    mMarkedCity.setText(city);

                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        });

    }




    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    mLastLocation = location;


                    myPosition = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraPosition position= new  CameraPosition.Builder().
                            target(myPosition).zoom(17).bearing(19).tilt(30).build();
                    //_googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));

                    if(t==1) {
                       dangerMarker= mMap.addMarker(new MarkerOptions().position(myPosition).title("My Location").draggable(true));
                        t=0;
                    }

                }
            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(SocialMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(SocialMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void connectUser() {
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    private void disconnectUser() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });
    }


    public  void setLocation(View v)
    {
//

        if(toggle==0) {
            dangerousArea = new ArrayList<>();

            dangerousArea.add(new LatLng(setupLatLng.latitude, setupLatLng.longitude));
            HashMap<String, Double> pronearea = new HashMap<>();
            pronearea.put("latitude", setupLatLng.latitude);
            pronearea.put("longitude", setupLatLng.longitude);

            FirebaseDatabase.getInstance().getReference("DangerousArea").child("MyCity").child(String.valueOf((count + 1))).setValue(pronearea)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            latestMarker=mMap.addMarker(new MarkerOptions().position(setupLatLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.poisonicon)));
                            removeC = count;
                            Toast.makeText(SocialMapsActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SocialMapsActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        if(toggle==0)
        {
            mSetLocationBtn.setText("Confirm");

            mRemoveMarkerBtn.setVisibility(View.VISIBLE);
            mRemoveMarkerBtn.setClickable(true);
            dangerMarker.setDraggable(false);


            toggle=1;

        }
        else
        {
            dangerMarker.setDraggable(true);
            mRemoveMarkerBtn.setClickable(false);

            mRemoveMarkerBtn.setVisibility(View.GONE);
            mSetLocationBtn.setText("Set Location");
            toggle=0;
        }




    }

    public void removeLocation(View v)
    {
        mRemoveMarkerBtn.setClickable(false);

        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DangerousArea").child("MyCity").child(String.valueOf(removeC));
        refAvailable.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {


                latestMarker.remove();
                mRemoveMarkerBtn.setVisibility(View.GONE);
                mSetLocationBtn.setText("Set Location");
                dangerMarker.setDraggable(true);
                toggle=0;
            }
        });




    }

}





