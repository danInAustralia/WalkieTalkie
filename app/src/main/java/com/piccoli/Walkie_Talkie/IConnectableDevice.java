package com.piccoli.Walkie_Talkie;

/**
 * Created by d.piccoli on 9/06/2016.
 */
public interface IConnectableDevice {
    /*Pre socket connection preparation. For example, wifi-direct device
    * must be connected prior to socket initiation. */
    void InitiateConnection();

    String IP_Address();
    String Name();
}
