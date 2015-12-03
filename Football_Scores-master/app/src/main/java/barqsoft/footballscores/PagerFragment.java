package barqsoft.footballscores;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by yehya khaled on 2/27/2015.
 */
public class PagerFragment extends Fragment
{
    public static final int NUM_PAGES = 5;
    public ViewPager mPagerHandler;
    private myPageAdapter mPagerAdapter;
    private int currentFragment;
    private final String CURRENT_FRAG_TAG = "currentFragment";
    private MainScreenFragment[] viewFragments = new MainScreenFragment[5];
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        if(savedInstanceState != null){
            currentFragment = savedInstanceState.getInt(CURRENT_FRAG_TAG);
        }else{
            currentFragment = MainActivity.current_fragment;
        }

        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);
        mPagerHandler = (ViewPager) rootView.findViewById(R.id.pager);
        mPagerAdapter = new myPageAdapter(getChildFragmentManager());
        for (int i = 0;i < NUM_PAGES;i++)
        {
            Date fragmentdate = new Date(System.currentTimeMillis()+((i-2)*86400000));
            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            viewFragments[i] = new MainScreenFragment();
            viewFragments[i].setFragmentDate(mformat.format(fragmentdate));
        }

        if (Utilies.isRTL(getActivity())){
            reverseFragmentList();
        }

        mPagerHandler.setAdapter(mPagerAdapter);
        mPagerHandler.setCurrentItem(MainActivity.current_fragment);
        return rootView;
    }

    // Reversing existing fragment list for RTL support
    public void reverseFragmentList(){
        for(int i=0;i<NUM_PAGES/2;i++){
            int j = NUM_PAGES-1-i;
            MainScreenFragment tempFrag = viewFragments[j];
            viewFragments[j] = viewFragments[i];
            viewFragments[i] =  tempFrag;
        }
    }

    private class myPageAdapter extends FragmentStatePagerAdapter
    {
        @Override
        public Fragment getItem(int i)
        {
            return viewFragments[i];
        }

        @Override
        public int getCount()
        {
            return NUM_PAGES;
        }

        public myPageAdapter(FragmentManager fm)
        {
            super(fm);
        }
        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position)
        {
            position = -2 + position;

            // if RTL layout, reverse the
            position = Utilies.isRTL(getActivity())?-position:position;
            return getDayName(getActivity(),System.currentTimeMillis()+(position*86400000));
        }
        public String getDayName(Context context, long dateInMillis) {

            // If the date is today, return the localized version of "Today" instead of the actual
            // day name.
            Time t = new Time();
            t.setToNow();
            int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
            int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
            if (julianDay == currentJulianDay) {
                return context.getString(R.string.today);
            } else if ( julianDay == currentJulianDay +1 ) {
                return context.getString(R.string.tomorrow);
            }
             else if ( julianDay == currentJulianDay -1)
            {
                return context.getString(R.string.yesterday);
            }
            else
            {
                Time time = new Time();
                time.setToNow();
                // Otherwise, the format is just the day of the week (e.g "Wednesday".
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                return dayFormat.format(dateInMillis);
            }
        }
    }
}
