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

public class InCallActivity extends Activity {

    Intent makeCallIntent = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent intent = getIntent();
        String ipAddress = intent.getStringExtra("ip_address");
        String peerName = intent.getStringExtra("peer_name");
        String myDeviceName = intent.getStringExtra("this_device_name");
        String initiator = intent.getStringExtra("initiator");
        boolean callInitiator = Objects.equals(initiator, "true");

        if(callInitiator) {
            makeCallIntent = new Intent(Intent.ACTION_SYNC, null, this, FullDuplexNetworkAudioCallService.class);
            makeCallIntent.putExtra("ip_address", ipAddress);
            makeCallIntent.putExtra("device_name", myDeviceName);
            startService(makeCallIntent);
        }
        else
        {

        }

        setContentView(R.layout.activity_in_call);
        TextView callerText = (TextView) findViewById(R.id.textViewCaller);
        callerText.setText(peerName);
    }

    public void btnEndPressed(View view)
    {
        Intent stopWTIntent = new Intent();
        stopWTIntent.setAction("WT.END_CALL");
        sendBroadcast(stopWTIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
