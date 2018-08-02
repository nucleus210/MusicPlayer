package com.example.android.musicplayer;

import android.net.Uri;


public class Song {

    private int    _ID;         // Song ID integer
    private byte[] _photo;      // Song Image
    private String _artist;     // Song Artist
    private String _title;      // Song Title
    private long   _albumID;    // Song Album ID
    private String _album;      // Song Album Name
    private String _dataPath;   // Song Path to External Public Storage
    private String _genres;     // Song Genres Name
    private String _duration;   // Song Duration
    private Uri    _songUri;    // Song Uri

//------------------------------------------------------------------------------------------------//
    /**
     * Empty Constructor
     */
//---------------------------------------------------------------------------------------------...//
    Song() {
    }

    public Song(byte[] photo, String artist, String title, long albumID, String album, String dataPath, String genres, String duration, Uri songUri) {

        this.set_photo(photo);
        this.set_artist(artist);
        this.set_title(title);
        this.set_albumID(albumID);
        this.set_album(album);
        this.set_dataPath(dataPath);
        this.set_genres(genres);
        this.set_duration(duration);
        this.set_songUri(songUri);
    }

//------------------------------------------------------------------------------------------------//
    /**
     * @return Song Metadata
     */
//---------------------------------------------------------------------------------------------...//

    public int get_ID() {
        return _ID;
    }

    public void set_ID(int _ID) {
        this._ID = _ID;
    }

    public byte[] get_photo() {
        return _photo;
    }

    private void set_photo(byte[] _photo) {
        this._photo = _photo;
    }

    public String get_artist() {
        return _artist;
    }

    public void set_artist(String _artist) {
        this._artist = _artist;
    }

    public String get_title() {
        return _title;
    }

    public void set_title(String _title) {
        this._title = _title;
    }

    public String get_dataPath() {
        return _dataPath;
    }

    private void set_dataPath(String _dataPath) {
        this._dataPath = _dataPath;
    }

    public long get_albumID() {
        return _albumID;
    }

    private void set_albumID(long _albumID) {
        this._albumID = _albumID;
    }

    public String get_album() {
        return _album;
    }

    public void set_album(String _album) {
        this._album = _album;
    }

    public String get_genres() {
        return _genres;
    }

    public void set_genres(String _genres) {
        this._genres = _genres;
    }

    public CharSequence get_duration() {
        return _duration;
    }

    public void set_duration(String _duration) {
        this._duration = _duration;
    }

    public Uri get_songUri() {
        return _songUri;
    }

    public void set_songUri(Uri _songUri) {
        this._songUri = _songUri;
    }
}
