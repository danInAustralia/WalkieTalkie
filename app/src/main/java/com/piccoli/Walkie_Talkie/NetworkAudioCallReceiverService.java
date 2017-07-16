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
import android.media.MediaRecorder;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

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
    boolean busy = false;
    private boolean instigatedEnd = false;
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
            mIntentFilter.addAction("WT.END_CALL_INSTIGATED");

            mReceiver = new CallStatusBroadcastReceiver(this);
            registerReceiver(mReceiver, mIntentFilter);


            byte[] data = new byte[1024];
            socket = new DatagramSocket(SERVERPORT);
            DatagramPacket packet = new DatagramPacket(data, data.length);

            while(true) {
                //listen for connection.
                socket.receive(packet);//locks until an initial packet is received
                if(!busy) {
                    addressOfPeer = packet.getAddress();
                    int port = packet.getPort();
                    String packetText = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    boolean containsGo = packetText.contains("<GO>");

                    if (containsGo) {
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

                        //start the receive part.
                        ReceiveSocketAudioThread receiveThread = new ReceiveSocketAudioThread(socket, this.getBaseContext());
                        new Thread(receiveThread).start();
                        //continue with the send part
                        MicToIP(socket, addressOfPeer, deviceName, port);
                    }
                }
                else
                {
                    //send back busy message.
                }
            }
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }

    }

    private void MicToIP(DatagramSocket socket, InetAddress ipAddress, String peerName, int port) {
        Log.i("Audio", "Running Audio Thread");
        AudioRecord microphone = null;
        AudioTrack track = null;
        short[] buffer  = new short[256];
        int ix = 0;

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            DatagramPacket packet = null;
            int N = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            microphone = new AudioRecord(MediaRecorder.AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10);
            //track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
            //        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
            microphone.startRecording();
            //track.play();
            //setup the socket to send the microphone output
            //Socket socket = new Socket();

            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            while(!stopped)
            {


                //Log.i("Map", "Writing new data to buffer");
                N = microphone.read(buffer,0,buffer.length);

                //write the audio to the socket.
                packet = new DatagramPacket(ShortToByteArray(buffer), buffer.length * 2, ipAddress, port);
                socket.send(packet);
                //track.write(buffer, 0, buffer.length);

            }
//            recorder.stop();
//            recorder.release();
            if(instigatedEnd) {
                byte[] protocolEndStr = "<STOP>".getBytes("UTF-8");
                packet = new DatagramPacket(protocolEndStr, protocolEndStr.length, ipAddress, port);
                socket.send(packet);
            }
            socket.close();
//            Intent stopWTIntent = new Intent();
//            stopWTIntent.setAction("WT.END_CALL_COMPLETE");
//            sendBroadcast(stopWTIntent);
            //out.write("<STOP>".getBytes(Charset.forName("UTF-8")), 0, 7);
        }
        catch(Throwable x)
        {
            Log.w("Audio", "Error reading voice audio", x);
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
        finally
        {
            microphone.stop();
            microphone.release();

            Intent stopWTIntent = new Intent();
            stopWTIntent.setAction("WT.END_CALL_COMPLETE");
            sendBroadcast(stopWTIntent);
            //track.stop();
            //track.release();
        }
    }

    /**
     * Converts a short array to a byte array.
     * @param input
     * @return
     */
    byte[] ShortToByteArray(short[] input)
    {
        int short_index, byte_index;
        int iterations = input.length;

        byte[] output = new byte[input.length * 2];
        short_index = byte_index = 0;

        for(;short_index != iterations;)
        {
            output[byte_index] = (byte) (input[short_index] & 0x00FF);
            output[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }
        return output;
    }

    public void Stop(boolean instigatedEnd)
    {
        this.instigatedEnd = instigatedEnd;
        stopped = true;
    }

    public void SendEndCallBroadcast()
    {
        //send out stop broadcast
        Intent stopWTIntent = new Intent();
        stopWTIntent.setAction("WT.END_CALL_COMPLETE");
        sendBroadcast(stopWTIntent);
    }
}

