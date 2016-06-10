package com.piccoli.hello;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;


public class SelectPeerActivity extends Activity {
    ArrayList<IConnectableDevice> peerDevices;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;//registers for the events we want to know about
    IntentFilter mIntentFilter;
    MicSourceToNetworkService audioCaptureThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntentFilter = new IntentFilter();

        //same intents that WifiDirectBroadcastReceiver checks for
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        //turns on wifi-direct programatically
        try {
            Class<?> wifiManager = Class
                    .forName("android.net.wifi.p2p.WifiP2pManager");
            Method method = wifiManager
                    .getMethod(
                            "enableP2p",
                            new Class[] { android.net.wifi.p2p.WifiP2pManager.Channel.class });
            method.invoke(mManager, mChannel);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_initiate_hello);
        SetupWidgetListeners();
        SetPeerList();
        setupWIFI();
    }

    /*initialises the peerList and updates the ListView

    */
    private void SetPeerList()
    {
        peerDevices = new ArrayList<IConnectableDevice>();
        IConnectableDevice hardCodedMachine = new IPConnectableDevice("134.115.93.89", "Daniel's Computer");
        peerDevices.add(hardCodedMachine);

        ListView peerView = (ListView) findViewById(R.id.listViewPeers);
        PopulatePeerView(peerView);
    }

    public void setIsWifiDirectEnabled(boolean wifiEnabled)
    {
        TextView txtStatus = (TextView) findViewById(R.id.textViewStatus);
        if(!wifiEnabled)
        {
            //update UI status
            updateStatus("Wifi direct enabled");
        }
        else
        {
            updateStatus("Wifi direct enabled");
        }
    }

    public void updateStatus(CharSequence info)
    {
        TextView txtStatus = (TextView) findViewById(R.id.textViewStatus);

        txtStatus.setText(info);

    }

    /*
        Populates listViewPeers with in range radios and updates the listview.
        this does too much.
     */
    public void updatePeerList(ArrayList<IConnectableDevice> peers)
    {
        SetPeerList();
        ListView peerView = (ListView) findViewById(R.id.listViewPeers);


        for(IConnectableDevice additionalPeer : peers)
        {
            peerDevices.add(additionalPeer);
        }
        PopulatePeerView(peerView);

    }

    //draws the list of peers in the ListView
    private void PopulatePeerView(ListView peerView) {
        //convert peerDevices to strings
        ArrayList<String> peerStrings = new ArrayList<String>();
        for(IConnectableDevice dev : peerDevices)
        {
            peerStrings.add(dev.Name() + " : " + dev.IP_Address() );
        }
        //populate the ListView
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, peerStrings);

        int numberOfPeers = peerDevices.size();
        updateStatus(numberOfPeers + " peers found.");
        Toast.makeText(SelectPeerActivity.this, numberOfPeers + " peers found.", Toast.LENGTH_SHORT).show();

        peerView.setAdapter(adapter);
    }

    public void startAudioCapture(View view)
    {

        //setupWIFI();
        //audioCaptureThread = new MicSourceToNetworkService();
        Button startButton = (Button) findViewById(R.id.btnStart);
        startButton.setText("Hang up now");
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopCall(v);
            }
        });

        //Intent intentToStartMic = new Intent(this, HelloRequestService.class);

       //startService(intentToStartMic);
        //audioCaptureThread.start();
        Intent intent = new Intent(Intent.ACTION_SYNC, null, this, MicSourceToNetworkService.class);
        startService(intent);
    }

    public void stopCall(View view)
    {
        audioCaptureThread.Stop();
        Button startButton = (Button) findViewById(R.id.btnStart);

        startButton.setText("Start transmission");
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startAudioCapture(v);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    /* /Discovers peers for use with wifi direct */
    public void setupWIFI()
    {
        //sets off the process to discover wifi peers but does not return peer list.
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                //notifies you that the discovery process succeeded.
                @Override
                public void onSuccess() {
                    //update the listbox of peers.
                    Toast.makeText(SelectPeerActivity.this, "Peer discovery initiated", Toast.LENGTH_SHORT).show();
                    Log.d("hello", "Peer discovery initiated");
                    updateStatus("Peer discovery initiated");
                }

                @Override
                public void onFailure(int reasonCode) {
                    Toast.makeText(SelectPeerActivity.this, "Peer discovery failed "+ reasonCode, Toast.LENGTH_SHORT).show();
                    Log.d("hello", "Peer discovery failed");
                    updateStatus("Peer discovery failed");
                }
            }
        );
    }

    private void  SetupWidgetListeners()
    {
        ListView peerView = (ListView) findViewById(R.id.listViewPeers);

        peerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                updateStatus("Item " + position + " clicked");
            }
        });
    }

    public void standby()
    {
        //listen for requests on socket
        //set up connection if request is received.
    }

    public void instigateCall()
    {
        //send request packet
        //wait for response
    }

    public void answerCall() {
        //listen for calls

    }

    public void duringCall(){
        //set up socket
        //while socket is still open
            //read from socket
            //play audio
            //read from microphone
            //send on socket.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_initiate_hello, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
