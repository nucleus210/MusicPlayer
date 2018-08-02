package com.example.android.musicplayer;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.musicplayer.MediaPlayerService.PlayerBinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static android.widget.Toast.LENGTH_SHORT;

public class MainPlayerActivity extends AppCompatActivity {

    private static final String TAG = "Main Player Activity";

    View actionBar;
    private int state;
    SongAdapter songAdapter;
    LinearLayout mBottomActionBar;
    private Boolean mBound;
    private Handler mHandler;
    private Context mContext;
    private SeekBar mSeekBar;
    private Runnable mRunnable;
    private TextView mSongTimer;
    private RotateAnimation anim;
    private ImageView mAnimatedView;
    private ImageButton mPlayButton;
    private ImageButton mNextButton;
    private ImageButton mAddSongList;
    private ArrayList<Song> mSongList;
    private ImageButton mPreviousButton;
    private boolean mHomeButtonPressed = false;
    private MediaDataProvider mediaDataProvider;
    private MediaPlayerService mediaPlayerService;
    private static final int USER_PRESS_LIST_BUTTON = 101;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        mContext = this.getApplicationContext();
        Log.d(TAG, "onCreate");
        mBound = false;

        // Run First Time and upload demo song to Android device. This code run only in installation
        // process. After that
        SharedPreferences wmbPreference =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = wmbPreference.getBoolean("FirstRun", true);
        if (isFirstRun) {
            if (ContextCompat.checkSelfPermission(MainPlayerActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainPlayerActivity.this, R.string.permission,
                        LENGTH_SHORT).show();
            } else {
                // Permission is granted
                // Start new AsyncTask to copy media files in background mode
                // without blocking first Run
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        copyRawData();
                        return null;
                    }
                }.execute();
                SharedPreferences.Editor editor = wmbPreference.edit();
                editor.putBoolean("FirstRun", false);
                editor.apply();
            }
        }

        requestPermission();                             // request permission and initialize player

        // Initialize play animation
        anim = new RotateAnimation(0f, 890f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        anim.setStartOffset(10);
        anim.setFillAfter(true);
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(10000);
    }

    private void init() {
        mSongList = new ArrayList<>();
        mediaDataProvider = new MediaDataProvider();
        mediaPlayerService = new MediaPlayerService();

        // check that list is empty or null and populate list with data
        if (mSongList.size() == 0 || mSongList == null) {
            mSongList = mediaDataProvider.scanSongs(this);
            songAdapter = new SongAdapter(MainPlayerActivity.this, mSongList);
            mediaPlayerService.setSongList(mSongList);
        }

        // define view objects
        mBottomActionBar = findViewById(R.id.bottom_action_bar);
        LinearLayout mBottomActionBar = findViewById(R.id.bottom_action_bar);

        // inflate bottom action bar to the UI
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        actionBar = inflater.inflate(R.layout.action_button_layout,
                mBottomActionBar, false);
        mBottomActionBar.addView(actionBar, mBottomActionBar.getChildCount() - 1);

        // initialize view objects by Ids
        mSongTimer = findViewById(R.id.timer);
        mNextButton = findViewById(R.id.next_button);
        mAnimatedView = findViewById(R.id.playing_image);
        mAddSongList = findViewById(R.id.add_list_button);
        mPlayButton = findViewById(R.id.play_pause_button);
        mPlayButton = findViewById(R.id.play_pause_button);
        mPreviousButton = findViewById(R.id.previous_button);

        // Add change listener on seekBar
        mSeekBar = findViewById(R.id.seekBar);
        // get progress change listener
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayerService.mPlayer.seekTo(progress * 1000);
                }
            }
        });

        /*
          Run UI thread to get data progress to update text view and update seekBar
         */
//------------------------------------------------------------------------------------------------//
        //Make new object handler for song timer
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                try {

                    if (mediaPlayerService.mPlayer.isPlaying()) {
                        mSongTimer.setText(milliSecondsToTimer
                                (mediaPlayerService.mPlayer.getCurrentPosition()));
                        mSeekBar.setMax(mediaPlayerService.mPlayer.getDuration() / 1000);
                        int mCurrentPosition =
                                mediaPlayerService.mPlayer.getCurrentPosition() / 1000;
                        mSeekBar.setProgress(mCurrentPosition);
                    }
                    mHandler.postDelayed(this, 1000);
                } catch (NullPointerException e) {
                    Log.e(TAG, "MainPlayerActivity: Song handler is null. No data.");

                }
            }
        };
        mRunnable.run();

        // Button onClick Listener for Songs list View
//------------------------------------------------------------------------------------------------//
        mAddSongList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButtonAnimation(mAddSongList);      //play button animation
                Intent intent = new Intent(MainPlayerActivity.this,
                        FullScreenPlayer.class);
                startActivity(intent);                  //start
                mSongList = null;
                if (mAnimatedView.getAnimation() != null) {
                    mAnimatedView.setAnimation(null);
                }
                state = USER_PRESS_LIST_BUTTON;
            }
        });

        // Play(Pause) Button onClick Listener
//------------------------------------------------------------------------------------------------//
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSongList != null && mSongList.size() > 0) {
                    try {
                        playButtonAnimation(mPlayButton);       //play onClick animation
                        if (mediaPlayerService.mPlayer.isPlaying()) {
                            mediaPlayerService.playPause();
                            mAnimatedView.setAnimation(null);

                        } else if (MediaPlayerService.SONG_POS == 0) {
                            mediaPlayerService.setSongList(mSongList);
                            mediaPlayerService.setSelectedSong(0,
                                    MediaPlayerService.NOTIFICATION_ID);
                            mAnimatedView.startAnimation(anim);

                        } else {
                            mediaPlayerService.playPause();
                            mAnimatedView.startAnimation(anim);
                        }
                        updateData();                           //update text views values
                    } catch (NullPointerException e) {
                        Log.d(TAG, "Null Media Player object", e);
                    }
                } else {
                    ScanForMediaFiles scanForMediaFiles = new ScanForMediaFiles();
                    scanForMediaFiles.execute();
                }
            }
        });

        // Next Song Button onClick Listener
//------------------------------------------------------------------------------------------------//
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSongList != null && mSongList.size() > 0) {
                    try {
                        playButtonAnimation(mNextButton);       //play onClick animation
                        mediaPlayerService.nextSong();          //play next song
                        updateData();                           //update text views values

                        if (mAnimatedView.getAnimation() == null) {
                            mAnimatedView.startAnimation(anim);
                        }
                    } catch (NullPointerException e) {
                        Log.d(TAG, "Null Media Player object", e);
                    }
                } else {
                    ScanForMediaFiles scanForMediaFiles = new ScanForMediaFiles();
                    scanForMediaFiles.execute();
                }
            }
        });

        // Previous Song Button onClick Listener
//------------------------------------------------------------------------------------------------//
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSongList != null && mSongList.size() > 0) {
                    try {
                        playButtonAnimation(mPreviousButton);   //play onClick animation
                        mediaPlayerService.previousSong();      //play previous song
                        updateData();                           //update text views values

                        if (mAnimatedView.getAnimation() == null) {
                            mAnimatedView.startAnimation(anim);
                        }
                    } catch (NullPointerException e) {
                        Log.d(TAG, "Null Media Player object", e);
                    }
                } else {
                    ScanForMediaFiles scanForMediaFiles = new ScanForMediaFiles();
                    scanForMediaFiles.execute();
                }
            }
        });
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to update Seek Bar
     * <p>
     * Refresh Song List in background task. This is invoke in first run. But Work not.
     */

//------------------------------------------------------------------------------------------------//
    @SuppressLint("StaticFieldLeak")
    private class ScanForMediaFiles extends AsyncTask<Void, Integer, Long> {
        // Do the long-running work in here
        @Override
        protected Long doInBackground(Void... voids) {
            mSongList = mediaDataProvider.scanSongs(mContext);
            songAdapter = new SongAdapter(MainPlayerActivity.this, mSongList);
            mediaPlayerService.setSongList(mSongList);
            return null;
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) {

        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(Long result) {
            int findSongs = mSongList.size();
            Log.d(TAG, "Media Scan Complete. List contains: " + findSongs + " songs");
        }
    }

//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to update Seek Bar
     *
     * @param milliseconds input time in milliseconds and convert duration into time
     */
//------------------------------------------------------------------------------------------------//
    private String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }
        // Prepending 0 to seconds if it is one digit
        String secondsString;
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }
        finalTimerString = finalTimerString + minutes + ":" + secondsString;
        // return timer string
        return finalTimerString;
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used play button animation.
     *
     * @param obj pass animation object
     */
//------------------------------------------------------------------------------------------------//
    private void playButtonAnimation(Object obj) {
        final Animator mButtonAnimationA =
                AnimatorInflater.loadAnimator(MainPlayerActivity.this,
                        R.animator.button_click_anim);
        mButtonAnimationA.setTarget(obj);
        mButtonAnimationA.start();
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to request writable permission to the External Storage.
     */
//------------------------------------------------------------------------------------------------//
    @SuppressWarnings("StatementWithEmptyBody")
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(MainPlayerActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainPlayerActivity.this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainPlayerActivity.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            init(); // init player after permission is granted
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Handle the permissions request response. Method is used to find out whether the permission
     * was granted or not.
     */
//------------------------------------------------------------------------------------------------//
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPermission();
                } else {
                    finish();
                }
        }
    }
//------------------------------------------------------------------------------------------------//
    /**
     * Start Media Player Service connection and bind to that service.
     */
//------------------------------------------------------------------------------------------------//
    private final ServiceConnection connectMusic = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerBinder binder = (PlayerBinder) service;
            mediaPlayerService = binder.getService();
            mediaPlayerService.setSongList(mSongList);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to check Writable access ot the Public External Storage
     */
//------------------------------------------------------------------------------------------------//
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to copy music files from raw directory to External Device Storage Dir
     *
     * @param context      Context
     * @param resourceId   Resource file from Raw directory
     * @param resourceName pass name of file
     */
//------------------------------------------------------------------------------------------------//
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createExternalStoragePublicFile(Context context, int resourceId,
                                                 String resourceName) {
        // Create a path where we will place our picture in the user's
        // public pictures directory.  Note that you should be careful about
        // what you place here, since the user often manages these files.  For
        // pictures and other media owned by the application, consider
        // Context.getExternalMediaDir().
        Boolean isExtStorageWritable = isExternalStorageWritable();
        if (isExtStorageWritable) {
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC);
            File file = new File(path, resourceName);

            try {
                // Make sure the Pictures directory exists.
                path.mkdirs();

                // Very simple code to copy a picture from the application's
                // resource into the external file.  Note that this code does
                // no error checking, and assumes the picture is small (does not
                // try to copy it in chunks).  Note that if external storage is
                // not currently mounted this will silently fail.
                InputStream is = getResources().openRawResource(resourceId);
                OutputStream os = new FileOutputStream(file);
                byte[] data = new byte[is.available()];
                is.read(data);
                os.write(data);
                is.close();
                os.close();

                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(context,
                        new String[]{file.toString()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                            }
                        });
            } catch (IOException e) {
                // Unable to create file, likely because external storage is
                // not currently mounted.
                Log.w("ExternalStorage", "Error writing " + file, e);
            }
        }
    }

//------------------------------------------------------------------------------------------------//

    /**
     * Override Activity Live Cycle methods
     */
//------------------------------------------------------------------------------------------------//
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (!mBound) {
            Intent playIntent = new Intent(this, MediaPlayerService.class);
            bindService(playIntent, connectMusic, Context.BIND_AUTO_CREATE);
        } else {
            Log.d(TAG, "Main Player Activity: Service is already started");
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (!mHomeButtonPressed) {
            if (mBound) {
                unbindService(connectMusic);
            }
            anim.cancel();
            mBound = true;
            mHandler.removeCallbacks(mRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mHomeButtonPressed = false;
        // bind to the service
        if (!mBound) {
            Intent playIntent = new Intent(this, MediaPlayerService.class);
            bindService(playIntent, connectMusic, Context.BIND_AUTO_CREATE);
        } else {
            Log.d(TAG, "Main Player Activity: Service is already started");
        }
        try {
            if (mediaPlayerService.mPlayer.isPlaying()) {
                playAnimation();
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Null media player object", e);
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Exit from player. After user press hardware back button player stop and exit from app.
     */
//------------------------------------------------------------------------------------------------//

    public void onBackPressed() {
        super.onBackPressed();
        Intent exitIntend = new Intent(Intent.ACTION_MAIN);
        exitIntend.addCategory(Intent.CATEGORY_HOME);
        exitIntend.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(exitIntend);
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                Log.d(TAG, "Menu button pressed");
                mHomeButtonPressed = true;
                break;
            case KeyEvent.KEYCODE_HOME:
                Log.d(TAG, "Home button pressed");
                mHomeButtonPressed = true;
                break;
        }
        return super.onKeyDown(keycode, e);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Log.d(TAG, "Home button pressed");
        if (state != USER_PRESS_LIST_BUTTON) {
            mHomeButtonPressed = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
        finish();
    }

//------------------------------------------------------------------------------------------------//

    /**
     * Update views with new data
     */
//------------------------------------------------------------------------------------------------//
    private void updateData() {
        TextView mSongTitle = findViewById(R.id.song_title);
        TextView mSongArtist = findViewById(R.id.song_artist);
        TextView mSongTime = findViewById(R.id.count_down_timer);
        if (mSongList != null && mSongList.size() != 0) {
            mSongTitle.setText(mSongList.get(MediaPlayerService.SONG_POS).get_title());
            mSongArtist.setText(mSongList.get(MediaPlayerService.SONG_POS).get_artist());
            mSongTime.setText(mSongList.get(MediaPlayerService.SONG_POS).get_duration());

        } else {
            ScanForMediaFiles scanForMediaFiles = new ScanForMediaFiles();
            scanForMediaFiles.execute();

        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Play animation
     */
//------------------------------------------------------------------------------------------------//
    private void playAnimation() {
        // Initialize play animation
        anim = new RotateAnimation(0f, 890f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setStartOffset(10);
        anim.setFillAfter(true);
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(10000);
        mAnimatedView.startAnimation(anim);
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Method is used to copy content of raw directory to Public External Device Storage Dir.
     */
//------------------------------------------------------------------------------------------------//
    private void copyRawData() {
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.always_the_alibi_ain_t_another_girl,
                "always_the_alibi_ain_t_another_girl.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.chris_zabriskie, "chris_zabriskie.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.igor_pumphonia_coffee_time_rock_me_roll_me,
                "igor_pumphonia_coffee_time_rock_me_roll_me.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.jasmine_jordan_time_travel_feat_blanchard_de_wave,
                "jasmine_jordan_time_travel_feat_blanchard_de_wave.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.jekk_so_strong_la_style_remix,
                "jekk_so_strong_la_style_remix.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.jekk_strong, "jekk_strong.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.michael_mc_eachern_gone, "michael_mc_eachern_gone.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.mickey_blue_what_i_wouldn_t_do,
                "mickey_blue_what_i_wouldn_t_do.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.roller_genoa_safe_and_warm_in_hunter_s_arms,
                "roller_genoa_safe_and_warm_in_hunter_s_arms.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.silent_partner_highway_danger,
                "silent_partner_highway_danger.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.silent_partner_pomade, "silent_partner_pomade.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.the_madpix_project_moments, "the_madpix_project_moments.mp3");
        createExternalStoragePublicFile(MainPlayerActivity.this,
                R.raw.tracing_arcs_voodoo_zengineers_undecided_remix,
                "tracing_arcs_voodoo_zengineers_undecided_remix.mp3");
    }

}
