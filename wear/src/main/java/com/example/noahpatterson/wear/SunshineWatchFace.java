/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.noahpatterson.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private Rect r = new Rect();

    private void drawFirstCenter(Canvas canvas, Paint paint, String text) {
        int cHeight = canvas.getClipBounds().height();
        int cWidth = canvas.getClipBounds().width();

        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), r);

        float x = cWidth / 2f - r.width() / 2f - r.left;
//        float y = cHeight / 2f + r.height() / 2f - r.bottom;
//        float y  = (cHeight /2f)  - ((r.height()/totalOffset - 0.5f) * totalOffset) ;
        float y = cHeight/2f - r.height()/2f;
        canvas.drawText(text, x, y, paint);
    }
    private void drawSecondCenter(Canvas canvas, Paint paint, String text) {
        int cHeight = canvas.getClipBounds().height();
        int cWidth = canvas.getClipBounds().width();

        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), r);

        float x = cWidth / 2f - r.width() / 2f - r.left;
//        float y = cHeight / 2f + r.height() / 2f - r.bottom;
//        float y  = (cHeight /2f)  - ((r.height()/totalOffset - 0.5f) * totalOffset) ;
        float y = cHeight/2f + r.height()/2f;
        canvas.drawText(text, x, y, paint);
    }
//    private void drawCenter(Canvas canvas, Paint paint, String text) {
//            int cHeight = canvas.getClipBounds().height();
//            int cWidth = canvas.getClipBounds().width();
//
//            paint.setTextAlign(Paint.Align.LEFT);
//            paint.getTextBounds(text, 0, text.length(), r);
//
//            float x = cWidth / 2f - r.width() / 2f - r.left;
//            float y = cHeight / 2f + r.height() / 2f - r.bottom;
//            canvas.drawText(text, x, y, paint);
//        }

    private float getTextHeight(Paint paint, String text) {
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect.height();
    }

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
//    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
//    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

//    private static class EngineHandler extends Handler {
//        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;
//
//        public EngineHandler(SunshineWatchFace.Engine reference) {
//            mWeakReference = new WeakReference<>(reference);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            SunshineWatchFace.Engine engine = mWeakReference.get();
//            if (engine != null) {
//                switch (msg.what) {
//                    case MSG_UPDATE_TIME:
//                        engine.handleUpdateTimeMessage();
//                        break;
//                }
//            }
//        }
//    }

    private class Engine extends CanvasWatchFaceService.Engine {
//        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;

        //add date
        Paint mDatePaint;
        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;

        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

        float mXOffset;
        float mYOffset;
        float mLineHeight;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mLineHeight = resources.getDimension(R.dimen.line_height);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.primary));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mDatePaint = new Paint();
            mDatePaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();
            mCalendar = Calendar.getInstance();
            mDate = new Date();
        }

        @Override
        public void onDestroy() {
//            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
//            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
//            mXOffset = resources.getDimension(isRound
//                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateTextSize = resources.getDimension(isRound ? R.dimen.date_square_text_size : R.dimen.date_circle_text_size);
            mTextPaint.setTextSize(textSize);
            mDatePaint.setTextSize(dateTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
//            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
//        @Override
//        public void onTapCommand(int tapType, int x, int y, long eventTime) {
//            Resources resources = SunshineWatchFace.this.getResources();
//            switch (tapType) {
//                case TAP_TYPE_TOUCH:
//                    // The user has started touching the screen.
//                    break;
//                case TAP_TYPE_TOUCH_CANCEL:
//                    // The user has started a different gesture or otherwise cancelled the tap.
//                    break;
//                case TAP_TYPE_TAP:
//                    // The user has completed the tap gesture.
//                    mTapCount++;
//                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
//                            R.color.background : R.color.background2));
//                    break;
//            }
//            invalidate();
//        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
//            String text = mAmbient
//                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
//                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
            String text = String.format("%d:%02d", mTime.hour, mTime.minute);
//            canvas.drawText(text, center, mYOffset, mTextPaint);

//            drawCenter(canvas,mTextPaint, text,-textOffset);

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            mDayOfWeekFormat = new SimpleDateFormat("EEE MMM dd");
            mDayOfWeekFormat.setCalendar(mCalendar);

//            canvas.drawText(mDayOfWeekFormat.format(mDate),center, mYOffset + mLineHeight, mDatePaint);
            String dateFormatted = mDayOfWeekFormat.format(mDate);
            float dateOffset = getTextHeight(mDatePaint, dateFormatted);
            float textOffset = getTextHeight(mTextPaint, text);
            float totalHeight = textOffset + dateOffset;

            drawFirstCenter(canvas, mTextPaint,text);
            drawSecondCenter(canvas,mDatePaint,dateFormatted);

//            drawCenter(canvas,mTextPaint,text,totalHeight);
//            drawCenter(canvas,mDatePaint,dateFormatted, textOffset + mLineHeight);
        }

        /**
         * Starts the mUpdateTimeHandler timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
//        private void updateTimer() {
//            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
//            if (shouldTimerBeRunning()) {
//                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
//            }
//        }

        /**
         * Returns whether the mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
//        private boolean shouldTimerBeRunning() {
//            return isVisible() && !isInAmbientMode();
//        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
//        private void handleUpdateTimeMessage() {
//            invalidate();
//            if (shouldTimerBeRunning()) {
//                long timeMs = System.currentTimeMillis();
//                long delayMs = INTERACTIVE_UPDATE_RATE_MS
//                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
//                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
//            }
//        }
    }
}
