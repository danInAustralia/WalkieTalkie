package com.piccoli.Walkie_Talkie;

import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by d.piccoli on 15/06/2016.
 */
public class ReceiveSocketAudioThread implements Runnable
{
    DatagramSocket callScoket;
    Context context;

    public ReceiveSocketAudioThread(DatagramSocket socket, Context context)
    {
        this.context = context;
        callScoket = socket;
    }

    public void run()
    {
        boolean firstIteration = true;
        short[] shortArr;
        int ix = 0;
        boolean stopped = false;

        int N = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        //reads packets from the network and sends to the speaker
        byte[] bytes = new byte[512];
        DatagramPacket receivedPacket = new DatagramPacket(bytes, bytes.length);
        int n = 0;//number of samples written
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 48000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*2, AudioTrack.MODE_STREAM);

        try {
            while (!stopped) {
                callScoket.receive(receivedPacket);
                bytes = receivedPacket.getData();
                String s = new String(bytes, "UTF-8");
                boolean containsStop = s.contains("<STOP>");

                if(containsStop)
                {
                    stopped = true;

                }
                else//convert to packet to audio and play it.
                {
                    shortArr = ByteArrayToShortArray(bytes);

                    n = track.write(shortArr, 0, shortArr.length);//sends to default speaker.
                    if (firstIteration) {
                        track.play();
                        firstIteration = false;
                    }
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            callScoket.close();
            track.release();
            SendEndCallBroadcast();
        }
    }

    public void SendEndCallBroadcast()
    {
        //broadcast an END_CALL event
        Intent stopWTIntent = new Intent();
        stopWTIntent.setAction("WT.END_CALL");

        context.sendBroadcast(stopWTIntent);
    }

    short[] ByteArrayToShortArray(byte[] input)
    {
        int short_index, byte_index;
        int iterations = input.length;

        short[] output = new short[input.length / 2];
        short_index = byte_index = 0;

        for(int i=0; i < input.length /2 ; i++)
        {
            output[i] = (short) (((short)input[i*2] & 0xFF) + (((short)input[i*2+1] & 0xFF) << 8));
        }
        return output;
    }
}
