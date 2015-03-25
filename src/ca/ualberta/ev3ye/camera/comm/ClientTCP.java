package ca.ualberta.ev3ye.camera.comm;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.util.Log;

public class ClientTCP {
	
	private static final String TAG = "ClientTCP";
	private final String SERVER_IP="192.168.43.248";
	private final int SERVER_PORT = 8888;
	private Socket clientSocket = null;
	private DataOutputStream dataOutput = null;
	private DataInputStream dataInput = null;
	private boolean isSendingData = false;
	private byte[] picture = null;
	private String controls = "0";
	
	public ClientTCP(){
	}
	
	public boolean connect2Server(){
		try {
			clientSocket = new Socket(SERVER_IP, SERVER_PORT);
			dataOutput = new DataOutputStream(clientSocket.getOutputStream());
			dataInput = new DataInputStream(clientSocket.getInputStream());
			clientSocket.setKeepAlive(true);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "*** Connection to the Server denied: "+SERVER_IP+":"+SERVER_PORT);
		}
		return false;
	}
	
	public boolean updateData(byte[] picture){
		if(isSendingData)
			return false; //Cannot send right now
		
		this.picture = picture;
		Thread thread = new Thread() {
			public void run() {
				makeClientRequest();
				updateControls();
			}
		};
		thread.start();
		return true;
	}
	
	private boolean updateControls(){
		// TODO  controls make the code to update controls.....
		return true;
	}
	private boolean makeClientRequest(){
		
		boolean requestCompleted = false;
		boolean reconnect = false;
		isSendingData = true;
		int requestNumber = 0;
		
		if(clientSocket==null)
			reconnect=!connect2Server();
		
		while (!requestCompleted && requestNumber++ < 100){ //100 tries
			Log.i(TAG, "Sending Data to server...");
			try {
				
				if(reconnect){
					
					if(clientSocket!=null && !clientSocket.isClosed())
						clientSocket.close();
					Log.e(TAG, "***Client disconnected, connecting...");
					clientSocket = new Socket(SERVER_IP, SERVER_PORT);
					dataOutput = new DataOutputStream(clientSocket.getOutputStream());
					dataInput = new DataInputStream(clientSocket.getInputStream());
					clientSocket.setKeepAlive(true);
					Log.i(TAG, "***Client connected");
					reconnect = false;
				}
				
				//Transfering picture
				dataOutput.writeInt(picture.length);
				dataOutput.write(picture);
				dataOutput.flush();
				
				//Reciving controls
				while(dataInput.available()==0){ //maybe this device is going too fast, so wait until there is new data...
					Thread.sleep(1);
				}
				setControls(dataInput.readUTF());
				
				//Data transfer completed
				requestCompleted = true;				
				Log.i(TAG, "***Data send successfully, tries: "+requestNumber);
			} catch (IOException e) {
				Log.e(TAG, "***Sudden disconnection from the Server °O° ");
				e.printStackTrace();
				reconnect = true;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isSendingData = false;
		return requestCompleted;
	}
	
	public void shutdown(){
		try {
			if(clientSocket!=null && !clientSocket.isClosed())
				clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "*** Client socket not closed properly, check port availability: "+SERVER_PORT);
		}
		clientSocket=null;
	}
	
	
	
	
	
	//check later how to use these methods for broadcasting.
	public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface
	                .getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {}
	    return null;
	}
	
	public static String getBroadcast(){
	    String found_bcast_address=null;
	     System.setProperty("java.net.preferIPv4Stack", "true"); 
	        try
	        {
	          Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
	          while (niEnum.hasMoreElements())
	          {
	            NetworkInterface ni = niEnum.nextElement();
	            if(!ni.isLoopback()){
	                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses())
	                {

	                  found_bcast_address = interfaceAddress.getBroadcast().toString();
	                  found_bcast_address = found_bcast_address.substring(1);

	                }
	            }
	          }
	        }
	        catch (SocketException e)
	        {
	          e.printStackTrace();
	        }

	        return found_bcast_address;
	}

	public String getControls() {
		return controls;
	}

	public void setControls(String controls) {
		this.controls = controls;
	}
	
	
}