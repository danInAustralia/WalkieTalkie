package com.piccoli.Walkie_Talkie;

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

    public ReceiveSocketAudioThread(DatagramSocket socket)
    {
        callScoket = socket;
    }

    public void run()
    {
        byte[] bytes = new byte[2048];
        short[] shortArr;
        int ix = 0;
        //reads packets from the network and sends to the speaker
        DatagramPacket receivedPacket = new DatagramPacket(bytes, bytes.length);
        int N = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 48000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
        track.play();
        while(true)
        {
            try {
                callScoket.receive(receivedPacket);
                bytes = receivedPacket.getData();
                shortArr = ByteArrayToShortArray(bytes);

                track.write(shortArr, 0, shortArr.length);//sends to default speaker.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
