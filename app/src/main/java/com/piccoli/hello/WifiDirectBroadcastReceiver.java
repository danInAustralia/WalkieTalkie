package com.piccoli.hello;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by d.piccoli on 16/12/2014.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver{
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private InitiateHelloActivity mActivity;
    private List peerList = new ArrayList();

    public WifiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       InitiateHelloActivity activity)
    {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    private WifiP2pManager.PeerListListener pll = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            peerList.clear();
            peerList.addAll(peers.getDeviceList());

            //update the ListView of available peers.
            //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();?
            ListView tvs = (ListView) mActivity.findViewById(R.id.listViewPeers);

            ArrayAdapter<WifiP2pDevice> arrayAdapter = new ArrayAdapter<WifiP2pDevice>(
                    mActivity,
                    android.R.layout.simple_expandable_list_item_1,
                    peerList
            );

            tvs.setAdapter(arrayAdapter);
        }
    };

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action))
        {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                //Wifi P2p is enabled

            }
            else
            {
                //not enabled
            }
            if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
                if(mManager != null){

                    mManager.requestPeers(mChannel, pll);
                }
            }
        }
    }
}
