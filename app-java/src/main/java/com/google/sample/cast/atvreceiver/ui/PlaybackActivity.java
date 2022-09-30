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
package com.google.sample.cast.atvreceiver.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.cast.tv.CastReceiverContext;
import com.google.android.gms.cast.tv.media.MediaManager;
/**
 * Loads {@link PlaybackVideoFragment}.
 */
public class PlaybackActivity extends FragmentActivity {

    private PlaybackVideoFragment playbackVideoFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playbackVideoFragment = new PlaybackVideoFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, playbackVideoFragment)
                    .commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        MediaManager mediaManager = CastReceiverContext.getInstance().getMediaManager();
        if (mediaManager.onNewIntent(intent)) {
            // If the SDK recognizes the intent, you should early return.
            return;
        }

        // If the SDK doesn’t recognize the intent, you can handle the intent with
        // your own logic.
        playbackVideoFragment.processIntent(intent);
    }
}