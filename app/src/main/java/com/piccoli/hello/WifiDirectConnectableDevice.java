package com.piccoli.hello;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by d.piccoli on 10/06/2016.
 */
public class WifiDirectConnectableDevice implements IConnectableDevice {
    private String ipAddress;
    private String name;
    private WifiP2pDevice device;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    public WifiDirectConnectableDevice(String ip,
                                       String name,
                                       WifiP2pDevice device,
                                       WifiP2pManager manager,
                                       WifiP2pManager.Channel channel)
    {
        ipAddress = ip;
        this.name = name;
        this.device = device;
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
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //success logic
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
            }
        });
        //Transferring data
    }
}
