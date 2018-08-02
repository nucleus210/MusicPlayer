package com.example.android.musicplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.example.android.musicplayer.MediaPlayerService.PlayerBinder;

import java.util.ArrayList;

public class FullScreenPlayer extends MainPlayerActivity {
    private static final String TAG = "List Activity";

    private Context mContext;
    private ListView mSongListView;
    private boolean mBound = false;
    private boolean mHomeButtonPressed = false;
    private MediaPlayerService mediaPlayerService;
    private ArrayList<Song> mSongList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_activity);
        mContext = this.getApplicationContext();
        Log.d(TAG, "onCreate");

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("adapter action"));

        mSongListView = findViewById(R.id.song_list_view);
        Toolbar mToolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(mToolbar);

        UpdateListView updateListView = new UpdateListView();
        updateListView.run();
    }

    class UpdateListView implements Runnable {

        @Override
        public void run() {
            mediaPlayerService = new MediaPlayerService();
            MediaDataProvider mediaDataProvider = new MediaDataProvider();
            mSongList = new ArrayList<>(mediaDataProvider.scanSongs(mContext));
            mediaDataProvider.destroy();
            SongAdapter songAdapter = new SongAdapter(FullScreenPlayer.this, mSongList);
            mSongListView.setAdapter(songAdapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.action_back:
                mSongList = null; // clear array list
                mSongListView.setAdapter(null); // clear adapter
                Intent intent = new Intent(FullScreenPlayer.this,
                        MainPlayerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // remove activity from back stack
                startActivity(intent); // start activity via intent
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Activity Live Cycle methods
     */
//------------------------------------------------------------------------------------------------//
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // unregister local broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // unbind created from user service
        if (!mHomeButtonPressed) {
            if (mBound) {
                unbindService(serviceConnection);
            }
            mBound = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mSongList = null;               // clear array list
        mSongListView.setAdapter(null); // clear adapter
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
    }
//------------------------------------------------------------------------------------------------//

    /**
     * Exit and go back in Main Player Activity. User pressed hardware back button.
     */
//------------------------------------------------------------------------------------------------//
    public void onBackPressed() {
        super.onBackPressed();
        Intent backToMainActivity = new Intent(getApplicationContext(), MainPlayerActivity.class);
        // Will clear out activity history stack till now
        backToMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(backToMainActivity);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Log.d(TAG, "Home button pressed");

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        mHomeButtonPressed = false;
        if (!mBound) {
            Intent listPlayIntent = new Intent(this, MediaPlayerService.class);
            bindService(listPlayIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

//------------------------------------------------------------------------------------------------//
    /**
     * Start Media Player Service connection and bind to that service.
     */
//------------------------------------------------------------------------------------------------//
    private final ServiceConnection serviceConnection = new ServiceConnection() {
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
     * Broadcast receiver. Receive message with Selected Song from List on button click.
     */
//------------------------------------------------------------------------------------------------//
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("song position");
            Log.d("receiver", "Got song position message: " + message);
            int myNum;

            try {
                myNum = Integer.parseInt(message);
                mediaPlayerService.setSelectedSong(myNum, MediaPlayerService.NOTIFICATION_ID);

            } catch (NumberFormatException nfe) {
                System.out.println("Could not parse " + nfe);
            }
        }
    };
}
