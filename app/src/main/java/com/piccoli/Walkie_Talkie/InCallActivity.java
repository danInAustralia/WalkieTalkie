package com.piccoli.Walkie_Talkie;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class InCallActivity extends Activity {

    Intent makeCallIntent = null;
    boolean callInitiator = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent intent = getIntent();
        String ipAddress = intent.getStringExtra("ip_address");
        String peerName = intent.getStringExtra("peer_name");

        makeCallIntent= new Intent(Intent.ACTION_SYNC, null, this, FullDuplexNetworkAudioCallService.class);
        makeCallIntent.putExtra("ip_address", ipAddress);
        startService(makeCallIntent);

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
