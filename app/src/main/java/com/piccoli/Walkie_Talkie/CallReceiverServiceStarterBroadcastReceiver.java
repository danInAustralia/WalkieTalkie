package com.piccoli.Walkie_Talkie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallReceiverServiceStarterBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent receiveCallIntent = new Intent("com.piccoli.Walkie_Talkie.NetworkAudioCallReceiverService");
        receiveCallIntent.setClass(context, NetworkAudioCallReceiverService.class);
        //receiveCallIntent = new Intent(Intent.ACTION_SYNC, null, this, NetworkAudioCallReceiverService.class);
        context.startService(receiveCallIntent);
    }
}