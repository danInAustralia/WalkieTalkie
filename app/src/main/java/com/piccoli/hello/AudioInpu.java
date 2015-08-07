package com.piccoli.hello;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Created by d.piccoli on 3/01/2015.
 */
public class AudioInpu extends Thread
{
    private boolean stopped = false;
    public static final String SERVERIP = "134.115.93.89";
    public static final int SERVERPORT = 1090;

    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public AudioInpu()
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        //start();
    }

    public void Stop()
    {
        stopped = true;
    }

    @Override
    public void run()
    {
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
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(HelloRequestService.SERVERIP, HelloRequestService.SERVERPORT), 5000);
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

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
                out.write(ShortToByteArray(buffer), 0, buffer.length * 2);
                //track.write(buffer, 0, buffer.length);
            }
            out.write("<STOP>".getBytes(Charset.forName("UTF-8")), 0, 7);
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
