package com.udacity.stockhawk.data;

import android.os.Handler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by thib146 on 21/03/2017.
 */

public class NetworkUtils {

    /**
     * This method checks if the device is connected to the internet
     *
     * @param handler Changes the mConnected global variable according to connection status
     * @param timeout Time to wait for the server response before considering that the connexion is lost
     */
    public static void isNetworkAvailable(final Handler handler, final int timeout) {
        // Asks for message '0' (not connected) or '1' (connected) on 'handler'
        // the answer must be sent within the 'timeout' (in milliseconds)
        new Thread() {
            private boolean responded = false;
            @Override
            public void run() {
                // set 'responded' to TRUE if is able to connect with google mobile (responds fast)
                new Thread() {
                    @Override
                    public void run() {
                        HttpGet requestForTest = new HttpGet("http://m.google.com");
                        try {
                            new DefaultHttpClient().execute(requestForTest);
                            responded = true;
                        }
                        catch (Exception e) {
                        }
                    }
                }.start();

                try {
                    int waited = 0;
                    while(!responded && (waited < timeout)) {
                        sleep(100);
                        if(!responded ) {
                            waited += 100;
                        }
                    }
                }
                catch(InterruptedException e) {} // do nothing
                finally {
                    if (!responded) { handler.sendEmptyMessage(0); }
                    else { handler.sendEmptyMessage(1); }
                }
            }
        }.start();
    }
}
