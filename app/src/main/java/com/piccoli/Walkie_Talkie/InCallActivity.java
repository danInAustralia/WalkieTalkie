package com.piccoli.Walkie_Talkie;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

public class InCallActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        String ipAddress = bundle.getString("ip_address");
        String peerName = bundle.getString("peer_name");
        setContentView(R.layout.activity_in_call);
    }
}
