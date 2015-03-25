package ca.ualberta.ev3ye.camera.comm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by Yuey on 2015-03-18.
 */
public class WiFiP2PBroadcastReceiver extends BroadcastReceiver {
	protected WiFiP2PBroadcastCallbacks callbackTarget = null;

	public WiFiP2PBroadcastReceiver( WiFiP2PBroadcastCallbacks callbackTarget ) {
		super();
		this.callbackTarget = callbackTarget;
	}

	@Override
	public void onReceive( Context context, Intent intent ){
		String action = intent.getAction();

		switch ( action ){
		case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
			// Check to see if Wi-Fi is enabled and notify appropriate activity
			this.callbackTarget.onP2pStateChanged( context, intent );
			break;

		case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
			// Call WifiP2pManager.requestPeers() to get a list of current peers
			this.callbackTarget.onP2pPeersChanged( context, intent );
			break;

		case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
			// Respond to new connection or disconnections
			this.callbackTarget.onP2pConnectionChanged( context, intent );
			break;

		case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
			// Respond to this device's wifi state changing
			this.callbackTarget.onP2pThisDeviceChanged( context, intent );
			break;
		}
	}

	public interface WiFiP2PBroadcastCallbacks{
		public void onP2pStateChanged( Context context, Intent intent );
		public void onP2pPeersChanged( Context context, Intent intent );
		public void onP2pConnectionChanged( Context context, Intent intent );
		public void onP2pThisDeviceChanged( Context context, Intent intent );
	}
}
