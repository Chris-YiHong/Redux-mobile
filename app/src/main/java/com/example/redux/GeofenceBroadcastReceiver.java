package com.example.redux;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceBroadcastReceiv";
    private DatabaseReference databaseReference;

    public static String geofenceDetected = null;
    public static String AdsplayerLocationState = "ExitGeofence";




    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
//        Toast.makeText(context, "Geofence triggered...", Toast.LENGTH_SHORT).show();

        NotificationHelper notificationHelper = new NotificationHelper(context);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);


        if (geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event...");
            return;
        }


        if (geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
            AdsplayerLocationState ="EnterGeofence";


                Log.i(TAG, "GeofencingEvent Enter");
                //Getting Trigger ID of the Geofence
                String geofenceDetectedID = null;
                List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
                for (Geofence geofence : geofenceList) {
                    Log.d(TAG, "Enter: " + geofence.getRequestId());
                    geofenceDetectedID = geofence.getRequestId();
                }
                geofenceDetected = geofenceDetectedID;
                long currentPosition = MapsActivity.player.getCurrentWindowIndex();
                long currentPlaylistTotal = MapsActivity.player.getMediaItemCount();

                if(currentPlaylistTotal != 0) {
                    if (currentPosition == 0) {
                        MapsActivity.player.removeMediaItems(1, (int) (currentPlaylistTotal));
                    }
                }



                databaseReference = FirebaseDatabase.getInstance()
                        .getReference("Advertisement")
                        .child(geofenceDetectedID);
                databaseReference.addValueEventListener(new ValueEventListener() {
                    List<Integer> adsArray = new ArrayList<>();

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot locationSnapShot : snapshot.getChildren()) {
                            int adsArrayretrieve = Integer.parseInt(locationSnapShot.getValue().toString());
                            adsArray.add(adsArrayretrieve);

                        }
                        Collections.shuffle(adsArray);

                        for (int i = 0; i < adsArray.size(); i++) {
                            VideoPlayerPathStorage videoPlayerPathStorage = new VideoPlayerPathStorage();

                            MapsActivity.player.addMediaItem(videoPlayerPathStorage.VideoPlayerSet(adsArray.get(i)));


                        }

                        MapsActivity.player.setPlayWhenReady(true);
                        MapsActivity.player.prepare();


                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        }

        if (geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_EXIT) {

            AdsplayerLocationState ="ExitGeofence";

            Log.i(TAG, "GeofencingEvent Exit");
            //Getting Trigger ID of the Geofence
            String geofenceDetectedID = null;
            List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence : geofenceList) {
                Log.d(TAG, "Exit: " + geofence.getRequestId());
                geofenceDetectedID = geofence.getRequestId();
            }
            geofenceDetected = "0";

            long currentPosition = MapsActivity.player.getCurrentWindowIndex();
            long currentPlaylistTotal = MapsActivity.player.getMediaItemCount();

            if(currentPlaylistTotal != 0) {
                if (currentPosition == 0) {
                    MapsActivity.player.removeMediaItems(1, (int) (currentPlaylistTotal));
                }
            }

            databaseReference = FirebaseDatabase.getInstance()
                    .getReference("Advertisement")
                    .child("FixedAds");
            databaseReference.addValueEventListener(new ValueEventListener() {
                List<Integer> adsArray = new ArrayList<>();

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot locationSnapShot : snapshot.getChildren()) {
                        int adsArrayretrieve = Integer.parseInt(locationSnapShot.getValue().toString());
                        adsArray.add(adsArrayretrieve);

                    }
                    Collections.shuffle(adsArray);

                    for (int i = 0; i < adsArray.size(); i++) {
                        VideoPlayerPathStorage videoPlayerPathStorage = new VideoPlayerPathStorage();

                        MapsActivity.player.addMediaItem(videoPlayerPathStorage.VideoPlayerSet(adsArray.get(i)));


                    }

                    MapsActivity.player.setPlayWhenReady(true);
                    MapsActivity.player.prepare();


                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

//       Location location = geofencingEvent.getTriggeringLocation();
        int transitionType = geofencingEvent.getGeofenceTransition();
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show();
                notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_ENTER", "", MapsActivity.class);
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show();
                notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_DWELL", "", MapsActivity.class);
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show();
                notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_EXIT", "", MapsActivity.class);
                break;
        }


    }

    public void geofenceTrigger() {
        if (AdsplayerLocationState == "EnterGeofence") {
            Log.i(TAG, "Enter Repeat");

            databaseReference = FirebaseDatabase.getInstance()
                    .getReference("Advertisement")
                    .child(geofenceDetected);
            databaseReference.addValueEventListener(new ValueEventListener() {
                List<Integer> adsArray = new ArrayList<>();

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot locationSnapShot : snapshot.getChildren()) {
                        int adsArrayretrieve = Integer.parseInt(locationSnapShot.getValue().toString());
                        adsArray.add(adsArrayretrieve);

                    }
                    Collections.shuffle(adsArray);

                    for (int i = 0; i < adsArray.size(); i++) {
                        VideoPlayerPathStorage videoPlayerPathStorage = new VideoPlayerPathStorage();

                        MapsActivity.player.addMediaItem(videoPlayerPathStorage.VideoPlayerSet(adsArray.get(i)));


                    }

                    MapsActivity.player.setPlayWhenReady(true);
                    MapsActivity.player.prepare();

                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
        if (AdsplayerLocationState == "ExitGeofence") {
            Log.i(TAG, "Exit Repeat");

            databaseReference = FirebaseDatabase.getInstance()
                    .getReference("Advertisement")
                    .child("FixedAds");
            databaseReference.addValueEventListener(new ValueEventListener() {
                List<Integer> adsArray = new ArrayList<>();

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot locationSnapShot : snapshot.getChildren()) {
                        int adsArrayretrieve = Integer.parseInt(locationSnapShot.getValue().toString());
                        adsArray.add(adsArrayretrieve);

                    }
                    Collections.shuffle(adsArray);

                    for (int i = 0; i < adsArray.size(); i++) {
                        VideoPlayerPathStorage videoPlayerPathStorage = new VideoPlayerPathStorage();

                        MapsActivity.player.addMediaItem(videoPlayerPathStorage.VideoPlayerSet(adsArray.get(i)));


                    }

                    MapsActivity.player.setPlayWhenReady(true);
                    MapsActivity.player.prepare();

                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
    }





}
