package com.piccoli.Walkie_Talkie;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.content.ContentValues.TAG;

@TargetApi(16)
public class SelectPeerActivity extends Activity {
    String thisDeviceName;
    WifiP2pDevice selectedDevice;
    WifiP2pDnsSdServiceRequest serviceRequest;
    WifiP2pManager.DnsSdTxtRecordListener txtListener;
    DnsSdServiceResponseListener servListener;
    boolean ignoreDiscovery = false;
    //ArrayList<String> peerStrings;
    WifiP2pManager mManager;
    WifiManager mWifiManager;
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
    WifiP2pDnsSdServiceInfo serviceInfo;
    PeerItemAdapter peersAdapter;
    ListView peerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SetPeerList();//resets peerDevices instance variable
        setContentView(R.layout.activity_initiate_hello);
        ArrayList<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();
        peersAdapter = new PeerItemAdapter(this,  peerDevices);
        peerView = (ListView) findViewById(R.id.listViewPeers);
        peerView.setAdapter(peersAdapter);
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
        //mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //mWifiManager.setWifiEnabled(true);

        //clearing existing physical layer connections
        /*mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {

                Log.d("Msg" , "removeGroup onSuccess -");
            }

            @Override
            public void onFailure(int reason) {
                //Log.d("Msg", "removeGroup onFailure -" + reason);
            }
        });*/


        //disconnect any existing physical layer connections.
        //disconnect();
        deletePersistentGroups();

        //startRegistration("daniel");
        setupServiceDiscoveryListeners();
        //start_peer_discovery();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //PopulatePeerView();
        //SetupWidgetListeners();
        //SetPeerList();
    }

    /// Broadcast the service availability
    private void startRegistration(String user) {
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", String.valueOf(1090));
        record.put("buddyname", user);
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
         serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_wt", "wt.udp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,F
                // Unless you want to update the UI or add logging statements.
                Log.d("Msg", "service registration success");
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Log.d("Msg", "service registration onFailure -" + arg0);
            }
        });
    }

    public void SetDeviceName(String devName)
    {
        thisDeviceName = devName;
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
            //start_peer_discovery();
            updateStatus("Wifi direct enabled");
        }
    }

    public void updateStatus(CharSequence info)
    {
        TextView txtStatus = (TextView) findViewById(R.id.textViewStatus);

        txtStatus.setText(info);

    }

    //public void clearPeerView

    /*
    *  startAudioCapture: this is called just after the wifi-direct connection has been established
    *   - parameter ipAddress (the ip address of the wifi direct group owner.
     *  - Updates the screen
     *  - starts the inCallIntent if this is the call initiator
     *  This should be called on both the server and the client, but the call will only be
     *  started on the initiator.
    * */
    public void initiateCall(String ipAddress)
    {
        updateStatus("Connected over Wifi direct");
        //stop discovery - either by setting ignoreDiscovery flag or by telling API to stop
        //stop_peer_discovery();
        ignoreDiscovery = true;
        //peerStrings.clear();

        peersAdapter.clear();
        //PopulatePeerView();

        if(callInitiator)
        {//start a call as client if the peer was selected on this device.
            updateStatus("Connecting to " + ipAddress);
            //start in-call activity
            Intent inCallIntent = new Intent(this, InCallActivity.class);
            inCallIntent.putExtra("ip_address", ipAddress);
            inCallIntent.putExtra("peer_name", selectedDevice.deviceName);
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

    }


    //this is called after an WIFI_P2P_CONNECTION_CHANGED_ACTION
    public void handleLostConnection()
    {
        if(!instigatedWifiDisconnect)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

    // 2. Chain together various setter methods to set the dialog characteristics

            builder.setMessage("Call ended")
                    .setTitle("Call ended");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        instigatedWifiDisconnect = false;
        //re-initiate peer discovery
        //setupServiceDiscoveryListeners();//start_peer_discovery();
    }

    @Override
    protected void onResume(){
        super.onResume();

        mCallEndIntentFilter.addAction("WT.END_CALL_COMPLETE");//listens for our custom end call intent
        //mCallEndBroadcastReceiver = new CallEndedBroadcastReceiver(this);
        //registerReceiver(mCallEndBroadcastReceiver, mCallEndIntentFilter);
        mWifiDirectBroadcastReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        mCallEndBroadcastReceiver = new CallEndedBroadcastReceiver(this);
        registerReceiver(mWifiDirectBroadcastReceiver, mIntentFilter);
        registerReceiver(mCallEndBroadcastReceiver, mCallEndIntentFilter);
        //start_peer_discovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mWifiDirectBroadcastReceiver);
        unregisterReceiver(mCallEndBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();

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

        //StopWifiDirect(); don't need this because it's already stopped
        //disconnect any existing physical layer connections.
        //disconnect();
        //if(instigated call), need to restart the receiver server.
        //if(!instigatedCall) {
        //    receiveCallIntent = new Intent(Intent.ACTION_SYNC, null, this, NetworkAudioCallReceiverService.class);
        //    startService(receiveCallIntent);
        //}
        instigatedCall = false;
        //resume searching for potential peers.
        ignoreDiscovery = false;
        disconnect();
        //setupServiceDiscoveryListeners();//start_peer_discovery();
        setupServiceDiscoveryListeners();
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
                    Toast.makeText(SelectPeerActivity.this, "WT: Wifi-direct handshake failed: "+i, Toast.LENGTH_SHORT).show();
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
                    if (group != null && mManager != null && mChannel != null) {
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

    final HashMap<String, String> buddies = new HashMap<String, String>();
    private void setupServiceDiscoveryListeners()
    {
        //peersAdapter.clear();

        txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            /* Callback includes:
             * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
             * record: TXT record dta as a map of key/value pairs.
             * device: The device running the advertised service.
             */
            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());

                if(fullDomain.contains("wt.udp")) {
                    buddies.put(device.deviceAddress, (String) record.get("buddyname"));
                }
            }
        };

        servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice service) {

                if(registrationType.contains("wt.udp")) {
                    // Update the device name with the human-friendly version from
                    // the DnsTxtRecord, assuming one arrived.
                    service.deviceName = buddies
                            .containsKey(service.deviceAddress) ? buddies
                            .get(service.deviceAddress) : service.deviceName;

                    // Add to the custom adapter defined specifically for showing
                    // wifi devices.

                    boolean devicePreviouslyFound = false;
                    for (int i = 0; i < peersAdapter.getCount(); i++) {
                        WifiP2pDevice device = peersAdapter.getItem(i);
                        if (device.deviceAddress.equals(service.deviceAddress)) {
                            device.deviceName = service.deviceName;
                            devicePreviouslyFound = true;
                        }
                    }
                    if (!devicePreviouslyFound) {
                        peersAdapter.add(service);
                    }
                    peersAdapter.notifyDataSetChanged();
                    Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                }
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
    }

    /* /Discovers peers for use with wifi direct */
    public void start_peer_discovery()
    {
        final int tryCounter = 0;
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        //sets off the process to discover wifi peers but does not return peer list.
        mManager.addServiceRequest(mChannel,
                serviceRequest,
                new WifiP2pManager.ActionListener() {
                //notifies you that the discovery process succeeded.
                @Override
                public void onSuccess() {
                    //update the listbox of peers.
                    Toast.makeText(SelectPeerActivity.this, "Peer discovery initiated", Toast.LENGTH_SHORT).show();
                    updateStatus("Searching for nearby walkies...");
                }

                @Override
                public void onFailure(int reasonCode) {
                    peersAdapter.clear();
                    //PopulatePeerView();
                    Toast.makeText(SelectPeerActivity.this, "Peer discovery failed "+ reasonCode, Toast.LENGTH_SHORT).show();
                    updateStatus("Peer discovery failed");
                    if(tryCounter < 3) {
                        start_peer_discovery();
                    }
                }
            }
        );
        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Success!
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(TAG, "P2P isn't supported on this device.");
                }
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
                if(peersAdapter.getCount() > position) {
                    updateStatus("Item " + position + " clicked");
                    selectedDevice = (WifiP2pDevice)adapterView.getSelectedItem();


                    instigatedCall = true;
                    //initiate the connection: does not start sending over the socket until
                    //wifi broadcast receiver figures out the ip address.
                    EstablishPhysicalLayerConnection(selectedDevice);
                }
                else{
                    updateStatus("Peer is no longer in range");
                    System.out.print("Log: selected peer no longer in range. Refreshing...");
                    //peerStrings.clear();
                }
            }
        });
    }

    public void EstablishPhysicalLayerConnection(WifiP2pDevice device)
    {
        if(device.status == WifiP2pDevice.UNAVAILABLE)
        {
            Toast.makeText(SelectPeerActivity.this, "Peer unavailable", Toast.LENGTH_SHORT).show();
        }
        else if(device.status == WifiP2pDevice.AVAILABLE)
        {
            final WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            //helps in negotiating who will be the wifi group owner.
            Random r = new Random();
            config.groupOwnerIntent = 1;

            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                //success logic: get the IP of the wifi direct group owner
                @Override
                public void onSuccess() {
                   Toast.makeText(SelectPeerActivity.this, "Requesting wifi connection", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    //failure logic
                    Toast.makeText(SelectPeerActivity.this, "WT: Unable to request wifi connection", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if (device.status == WifiP2pDevice.CONNECTED)
        {
            Toast.makeText(SelectPeerActivity.this, "Wifi direct connection already established. Resetting connection", Toast.LENGTH_SHORT).show();
            disconnect();
            //setupServiceDiscoveryListeners();
        }
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

    public void searchButtonPressed(View view) {
        EditText textEditor = (EditText) findViewById(R.id.myNameEdit);
        String userName = textEditor.getText().toString();
        Context context = view.getContext();
        if(userName.equals(""))
        {
            Toast.makeText(SelectPeerActivity.this, "Type a name so that your friends can find you", Toast.LENGTH_SHORT).show();
        }
        else
        {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
            startRegistration(userName);
            start_peer_discovery();
            //set up callback methods to notify application of available services.
            textEditor.setEnabled(false);
            view.setEnabled(false);

            //start server
            //TODO: move this to a 'start on boot' service.
            receiveCallIntent = new Intent(Intent.ACTION_SYNC, null, this, NetworkAudioCallReceiverService.class);
            startService(receiveCallIntent);
        }
    }

    public void connectClicked(View view) {
        Button button = (Button)view;
        //CharSequence selectedItem = button.getText();
        int position = (Integer) view.getTag();
        PeerItemAdapter peerItemAdapter = (PeerItemAdapter)peerView.getAdapter();
        WifiP2pDevice selectedPeer = peerItemAdapter.getItem(position);

        if(selectedPeer != null)
        {
            Toast.makeText(SelectPeerActivity.this, "Requesting connection to "+selectedPeer.deviceName, Toast.LENGTH_SHORT).show();
            instigatedCall = true;
            callInitiator = true;
            //initiate the connection: does not start sending over the socket until
            //wifi broadcast receiver figures out the ip address.
            //selectedPeer.InitiateConnection();
            EstablishPhysicalLayerConnection(selectedPeer);
            selectedDevice = selectedPeer;
        }
    }
}
