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
package com.google.sample.cast.atvreceiver.player;

import android.content.Context;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import java.util.concurrent.TimeUnit;

public class VideoPlayerGlue extends PlaybackTransportControlGlue<LeanbackPlayerAdapter> {

  private static final long TEN_SECONDS = TimeUnit.SECONDS.toMillis(10);

  /** Listens for when skip to next and previous actions have been dispatched. */
  public interface OnActionClickedListener {

    /** Skip to the previous item in the queue. */
    void onPrevious();

    /** Skip to the next item in the queue. */
    void onNext();
  }

  private final OnActionClickedListener mActionListener;

  private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
  private PlaybackControlsRow.SkipNextAction mSkipNextAction;
  private PlaybackControlsRow.FastForwardAction mFastForwardAction;
  private PlaybackControlsRow.RewindAction mRewindAction;

  public VideoPlayerGlue(
      Context context,
      LeanbackPlayerAdapter playerAdapter,
      OnActionClickedListener actionListener) {
    super(context, playerAdapter);

    mActionListener = actionListener;

    mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
    mSkipNextAction = new PlaybackControlsRow.SkipNextAction(context);
    mFastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
    mRewindAction = new PlaybackControlsRow.RewindAction(context);
  }

  @Override
  protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
    super.onCreatePrimaryActions(primaryActionsAdapter);
    primaryActionsAdapter.add(mSkipPreviousAction);
    primaryActionsAdapter.add(mRewindAction);
    primaryActionsAdapter.add(mFastForwardAction);
    primaryActionsAdapter.add(mSkipNextAction);
  }

  @Override
  public void onActionClicked(Action action) {
    if (action == mRewindAction) {
      rewind();
    } else if (action == mFastForwardAction) {
      fastForward();
    }else {
      super.onActionClicked(action);
    }
  }

  @Override
  public void next() {
    mActionListener.onNext();
  }

  @Override
  public void previous() {
    mActionListener.onPrevious();
  }

  /** Skips backwards 10 seconds. */
  public void rewind() {
    long newPosition = getCurrentPosition() - TEN_SECONDS;
    newPosition = (newPosition < 0) ? 0 : newPosition;
    getPlayerAdapter().seekTo(newPosition);
  }

  /** Skips forward 10 seconds. */
  public void fastForward() {
    if (getDuration() > -1) {
      long newPosition = getCurrentPosition() + TEN_SECONDS;
      newPosition = (newPosition > getDuration()) ? getDuration() : newPosition;
      getPlayerAdapter().seekTo(newPosition);
    }
  }
}
