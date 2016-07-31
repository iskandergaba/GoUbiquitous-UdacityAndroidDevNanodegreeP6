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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineDigitalWatchFace extends CanvasWatchFaceService {
    private static final int MSG_UPDATE_TIME = 0;
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private boolean mRegisteredTimeZoneReceiver;
    private boolean mLowBitAmbient;
    private boolean mAmbient;

    private Bitmap weatherIcon;
    private String lowTemp = "--";
    private String highTemp = "--";
    private String descString = "--";
    private String date = Utility.getWearableFriendlyDayString(Calendar.getInstance().getTimeInMillis());
    private int weatherID = -1;

    private int[] colorArray = {R.color.backgroundRed, R.color.backgroundPink,
            R.color.backgroundPurple, R.color.BackgroundDeepPurple, R.color.backgroundIndigo,
            R.color.backgroundBlue, R.color.backgroundDefaultBlue, R.color.backgroundCyan,
            R.color.backgroundTeal, R.color.backgroundGreen, R.color.backgroundLightGreen,
            R.color.backgroundLime, R.color.backgroundYellow, R.color.backgroundAmber,
            R.color.backgroundOrange, R.color.backgroundDarkOrange};

    private float mCenterHeight;
    private float weatherY;

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineDigitalWatchFace.Engine> mWeakReference;


        public EngineHandler(SunshineDigitalWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineDigitalWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }


    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                lowTemp = intent.getStringExtra(getString(R.string.LOW_TEMP_KEY));
                highTemp = intent.getStringExtra(getString(R.string.HIGH_TEMP_KEY));
                descString = intent.getStringExtra(getString(R.string.DESC_KEY));
                weatherID = intent.getIntExtra(getString(R.string.WEATID_KEY), -1);
                invalidate();
            }
        };
        private float mYOffset;
        private float mXOffset;
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private Paint mDatePaint;
        private Paint mLowTempPaint;
        private Paint mHighTempPaint;
        private Paint mDescPaint;
        private Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        private int mTapCount;
        private float mCenterHeightForWeatherIcon;
        private boolean isRound;
        /**
         * Update rate in milliseconds for interactive mode. We update once a second since seconds are
         * displayed in interactive mode.
         */

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineDigitalWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.END)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(getColor(R.color.backgroundDefaultBlue));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(getColor(R.color.digital_text));

            mDatePaint = new Paint();
            mDatePaint = createTextPaint(getColor(R.color.digital_text));

            mLowTempPaint = new Paint();
            mLowTempPaint = createTextPaint(getColor(R.color.digital_text));

            mHighTempPaint = new Paint();
            mHighTempPaint = createHighTempTextPaint(getColor(R.color.digital_text));

            mDescPaint = new Paint();
            mDescPaint = createTextPaint(getColor(R.color.digital_text));

            mTime = new Time();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        private Paint createHighTempTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(BOLD_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineDigitalWatchFace.this.getResources();
            isRound = insets.isRound();
            mYOffset = resources.getDimension(isRound
                    ? R.dimen.digital_y_offset_round : R.dimen.digital_y_offset);
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
            mDatePaint.setTextSize(isRound ? 40 : 30);
            mHighTempPaint.setTextSize(isRound ? 35 : 30);
            mLowTempPaint.setTextSize(isRound ? 35 : 30);
            mDescPaint.setTextSize(isRound ? 30 : 25);


        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            date = Utility.getWearableFriendlyDayString(Calendar.getInstance().getTimeInMillis());
            /* the time changed */
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
            updateTimer();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(getColor(colorArray[(mTapCount + 6) % 16]));
                    break;
            }
            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mCenterHeight = height / 2f;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);
            canvas.drawText(date, mXOffset, mYOffset + 40, mDatePaint);

            Rect timeBounds = new Rect();
            mTextPaint.getTextBounds(text, 0, text.length() - 1, timeBounds);
            int textHeight = timeBounds.height();
            //draw the weatcher icon
            if (weatherID != -1) {
                weatherIcon = BitmapFactory.decodeResource(getResources(), Utility.getIconResourceForWeatherCondition(weatherID));
                mCenterHeightForWeatherIcon = mCenterHeight - weatherIcon.getHeight() / 2f;

                weatherY = isRound
                        ? mCenterHeightForWeatherIcon + 2 * textHeight / 3 : mCenterHeightForWeatherIcon + textHeight - 5;

                canvas.drawBitmap(weatherIcon, mXOffset, weatherY, null);
            }

            //set the Y of the temp
            if (weatherIcon != null) {
                float tempY =  weatherY + 2 * weatherIcon.getHeight() / 3 ;
                float descY = tempY + 2 * weatherIcon.getHeight() / 3;
                float highTempX = isRound ? (7 * weatherIcon.getWidth() / 4) : (weatherIcon.getWidth() + 25);
                float lowTempX = highTempX + mHighTempPaint.measureText(highTemp) + 5;
                //draw the high temp
                canvas.drawText(highTemp, highTempX, tempY, mHighTempPaint);

                //draw the low temp
                canvas.drawText(lowTemp, lowTempX, tempY, mLowTempPaint);

                //draw the weather description
                canvas.drawText(descString, mXOffset, descY, mDescPaint );

            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            if (visible) {
                registerReceiver();
                registerTextReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
                unregisterTextReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineDigitalWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineDigitalWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void registerTextReceiver() {

            IntentFilter filter = new IntentFilter();
            filter.addAction(getResources().getString(R.string.Text_RECEIVER_ACTION));
            SunshineDigitalWatchFace.this.registerReceiver(messageReceiver, filter);
        }

        private void unregisterTextReceiver() {

            if (weatherID != -1)
                SunshineDigitalWatchFace.this.unregisterReceiver(messageReceiver);
        }
    }
}