package com.piccoli.Walkie_Talkie;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class HelloRequestService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_CALL = "com.piccoli.hello.action.CALL";
    private static final String ACTION_HANGUP = "com.piccoli.hello.action.HANGUP";
    private static final String ACTION_RECEIVE = "com.piccoli.hello.action.RECEIVE";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.piccoli.hello.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.piccoli.hello.extra.PARAM2";

    public static final String SERVERIP = "134.115.93.89";
    public static final int SERVERPORT = 1090;

    private boolean stopped;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionCall(Context context, String param1, String param2) {
        Intent intent = new Intent(context, HelloRequestService.class);
        intent.setAction(ACTION_CALL);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionHangup(Context context, String param1, String param2) {
        Intent intent = new Intent(context, HelloRequestService.class);
        intent.setAction(ACTION_HANGUP);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public HelloRequestService() {
        super("HelloRequestService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            handleActionCall();
            /*if (ACTION_CALL.equals(action)) {
                //final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                //final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionCall();
            } else if (ACTION_HANGUP.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionHangup(param1, param2);

            }*/
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionCall() {
        // TODO: Handle action Foo
        Log.i("Audio", "Running Audio Thread");
        AudioRecord recorder = null;
        AudioTrack track = null;
        short[][]   buffers  = new short[256][160];
        int ix = 0;

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            int N = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
            recorder.startRecording();
            //track.play();
            //setup the socket to send the microphone output
            //Socket socket = new Socket(HelloRequestService.SERVERIP, HelloRequestService.SERVERPORT);
            DatagramSocket socket = new DatagramSocket();
            //BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            DatagramPacket packet = null;
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
                //out.write(ShortToByteArray(buffer), 0, buffer.length * 2);
                packet = new DatagramPacket(ShortToByteArray(buffer), buffer.length * 2, InetAddress.getByName(HelloRequestService.SERVERIP), HelloRequestService.SERVERPORT);
                socket.send(packet);
                //track.write(buffer, 0, buffer.length);
            }
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
            output[byte_index + 1] = (byte) ((input[short_index] & 0x00FF) >> 8);

            ++short_index; byte_index += 2;
        }
        return output;
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionHangup(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
