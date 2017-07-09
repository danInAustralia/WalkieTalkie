package com.piccoli.Walkie_Talkie;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;


public class SelectPeerActivity extends Activity {
    String thisDeviceName;
    IConnectableDevice selectedDevice;
    boolean ignoreDiscovery = false;
    ArrayList<IConnectableDevice> peerDevices;
    //ArrayList<String> peerStrings;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mWifiDirectBroadcastReceiver;//registers for the events we want to know about
    BroadcastReceiver mCallEndBroadcastReceiver;
    IntentFilter mIntentFilter;
    IntentFilter mCallEndIntentFilter;
    FullDuplexNetworkAudioCallService audioCaptureThread;
    boolean callInitiator = false;
    boolean instigatedWifiDisconnect = false;
    Intent makeCallIntent = null;
    Intent receiveCallIntent = null;
    boolean finishThis = false;
    boolean instigatedCall = false;
    ArrayAdapter peersAdapter;
    ListView peerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SetPeerList();//resets peerDevices instance variable
        peerDevices = new ArrayList<IConnectableDevice>();
        //peerStrings = new ArrayList<String>();
        mIntentFilter = new IntentFilter();
        mCallEndIntentFilter = new IntentFilter();

        //same intents that WifiDirectBroadcastReceiver checks for
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mCallEndIntentFilter.addAction("WT.END_CALL_COMPLETE");//listens for our custom end call intent

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mWifiDirectBroadcastReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        mCallEndBroadcastReceiver = new CallEndedBroadcastReceiver(this);
        registerReceiver(mWifiDirectBroadcastReceiver, mIntentFilter);
        registerReceiver(mCallEndBroadcastReceiver, mCallEndIntentFilter);
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
        //disconnect any existing physical layer connections.
        //disconnect();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_initiate_hello);
        PopulatePeerView();
        SetupWidgetListeners();
        //SetPeerList();
        deletePersistentGroups();
        start_peer_discovery();

        //start server
        //TODO: move this to a 'start on boot' service.
        receiveCallIntent = new Intent(Intent.ACTION_SYNC, null, this, NetworkAudioCallReceiverService.class);
        startService(receiveCallIntent);
    }

    public void SetDeviceName(String devName)
    {
        thisDeviceName = devName;
    }

    /*initialises the peerList and updates the ListView

    */
    private void SetPeerList()
    {
        peerDevices = new ArrayList<IConnectableDevice>();

//        ListView peerView = (ListView) findViewById(R.id.listViewPeers);
//        PopulatePeerView(peerView);
    }

    public void setIsWifiDirectEnabled(boolean wifiEnabled)
    {
        TextView txtStatus = (TextView) findViewById(R.id.textViewStatus);
        if(!wifiEnabled)
        {
            //update UI status
            updateStatus("Wifi direct not enabled");
        }
        else
        {
            start_peer_discovery();
            updateStatus("Wifi direct enabled");
        }
    }

    public void updateStatus(CharSequence info)
    {
        TextView txtStatus = (TextView) findViewById(R.id.textViewStatus);

        txtStatus.setText(info);

    }

    /*
        updatePeerList
        Populates listViewPeers with in range radios and updates the listview.
        this does too much.

        Called from the WifiDriectBroadcastReceiver when the list of peers is queried.
     */
    public void updatePeerList(ArrayList<IConnectableDevice> peers)
    {
        //TODO: as this function draws the peer list on the screen and references the peerStrings
        //array, it should only have to be called once.
        //if(peersAdapter == null) {
        //   PopulatePeerView();
        //}

        if(!ignoreDiscovery) {
            peerDevices.clear();
            peersAdapter.clear();

            for (IConnectableDevice peer : peers) {
                peerDevices.add(peer);
                peersAdapter.add(peer.Name());
            }
            int numberOfPeers = peerDevices.size();
            updateStatus(numberOfPeers + " peer(s) found.");
            Toast.makeText(SelectPeerActivity.this, numberOfPeers + " peer(s) found.", Toast.LENGTH_SHORT).show();
        }
    }

    //public void clearPeerView

    /*
    draws the list of peers in the ListView
     */
    private void PopulatePeerView() {
        if (peerDevices.size() == 0 || !ignoreDiscovery) {
            if(peerView == null) {
                peerView = (ListView) findViewById(R.id.listViewPeers);
                //populate the ListView
                peersAdapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, new ArrayList<String>());
                peerView.setAdapter(peersAdapter);
            }
            else
            {
                //adapter references peerStrings so this is not necessary (changes are seen)
                //peersAdapter.clear();
                //peersAdapter.addAll(peerStrings);
            }

            //peerView.deferNotifyDataSetChanged();
        }
    }

    /*
    *  startAudioCapture: this is called just after the wifi-direct connection has been established
    *   - parameter ipAddress (the ip address of the wifi direct group owner.
     *  - Updates the screen
     *  - starts the inCallIntent if this is the call initiator
     *  This should be called on both the server and the client, but the call will only be
     *  started on the initiator.
    * */
    public void startAudioCapture(String ipAddress)
    {
        updateStatus("Connected over Wifi direct");
        //stop discovery - either by setting ignoreDiscovery flag or by telling API to stop
        //stop_peer_discovery();
        ignoreDiscovery = true;
        //peerStrings.clear();
        //PopulatePeerView();

        if(callInitiator)
        {//start a call as client if the peer was selected on this device.
            updateStatus("Connecting to " + ipAddress);
            //start in-call activity
            Intent inCallIntent = new Intent(this, InCallActivity.class);
            inCallIntent.putExtra("ip_address", ipAddress);
            inCallIntent.putExtra("peer_name", selectedDevice.Name());
            inCallIntent.putExtra("this_device_name", thisDeviceName);
            inCallIntent.putExtra("initiator", "true");
            startActivity(inCallIntent);

            finishThis = true;

//            makeCallIntent= new Intent(Intent.ACTION_SYNC, null, this, FullDuplexNetworkAudioCallService.class);
//            makeCallIntent.putExtra("ip_address", ipAddress);
//            startService(makeCallIntent);
            selectedDevice = null;
            callInitiator = false;
        }

        //if not the call initiator, the call will be handled by the NetworkAudioCallReceiverService
//
//        // 1. Instantiate an AlertDialog.Builder with its constructor
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//// 2. Chain together various setter methods to set the dialog characteristics
//
//        builder.setMessage("Click to disconnect")
//                .setTitle("Wifi direct connected");
//
//        builder.setPositiveButton("Disconnect", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//
//                instigatedWifiDisconnect = true;
//                disconnect();
//                // User clicked OK button
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                // User cancelled the dialog
//            }
//        });
//
//// 3. Get the AlertDialog from create()
//        AlertDialog dialog = builder.create();
//        dialog.show();
    }


    //this is called after an WIFI_P2P_CONNECTION_CHANGED_ACTION
    public void handleLostConnection()
    {
        if(!instigatedWifiDisconnect)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

    // 2. Chain together various setter methods to set the dialog characteristics

            builder.setMessage("Wifi-direct connection lost")
                    .setTitle("Wifi-direct connection lost");
            builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        instigatedWifiDisconnect = false;
        //re-initiate peer discovery
        start_peer_discovery();
    }

    /*public void StopPeerDiscovery(){
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(WiFiDirectActivity.TAG,"Stopped Peer discovery");
            }

            @Override
            public void onFailure(int i) {
                Log.d(WiFiDirectActivity.TAG,"Not Stopped Peer discovery");
            }
        });
    }*/

    @Override
    protected void onResume(){
        super.onResume();
        mIntentFilter = new IntentFilter();
        mCallEndIntentFilter = new IntentFilter();

        //same intents that WifiDirectBroadcastReceiver checks for
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mWifiDirectBroadcastReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        this.registerReceiver(mWifiDirectBroadcastReceiver, mIntentFilter);

        mCallEndIntentFilter.addAction("WT.END_CALL_COMPLETE");//listens for our custom end call intent
        mCallEndBroadcastReceiver = new CallEndedBroadcastReceiver(this);
        registerReceiver(mCallEndBroadcastReceiver, mCallEndIntentFilter);
        //start_peer_discovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiDirectBroadcastReceiver);
        unregisterReceiver(mCallEndBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //this.finish();
        /*
        if(makeCallIntent != null)
        {
            stopService(makeCallIntent);
        }
        if(receiveCallIntent != null)
        {
            stopService(receiveCallIntent);
        }


        StopWifiDirect();*/

        //WifiP2pConfig config = new WifiP2pConfig();
        //config.groupOwnerIntent = 15;
    }

    ///This is called with the CALL_END_COMPLETE event
    public void EndCall()
    {
        //Thread.sleep(1000);
        Toast.makeText(SelectPeerActivity.this, "Call Ended", Toast.LENGTH_SHORT).show();
        updateStatus("Wifi direct call ended");
        //this.finish();
//        if(makeCallIntent != null)
//        {
//            stopService(makeCallIntent);
//        }
//        if(receiveCallIntent != null)
//        {
//            stopService(receiveCallIntent);
//        }

        StopWifiDirect();
        //if(instigated call), need to restart the receiver server.
        if(!instigatedCall) {
            receiveCallIntent = new Intent(Intent.ACTION_SYNC, null, this, NetworkAudioCallReceiverService.class);
            startService(receiveCallIntent);
        }
        instigatedCall = false;
        //resume searching for potential peers.
        ignoreDiscovery = false;
        start_peer_discovery();
    }

    public void StopWifiDirect() {
        //closes physical connection
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(SelectPeerActivity.this, "WD Group Deactivated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(SelectPeerActivity.this, "WD Group not Deactivated", Toast.LENGTH_SHORT).show();
//                    Activity currentActivity = ((MyApp)context.getApplicationContext()).getCurrentActivity();
//                    Toast.makeText(currentActivity, "WT: Wifi-direct handshake failed", Toast.LENGTH_SHORT).show();
            }
        });
        deletePersistentGroups();
    }

    /*
    * Disconnects from the wifi direct.
    * This should be called by the group owner
    * */
    public void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("Msg" , "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("Msg", "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }

    //Hacky to get android to forget previously connected devices and who was group owner.
    private void deletePersistentGroups(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mManager, mChannel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /* /Discovers peers for use with wifi direct */
    public void start_peer_discovery()
    {
        final int tryCounter = 0;
        //sets off the process to discover wifi peers but does not return peer list.
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                //notifies you that the discovery process succeeded.
                @Override
                public void onSuccess() {
                    //update the listbox of peers.
                    Toast.makeText(SelectPeerActivity.this, "Peer discovery initiated", Toast.LENGTH_SHORT).show();
                    updateStatus("Peer discovery initiated");
                }

                @Override
                public void onFailure(int reasonCode) {
                    peerDevices.clear();
                    //PopulatePeerView();
                    Toast.makeText(SelectPeerActivity.this, "Peer discovery failed "+ reasonCode, Toast.LENGTH_SHORT).show();
                    updateStatus("Peer discovery failed");
                    if(tryCounter < 3) {
                        start_peer_discovery();
                    }
                }
            }
        );
    }

    public void stop_peer_discovery()
    {
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });
    }

    /*
    * SetupWidgetListeners: called once when activity is spawned. Listens for when an item
    * in the PeerList is clicked.
    */
    private void  SetupWidgetListeners()
    {
        ListView peerView = (ListView) findViewById(R.id.listViewPeers);

        peerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                callInitiator = true;
                if(peerDevices.size() > position) {
                    updateStatus("Item " + position + " clicked");
                    IConnectableDevice deviceToConnect = peerDevices.get(position);
                    selectedDevice = deviceToConnect;

                    instigatedCall = true;
                    //initiate the connection: does not start sending over the socket until
                    //wifi broadcast receiver figures out the ip address.
                    deviceToConnect.InitiateConnection();
                }
                else{
                    updateStatus("Peer is no longer in range");
                    System.out.print("Log: selected peer no longer in range. Refreshing...");
                    //peerStrings.clear();
                }
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
