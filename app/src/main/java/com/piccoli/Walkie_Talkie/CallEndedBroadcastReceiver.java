package com.piccoli.Walkie_Talkie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by d.piccoli on 29/07/2016.
 */
public class CallEndedBroadcastReceiver extends BroadcastReceiver {
    private SelectPeerActivity mActivity;

    public CallEndedBroadcastReceiver(SelectPeerActivity cs
    ) {
        super();
        mActivity = cs;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == "WT.END_CALL_COMPLETE") {
            mActivity.EndCall();
            //mActivity.start_peer_discovery();
        }
    }
}