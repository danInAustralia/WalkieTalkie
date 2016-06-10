package com.piccoli.hello;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;

/**
 * Created by d.piccoli on 3/01/2015.
 */
public class MicSourceToNetworkService extends IntentService
{
    private boolean stopped = false;
    public static final int SERVERPORT = 1090;

    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public MicSourceToNetworkService()
    {
        super(MicSourceToNetworkService.class.getName());
        //start();
    }

    public void Stop()
    {
        stopped = true;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        String ipAddress = bundle.getString("ip_address");
        MicToIP("134.115.93.89");
    }

    private void MicToIP(String ipAddress) {
        Log.i("Audio", "Running Audio Thread");
        AudioRecord recorder = null;
        AudioTrack track = null;
        short[][]   buffers  = new short[256][9600];
        int ix = 0;

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            int N = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10);
            //track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
            //        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
            recorder.startRecording();
            //track.play();
            //setup the socket to send the microphone output
            //Socket socket = new Socket();
            DatagramSocket socket = new DatagramSocket();
            //socket.connect(new InetSocketAddress(HelloRequestService.SERVERIP, HelloRequestService.SERVERPORT), 5000);
            //BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            DatagramPacket packet = null;
            InetAddress address = InetAddress.getByName(ipAddress);

            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            while(!stopped)
            {
                Log.i("Map", "Writing new data to buffer");
                short[] buffer = buffers[ix++ % buffers.length];
                N = recorder.read(buffer,0,buffer.length);
                //write the audio to the socket.
                /*if(BufferContainsGreaterThan256(buffer))
                {
                    Log.w("Audio", "Contains value greater than 256");
                }*/
                //out.write(ShortToByteArray(buffer), 0, buffer.length * 2);
                packet = new DatagramPacket(ShortToByteArray(buffer), buffer.length * 2, address, HelloRequestService.SERVERPORT);
                socket.send(packet);
                //track.write(buffer, 0, buffer.length);
            }
            packet = new DatagramPacket("<STOP>".getBytes(Charset.forName("UTF-8")), 7, address, HelloRequestService.SERVERPORT);
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
            recorder.stop();
            recorder.release();
            //track.stop();
            //track.release();
        }
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
