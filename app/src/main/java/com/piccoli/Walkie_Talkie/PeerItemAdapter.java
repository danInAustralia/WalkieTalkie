package com.piccoli.Walkie_Talkie;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class PeerItemAdapter extends ArrayAdapter<WifiP2pDevice> {
    public PeerItemAdapter(Context context, ArrayList<WifiP2pDevice> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        WifiP2pDevice peer = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_peer, parent, false);
        }
        // set the text of the button to the peer
        Button connectButton = (Button) convertView.findViewById(R.id.btnConnect);
        //TextView tvHome = (TextView) convertView.findViewById(R.id.tvHome);
        // Populate the data into the template view using the data object
        connectButton.setText(peer.deviceName);

        connectButton.setTag(position);
//        LayoutInflater inflater = LayoutInflater.from(this.getContext());//getLayoutInflater();
//        View row = inflater.inflate(R.layout.item_peer, parent, false);
//        Button connect_btn = (Button) row.findViewById(R.id.btnConnect);
//        connectButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                //...
//                int position = (Integer) v.getTag();
//                IConnectableDevice device = getItem(position);
//                Context context = MainActivity.this;
//                context.updateStatus("Item " + position + " clicked");
//                System.out.println("");
//            }
//        });
        //tvHome.setText(peer.hometown);
        // Return the completed view to render on screen
        return convertView;
    }
}
