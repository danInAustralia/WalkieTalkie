package com.piccoli.Walkie_Talkie;

import android.os.AsyncTask;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SendEndInBackground extends AsyncTask<SocketAndAddress, Void, Void>
{

    @Override
    protected Void doInBackground(SocketAndAddress... sad)
    {
        byte[] protocolEndStr = new byte[0];
        try {
            protocolEndStr = "<STOP>".getBytes("UTF-8");


            DatagramPacket packet = new DatagramPacket(protocolEndStr, protocolEndStr.length, sad[0].AddressOfPeer, HelloRequestService.SERVERPORT);
            sad[0].Socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        return null;
    }
}

