package com.piccoli.hello;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by d.piccoli on 16/12/2014.
 * allows receival from events (intents) broadcast by the android system.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver{
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private SelectPeerActivity mActivity;

    private ArrayList peers;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       SelectPeerActivity activity)
    {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        peers = new ArrayList();
    }

    /* activated when requestPeers is called */
    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            peers.clear();
            Collection<WifiP2pDevice> deviceList = peerList.getDeviceList();
            mActivity.updateStatus(deviceList.size()+ " peers found.");
            peers.addAll(peerList.getDeviceList());

            mActivity.setPeerList(peers);
        }
    };




    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            {
                // Wifi direct is enabled
                mActivity.setIsWifiDirectEnabled(true);
            }
            else
            {
                //Wifi direct is not enabled
                mActivity.setIsWifiDirectEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // fired when list of peers changes
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            Log.d("Hello", "p2p change");
            if(mManager != null)
            {
                mActivity.updateStatus("Searching for peers...");
                mManager.requestPeers(mChannel, peerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }
}
