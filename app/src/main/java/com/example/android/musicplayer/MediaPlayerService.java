package com.example.android.musicplayer;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "Media Service";
    private Uri mSongUri;
    private ArrayList<Song> mSongList;
    public static int SONG_POS;

    // Binder given to clients
    private final IBinder songBinder = new PlayerBinder();
    private final String ACTION_STOP = "com.example.android.musicplayer.STOP";
    private final String ACTION_NEXT = "com.example.android.musicplayer.NEXT";
    private final String ACTION_PREVIOUS = "com.example.android.musicplayer.PREVIOUS";
    private final String ACTION_PAUSE = "com.example.android.musicplayer.PAUSE";
    private static final String PLAYER_CHANNEL_ID = "Player";
    private static final int STATE_PAUSED = 1;
    private static final int STATE_PLAYING = 2;
    private int mCurrentState = 0;

    private static final int REQUEST_CODE_PAUSE = 101;
    private static final int REQUEST_CODE_PREVIOUS = 102;
    private static final int REQUEST_CODE_NEXT = 103;
    private static final int REQUEST_CODE_STOP = 104;
    public static int NOTIFICATION_ID = 11;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager notificationManager;
    private RemoteViews mRemoteViews;
    final MediaPlayer mPlayer = new MediaPlayer();

    public class PlayerBinder extends Binder {

        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Starting.");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        initPlayer();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setVolume(100, 100);

    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used when service Activity Life cycle start.
     * Also to check what action is taken by the user and notify system with passing intent to
     * the pending intent for user notification.
     */
//------------------------------------------------------------------------------------------------//
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service Running.");
        if (intent != null) { //check intent object for data and get user action
            String actions = intent.getAction();
            if (!TextUtils.isEmpty(actions)) {
                switch (actions) {
                    case ACTION_PAUSE:
                        playPause();
                        break;
                    case ACTION_NEXT:
                        nextSong();
                        break;
                    case ACTION_PREVIOUS:
                        previousSong();
                        break;
                    case ACTION_STOP:
                        stopSong();
                        stopSelf();
                        break;
                }
            }
        }
        return flags;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Binding to service.");
        return songBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnBind: Unbind service.");
        mPlayer.stop();   // When service is stop and all user are unbind from service - stop player
        mPlayer.release();// Release media player object
        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind: Rebind to service.");
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Stop");
        super.onDestroy();
    }

//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to prepare song for playing and update with data notification manager about
     * user action.
     */
//------------------------------------------------------------------------------------------------//
    private void startSong(Uri songUri, String songTitle) {
        mPlayer.reset();        // Reset Media Player object
        mCurrentState = STATE_PLAYING;
        mSongUri = songUri;     // Get song Uri
        try {
            mPlayer.setDataSource(getApplicationContext(), mSongUri);
            Log.d(TAG, "startingSong: " + songTitle);

        } catch (Exception e) {
            Log.e("Media Service:", "Error setting data", e);
        }
        try {
            mPlayer.prepareAsync(); // Prepare Media Player object
        } catch (Exception e) {
            Log.e("Media Service:", "Error preparing data", e);
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to check what action is taken by the user and notify system with pending intent
     */
//------------------------------------------------------------------------------------------------//
    private void initPlayer() {
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    /**
     * public void setSongService() {
     * MediaDataProvider mediaDataProvider = new MediaDataProvider();
     * mSongList = mediaDataProvider.scanSongs(this);
     * }
     */
    public void setSongList(ArrayList<Song> listSong) {
        mSongList = listSong;
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Handle play/pause Song action
     */
//------------------------------------------------------------------------------------------------//
    public void playPause() {

        // Check current state of mediaPlayer object
        if (mCurrentState == STATE_PAUSED) {
            mPlayer.start();                          // if player is not playing start Media Player
            mCurrentState = STATE_PLAYING;            // set state Playing
        } else if (mCurrentState == STATE_PLAYING) {
            mPlayer.pause();                          // if player is playing stop Media Player
            mCurrentState = STATE_PAUSED;             // set state Paused
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Handle stop Song action
     */
//------------------------------------------------------------------------------------------------//
    private void stopSong() {
        mPlayer.stop();                               // stop mediaPlayer
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE); // create system service object
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);              // trigger cancel object Id
        }
        try {
            System.exit(0);                     // calls the exit method in class Runtime
        } catch (Exception SecurityException) {
            Log.e("Media Service:", "Error Stop Song", SecurityException);
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Handle play next Song action
     */
//------------------------------------------------------------------------------------------------//
    public void nextSong() {
        if (SONG_POS == 0) {
            startSong(mSongList.get(SONG_POS + 1).get_songUri(),
                    mSongList.get(SONG_POS + 1).get_title());
            showNotification();
            SONG_POS++;

        } else if (SONG_POS != mSongList.size() - 1) {
            startSong(mSongList.get(SONG_POS + 1).get_songUri(),
                    mSongList.get(SONG_POS + 1).get_title());
            SONG_POS++;
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Handle play previous Song
     */
//------------------------------------------------------------------------------------------------//
    public void previousSong() {
        if (SONG_POS != 0) {
            startSong(mSongList.get(SONG_POS - 1).get_songUri(),
                    mSongList.get(SONG_POS - 1).get_title());
            showNotification();
            SONG_POS--;
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Set Song Uri path
     *
     * @param uri File uri
     */
//------------------------------------------------------------------------------------------------//
    private void setSongUri(Uri uri) {
        this.mSongUri = uri;
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Invoked indicating buffering status of
     *
     * @param mp Media Player object
     */
//------------------------------------------------------------------------------------------------//
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to start selected song
     * What's happen when song is finish. Play next Song
     *
     * @param mp Media Player object
     */
//------------------------------------------------------------------------------------------------//
    @Override
    public void onCompletion(MediaPlayer mp) {
        mPlayer.reset();
        try {
            if (SONG_POS != mSongList.size() - 1) {
                SONG_POS++;
            } else SONG_POS = 0;
            mPlayer.setDataSource(getApplicationContext(), mSongList.get(SONG_POS).get_songUri());
        } catch (Exception ignored) {
            Log.e("Media Service:", "Ignore playing music");
        }
        mPlayer.prepareAsync();
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to start selected song
     *
     * @param pos             Song position in List
     * @param notification_id Show user notification with play/pause, next and previous buttons
     */
//------------------------------------------------------------------------------------------------//
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setSelectedSong(int pos, int notification_id) {
        SONG_POS = pos;
        NOTIFICATION_ID = notification_id;
        if (mSongList.size() != 0) {
            setSongUri(mSongList.get(SONG_POS).get_songUri());
            startSong(mSongList.get(SONG_POS).get_songUri(), mSongList.get(SONG_POS).get_title());
            showNotification();
        } else {
            int listSize = mSongList.size();
            Log.d(TAG, "Song Array List size is:" + listSize);
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to notify user about current playing song and provide Next, Previous and
     * Play/Pause Buttons. Also provide song Title, song Duration data.
     */
//------------------------------------------------------------------------------------------------//
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification() {

        PendingIntent pendingIntent;
        Intent intentService;

        // Create Remote View object
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_song_selection);

        // Set Views inside Remote View object
        mRemoteViews.setTextViewText(R.id.notify_song_title, mSongList.get(SONG_POS).get_title());

        // Intent Actions inside Pending intent
        intentService = new Intent(ACTION_STOP);
        pendingIntent = PendingIntent.getService(getApplicationContext(),
                REQUEST_CODE_STOP, intentService, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notify_play_pause_button, pendingIntent);

        intentService = new Intent(ACTION_PAUSE);
        pendingIntent = PendingIntent.getService(getApplicationContext(),
                REQUEST_CODE_PAUSE, intentService, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notify_play_pause_button, pendingIntent);

        intentService = new Intent(ACTION_NEXT);
        pendingIntent = PendingIntent.getService(getApplicationContext(),
                REQUEST_CODE_NEXT, intentService, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notify_next_button, pendingIntent);

        intentService = new Intent(ACTION_PREVIOUS);
        pendingIntent = PendingIntent.getService(getApplicationContext(),
                REQUEST_CODE_PREVIOUS, intentService, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notify_previous_button, pendingIntent);

        // build notification first, then update it
        intentService.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_RECEIVER_FOREGROUND);

        CharSequence channelName = getResources().getString(R.string.channel_name);

        // check current SDK build version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel =
                    new NotificationChannel(PLAYER_CHANNEL_ID, name, importance);
            mChannel.setSound(null, null);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
            mBuilder = new NotificationCompat.Builder(this, PLAYER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_media_play_dark)
                    .setContentTitle(mSongList.get(SONG_POS).get_title())
                    .setChannelId("Player").setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setWhen(System.currentTimeMillis()) // immediately trigger notification
                    .setCustomContentView(mRemoteViews)  // send content RemoteView or CustomView
                    .setDefaults(Notification.FLAG_NO_CLEAR)
                    .setContentIntent(pendingIntent)
                    .setContent(mRemoteViews)
                    .setTicker(channelName);

            // starting service with notification
            notificationManager.notify(PLAYER_CHANNEL_ID, NOTIFICATION_ID, mBuilder.build());

        } else {
            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_media_play_dark)
                    .setWhen(System.currentTimeMillis()) // immediately trigger notification
                    .setCustomContentView(mRemoteViews)  // send content RemoteView or CustomView
                    .setDefaults(Notification.FLAG_NO_CLEAR)
                    .setContentIntent(pendingIntent)
                    .setContent(mRemoteViews)
                    .setTicker(channelName);

            // starting service with notification
            startForeground(NOTIFICATION_ID, mBuilder.build());
        }

    }
//------------------------------------------------------------------------------------------------//

    /**
     * Update the Notification's UI.
     * Method is used to notify user about current playing song and provide Next, Previous and
     * Play/Pause Buttons. Also provide song Title, song Duration data.
     */
//------------------------------------------------------------------------------------------------//
    private void updateNotification() {
        int api = Build.VERSION.SDK_INT;
        // update the icon and text views
        try {
            mRemoteViews.setTextViewText(R.id.notify_song_title, mSongList.get(SONG_POS).get_title());
        } catch (Exception ignored) {
        }
        // update the notification
        if (api >= Build.VERSION_CODES.JELLY_BEAN && api < Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, mBuilder.build());
        } else {
            notificationManager.notify(PLAYER_CHANNEL_ID, NOTIFICATION_ID, mBuilder.build());
        }
    }

    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation.
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info.
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start(); // After prepare is done play song
        updateNotification();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated.
    }

}