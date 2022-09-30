/**
 * Copyright 2022 Google LLC. All Rights Reserved.
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
package com.google.sample.cast.atvreceiver.ui

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import com.google.android.gms.cast.tv.media.MediaManager
import com.google.android.gms.cast.tv.CastReceiverContext
import androidx.leanback.app.VideoSupportFragment
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.sample.cast.atvreceiver.player.VideoPlayerGlue
import com.google.android.gms.cast.tv.media.MediaManager.MediaStatusInterceptor
import org.json.JSONObject
import org.json.JSONException
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.leanback.app.VideoSupportFragmentGlueHost
import com.google.android.exoplayer2.SimpleExoPlayer
import androidx.leanback.widget.PlaybackControlsRow
import com.google.sample.cast.atvreceiver.data.MovieList
import android.widget.Toast
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.gms.cast.tv.media.MediaInfoWriter
import com.google.android.gms.common.images.WebImage
import com.google.android.gms.cast.tv.media.MediaLoadCommandCallback
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.cast.tv.media.MediaException
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaError.DetailedErrorCode
import com.google.android.gms.tasks.Task
import com.google.sample.cast.atvreceiver.data.Movie
import java.util.ArrayList
import java.util.function.Consumer

/**
 * Handles video playback with media controls.
 */
class PlaybackVideoFragment : VideoSupportFragment() {
    private var mMediaSession: MediaSessionCompat? = null
    private var mMediaSessionConnector: MediaSessionConnector? = null
    private var mPlayer: Player? = null
    private var mPlayerAdapter: LeanbackPlayerAdapter? = null
    private var mPlayerGlue: VideoPlayerGlue? = null
    private var mPlaylistActionListener: PlaylistActionListener? = null
    private var mMediaManager: MediaManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "onCreate")
        mMediaSession = MediaSessionCompat(requireContext(), LOG_TAG)
        mMediaSessionConnector = MediaSessionConnector(mMediaSession!!)
        initializePlayer()
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "onStart")
        mMediaManager = CastReceiverContext.getInstance().mediaManager
        mMediaManager?.setSessionCompatToken(mMediaSession!!.sessionToken)
        mMediaManager?.setMediaLoadCommandCallback(MyMediaLoadCommandCallback())
        mMediaManager?.setMediaStatusInterceptor(MediaStatusInterceptor { mediaStatusWriter ->
            try {
                mediaStatusWriter.setCustomData(JSONObject("{data: 'CustomData'}"))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        })
        initializePlayer()
        mMediaSessionConnector!!.setPlayer(mPlayer)
        val timelineQueueNavigator: TimelineQueueNavigator = object : TimelineQueueNavigator(mMediaSession!!) {
            override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                val mediaMetadata = player.getMediaItemAt(windowIndex).mediaMetadata
                return MediaDescriptionCompat.Builder()
                        //.setMediaUri(mediaMetadata.mediaUri)
                        .setIconUri(mediaMetadata.artworkUri)
                        .setTitle(mediaMetadata.title)
                        .setSubtitle(mediaMetadata.subtitle)
                        .build()
            }
        }
        mMediaSessionConnector!!.setQueueNavigator(timelineQueueNavigator)
        mMediaSession!!.isActive = true
        if (mMediaManager!!.onNewIntent(requireActivity().intent)) {
            // If the SDK recognizes the intent, you should early return.
            return
        }

        // If the SDK doesn't recognize the intent, you can handle the intent with
        // your own logic.
        processIntent(requireActivity().intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "onPause")
        if (mPlayerGlue != null && mPlayerGlue!!.isPlaying) {
            mPlayerGlue!!.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "onStop")
        mMediaSessionConnector!!.setPlayer(null)
        mMediaSession!!.isActive = false
        mMediaSession!!.release()
        mMediaManager!!.setSessionCompatToken(null)
        releasePlayer()
    }

    public override fun onError(errorCode: Int, errorMessage: CharSequence) {
        Log.d(LOG_TAG, "onError")
        logAndDisplay(errorMessage.toString())
        requireActivity().finish()
    }

    fun processIntent(intent: Intent) {
        Log.d(LOG_TAG, "processIntent()")
        if (intent.hasExtra(MainActivity.MOVIE)) {
            // Intent came from MainActivity (User chose an item inside ATV app).
            val movie = intent.getSerializableExtra(MainActivity.MOVIE) as Movie?
            startPlayback(movie, 0)
        } else {
            logAndDisplay("Null or unrecognized intent action")
            requireActivity().finish()
        }
    }

    private fun initializePlayer() {
        if (mPlayer == null) {
            Log.d(LOG_TAG, "initializePlayer")
            val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
            mPlayer = SimpleExoPlayer.Builder(requireContext()).build()
            mPlayerAdapter = LeanbackPlayerAdapter(requireContext(), mPlayer!!, UPDATE_DELAY)
            mPlayerAdapter!!.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)
            mPlaylistActionListener = PlaylistActionListener()
            mPlayerGlue = VideoPlayerGlue(context, mPlayerAdapter, mPlaylistActionListener!!)
            mPlayerGlue!!.host = glueHost
            mPlayerGlue!!.isSeekEnabled = true
            mPlayer?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    var title: CharSequence? = ""
                    var subtitle: CharSequence? = ""
                    if (mediaItem != null) {
                        // mediaIem is null if player has been stopped or
                        // all media items have been removed from the playlist
                        title = mediaItem.mediaMetadata.title
                        subtitle = mediaItem.mediaMetadata.subtitle
                    }
                    mMediaManager!!.mediaStatusModifier.clear()
                    mPlayerGlue!!.title = title
                    mPlayerGlue!!.subtitle = subtitle
                }
            })
        }
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            Log.d(LOG_TAG, "releasePlayer")
            mPlayer!!.release()
            mPlayer = null
            mPlayerAdapter = null
        }
    }

    private fun startPlayback(movie: Movie?, startPosition: Long) {
        val mediaItems: MutableList<MediaItem> = ArrayList()
        val firstMediaItem = MediaItem.Builder()
                .setUri(movie!!.videoUrl)
                .setMediaMetadata(
                        MediaMetadata.Builder()
                                .setArtworkUri(Uri.parse(movie.cardImageUrl))
                                .setTitle(movie.title)
                                .setSubtitle(movie.description)
                                .build()
                ).build()
        mediaItems.add(firstMediaItem)
        MovieList.getList()!!.forEach(Consumer { movieItem: Movie ->
            mediaItems.add(MediaItem.Builder()
                    .setUri(movieItem.videoUrl)
                    .setMediaMetadata(
                            MediaMetadata.Builder()
                                    .setArtworkUri(Uri.parse(movieItem.cardImageUrl))
                                    .setTitle(movieItem.title)
                                    .setSubtitle(movieItem.description)
                                    .build()
                    ).build()
            )
        })
        mPlayer!!.setMediaItems(mediaItems)
        mPlayer!!.prepare()
        mPlayerGlue!!.playWhenPrepared()
        mPlayerGlue!!.seekTo(startPosition)
        mMediaManager!!.mediaStatusModifier.clear()
    }

    private fun logAndDisplay(error: String) {
        Log.d(LOG_TAG, error)
        Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
    }

    internal inner class PlaylistActionListener : VideoPlayerGlue.OnActionClickedListener {
        override fun onPrevious() {
            mPlayer!!.previous()
        }

        override fun onNext() {
            mPlayer!!.next()
        }
    }

    private fun myFillMediaInfo(mediaInfoWriter: MediaInfoWriter) {
        val mediaInfo = mediaInfoWriter.mediaInfo
        Log.d(LOG_TAG, "***Type:" + mediaInfo.contentType)
        if (mediaInfo.contentUrl == null && mediaInfo.entity != null) {
            // Load By Entity
            val entity = mediaInfo.entity
            val movieMetadata = com.google.android.gms.cast.MediaMetadata(com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MOVIE)
            val movie = entity?.let { convertEntityToMovie(it) }
            if(movie!=null) {
                movie.title?.let {
                    movieMetadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE, it)
                }
                movie.description?.let {
                    movieMetadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE, it)
                }
                movie.studio?.let {
                    movieMetadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_STUDIO, it)
                }
                movie.cardImageUrl?.let {
                    movieMetadata.addImage(WebImage(Uri.parse(it)))
                }
                movie.backgroundImageUrl?.let {
                    movieMetadata.addImage(WebImage(Uri.parse(it)))
                }
                movie.videoUrl?.let {
                    mediaInfoWriter.setContentUrl(it).setMetadata(movieMetadata)
                }
            }
        }
    }

    internal inner class MyMediaLoadCommandCallback : MediaLoadCommandCallback() {
        override fun onLoad(senderId: String?, loadRequestData: MediaLoadRequestData): Task<MediaLoadRequestData> {
            Toast.makeText(activity, "onLoad()", Toast.LENGTH_SHORT).show()
            return if (loadRequestData == null) {
                // Throw MediaException to indicate load failure.
                Tasks.forException(MediaException(
                        MediaError.Builder()
                                .setDetailedErrorCode(DetailedErrorCode.LOAD_FAILED)
                                .setReason(MediaError.ERROR_REASON_INVALID_REQUEST)
                                .build()))
            } else Tasks.call {

                // Resolve the entity into your data structure and load media.
                myFillMediaInfo(MediaInfoWriter(loadRequestData!!.mediaInfo!!))
                startPlayback(convertLoadRequestToMovie(loadRequestData), 0)

                // Update media metadata and state (this clears all previous status
                // overrides).
                mMediaManager!!.setDataFromLoad(loadRequestData)
                mMediaManager!!.broadcastMediaStatus()
                loadRequestData
            }
        }
    }

    companion object {
        private const val LOG_TAG = "PlaybackVideoFragment"
        private const val UPDATE_DELAY = 16
        private fun convertEntityToMovie(entity: String): Movie {
            return MovieList.getList()!![0]
        }

        private fun convertLoadRequestToMovie(loadRequestData: MediaLoadRequestData?): Movie? {
            if (loadRequestData == null) {
                return null
            }
            val mediaInfo = loadRequestData.mediaInfo ?: return null
            var videoUrl = mediaInfo.contentId
            if (mediaInfo.contentUrl != null) {
                videoUrl = mediaInfo.contentUrl!!
            }
            val metadata = mediaInfo.metadata
            val movie = Movie()
            movie.videoUrl = videoUrl
            if (metadata != null) {
                movie.title = metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE)
                movie.description = metadata.getString(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE)
                movie.cardImageUrl = metadata.images[0].url.toString()
            }
            return movie
        }
    }
}