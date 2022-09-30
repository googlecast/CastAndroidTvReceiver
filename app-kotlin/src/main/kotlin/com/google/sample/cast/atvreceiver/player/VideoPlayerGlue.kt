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
package com.google.sample.cast.atvreceiver.player

import android.content.Context
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.PlaybackControlsRow.SkipPreviousAction
import androidx.leanback.widget.PlaybackControlsRow.SkipNextAction
import androidx.leanback.widget.PlaybackControlsRow.FastForwardAction
import androidx.leanback.widget.PlaybackControlsRow.RewindAction
import androidx.leanback.widget.ArrayObjectAdapter
import java.util.concurrent.TimeUnit

class VideoPlayerGlue(
        context: Context?,
        playerAdapter: LeanbackPlayerAdapter?,
        private val mActionListener: OnActionClickedListener) : PlaybackTransportControlGlue<LeanbackPlayerAdapter?>(context, playerAdapter) {
    /** Listens for when skip to next and previous actions have been dispatched.  */
    interface OnActionClickedListener {
        /** Skip to the previous item in the queue.  */
        fun onPrevious()

        /** Skip to the next item in the queue.  */
        fun onNext()
    }

    private val mSkipPreviousAction: SkipPreviousAction
    private val mSkipNextAction: SkipNextAction
    private val mFastForwardAction: FastForwardAction
    private val mRewindAction: RewindAction
    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        super.onCreatePrimaryActions(primaryActionsAdapter)
        primaryActionsAdapter.add(mSkipPreviousAction)
        primaryActionsAdapter.add(mRewindAction)
        primaryActionsAdapter.add(mFastForwardAction)
        primaryActionsAdapter.add(mSkipNextAction)
    }

    override fun onActionClicked(action: Action) {
        if (action === mRewindAction) {
            rewind()
        } else if (action === mFastForwardAction) {
            fastForward()
        } else {
            super.onActionClicked(action)
        }
    }

    override fun next() {
        mActionListener.onNext()
    }

    override fun previous() {
        mActionListener.onPrevious()
    }

    /** Skips backwards 10 seconds.  */
    fun rewind() {
        var newPosition = currentPosition - TEN_SECONDS
        newPosition = if (newPosition < 0) 0 else newPosition
        playerAdapter!!.seekTo(newPosition)
    }

    /** Skips forward 10 seconds.  */
    fun fastForward() {
        if (duration > -1) {
            var newPosition = currentPosition + TEN_SECONDS
            newPosition = if (newPosition > duration) duration else newPosition
            playerAdapter!!.seekTo(newPosition)
        }
    }

    companion object {
        private val TEN_SECONDS = TimeUnit.SECONDS.toMillis(10)
    }

    init {
        mSkipPreviousAction = SkipPreviousAction(context)
        mSkipNextAction = SkipNextAction(context)
        mFastForwardAction = FastForwardAction(context)
        mRewindAction = RewindAction(context)
    }
}