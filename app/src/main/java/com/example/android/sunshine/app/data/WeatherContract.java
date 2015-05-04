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
package com.example.android.sunshine.app.data;

import android.provider.BaseColumns;

/**
 * Defines table and column names for the weather database.
 */
public class WeatherContract {

    /* Inner class that defines the table contents of the location table */
    public static final class RestaurantsEntry implements BaseColumns {

        // Table name
        public static final String TABLE_NAME = "restaurants";

        // Human readable location string, provided by the API.  Because for styling,
        // "Mountain View" is more recognizable than 94043.
        public static final String COLUMN_NOM = "nom";

        // In order to uniquely pinpoint the location on the map when we launch the
        // map intent, we store the latitude and longitude as returned by openweathermap.
        public static final String COLUMN_TELEPHONE = "telephone";
        public static final String COLUMN_ADRESSE = "adresse";
        public static final String COLUMN_DESCRIPTION = "description";
    }
}
