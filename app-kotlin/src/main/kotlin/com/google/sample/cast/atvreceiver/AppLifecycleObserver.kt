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
package com.google.sample.cast.atvreceiver

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.cast.tv.CastReceiverContext

class AppLifecycleObserver : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {}
    override fun onPause(owner: LifecycleOwner) {}
    override fun onDestroy(owner: LifecycleOwner) {}
    override fun onCreate(owner: LifecycleOwner) {}
    override fun onStart(owner: LifecycleOwner) {
        CastReceiverContext.getInstance().start()
    }

    override fun onStop(owner: LifecycleOwner) {
        CastReceiverContext.getInstance().stop()
    }
}