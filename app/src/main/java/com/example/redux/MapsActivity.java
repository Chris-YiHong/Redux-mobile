package com.example.redux;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.redux.Interface.IOnLoadLocationListener;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import android.os.Build;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.annotation.Target;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, IOnLoadLocationListener {
    // Varibles for Map activity
    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private DatabaseReference myCity;
    private DatabaseReference mDatabase;
    private IOnLoadLocationListener listener;
    private List<LatLng> geoAdsArea;
    private List<Integer> adsarray;
    private float GEOFENCE_RADIUS = 200;
    private int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;
    private DatabaseReference databaseReference;

    //Variables for ExoPlayers
    public static SimpleExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    PlayerView playerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);

        if (Build.VERSION.SDK_INT >= 29) {
            //We need background permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initArea();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    //We show a dialog and ask for permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                }
            }

        } else {
            initArea();
        }
        initializePlayer();
        hideSystemUi();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                emptyCheck();
            }
        }, 3000);
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        LatLng wheaton = new LatLng(37.3248, -122.0232);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wheaton, 16));
        enableUserLocation();
        mMap.setOnMapLongClickListener(this);
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            //Ask for permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //We need to show user a dialog for displaying why the permission is needed and then ask for the permission...
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(true);
            } else {
                //We do not have the permission..

            }
        }

        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
            } else {
                //We do not have the permission..
                Toast.makeText(this, "Background location access is neccessary for geofences to trigger...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (Build.VERSION.SDK_INT >= 29) {
            //We need background permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                handleMapLongClick(latLng);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    //We show a dialog and ask for permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                }
            }

        } else {
            handleMapLongClick(latLng);
        }

    }

    private void handleMapLongClick(LatLng latLng) {
       initArea();
    }

    private void initArea(){

        listener = this;
        myCity = FirebaseDatabase.getInstance()
                .getReference("GeoFence")
                .child("Selangor");

        myCity.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MyLatLng> latLngList = new ArrayList<>();
                for (DataSnapshot locationSnapShot : snapshot.getChildren())
                {
                    MyLatLng latLng = locationSnapShot.getValue(MyLatLng.class);
                    latLngList.add(latLng);

                }

                listener.onLoadLocationSuccess(latLngList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }



    private void addGeofence(LatLng latLng, float radius,String ID) {

        Geofence geofence = geofenceHelper.getGeofence(ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Geofence Added...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    }
                });
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }

    private void addCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0,0));
        circleOptions.fillColor(Color.argb(64, 255, 0,0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }


    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        geoAdsArea = new ArrayList<>();
        for(MyLatLng myLatLng : latLngs){
            LatLng convert = new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            geoAdsArea.add(convert);
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        //clear map and add again
        if(mMap != null)
        {

            mMap.clear();
            //Add user marker
            int counter = 100 ;
            for(LatLng latLng : geoAdsArea){
                addCircle(latLng,GEOFENCE_RADIUS);
                addGeofence(latLng,GEOFENCE_RADIUS,""+counter);
                counter++;
            }

            //Add Circle of GeoFence Area


        }
    }

    @Override
    public void onLoadLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
    }


    // VIDEO PLAYER FUNCTIONS




    private void initializePlayer() {
        playerView = findViewById(R.id.video_view);
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);



/*

            databaseReference = FirebaseDatabase.getInstance()
                    .getReference("Advertisement")
                    .child("FixedAds");

            databaseReference.addValueEventListener(new ValueEventListener() {
                List<Integer> adsArray = new ArrayList<>();
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot locationSnapShot : snapshot.getChildren())
                    {
                        int adsArrayretrieve = Integer.parseInt(locationSnapShot.getValue().toString());
                        adsArray.add(adsArrayretrieve);

                    }
                    Collections.shuffle(adsArray);

                    for(int i = 0;  i < adsArray.size(); i++){
                        VideoPlayerPathStorage videoPlayerPathStorage = new VideoPlayerPathStorage();

                        player.addMediaItem(videoPlayerPathStorage.VideoPlayerSet(adsArray.get(i)));


                    }

                    player.setPlayWhenReady(true);
                    player.prepare();
                    long currentPosition = player.getCurrentWindowIndex();
                    long currentPlaylistTotal = player.getMediaItemCount();
                    Log.d(TAG, "Total Video in the list: " + currentPlaylistTotal);
                    Log.d(TAG, "CurrentPosition " + currentPosition);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });





*/

       player.addListener(new Player.EventListener() {

           @Override
           public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
               getLog();
               Date currentTime = Calendar.getInstance().getTime();
               MediaItem currentItem = player.getCurrentMediaItem();


               long currentPosition = MapsActivity.player.getCurrentWindowIndex();
               long currentPlaylistTotal = MapsActivity.player.getMediaItemCount();

               if(player.getCurrentPosition() != 0)
                   player.removeMediaItem(0);

               Log.d(TAG, currentPosition + " AND "+ currentPlaylistTotal+" Name : "+ "Time :" + currentTime);

               if (currentPlaylistTotal == 2) {
                    GeofenceBroadcastReceiver geofenceBroadcastReceiver = new GeofenceBroadcastReceiver();
                    geofenceBroadcastReceiver.geofenceTrigger();

               }
           }

       });



    }


    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.release();
            player = null;
        }
    }

    private void emptyCheck(){
        if(player.getMediaItemCount()==0){
            databaseReference = FirebaseDatabase.getInstance()
                    .getReference("Advertisement")
                    .child("FixedAds");

            databaseReference.addValueEventListener(new ValueEventListener() {
                List<Integer> adsArray = new ArrayList<>();
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot locationSnapShot : snapshot.getChildren())
                    {
                        int adsArrayretrieve = Integer.parseInt(locationSnapShot.getValue().toString());
                        adsArray.add(adsArrayretrieve);

                    }
                    // Collections.shuffle(adsArray);

                    for(int i = 0;  i < adsArray.size(); i++){
                        VideoPlayerPathStorage videoPlayerPathStorage = new VideoPlayerPathStorage();

                        player.addMediaItem(videoPlayerPathStorage.VideoPlayerSet(adsArray.get(i)));


                    }

                    player.setPlayWhenReady(true);
                    player.prepare();
                    long currentPosition = player.getCurrentWindowIndex();
                    long currentPlaylistTotal = player.getMediaItemCount();
                    Log.d(TAG, "Total Video in the list: " + currentPlaylistTotal);
                    Log.d(TAG, "CurrentPosition " + currentPosition);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
    // Saving Logs to Database

    private void getLog() {

        MediaItem currentItem = player.getCurrentMediaItem();
        String id = currentItem.mediaId;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        String start = df.format(Calendar.getInstance().getTime());
        int timeMs=(int) player.getDuration();
        int totalSeconds = timeMs / 1000;

        Log.d(TAG,  "Time: " + start + "duration: " + totalSeconds + " seconds" + "id " + id);


        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("Logs")
                .child("Time: " + start + "duration: " + totalSeconds + " seconds"+" id: " + id + " " )
                .setValue("Log");

    }
}
