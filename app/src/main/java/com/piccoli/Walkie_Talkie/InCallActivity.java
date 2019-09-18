package com.piccoli.Walkie_Talkie;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Objects;

///
public class InCallActivity extends Activity {

    Intent makeCallIntent = null;
    CallEndedForInCallBroadcastReceiver callEndedBR;
    IntentFilter mCallEndIntentFilter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent intent = getIntent();
        String ipAddress = intent.getStringExtra("ip_address");
        String peerName = intent.getStringExtra("peer_name");
        String myDeviceName = intent.getStringExtra("this_device_name");
        String initiator = intent.getStringExtra("initiator");
        boolean callInitiator = initiator.equals("true");//Objects.equals(initiator, "true");

        if(callInitiator) {
            makeCallIntent = new Intent(Intent.ACTION_SYNC, null, this, FullDuplexNetworkAudioCallService.class);
            makeCallIntent.putExtra("ip_address", ipAddress);
            makeCallIntent.putExtra("device_name", myDeviceName);
            startService(makeCallIntent);
        }
        else
        {

        }

        mCallEndIntentFilter = new IntentFilter();
        mCallEndIntentFilter.addAction("WT.END_CALL_COMPLETE");//listens for our custom end call intent
        callEndedBR = new CallEndedForInCallBroadcastReceiver(this);
        registerReceiver(callEndedBR, mCallEndIntentFilter);

        setContentView(R.layout.activity_in_call);
        TextView callerText = (TextView) findViewById(R.id.textViewCaller);
        callerText.setText(peerName);
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(callEndedBR, mCallEndIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(callEndedBR);
    }

    public void btnEndPressed(View view)
    {
        Intent stopWTIntent = new Intent();
        stopWTIntent.setAction("WT.END_CALL_INSTIGATED");
        sendBroadcast(stopWTIntent);
        finish();
    }

    public void Stop()
    {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
