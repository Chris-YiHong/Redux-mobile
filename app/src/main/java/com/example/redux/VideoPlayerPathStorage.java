package com.example.redux;

import android.media.browse.MediaBrowser;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;

public class VideoPlayerPathStorage {
    public Uri uri1 = Uri.parse("file:///android_asset/video/video1.mp4");
    public Uri uri2 = Uri.parse("file:///android_asset/video/video2.mp4");
    public Uri uri3 = Uri.parse("file:///android_asset/video/video3.mp4");
    public Uri uri4 = Uri.parse("file:///android_asset/video/video4.mp4");
    public Uri uri5 = Uri.parse("file:///android_asset/video/video5.mp4");
    public Uri uri6 = Uri.parse("file:///android_asset/video/video6.mp4");

    public Uri uriArray [] ={uri1,uri2,uri3,uri4,uri5,uri6};

    public MediaItem VideoPlayerSet (int adsNo){
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uriArray[adsNo])
                .setMediaId(""+(adsNo+1))
                .build();

        return mediaItem;
    }
}
