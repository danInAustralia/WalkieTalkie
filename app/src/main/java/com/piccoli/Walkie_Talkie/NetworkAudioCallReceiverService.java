package com.piccoli.Walkie_Talkie;

import android.app.IntentService;
import android.content.Intent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by d.piccoli on 15/06/2016.
 * Service that waits for calls to it's socket port.
 * Once socket is open the call is processed.
 */
public class NetworkAudioCallReceiverService extends IntentService {

    public static final int SERVERPORT = 1090;

    public NetworkAudioCallReceiverService()
    {
        super(NetworkAudioCallReceiverService.class.getName());
        //start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            byte[] data = new byte[1024];
            DatagramSocket socket = new DatagramSocket(SERVERPORT);
            DatagramPacket packet = new DatagramPacket(data, data.length);

            socket.receive(packet);//locks until an initial packet is received
            String packetText = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
            boolean containsGo = packetText.contains("<GO>");

            if(containsGo)
            {
                //get the caller info from the network
                packetText = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                String peerName = packetText.substring(packetText.indexOf("<peerName>") + "<peerName>".length(),
                                    packetText.indexOf("</peerName>"));

                Runnable audioReceiveThread = new ReceiveSocketAudioThread(socket);
                new Thread(audioReceiveThread).start();
            }
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }

    }
}
