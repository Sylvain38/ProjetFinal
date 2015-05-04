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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class RestoFragment extends Fragment {

    private ArrayAdapter<String> mRestoAdapter;


    public RestoFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateResto();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mRestoAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_resto, // The name of the layout ID.
                        R.id.list_item_resto_textview, // The ID of the textview to populate.
                        new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mRestoAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String resto = mRestoAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, resto);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void updateResto() {
        FetchRestaurantTask restoTask = new FetchRestaurantTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        restoTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateResto();
    }

    public class FetchRestaurantTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchRestaurantTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String restoJsonStr = null;

            String format = "json";
            String units = "metric";
            String apiKey = "\"m4VH2Zee\"";
            String typeR  = "\"type:RESTAURATION\"";
            String projetId = "\"1143\"";
            String r =  prefs.getString(getString(R.string.pref_distance_key),
                    getString(R.string.pref_distance_pieton));
            String rayon="2000";
            if (r.equals("voiture"))rayon="30000";
            String longitude = String.valueOf(((MainActivity)getActivity()).longitude);
            String latitude = String.valueOf(((MainActivity)getActivity()).latitude);

            //http://api.sitra-tourisme.com/api/v002/recherche/list-objets-touristiques?query={"projetId":"1143","apiKey":"m4VH2Zee","criteresQuery":"type:RESTAURATION”}

            //http://api.sitra-tourisme.com/api/v002/recherche/list-objets-touristiques?query={%22projetId%22%3A%221143%22%2C%22apiKey%22%3A%22m4VH2Zee%22%2C%22criteresQuery%22%3A%22type%3ARESTAURATION%22}
            try {
                // Construct the URL for the Sitra2 query
                final String FORECAST_BASE_URL =
                        "http://api.sitra-tourisme.com/api/v002/recherche/list-objets-touristiques?query={";

                final String projetId_PARAM = "projetId";
                final String apiKey_PARAM = "apiKey";
                final String criteresQuery_PARAM = "criteresQuery";
                // en metres
                final String rayon_PARAM = "radius";
                final String typeCoordonnees_PARAM ="Point";
                String baseId = FORECAST_BASE_URL.concat("\""+projetId_PARAM+"\":".concat(projetId).concat(","));
                String baseKeyApi = baseId.concat("\""+apiKey_PARAM+"\":".concat(apiKey).concat(","));
                String keyApiCriteresQuery = baseKeyApi.concat("\""+criteresQuery_PARAM+"\":".concat(typeR).concat(","));
                String rayonLatitudeLongitude = keyApiCriteresQuery.concat("\"order\":\"DISTANCE\",\""+rayon_PARAM+"\":\""+rayon+"\",\"center\":{\"type\":\""+typeCoordonnees_PARAM+"\",\"coordinates\":["+longitude+","+latitude+"]".concat("}}"));

                URL url = new URL(rayonLatitudeLongitude);
                Log.v("sitra",url.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                restoJsonStr = buffer.toString();
                //Log.v("sitra",restoJsonStr.toString());
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                Parser p = new Parser(getActivity());
                return p.getRestaurantsDataFromJson(restoJsonStr,((MainActivity) getActivity()).longitude, ((MainActivity) getActivity()).latitude);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mRestoAdapter.clear();
                for(String lesRestoStr : result) {
                    mRestoAdapter.add(lesRestoStr);
                }
                // New data is back from the server.  Hooray!
            }
        }

    }
}
