package com.piccoli.Walkie_Talkie;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.Random;

/**
 * Created by d.piccoli on 10/06/2016.
 */
public class WifiDirectConnectableDevice implements IConnectableDevice {
    private String ipAddress;
    private String name;
    private boolean connected;
    private WifiP2pDevice device;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    public WifiDirectConnectableDevice(String ip,
                                       String name,
                                       WifiP2pDevice device,
                                       boolean connected,
                                       WifiP2pManager manager,
                                       WifiP2pManager.Channel channel)
    {
        ipAddress = ip;
        this.name = name;
        this.device = device;
        this.connected = connected;
        mManager = manager;
        mChannel = channel;
    }

    @Override
    public String Name()
    {
        return name;
    }

    public String IP_Address()
    {
        return ipAddress;
    }

    public void InitiateConnection()
    {
        //if the device is not yet connected, then connect
        if(!connected) {
            final WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            //helps in negotiating who will be the wifi group owner.
            Random r = new Random();
            config.groupOwnerIntent = 1;

            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int i) {
//                    Activity currentActivity = ((MyApp)context.getApplicationContext()).getCurrentActivity();
//                    Toast.makeText(currentActivity, "WT: Wifi-direct handshake failed", Toast.LENGTH_SHORT).show();
                }
            });

            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                //success logic: get the IP of the wifi direct group owner
                @Override
                public void onSuccess() {
//                    mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
//                        @Override
//                        public void onSuccess() {
//
//                        }
//
//                        @Override
//                        public void onFailure(int i) {
//
//                        }
//                    });
                }

                @Override
                public void onFailure(int reason) {
                    //failure logic
                }
            });

        }
    }
}
