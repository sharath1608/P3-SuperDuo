package it.jaschke.alexandria;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Asus1 on 10/28/2015.
 */
public class Utilities {

    public static boolean isConnectedToNetwork(Context context){

        ConnectivityManager cm =  (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if(networkInfo!=null && networkInfo.isConnectedOrConnecting()){
            return true;
        }

        return false;
    }
}
