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
 * Created by d.piccoli on 16/12/2014.
 * allows handling of wifi events (intents) broadcast by the android system.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver{
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private SelectPeerActivity mActivity;
    private boolean connected = false;
    private boolean initiated = false;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       SelectPeerActivity activity)
    {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    /* activated after requestPeers is called and broadcast receiver is activated*/
    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            ArrayList<IConnectableDevice> peers = new ArrayList();
            peers.clear();



            for(WifiP2pDevice wifiDevice: peerList.getDeviceList())
            {
                WifiDirectConnectableDevice device = new WifiDirectConnectableDevice(
                                                        wifiDevice.deviceAddress,
                                                        wifiDevice.deviceName,
                                                        wifiDevice,
                                                        wifiDevice.status == WifiP2pDevice.CONNECTED,
                                                        mManager,
                                                        mChannel);
                peers.add(device);
            }
            mActivity.updateStatus(peers.size() + " peers found.");

            mActivity.updatePeerList(peers);
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
            if(!connected) {
                Log.d("Hello", "p2p change");
                if (mManager != null) {
                    mActivity.updateStatus("Searching for peers...");
                    mManager.requestPeers(mChannel, peerListListener);
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected())
            {
                    connected = true;
                    // We have a wifi-direct connection with the other device, request connection
                    // info to find group owner IP

                    mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                            String ipAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
                            //only call this if explicitly asked for connection.
                            //need some way of figuring out if this is an already established connection
                            mActivity.initiateCall(ipAddress);
                        }
                    });
            }
            else
            {
                connected = false;

                if(initiated) {
                    //call something that handles lost connection
                    mActivity.handleLostConnection();
                }
                else{
                    initiated= true;
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            String thisDeviceName = device.deviceName;
            mActivity.SetDeviceName(thisDeviceName);
        }
//        else if (action == "WT.END_CALL_COMPLETE")
//        {
//            mActivity.EndCall();
//        }
    }
}
