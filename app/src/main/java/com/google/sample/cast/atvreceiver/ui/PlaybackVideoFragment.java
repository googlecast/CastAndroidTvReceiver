/**
 * Copyright 2020 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.sample.cast.atvreceiver.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.widget.PlaybackControlsRow;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.gms.cast.MediaError;
import com.google.android.gms.cast.MediaError.DetailedErrorCode;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.tv.CastReceiverContext;
import com.google.android.gms.cast.tv.media.MediaException;
import com.google.android.gms.cast.tv.media.MediaInfoWriter;
import com.google.android.gms.cast.tv.media.MediaLoadCommandCallback;
import com.google.android.gms.cast.tv.media.MediaManager;
import com.google.android.gms.cast.tv.media.MediaManager.MediaStatusInterceptor;
import com.google.android.gms.cast.tv.media.MediaStatusWriter;
import com.google.android.gms.common.images.WebImage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.sample.cast.atvreceiver.data.Movie;
import com.google.sample.cast.atvreceiver.data.MovieList;
import com.google.sample.cast.atvreceiver.player.VideoPlayerGlue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles video playback with media controls.
 */
public class PlaybackVideoFragment extends VideoSupportFragment {

    private static final String LOG_TAG = "PlaybackVideoFragment";
    private static final int UPDATE_DELAY = 16;

    private MediaSessionCompat mMediaSession;
    private MediaSessionConnector mMediaSessionConnector;

    private Player mPlayer;
    private LeanbackPlayerAdapter mPlayerAdapter;
    private VideoPlayerGlue mPlayerGlue;
    private PlaylistActionListener mPlaylistActionListener;

    private MediaManager mMediaManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");

        mMediaSession = new MediaSessionCompat(requireContext(), LOG_TAG);
        mMediaSessionConnector = new MediaSessionConnector(mMediaSession);
        initializePlayer();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");

        mMediaManager = CastReceiverContext.getInstance().getMediaManager();
        mMediaManager.setSessionCompatToken(mMediaSession.getSessionToken());
        mMediaManager.setMediaLoadCommandCallback(new MyMediaLoadCommandCallback());

        mMediaManager.setMediaStatusInterceptor(new MediaStatusInterceptor() {
            @Override
            public void intercept(MediaStatusWriter mediaStatusWriter) {
                try {
                    mediaStatusWriter.setCustomData(new JSONObject("{data: 'CustomData'}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        initializePlayer();
        mMediaSessionConnector.setPlayer(mPlayer);

        TimelineQueueNavigator timelineQueueNavigator = new TimelineQueueNavigator(mMediaSession) {
            @Override
            public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
                com.google.android.exoplayer2.MediaMetadata mediaMetadata =
                        player.getMediaItemAt(windowIndex).mediaMetadata;

                return new MediaDescriptionCompat.Builder()
                        .setMediaUri(mediaMetadata.mediaUri)
                        .setIconUri(mediaMetadata.artworkUri)
                        .setTitle(mediaMetadata.title)
                        .setSubtitle(mediaMetadata.subtitle)
                        .build();
            }
        };
        mMediaSessionConnector.setQueueNavigator(timelineQueueNavigator);

        mMediaSession.setActive(true);

        if (mMediaManager.onNewIntent(requireActivity().getIntent())) {
            // If the SDK recognizes the intent, you should early return.
            return;
        }

        // If the SDK doesn't recognize the intent, you can handle the intent with
        // your own logic.
        processIntent(requireActivity().getIntent());

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
        if (mPlayerGlue != null && mPlayerGlue.isPlaying()) {
            mPlayerGlue.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");

        mMediaSessionConnector.setPlayer(null);
        mMediaSession.setActive(false);
        mMediaSession.release();
        mMediaManager.setSessionCompatToken(null);
        releasePlayer();
    }

    @Override
    public void onError(int errorCode, CharSequence errorMessage) {
        Log.d(LOG_TAG, "onError");
        logAndDisplay(errorMessage.toString());
        requireActivity().finish();
    }

    void processIntent(Intent intent) {
        Log.d(LOG_TAG, "processIntent()");

        if (intent.hasExtra(MainActivity.MOVIE)) {
            // Intent came from MainActivity (User chose an item inside ATV app).
            Movie movie = (Movie) intent.getSerializableExtra(MainActivity.MOVIE);
            startPlayback(movie, 0);
        } else {
            logAndDisplay("Null or unrecognized intent action");
            requireActivity().finish();
        }
    }

    private static Movie convertEntityToMovie(String entity) {
        return MovieList.getList().get(0);
    }

    private static Movie convertLoadRequestToMovie(MediaLoadRequestData loadRequestData) {
        if (loadRequestData == null) {
            return null;
        }
        MediaInfo mediaInfo = loadRequestData.getMediaInfo();
        if (mediaInfo == null) {
            return null;
        }

        String videoUrl = mediaInfo.getContentId();
        if (mediaInfo.getContentUrl() != null) {
            videoUrl = mediaInfo.getContentUrl();
        }

        MediaMetadata metadata = mediaInfo.getMetadata();
        Movie movie = new Movie();
        movie.setVideoUrl(videoUrl);
        if (metadata != null) {
            movie.setTitle(metadata.getString(MediaMetadata.KEY_TITLE));
            movie.setDescription(metadata.getString(MediaMetadata.KEY_SUBTITLE));
            movie.setCardImageUrl(metadata.getImages().get(0).getUrl().toString());
        }
        return movie;
    }

    private void initializePlayer() {
        if (mPlayer == null) {
            Log.d(LOG_TAG, "initializePlayer");
            VideoSupportFragmentGlueHost glueHost =
                new VideoSupportFragmentGlueHost(PlaybackVideoFragment.this);

            mPlayer = new SimpleExoPlayer.Builder(requireContext()).build();
            mPlayerAdapter = new LeanbackPlayerAdapter(requireContext(), mPlayer, UPDATE_DELAY);
            mPlayerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE);
            mPlaylistActionListener = new PlaylistActionListener();
            mPlayerGlue = new VideoPlayerGlue(getContext(), mPlayerAdapter, mPlaylistActionListener);
            mPlayerGlue.setHost(glueHost);
            mPlayerGlue.setSeekEnabled(true);

            mPlayer.addListener(new Player.Listener() {
                @Override
                public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                    CharSequence title = "";
                    CharSequence subtitle = "";
                    if (mediaItem != null) {
                        // mediaIem is null if player has been stopped or
                        // all media items have been removed from the playlist
                        title = mediaItem.mediaMetadata.title;
                        subtitle = mediaItem.mediaMetadata.subtitle;
                    }
                    mMediaManager.getMediaStatusModifier().clear();
                    mPlayerGlue.setTitle(title);
                    mPlayerGlue.setSubtitle(subtitle);
                }
            });
        }
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            Log.d(LOG_TAG, "releasePlayer");
            mPlayer.release();
            mPlayer = null;
            mPlayerAdapter = null;
        }
    }

    private void startPlayback(Movie movie, long startPosition) {
        List<MediaItem> mediaItems = new ArrayList<>();

        MediaItem firstMediaItem = new MediaItem.Builder()
            .setUri(movie.getVideoUrl())
            .setMediaMetadata(
                new com.google.android.exoplayer2.MediaMetadata.Builder()
                    .setMediaUri(Uri.parse(movie.getVideoUrl()))
                    .setArtworkUri(Uri.parse(movie.getCardImageUrl()))
                    .setTitle(movie.getTitle())
                    .setSubtitle(movie.getDescription())
                    .build()
            ).build();
        mediaItems.add(firstMediaItem);

        MovieList.getList().forEach( movieItem -> {
            mediaItems.add(new MediaItem.Builder()
                .setUri(movieItem.getVideoUrl())
                .setMediaMetadata(
                    new com.google.android.exoplayer2.MediaMetadata.Builder()
                        .setMediaUri(Uri.parse(movieItem.getVideoUrl()))
                        .setArtworkUri(Uri.parse(movieItem.getCardImageUrl()))
                        .setTitle(movieItem.getTitle())
                        .setSubtitle(movieItem.getDescription())
                        .build()
                ).build()
            );
        });

        mPlayer.setMediaItems(mediaItems);
        mPlayer.prepare();
        mPlayerGlue.playWhenPrepared();
        mPlayerGlue.seekTo(startPosition);
        mMediaManager.getMediaStatusModifier().clear();
    }

    private void logAndDisplay(String error) {
        Log.d(LOG_TAG, error);
        Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
    }

    class PlaylistActionListener implements VideoPlayerGlue.OnActionClickedListener {
        @Override
        public void onPrevious() {
            mPlayer.previous();
        }

        @Override
        public void onNext() {
            mPlayer.next();
        }
    }

    private void myFillMediaInfo(MediaInfoWriter mediaInfoWriter) {
        MediaInfo mediaInfo = mediaInfoWriter.getMediaInfo();
        Log.d(LOG_TAG,"***Type:"+mediaInfo.getContentType());
        if (mediaInfo.getContentUrl() == null && mediaInfo.getEntity() != null) {
            // Load By Entity
            String entity = mediaInfo.getEntity();
            Movie movie = convertEntityToMovie(entity);

            MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            movieMetadata.putString(MediaMetadata.KEY_TITLE, movie.getTitle());
            movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, movie.getDescription());
            movieMetadata.putString(MediaMetadata.KEY_STUDIO, movie.getStudio());
            movieMetadata.addImage(new WebImage(Uri.parse(movie.getCardImageUrl())));
            movieMetadata.addImage(new WebImage(Uri.parse(movie.getBackgroundImageUrl())));

            mediaInfoWriter.setContentUrl(movie.getVideoUrl()).setMetadata(movieMetadata);
        }
    }

    class MyMediaLoadCommandCallback extends MediaLoadCommandCallback {
        @Override
        public Task<MediaLoadRequestData> onLoad(String senderId, MediaLoadRequestData loadRequestData) {
            Toast.makeText(getActivity(), "onLoad()", Toast.LENGTH_SHORT).show();

            if (loadRequestData == null) {
                // Throw MediaException to indicate load failure.
                return Tasks.forException(new MediaException(
                    new MediaError.Builder()
                        .setDetailedErrorCode(DetailedErrorCode.LOAD_FAILED)
                        .setReason(MediaError.ERROR_REASON_INVALID_REQUEST)
                        .build()));
            }

            return Tasks.call(() -> {
                // Resolve the entity into your data structure and load media.
                myFillMediaInfo(new MediaInfoWriter(loadRequestData.getMediaInfo()));
                startPlayback(convertLoadRequestToMovie(loadRequestData), 0);

                // Update media metadata and state (this clears all previous status
                // overrides).
                mMediaManager.setDataFromLoad(loadRequestData);
                mMediaManager.broadcastMediaStatus();

                return loadRequestData;
            });
        }
    }
}