package ca.ualberta.ev3ye.camera.comm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Yuey on 2015-03-18.
 */
public class BluetoothCom{
	
	private static final String TAG = "BluetoothCom";
	private String robotMac = "";
	private String robotName = "";
	private BluetoothAdapter btAdapter;
	private boolean success = false;
	private boolean reConnect = false;
	private boolean isTransferingData = false;
	private BluetoothSocket btSocket = null;
	private DataOutputStream dataOut = null;
	private DataInputStream dataIn = null;

	public BluetoothCom(){
		
	}
	
	public void startBT( Activity activity ){
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if ( !btAdapter.isEnabled() ){
			Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
			activity.startActivityForResult( enableBtIntent, 1 );
		}
	}
	
	public void searchForRobot(){
		if(robotMac.equals("")){ //It's the first time it runs.
			Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
			for (BluetoothDevice device : pairedDevices) {
				checkLink(device.getName(),device.getAddress());
			}
		}else{
			checkLink(robotName,robotMac);
		}
	}
	
	private void checkLink(final String name, final String mac){
		Thread thread = new Thread() {
            public void run() {
            	Log.d(TAG, "Connecting to "+mac);
            	try {
            		BluetoothDevice btDevice = btAdapter.getRemoteDevice(mac);
            		BluetoothSocket btSock = btDevice.createRfcommSocketToServiceRecord( UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            		btSock.connect();
            		
            		DataOutputStream dOut = new DataOutputStream( btSock.getOutputStream() );
            		DataInputStream dIn = new DataInputStream( btSock.getInputStream() );
            		
            		dOut.writeUTF("Are you Robot?");
            		dOut.flush();
            		while(dIn.available()==0){//I HAVE to do this, the Smartphone is way faster than the EV3
            			Thread.sleep(1);
            		}
            		boolean confirm = dIn.readBoolean();
            		if(confirm){
            			Log.d(TAG, "Found Robot '"+name+"' at "+mac);
            			robotMac = mac;
            			robotName = name;
            			btSocket = btSock;
            			dataOut = dOut;
            			dataIn = dIn;
            			success = true;
            			return;
            		}
            		dOut.close();
            		dIn.close();
            		btSock.close();
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
            }
        };
        thread.start();
	}
	
	public void sendCommands(final String msg){
		if (isTransferingData)
			return;
		Log.d(TAG, "Sending "+msg);
		Thread thread = new Thread() {
			public void run() {
				try {
					isTransferingData=true;
					
					if(reConnect)
						reconnect();
					Log.d(TAG, "Writing");
					dataOut.writeUTF(msg);
					Log.d(TAG, "Reading");
					dataIn.readBoolean();
					isTransferingData=false;
				} catch (IOException e) {
					e.printStackTrace();
					Log.d(TAG, "Error in connection...");
					reConnect = true;
				}
			}
		};
		thread.start();
	}
	
	private void reconnect() throws IOException{
		
		BluetoothDevice btDevice = btAdapter.getRemoteDevice(robotMac);
		btSocket = btDevice.createRfcommSocketToServiceRecord( UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
		btSocket.connect();
		dataOut = new DataOutputStream( btSocket.getOutputStream() );
		dataIn = new DataInputStream( btSocket.getInputStream() );
		reConnect = false;
	}
	
	public void close(){
		if(!success)
			return;
		
		Thread thread = new Thread() {
			public void run() {
				try {
					while(isTransferingData)
						Thread.sleep(1);
					sendCommands("-1;-1;-1");
					success=false;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getRobotName() {
		return robotName;
	}

	public void setRobotName(String robotName) {
		this.robotName = robotName;
	}
	
	public String getRobotMac() {
		return robotMac;
	}

	public void setRobotMac(String robotMac) {
		this.robotMac = robotMac;
	}
	
}
