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
import android.view.LayoutInflater;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.collection.LLRBNode;
import com.google.firebase.database.core.RepoManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SocialMapsActivity extends FragmentActivity implements OnMapReadyCallback , GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    LatLng myPosition;
    GeoFire geoFire;
    DatabaseReference ref;
    private List<LatLng> dangerousArea;
    private HashMap<String, Marker> mMarkers = new HashMap<>();

    private DatabaseReference countref;

    int count;
    String postId;
    List<Address> addresses;
    private FusedLocationProviderClient mFusedLocationClient;
    private Button mLogout, mSetLocationBtn, mRemoveMarkerBtn;
    private Switch mWorkingSwitch;

    private HashMap<String, Marker> mRepMarkers = new HashMap<>();
    private int status = 0;
    private String customerId = "", destination;
    private LatLng setupLatLng;
    int t = 1, removeC;
    int toggle = 0;           // toggle           t=0 setlocation   t=1 confirm


    private SupportMapFragment mapFragment;
    private LinearLayout mMarkerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mMarkerAdrress, mMarkedCity, mCustomerDestination;

    private Marker dangerMarker, latestMarker;

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


                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(SocialMapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        countref = FirebaseDatabase.getInstance().getReference().child("DangerousArea");
        countref.child("MyCity").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    count = (int) snapshot.getChildrenCount();
                } else {
                    count = 0;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

       removeMarker14();   //revmove marker after 14 days

    }

    private void removeMarker14() {

        //-----------------------------remove marker after 14 days-----------------------------------------//
        long cutoff=new Date().getTime()- TimeUnit.MILLISECONDS.convert(14,TimeUnit.DAYS);
        Query oldMarker=FirebaseDatabase.getInstance().getReference("TimeMarker").orderByChild("timestamp").endAt(cutoff);
        oldMarker.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot itemSnapshot: snapshot.getChildren())
                {
                    String markerKey=itemSnapshot.getKey();
                    itemSnapshot.getRef().removeValue();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DangerousArea").child("MyCity").child(markerKey);
                    refAvailable.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });

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
        googleMap.setOnMarkerClickListener(this);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


            } else {
                checkLocationPermission();
            }
        }




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

                setupLatLng = marker.getPosition();
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                try {
                    addresses = geocoder.getFromLocation(setupLatLng.latitude, setupLatLng.longitude, 1);
                    String address = addresses.get(0).getAddressLine(0);
                    String city = addresses.get(0).getLocality();

                    mMarkerInfo.setVisibility(View.VISIBLE);
                    mMarkerAdrress.setText(address);
                    mMarkedCity.setText(city);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private void getreported()
    {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Reported");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                addCircle(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                addCircle(dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("Failed to read value.", String.valueOf(error.toException()));
            }
        });
    }

    private void addCircle(DataSnapshot snapshotReported) {
        String key=snapshotReported.getKey();
        HashMap<String, Object> value = (HashMap<String, Object>) snapshotReported.getValue();
        double lat = Double.parseDouble(value.get("latitude").toString());
        double lng = Double.parseDouble(value.get("longitude").toString());
        LatLng location = new LatLng(lat, lng);
         if (!mRepMarkers.containsKey(key)) {
            mRepMarkers.put(key, mMap.addMarker(new MarkerOptions().title(key).position(location).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_zone))));
        } else {
            mRepMarkers.get(key).setPosition(location);
        }

    }


    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    mLastLocation = location;


                    myPosition = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraPosition position = new CameraPosition.Builder().
                            target(myPosition).zoom(17).bearing(19).tilt(30).build();
                    //_googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                    if(t<2) {
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
                        t++;
                    }
                    if (dangerMarker==null) {
                        dangerMarker = mMap.addMarker(new MarkerOptions().position(myPosition).draggable(true));
                        dangerMarker.setTag("Drag me to\n Set Covid Zones");
                        //t = 0;
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
        getreported();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);

    }

    private void disconnectUser() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

    }


    public void setLocation(View v) {

        setLocationinFirebase(setupLatLng);
    }
        public void setLocationinFirebase(final LatLng setLatLng){
        if (toggle == 0) {
            dangerousArea = new ArrayList<>();

            dangerousArea.add(new LatLng(setLatLng.latitude, setLatLng.longitude));
            HashMap<String, Double> pronearea = new HashMap<>();
            pronearea.put("latitude", setLatLng.latitude);
            pronearea.put("longitude", setLatLng.longitude);

            DatabaseReference pushDangerous=FirebaseDatabase.getInstance().getReference("DangerousArea").child("MyCity").push();
            postId=pushDangerous.getKey();
            pushDangerous.setValue(pronearea)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            latestMarker = mMap.addMarker(new MarkerOptions().position(setLatLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.poisonicon)));
                            removeC = count;
                            Toast.makeText(SocialMapsActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                            FirebaseDatabase.getInstance().getReference("TimeMarker").child(postId).child("timestamp").setValue(ServerValue.TIMESTAMP);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SocialMapsActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (toggle == 0) {
            mSetLocationBtn.setText("Confirm");

            mRemoveMarkerBtn.setVisibility(View.VISIBLE);
            mRemoveMarkerBtn.setClickable(true);
            dangerMarker.setDraggable(false);
            toggle = 1;

        } else {
            dangerMarker.setDraggable(true);
            mRemoveMarkerBtn.setClickable(false);

            mRemoveMarkerBtn.setVisibility(View.GONE);
            mSetLocationBtn.setText("Set Location");
            toggle = 0;
        }


    }

    public void removeLocation(View v) {
        mRemoveMarkerBtn.setClickable(false);
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DangerousArea").child("MyCity").child(postId);
        refAvailable.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                DatabaseReference timeref= FirebaseDatabase.getInstance().getReference("TimeMarker").child(postId);
                timeref.removeValue();
                latestMarker.remove();
                mRemoveMarkerBtn.setVisibility(View.GONE);
                mSetLocationBtn.setText("Set Location");
                dangerMarker.setDraggable(true);
                toggle = 0;
            }
        });


    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if(marker.equals(dangerMarker))
            return false;

        final String key = marker.getTitle();
        final LatLng latLng = marker.getPosition();

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.custom_dialogbox, null);
        Button acceptButton = view.findViewById(R.id.acceptBtn);
        Button removeButton = view.findViewById(R.id.removeBtn);
        final TextView name = view.findViewById(R.id.name);
        final TextView phone = view.findViewById(R.id.phone);
        final TextView enteredzone = view.findViewById(R.id.encountered);
        final androidx.appcompat.app.AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();

            name.setText(address);
            phone.setText(city);
            enteredzone.setText("Marked By: "+key);
            acceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setLocationinFirebase(latLng);
                    marker.remove();
                    Toast.makeText(SocialMapsActivity.this, "Marker Set", Toast.LENGTH_SHORT).show();
                    alertDialog.cancel();
                }
            });
            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseDatabase.getInstance().getReference("Reported").child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            marker.remove();
                            Toast.makeText(SocialMapsActivity.this, "Marker Removed ", Toast.LENGTH_SHORT).show();

                        }
                    });

                    alertDialog.cancel();
                }
            });
            alertDialog.show();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return  true;
    }
}





