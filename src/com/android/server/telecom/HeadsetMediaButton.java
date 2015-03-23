/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.session.MediaSession;
import android.view.KeyEvent;

/**
 * Static class to handle listening to the headset media buttons.
 */
public class HeadsetMediaButton extends CallsManagerListenerBase {

    // Types of media button presses
    static final int SHORT_PRESS = 1;
    static final int LONG_PRESS = 2;

    private static final AudioAttributes AUDIO_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build();

    private final MediaSession.Callback mSessionCallback = new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent intent) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            Log.v(this, "SessionCallback.onMediaButton()...  event = %s.", event);
            if ((event != null) && (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK)) {
                Log.v(this, "SessionCallback: HEADSETHOOK");
                boolean consumed = handleHeadsetHook(event);
                Log.v(this, "==> handleHeadsetHook(): consumed = %b.", consumed);
                return consumed;
            }
            return true;
        }
    };

    private final CallsManager mCallsManager;

    private final MediaSession mSession;

    public HeadsetMediaButton(Context context, CallsManager callsManager) {
        mCallsManager = callsManager;

        // Create a MediaSession but don't enable it yet. This is a
        // replacement for MediaButtonReceiver
        mSession = new MediaSession(context, HeadsetMediaButton.class.getSimpleName());
        mSession.setCallback(mSessionCallback);
        mSession.setFlags(MediaSession.FLAG_EXCLUSIVE_GLOBAL_PRIORITY
                | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        mSession.setPlaybackToLocal(AUDIO_ATTRIBUTES);
    }

    /**
     * Handles the wired headset button while in-call.
     *
     * @return true if we consumed the event.
     */
    private boolean handleHeadsetHook(KeyEvent event) {
        Log.d(this, "handleHeadsetHook()...%s %s", event.getAction(), event.getRepeatCount());

        if (event.isLongPress()) {
            return mCallsManager.onMediaButton(LONG_PRESS);
        } else if (event.getAction() == KeyEvent.ACTION_UP && event.getRepeatCount() == 0) {
            return mCallsManager.onMediaButton(SHORT_PRESS);
        }

        return true;
    }

    /** ${inheritDoc} */
    @Override
    public void onCallAdded(Call call) {
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
    }

    /** ${inheritDoc} */
    @Override
    public void onCallRemoved(Call call) {
        if (!mCallsManager.hasAnyCalls()) {
            if (mSession.isActive()) {
                mSession.setActive(false);
            }
        }
    }
}
