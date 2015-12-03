package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class myFetchService extends IntentService
{
    public static final String LOG_TAG = "myFetchService";
    public static final String ACTION_DATA_UPDATED = "barqsoft.footballscores.ACTION_DATA_UPDATED";

    public myFetchService()
    {
        super("myFetchService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        getData(getString(R.string.fixture_n));
        getData(getString(R.string.fixture_p));
    }

    private void getData (String timeFrame)
    {
        //Creating fetch URL
        final String BASE_URL = getString(R.string.base_url); //Base URL
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days
        //final String QUERY_MATCH_DAY = "matchday";

        Uri fetch_build = Uri.parse(BASE_URL).buildUpon().
                appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();
        //Log.v(LOG_TAG, "The url we are looking at is: "+fetch_build.toString()); //log spam
        HttpURLConnection m_connection = null;
        BufferedReader reader = null;
        String JSON_data = null;
        //Opening Connection
        try {
            URL fetch = new URL(fetch_build.toString());
            m_connection = (HttpURLConnection) fetch.openConnection();
            m_connection.setRequestMethod("GET");
            m_connection.addRequestProperty("X-Auth-Token",getString(R.string.api_key));
            m_connection.connect();

            // Read the input stream into a String
            InputStream inputStream = m_connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
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
                return;
            }
            JSON_data = buffer.toString();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG,"Exception here" + e.getMessage());
        }
        finally {
            if(m_connection != null)
            {
                m_connection.disconnect();
            }
            if (reader != null)
            {
                try {
                    reader.close();
                }
                catch (IOException e)
                {
                    Log.e(LOG_TAG,"Error Closing Stream");
                }
            }
        }
        try {
            if (JSON_data != null) {
                //This bit is to check if the data contains any matches. If not, we call processJson on the dummy data
                JSONArray matches = new JSONObject(JSON_data).getJSONArray("fixtures");
                if (matches.length()== 0) {
                    //if there is no data, call the function on dummy data
                    //this is expected behavior during the off season.
                    processJSONdata(getString(R.string.dummy_data), getApplicationContext(), false);
                    return;
                }


                processJSONdata(JSON_data, getApplicationContext(), true);
            } else {
                //Could not Connect
                Log.d(LOG_TAG, "Could not connect to server.");
            }

            Intent broadcastIntent = new Intent(ACTION_DATA_UPDATED).setPackage(getPackageName());
            this.sendBroadcast(broadcastIntent);
        }
        catch(Exception e)
        {
            Log.e(LOG_TAG,e.getMessage());
        }
    }
    private void processJSONdata (String JSONdata,Context mContext, boolean isReal)
    {

        String[] leagueCodes = getResources().getStringArray(R.array.league_codes);
        final String SEASON_LINK = getString(R.string.season_link);
        final String MATCH_LINK = getString(R.string.match_link);
        final String FIXTURES = getString(R.string.fixture_tag);
        final String LINKS = getString(R.string.links_tag);
        final String SOCCER_SEASON = getString(R.string.soccerseason_tag);
        final String SELF = getString(R.string.self_tag);
        final String MATCH_DATE = getString(R.string.date_tag);
        final String HOME_TEAM = getString(R.string.home_team_tag);
        final String AWAY_TEAM = getString(R.string.away_team_tag);
        final String RESULT = getString(R.string.result_tag);
        final String HOME_GOALS = getString(R.string.goals_home_team_tag);
        final String AWAY_GOALS = getString(R.string.goals_away_team_tag);
        final String MATCH_DAY = getString(R.string.match_day_tag);

        //Match data
        String league = null;
        String mDate = null;
        String mTime = null;
        String home = null;
        String away = null;
        String homeGoals = null;
        String awayGoals = null;
        String match_id = null;
        String matchDay = null;


        try {
            JSONArray matches = new JSONObject(JSONdata).getJSONArray(FIXTURES);


            //ContentValues to be inserted
            Vector<ContentValues> values = new Vector <ContentValues> (matches.length());
            for(int i = 0;i < matches.length();i++)
            {

                JSONObject match_data = matches.getJSONObject(i);
                league = match_data.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).
                        getString("href");
                league = league.replace(SEASON_LINK,"");
                //This if statement controls which leagues we're interested in the data from.
                //add leagues here in order to have them be added to the DB.
                // If you are finding no data in the app, check that this contains all the leagues.
                // If it doesn't, that can cause an empty DB, bypassing the dummy data routine.

                for(String leaguecode:leagueCodes) {
                    if (league.equals(leaguecode)) {
                        match_id = match_data.getJSONObject(LINKS).getJSONObject(SELF).
                                getString("href");
                        match_id = match_id.replace(MATCH_LINK, "");
                        if (!isReal) {
                            //This if statement changes the match ID of the dummy data so that it all goes into the database
                            match_id = match_id + Integer.toString(i);
                        }

                        String jsonDate = match_data.getString(MATCH_DATE);
                        try {

                            // Using Joda library to parse the date from JSON
                            DateTime dt = Utilies.getDateTime(jsonDate,getString(R.string.date_patter_1),this);
                            mDate = dt.getYear() + "-" + dt.getMonthOfYear() + "-" + dt.getDayOfMonth(); // yy -mm-dd
                            mTime = dt.getHourOfDay()+":" + dt.getMinuteOfHour(); // hh:mm

                            if (!isReal) {
                                //This if statement changes the dummy data's date to match our current date range.
                                LocalDateTime ldt = LocalDateTime.now();
                                mDate = ldt.getYear() + "-" + ldt.getMonthOfYear() + "-" + ldt.getDayOfMonth();
                            }
                        } catch (Exception e) {
                            Log.d(LOG_TAG, "error here!");
                            Log.e(LOG_TAG, e.getMessage());
                        }
                        home = match_data.getString(HOME_TEAM);
                        away = match_data.getString(AWAY_TEAM);
                        homeGoals = match_data.getJSONObject(RESULT).getString(HOME_GOALS);
                        awayGoals = match_data.getJSONObject(RESULT).getString(AWAY_GOALS);
                        matchDay = match_data.getString(MATCH_DAY);
                        ContentValues match_values = new ContentValues();
                        match_values.put(DatabaseContract.scores_table.MATCH_ID, match_id);
                        match_values.put(DatabaseContract.scores_table.DATE_COL, mDate);
                        match_values.put(DatabaseContract.scores_table.TIME_COL, mTime);
                        match_values.put(DatabaseContract.scores_table.HOME_COL, home);
                        match_values.put(DatabaseContract.scores_table.AWAY_COL, away);
                        match_values.put(DatabaseContract.scores_table.HOME_GOALS_COL, homeGoals);
                        match_values.put(DatabaseContract.scores_table.AWAY_GOALS_COL, awayGoals);
                        match_values.put(DatabaseContract.scores_table.LEAGUE_COL, league);
                        match_values.put(DatabaseContract.scores_table.MATCH_DAY, matchDay);
                        //log spam

                        //Log.v(LOG_TAG,match_id);
                        //Log.v(LOG_TAG,mDate);
                        //Log.v(LOG_TAG,mTime);
                        //Log.v(LOG_TAG,Home);
                        //Log.v(LOG_TAG,Away);
                        //Log.v(LOG_TAG,Home_goals);
                        //Log.v(LOG_TAG,Away_goals);

                        values.add(match_values);
                    }
                }
            }
            int inserted_data = 0;
            ContentValues[] insert_data = new ContentValues[values.size()];
            values.toArray(insert_data);
            inserted_data = mContext.getContentResolver().bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI,insert_data);

            //Log.v(LOG_TAG,"Succesfully Inserted : " + String.valueOf(inserted_data));
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG,e.getMessage());
        }

    }
}

