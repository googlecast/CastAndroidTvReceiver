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
package com.google.sample.cast.atvreceiver;

import android.app.Application;

import android.widget.Toast;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.cast.tv.CastReceiverContext;
import com.google.android.gms.cast.tv.SenderDisconnectedEventInfo;
import com.google.android.gms.cast.tv.SenderInfo;

public class CastDemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CastReceiverContext.initInstance(this);
        CastReceiverContext.getInstance().registerEventCallback(new EventCallback());
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());
    }

    private class EventCallback extends CastReceiverContext.EventCallback {
        @Override
        public void onSenderConnected(SenderInfo senderInfo) {
            Toast.makeText(
                    CastDemoApplication.this,
                    "Sender connected " + senderInfo.getSenderId(),
                    Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onSenderDisconnected(SenderDisconnectedEventInfo eventInfo) {
            Toast.makeText(
                    CastDemoApplication.this,
                    "Sender disconnected " + eventInfo.getSenderInfo().getSenderId(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }
}
