package com.piccoli.Walkie_Talkie;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by d.piccoli on 15/06/2016.
 * Service that waits for calls to it's socket port.
 * Once socket is open the call is processed.
 */
public class NetworkAudioCallReceiverService extends IntentService implements IStoppable {
    DatagramSocket socket;
    public static final int SERVERPORT = 1090;
    boolean stopped = false;
    InetAddress addressOfPeer = null;

    BroadcastReceiver mReceiver;//registers for the events we want to know about

    public NetworkAudioCallReceiverService()
    {
        super(NetworkAudioCallReceiverService.class.getName());


        //start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction("WT.END_CALL");//listens for our custom end call intent

            mReceiver = new CallStatusBroadcastReceiver(this);
            registerReceiver(mReceiver, mIntentFilter);

            byte[] data = new byte[1024];
            socket = new DatagramSocket(SERVERPORT);
            DatagramPacket packet = new DatagramPacket(data, data.length);

            packet.getAddress();

            socket.receive(packet);//locks until an initial packet is received
            addressOfPeer = packet.getAddress();
            String packetText = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
            boolean containsGo = packetText.contains("<GO>");

            if(containsGo)
            {
                //get the caller info from the network
//                packetText = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
//                String peerName = packetText.substring(packetText.indexOf("<peerName>") + "<peerName>".length(),
//                                    packetText.indexOf("</peerName>"));
                String deviceName = packetText.substring(
                        packetText.indexOf("<GO>") + "<GO>".length(), packetText.indexOf("</GO>"));
                Intent inCallIntent = new Intent(this, InCallActivity.class);
                inCallIntent.putExtra("ip_address", "NA");
                inCallIntent.putExtra("initiator", "false");
                inCallIntent.putExtra("peer_name", deviceName);
                inCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(inCallIntent);
                ReceiveAudio(socket);
                //Runnable audioReceiveThread = new ReceiveSocketAudioThread(socket);
                //new Thread(audioReceiveThread).start();
            }
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }

    }

    public void ReceiveAudio(DatagramSocket callSocket) {
        boolean firstIteration = true;
        short[] shortArr;
        int ix = 0;

        int N = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        //reads packets from the network and sends to the speaker
        byte[] bytes = new byte[512];
        DatagramPacket receivedPacket = new DatagramPacket(bytes, bytes.length);
        int n = 0;//number of samples written
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 48000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 2, AudioTrack.MODE_STREAM);
        boolean containsStop = false;
        try {
            while (!stopped) {
                callSocket.receive(receivedPacket);
                bytes = receivedPacket.getData();
                String s = new String(bytes, "UTF-8");
                containsStop = s.contains("<STOP>");

                if (containsStop) {
                    stopped = true;
                    //SendEndCallBroadcast();

                } else//convert to packet to audio and play it.
                {
                    shortArr = Processor.ByteArrayToShortArray(bytes);

                    n = track.write(shortArr, 0, shortArr.length);//sends to default speaker.
                    if (firstIteration) {
                        track.play();
                        firstIteration = false;
                    }
                }
            }


            //callSocket.close();
            //track.release();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            callSocket.close();
            track.release();
            SendEndCallBroadcast();
        }
    }

    public void Stop()
    {
        stopped = true;
        if(addressOfPeer != null) {
            //therefore, we must send "STOP" to the peer.
            try {
                SocketAndAddress sad = new SocketAndAddress(socket, addressOfPeer);
                SendEndInBackground sendEndTask = new SendEndInBackground();
                sendEndTask.execute(sad);
                //SendEndCallBroadcast();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void SendEndCallBroadcast()
    {
        //send out stop broadcast
        Intent stopWTIntent = new Intent();
        stopWTIntent.setAction("WT.END_CALL_COMPLETE");
        sendBroadcast(stopWTIntent);
    }
}

