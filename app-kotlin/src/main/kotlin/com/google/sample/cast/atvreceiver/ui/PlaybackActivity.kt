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
package com.google.sample.cast.atvreceiver.ui

import android.R
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.content.Intent
import com.google.android.gms.cast.tv.CastReceiverContext

/**
 * Loads [PlaybackVideoFragment].
 */
class PlaybackActivity : FragmentActivity() {
    private var playbackVideoFragment: PlaybackVideoFragment? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playbackVideoFragment = PlaybackVideoFragment()
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.content, playbackVideoFragment!!)
                    .commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val mediaManager = CastReceiverContext.getInstance().mediaManager
        if (mediaManager.onNewIntent(intent)) {
            // If the SDK recognizes the intent, you should early return.
            return
        }

        // If the SDK doesn’t recognize the intent, you can handle the intent with
        // your own logic.
        playbackVideoFragment!!.processIntent(intent)
    }
}