package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by Asus1 on 11/15/2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class widgetRemoteService extends RemoteViewsService {
    private Cursor[] data = new Cursor[2*FIXTURE_DATE+1];
    private MergeCursor mergeCursor;


    final String[] dbColumns = {
            DatabaseContract.SCORES_TABLE + "." + DatabaseContract.scores_table._ID,
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.DATE_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL};

    LocalDateTime ldt;
    static final int INDEX_SCORES_ID = 0;
    static final int INDEX_TIME = 1;
    static final int INDEX_DATE = 2;
    static final int INDEX_AWAY_TEAM = 3;
    static final int INDEX_AWAY_GOAL = 4;
    static final int INDEX_HOME_TEAM = 5;
    static final int INDEX_HOME_GOAL = 6;
    static final int FIXTURE_DATE = 2;

    static final String CURRENT_FRAGMENT = "currentFragment";

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {


        return new RemoteViewsFactory() {
            @Override
            public void onCreate() {
                // Do nothing
            }

            @Override
            public void onDataSetChanged() {

                final long identityToken = Binder.clearCallingIdentity();
                ldt = LocalDateTime.now(DateTimeZone.UTC);
                Uri uri = DatabaseContract.scores_table.buildScoreWithDate();
                int j=data.length-1;
                if(mergeCursor!=null){
                    mergeCursor.close();
                }

                // Merge the cursor from querying all the dates available
                for(int i= -FIXTURE_DATE;i <= FIXTURE_DATE;i++) {
                    String matchDate = ldt.getYear() +"-" + (ldt.getMonthOfYear()) +"-"+ (ldt.getDayOfMonth()-i);
                    data[j--] = getContentResolver().query(
                                    uri,
                                    dbColumns,
                            null,
                            new String[]{matchDate},
                            null);
                }
                mergeCursor = new MergeCursor(data);
                String tempDateString = DatabaseUtils.dumpCursorToString(mergeCursor);
                Log.v(getClass().getSimpleName(),tempDateString);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if(mergeCursor!=null){
                    mergeCursor.close();
                    mergeCursor = null;
                }
            }

            @Override
            public int getCount() {
                if(mergeCursor == null){
                    return 0;
                }
                return mergeCursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int i) {
                if(i == AdapterView.INVALID_POSITION || mergeCursor == null || !mergeCursor.moveToPosition(i)){
                    return null;
                }

                RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.widget_item_layout);
                //String scoresID = data.getString(INDEX_SCORES_ID);
                String homeTeam = mergeCursor.getString(INDEX_HOME_TEAM);
                String homeGoal = mergeCursor.getString(INDEX_HOME_GOAL);
                String awayTeam = mergeCursor.getString(INDEX_AWAY_TEAM);
                String awayGoal = mergeCursor.getString(INDEX_AWAY_GOAL);
                String matchTime = mergeCursor.getString(INDEX_TIME);
                String matchDate = mergeCursor.getString(INDEX_DATE);

                DateTime dateTime = Utilies.getDateTime(matchDate,getString(R.string.date_pattern_2),getApplication());
                String matchDay = dateTime.dayOfWeek().getAsShortText()+", "+dateTime.dayOfMonth().getAsText();
                String scoreText = Utilies.getScores(Integer.parseInt(homeGoal),Integer.parseInt(awayGoal));
                remoteViews.setTextViewText(R.id.widget_team1_name,homeTeam);
                remoteViews.setTextViewText(R.id.widget_team2_name,awayTeam);
                remoteViews.setTextViewText(R.id.widget_score_text, scoreText);
                remoteViews.setTextViewText(R.id.widget_time_text, matchTime);
                remoteViews.setTextViewText(R.id.widget_match_day,matchDay);

                int homeTeamImage = Utilies.getTeamCrestByTeamName(homeTeam);
                int awayTeamImage = Utilies.getTeamCrestByTeamName(awayTeam);
                if(homeTeamImage!= R.drawable.no_icon) {
                    remoteViews.setImageViewResource(R.id.widget_team1_image, Utilies.getTeamCrestByTeamName(homeTeam));
                }else{
                    remoteViews.setImageViewResource(R.id.widget_team1_image,R.drawable.no_icon);
                }

                if(awayTeamImage!=R.drawable.no_icon) {
                    remoteViews.setImageViewResource(R.id.widget_team2_image, Utilies.getTeamCrestByTeamName(awayTeam));
                }else{
                    remoteViews.setImageViewResource(R.id.widget_team2_image,R.drawable.no_icon);
                }


                // UTC date might be different from local time.
                int dateDiff = ldt.getDayOfWeek() - dateTime.getDayOfWeek();
                if(Utilies.isRTL(getApplicationContext())) {
                    dateDiff = dateDiff*-1 - 1;
                }
                int currentFragmentIndex = 2 - dateDiff;
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(CURRENT_FRAGMENT,currentFragmentIndex);
                remoteViews.setOnClickFillInIntent(R.id.widget_list_view_item,fillInIntent);
                return remoteViews;
            }


            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(),R.id.widget_list_view_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int i) {
                    if(mergeCursor.moveToPosition(i)){
                    return mergeCursor.getLong(INDEX_SCORES_ID);
                }
                return i;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
