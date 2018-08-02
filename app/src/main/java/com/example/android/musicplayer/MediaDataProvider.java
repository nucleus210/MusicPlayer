package com.example.android.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Data provider class
 */

class MediaDataProvider {

    private final ArrayList<Song> songs = new ArrayList<>();

//------------------------------------------------------------------------------------------------//

    /**
     * Scanning for all music files in External Storage
     *
     * @param context given context.
     * @return song object.
     */
//------------------------------------------------------------------------------------------------//
    public ArrayList<Song> scanSongs(Context context) {

        Uri musicUri = (android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        ContentResolver resolver = context.getContentResolver();
        Cursor mCursor;

        // Strings metaData projections
        // String[] genresProjection = {MediaStore.Audio.Genres.NAME,
        //                             MediaStore.Audio.Genres._ID};

        String SONG_ID = android.provider.MediaStore.Audio.Media._ID;
        String SONG_TITLE = android.provider.MediaStore.Audio.Media.TITLE;
        String SONG_ARTIST = android.provider.MediaStore.Audio.Media.ARTIST;
        String SONG_ALBUM = android.provider.MediaStore.Audio.Media.ALBUM;
        String SONG_FILEPATH = android.provider.MediaStore.Audio.Media.DATA;
        String SONG_DURATION = android.provider.MediaStore.Audio.Media.DURATION;
        String SONG_GENRES = MediaStore.Audio.Genres._ID;

        String[] columns =
                        {
                        SONG_ID,
                        SONG_TITLE,
                        SONG_ARTIST,
                        SONG_ALBUM,
                        SONG_FILEPATH,
                        SONG_DURATION,
                        SONG_GENRES
                        };

        final String musicsOnly = MediaStore.Audio.Media.IS_MUSIC + "=1";

        // Actually querying the system
        mCursor = resolver.query(musicUri, columns, musicsOnly, null, null);

        if (isExternalStorageReadable() && mCursor != null && mCursor.moveToFirst()) {
            do {
                Song song = new Song();
                song.set_genres(mCursor.getString(mCursor.getColumnIndex(SONG_GENRES)));
                song.set_title(mCursor.getString(mCursor.getColumnIndex(SONG_TITLE)));
                song.set_artist(mCursor.getString(mCursor.getColumnIndex(SONG_ARTIST)));
                song.set_album(mCursor.getString(mCursor.getColumnIndex(SONG_ALBUM)));
                song.set_duration(mCursor.getString(mCursor.getColumnIndex(SONG_DURATION)));
                song.set_songUri(ContentUris.withAppendedId
                        (android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media._ID))));
                String duration = getDuration(Integer.parseInt(mCursor.getString
                        (mCursor.getColumnIndex(MediaStore.Audio.Media.DURATION))));
                song.set_duration(duration);

                // String genre_column_index = Integer.toString(mCursor.getColumnIndexOrThrow
                //        (MediaStore.Audio.Genres._ID));

                MediaMetadataRetriever mr = new MediaMetadataRetriever();

                Uri trackUri = ContentUris.withAppendedId
                        (android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                try {
                    mr.setDataSource(context, trackUri);
                } catch (Exception ignored) {
                }

                songs.add(song);  //add object to the list
            } while (mCursor.moveToNext());
        } else {
            Log.d("mCursor", "Cursor is empty.");
        }
        if (mCursor != null) {
            mCursor.close();
        }

        // sort the song list alphabetically based on the song title.
        Collections.sort(songs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.get_title().compareTo(b.get_title());
            }
        });

        return songs;
    }

    public void destroy() {
        songs.clear();
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to get Song Duration
     */
//------------------------------------------------------------------------------------------------//
    private static String getDuration(long millis) {
        if (millis < 1) {
            throw new IllegalArgumentException("Duration more than zero.");
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MICROSECONDS.toSeconds(millis);

        return (minutes < 10 ? "0" + minutes : minutes) +
                ":" +
                (seconds < 10 ? "0" + seconds : seconds);
    }

    /**
     * Method is used to check Readable Permission
     */
//------------------------------------------------------------------------------------------------//
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}