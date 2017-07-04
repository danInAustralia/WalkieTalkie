package com.piccoli.Walkie_Talkie;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;

/**
 * Created by d.piccoli on 3/01/2015.
 */
public class FullDuplexNetworkAudioCallService extends IntentService implements IStoppable
{
    private boolean stopped = false;
    private boolean instigatedEnd = false;
    public static final int SERVERPORT = 1090;

    BroadcastReceiver mReceiver;//registers for the events we want to know about
    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public FullDuplexNetworkAudioCallService()
    {
        super(FullDuplexNetworkAudioCallService.class.getName());
        //start();

    }

    public void Stop(boolean instigatedEnd)
    {
        this.instigatedEnd = instigatedEnd;
        stopped = true;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = intent.getExtras();

        IntentFilter mIntentFilter = new IntentFilter();
        //listens for our custom end call intents
        mIntentFilter.addAction("WT.END_CALL");
        mIntentFilter.addAction("WT.END_CALL_INSTIGATED");

        mReceiver = new CallStatusBroadcastReceiver(this);
        registerReceiver(mReceiver, mIntentFilter);
        String ipAddress = bundle.getString("ip_address");
        String peerName = bundle.getString("device_name");

        try {
            DatagramSocket socket = new DatagramSocket();
            //socket.connect(new InetSocketAddress(HelloRequestService.SERVERIP, HelloRequestService.SERVERPORT), 5000);
            //BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            InetAddress address = InetAddress.getByName(ipAddress);
            SendProtocolHeader(address, peerName, socket);

            //start the receive thread
            ReceiveSocketAudioThread receiveThread = new ReceiveSocketAudioThread(socket, this.getBaseContext());
            new Thread(receiveThread).start();
            //continue with the send part
            MicToIP(socket, address, peerName);
        }
        catch(Throwable x)
        {
            x.printStackTrace();
        }


    }

    private void SendProtocolHeader(InetAddress ipAddress, String peerName, DatagramSocket socket) throws IOException {
        DatagramPacket packet = null;

        //out.write("<STOP>".getBytes(Charset.forName("UTF-8")), 0, 7);
        //part of protocol to send <GO> to the receiver.
        byte[] protocolStartStr = ("<GO>" + peerName + "</GO>").getBytes("UTF-8");
        packet = new DatagramPacket(protocolStartStr, protocolStartStr.length, ipAddress, HelloRequestService.SERVERPORT);
        socket.send(packet);
    }

    private void MicToIP(DatagramSocket socket, InetAddress ipAddress, String peerName) {
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
                packet = new DatagramPacket(ShortToByteArray(buffer), buffer.length * 2, ipAddress, HelloRequestService.SERVERPORT);
                socket.send(packet);
                //track.write(buffer, 0, buffer.length);

            }
            //this should only send a stop message over the netowrk, if the stop button was
            //pressed on this end
            if(instigatedEnd) {
                byte[] protocolEndStr = "<STOP>".getBytes("UTF-8");
                packet = new DatagramPacket(protocolEndStr, protocolEndStr.length, ipAddress, HelloRequestService.SERVERPORT);
                socket.send(packet);
            }
            socket.close();
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

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    boolean BufferContainsGreaterThan256(short[] buffer)
    {
        for(short number: buffer)
        {
            if(number > 256)
            {
                return true;
            }
        }
        return false;
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

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    private void close()
    {
        stopped = true;
    }


}
