package com.example.android.sunshine.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bruno on 03/05/2015.
 */
public class Parser {
    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */

    private Context context;

    public Parser(Context context) {
        this.context = context;
    }

    public String[] getRestaurantsDataFromJson(String RestaurantsJsonStr,double longitude, double latitude)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String SITRA_OBJETSTOURISTIQUES = "objetsTouristiques";
        final String SITRA_NOMRESTAU = "nom";
        final String SITRA_INFORMATIONS = "informations";
        final String SITRA_MOYENSCOMMUNICATION = "moyensCommunication";
        final String SITRA_MOYENSCOMMUNICATION_TYPE = "type";
        final String SITRA_MOYENSCOMMUNICATION_TYPE_LIBELLEFR = "libelleFr";
        final String SITRA_COORDONNEES = "coordonnees";
        final String SITRA_COORDONNEES_FR = "fr";
        final String SITRA_PRESENTATION = "presentation";
        final String SITRA_DESCR = "descriptifCourt";
        final String SITRA_DESCR_LIBELLE = "libelleFr";
        final String SITRA_LOCALISATION = "localisation";
        final String SITRA_LOCALISATION_ADDR = "adresse";
        final String SITRA_LOCALISATION_ADDR1 = "adresse1";
        final String SITRA_LOCALISATION_COMMUNE = "commune";

        final String SITRA_GEOLOCALISATION = "geolocalisation";
        final String SITRA_GEOJSON = "geoJson";


        JSONObject restaurantJson = new JSONObject(RestaurantsJsonStr);
        JSONArray restaurantsArray = restaurantJson.getJSONArray(SITRA_OBJETSTOURISTIQUES);



        String[] resultStrs = new String[restaurantsArray.length()];
        String[] resultStrsDetail = new String[restaurantsArray.length()];

        for(int i = 0; i < restaurantsArray.length(); i++) {


            // Get the JSON object representing the day
            JSONObject restaurant = restaurantsArray.getJSONObject(i);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject restaurantObject = restaurant.getJSONObject(SITRA_NOMRESTAU);
            // nom du restaurant
            String nomRestau = restaurantObject.getString("libelleFr");

            JSONObject informationsRestauObject = restaurant.getJSONObject(SITRA_INFORMATIONS);
            String[] arrayMoyensComs = null;
            if(informationsRestauObject.length() != 0) {
                JSONArray moyensCommunications = informationsRestauObject.getJSONArray(SITRA_MOYENSCOMMUNICATION);

                // moyens de communication
                arrayMoyensComs = new String[moyensCommunications.length()];
                for (int j = 0; j < moyensCommunications.length(); j++) {
                    JSONObject typeCommunicationObject = moyensCommunications.getJSONObject(j).getJSONObject(SITRA_MOYENSCOMMUNICATION_TYPE);
                    JSONObject coordonneesObject = moyensCommunications.getJSONObject(j).getJSONObject(SITRA_COORDONNEES);

                    String typeCommunication = typeCommunicationObject.getString(SITRA_MOYENSCOMMUNICATION_TYPE_LIBELLEFR);
                    String coordonnees = coordonneesObject.getString(SITRA_COORDONNEES_FR);
                    arrayMoyensComs[j] = "\n"+typeCommunication + " - " + coordonnees;
                }
            }
            String descriptifCourt  = "";
            // presentation du restau avec un descriptif court
            JSONObject presentationRestauObject = restaurant.getJSONObject(SITRA_PRESENTATION);
            if(presentationRestauObject.length() != 0){
                JSONObject descriptifRestauObject = presentationRestauObject.getJSONObject(SITRA_DESCR);
                descriptifCourt = descriptifRestauObject.getString(SITRA_DESCR_LIBELLE);
            }
            // localisation
            JSONObject localisationRestauObject = restaurant.getJSONObject(SITRA_LOCALISATION);
            // adresse du restaurant
            JSONObject adresseRestauObject = localisationRestauObject.getJSONObject(SITRA_LOCALISATION_ADDR);
            String adresse1 = adresseRestauObject.getString(SITRA_LOCALISATION_ADDR1);
            // recuperaiton du code postal et du nom de la commune
            JSONObject communeRestauObject = adresseRestauObject.getJSONObject(SITRA_LOCALISATION_COMMUNE);
            String commune = communeRestauObject.getString("nom");

            // geocalisation
            JSONObject geocalisationRestauObject = localisationRestauObject.getJSONObject(SITRA_GEOLOCALISATION);
            JSONObject geoJsonObject = geocalisationRestauObject.getJSONObject(SITRA_GEOJSON);
            JSONArray coordonneesArray = geoJsonObject.getJSONArray("coordinates");

            //distance du restaurant, calculee a partir des coordonnees
            int distanceRestau=0;

            double coordonneeX = coordonneesArray.getDouble(0);
            double coordonneeY = coordonneesArray.getDouble(1);
            double longitude0 = convertRad(coordonneeX);
            double latitude0 = convertRad(coordonneeY);
            double longitude1 = convertRad(longitude);
            double latitude1 = convertRad(latitude);
            distanceRestau = (int)(Math.round(6371030*(Math.PI/2 - Math.asin( Math.sin(latitude1) * Math.sin(latitude0) + Math.cos(longitude1 - longitude0) * Math.cos(latitude1) * Math.cos(latitude0))))*100)/100;

            resultStrs[i] =distanceRestau+ " m : " + nomRestau + " - ";

            resultStrsDetail[i] =distanceRestau+ " m : " + nomRestau + " \n ";
            if(arrayMoyensComs != null) {
                for (int z = 0; z < arrayMoyensComs.length; z++) {
                    resultStrsDetail[i] += arrayMoyensComs[z];
                }
            }
            resultStrsDetail[i] +=  "\n" +
                    "Adresse : " + adresse1 + ", " + "\n"+ commune +  "\n" + "Description : " + descriptifCourt+"\n\n"

            ;

            // Base de données
            WeatherDbHelper mDbHelper = new WeatherDbHelper(context);

            // Ajout dans la base de données
            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(WeatherContract.RestaurantsEntry.COLUMN_NOM, nomRestau);
            values.put(WeatherContract.RestaurantsEntry.COLUMN_ADRESSE, adresse1 + " " + commune);
            values.put(WeatherContract.RestaurantsEntry.COLUMN_TELEPHONE, "00000");
            values.put(WeatherContract.RestaurantsEntry.COLUMN_DESCRIPTION, descriptifCourt);

            // Insert the new row, returning the primary key value of the new row
            long newRowId;
            newRowId = db.insert(WeatherContract.RestaurantsEntry.TABLE_NAME,"null",values);
            Log.v("Donnée","Ajout de la donné " + newRowId);
        }
        Log.v("sitra_array", resultStrs[0]);


    return resultStrsDetail;

    }

    //Conversion des degres en radian pour le calcul de la distance
    private double convertRad(double input){
        return (Math.PI * input)/180;
    }
}
