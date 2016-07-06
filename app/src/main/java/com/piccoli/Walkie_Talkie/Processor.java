package com.piccoli.Walkie_Talkie;

/**
 * Created by d.piccoli on 6/07/2016.
 */
public class Processor {

    public static short[] ByteArrayToShortArray(byte[] input)
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
