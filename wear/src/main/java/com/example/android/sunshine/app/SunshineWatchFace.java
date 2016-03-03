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

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;


import com.example.android.sunshine.app.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final String LOG_TAG = "WatchFaceService";


    private Rect r = new Rect();

    private void drawCenter(Canvas canvas, Paint paint, String text, float yOffset, float xOffset) {
            int cHeight = canvas.getClipBounds().height();
            int cWidth = canvas.getClipBounds().width();

            paint.setTextAlign(Paint.Align.LEFT);
            paint.getTextBounds(text, 0, text.length(), r);

            float x = cWidth / 2f - r.width() / 2f - r.left;
            float y = cHeight / 2f + r.height() / 2f - r.bottom;
            canvas.drawText(text, x + xOffset, y + yOffset, paint);
        }

    private void drawWeatherIconCenter(Canvas canvas, Bitmap bitmap, float yOffset, float xOffset){
        float centerX = canvas.getClipBounds().width()/2f;
        float centerY = canvas.getClipBounds().height()/2f;
        canvas.drawBitmap(bitmap, centerX + xOffset, centerY + yOffset, null);

    }

    private float getTextHeight(Paint paint, String text) {
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect.height();
    }


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        boolean mRegisteredTimeZoneReceiver = false;


        GoogleApiClient mGoogleApiClient;

        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mHighTempPaint;
        String mHighTemp;
        String mLowTemp;
        int mWeatherId;
        Bitmap weatherIconBitmap;

        //add date
        Paint mDatePaint;
        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;

        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        float mYOffset;
        float mLineHeight;

        boolean weatherChanged = false;

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

            mHighTempPaint = new Paint();
            mHighTempPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();
            mCalendar = Calendar.getInstance();
            mDate = new Date();

            //default for missing weather temps
            mHighTemp = "none";
            mLowTemp = "none";

            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        }


        @Override
        public void onDestroy() {
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
                mGoogleApiClient.connect();
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();

            } else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
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
            mHighTempPaint.setTextSize(dateTextSize);
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

            mTime.setToNow();
            String text = String.format("%d:%02d", mTime.hour, mTime.minute);
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            mDayOfWeekFormat = new SimpleDateFormat("EEE MMM dd");
            mDayOfWeekFormat.setCalendar(mCalendar);

            String dateFormatted = mDayOfWeekFormat.format(mDate);
            float dateOffset = getTextHeight(mDatePaint, dateFormatted);
            float textOffset = getTextHeight(mTextPaint, text);
            float totalHeight = textOffset + dateOffset;

            drawCenter(canvas, mTextPaint, text, -20f, 0);
            drawCenter(canvas, mDatePaint, dateFormatted, mLineHeight -15f, 0);


            String tempStr = mHighTemp + " " + mLowTemp;
            //load icon
            if (mWeatherId > 0) {
                int iconResource = Utility.getArtResourceForWeatherCondition(mWeatherId);
                Log.d(LOG_TAG, "iconResource: " + String.valueOf(iconResource));
                Drawable weatherIconDrawable = getResources().getDrawable(iconResource, null);
                Bitmap weatherIcon = ((BitmapDrawable) weatherIconDrawable).getBitmap();
                weatherIconBitmap = Bitmap.createScaledBitmap(weatherIcon, 50, 50, false);
                drawWeatherIconCenter(canvas, weatherIconBitmap, (mLineHeight * 3) - 30f, -25f);
            }
            drawCenter(canvas, mHighTempPaint, tempStr, (mLineHeight * 2) - 20f, 0);
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d("watchfaceService", "onDataChanged(): " + dataEvents);

            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {

                    DataItem dataItem = event.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap dataMap = dataMapItem.getDataMap();

                    mHighTemp = dataMap.getString("high");
                    mLowTemp = dataMap.getString("low");

                    mWeatherId = dataMap.getInt("weatherId");

                    weatherChanged = true;
                    invalidate();
                }
            }

        }

        @Override
        public void onConnected(Bundle bundle) {
            Log.d(LOG_TAG, "onConnected(): Successfully connected to Google API client");
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(LOG_TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e(LOG_TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
        }
    }
}

