package com.piccoli.Walkie_Talkie;

/**
 * Created by d.piccoli on 9/06/2016.
 */
public class IPConnectableDevice implements IConnectableDevice {
    private String ipAddress;
    private String name;

    public IPConnectableDevice(String ip, String name)
    {
        ipAddress = ip;
        this.name = name;
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

    }
}
