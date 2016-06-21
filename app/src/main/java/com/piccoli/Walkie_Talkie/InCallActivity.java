package com.piccoli.Walkie_Talkie;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.TextView;

public class InCallActivity extends ActionBarActivity {

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
}
