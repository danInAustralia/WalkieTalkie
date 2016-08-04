package com.piccoli.Walkie_Talkie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by d.piccoli on 05/07/2016
 * calls Stop function of ICallService when END_CALL broadcast is detected
 */
public class CallEndedForInCallBroadcastReceiver extends BroadcastReceiver{

    InCallActivity ica;

    public CallEndedForInCallBroadcastReceiver(InCallActivity ica)
    {
        super();
        this.ica = ica;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();

        if (action == "WT.END_CALL_COMPLETE")
        {
            ica.Stop();
        }
    }
}