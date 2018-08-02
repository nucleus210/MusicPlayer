package com.example.android.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;


class SongAdapter extends ArrayAdapter<Song> {
    private final Context mContext;

    SongAdapter(@NonNull Context context, @NonNull ArrayList<Song> songs) {
        super(context, R.layout.song_list, songs);
        this.mContext = context;
    }

    class ViewHolder {
        TextView songTitle;
        TextView songPath;
        TextView songDuration;
        ImageButton btnNxt;
    }

    @Nullable
    @Override
    public Song getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public int getPosition(@Nullable Song item) {
        return super.getPosition(item);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder = new ViewHolder();

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.song_list, parent, false);
            viewHolder.songTitle = convertView.findViewById(R.id.song_title);
            viewHolder.songPath = convertView.findViewById(R.id.song_path);
            viewHolder.songDuration = convertView.findViewById(R.id.song_duration);
            viewHolder.btnNxt = convertView.findViewById(R.id.media_play);

        } else {

            viewHolder = (ViewHolder) convertView.getTag(R.layout.song_list);
        }
        Song data = getItem(position);

        if (data != null) {
            if (viewHolder != null) {
                viewHolder.songTitle.setText(data.get_title());
            }
            if (viewHolder != null) {
                viewHolder.songPath.setText(data.get_dataPath());
            }
            if (viewHolder != null) {
                viewHolder.songDuration.setText(data.get_duration());
            }
            if (viewHolder != null) {
                viewHolder.btnNxt.setTag(position);

                viewHolder.btnNxt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        String curPos = Integer.toString(position);
                        sendMessage(curPos);
                    }
                });
            }
        }
        return convertView;
    }
//------------------------------------------------------------------------------------------------//
    /**
    // Asynchronous call to Broadcast given intent to all interested BroadcastReceivers
    // Broadcast selected Song position to FullScreenPlayer
     */
//------------------------------------------------------------------------------------------------//
    private void sendMessage(String mPos) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("adapter action");
        // You can also include some extra data.
        intent.putExtra("song position", mPos);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}

