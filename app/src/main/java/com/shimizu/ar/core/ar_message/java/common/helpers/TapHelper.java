/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shimizu.ar.core.ar_message.java.common.helpers;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Helper to detect taps using Android GestureDetector, and pass the taps between UI thread and
 * render thread.
 */
public final class TapHelper implements OnTouchListener {
  private final GestureDetector gestureDetector;
  private final BlockingQueue<MotionEvent> queuedLongPress = new ArrayBlockingQueue<>(16);
  private final BlockingQueue<MotionEvent> queuedSwipes = new ArrayBlockingQueue<>(16);
  private final BlockingQueue<Map> queuedTwoFingerSwipes = new ArrayBlockingQueue<>(16);
  private boolean isTwoFingerEvent = false;

    /**
   * Creates the tap helper.
   *
   * @param context the application's context.
   */
  public TapHelper(Context context) {
    gestureDetector =
        new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public void onLongPress(MotionEvent e) {
                // Queue tap if there is space. Tap is lost if queue is full.
                  queuedLongPress.offer(e);
              }

              @Override
              public boolean onScroll( MotionEvent e1,  MotionEvent e2, float distanceX, float distanceY) {
                  int pointerCount = e2.getPointerCount();
                  if (pointerCount == 2) {
                      Map<String, Float> map = new HashMap<>();
                      map.put("x",distanceX);
                      map.put("y",distanceY);
                      queuedTwoFingerSwipes.offer(map);
                      isTwoFingerEvent = true;
                  } else if (pointerCount == 1 && !isTwoFingerEvent)  {
                      queuedSwipes.offer(e2);
                  }
                  return true;
              }

              @Override
              public boolean onDown(MotionEvent e) {
                  isTwoFingerEvent = false;
                  return true;
              }
            });
  }

  /**
   * Polls for a tap.
   *
   * @return if a tap was queued, a MotionEvent for the tap. Otherwise null if no taps are queued.
   */
  public MotionEvent poll() {
    return queuedLongPress.poll();
  }

  public MotionEvent swipePoll() {
      return queuedSwipes.poll();
  }

  public Map twoFingerSwipePoll() {
      return queuedTwoFingerSwipes.poll();
  }


    @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    return gestureDetector.onTouchEvent(motionEvent);
  }
}
