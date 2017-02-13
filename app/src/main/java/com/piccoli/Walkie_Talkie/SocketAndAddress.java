package com.piccoli.Walkie_Talkie;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class SocketAndAddress
{
    public DatagramSocket Socket;
    public InetAddress AddressOfPeer;

    public SocketAndAddress(DatagramSocket socket, InetAddress address)
    {
        Socket = socket;
        AddressOfPeer = address;
    }
}
