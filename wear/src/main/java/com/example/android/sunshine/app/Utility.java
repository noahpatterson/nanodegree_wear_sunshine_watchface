/*
 * Copyright (C) 2015 The Android Open Source Project
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

public class Utility {


    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId, boolean isAmbient) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return isAmbient ? R.drawable.storm_outline : R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return isAmbient ? R.drawable.light_rain_outline : R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return isAmbient ? R.drawable.rain_outline : R.drawable.art_rain;
        } else if (weatherId == 511) {
            return isAmbient ? R.drawable.snow_outline : R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return isAmbient ? R.drawable.rain_outline : R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return isAmbient ? R.drawable.snow_outline : R.drawable.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return isAmbient ? R.drawable.fog_outline : R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return isAmbient ? R.drawable.storm_outline : R.drawable.art_storm;
        } else if (weatherId == 800) {
            return isAmbient ? R.drawable.clear_outline : R.drawable.art_clear;
        } else if (weatherId == 801) {
            return isAmbient ? R.drawable.light_clouds_outline : R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return isAmbient ? R.drawable.clouds_outline : R.drawable.art_clouds;
        }
        return -1;
    }
}