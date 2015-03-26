package ca.ualberta.ev3ye.camera.comm;


import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTCP {
	
	private static final String TAG = "ServerTCP";
	private static final int GREETING_PORT = 5555;
	private static final int TRANSFER_PORT = 8888;
    
	private ServerSocket greetingSocket = null;
	private ServerSocket transferSocket = null;
	
	private boolean serverOnline = false;
	private boolean clientOnline = false;
    
    private Socket clientSocket = null;
    private DataInputStream dataInput = null;
    private DataOutputStream dataOutput = null;
    private boolean isTransferingData = false;
    private byte[] picture = null;
    private String controls = "0";
    
	public ServerTCP(){
		try {
			greetingSocket = new ServerSocket(GREETING_PORT);
			transferSocket = new ServerSocket(TRANSFER_PORT);
			serverOnline = true;
            Log.d(TAG,"connected...");
        } catch (IOException e) {
			serverOnline = false;
			e.printStackTrace();
		}
	}
	
	public void shutdown(){
		try {
			if(!greetingSocket.isClosed())
				greetingSocket.close();
			if(!transferSocket.isClosed())
				transferSocket.close();
			serverOnline = false;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("**** Error shutting down the server: "+TRANSFER_PORT);
		}
	}
	
	public void initGreeting(){
		Thread thread = new Thread() {
            public void run() {
            	try {
            		System.out.println("Waiting for greeting");
            		for (int i=0; i < 100;i++){
            			Socket clientGreeting = greetingSocket.accept(); // This is blocking. It will wait.
                		DataInputStream dataInput = new DataInputStream(clientGreeting.getInputStream());
                		DataOutputStream dataOutput = new DataOutputStream(clientGreeting.getOutputStream());
                		
                		String greeting = dataInput.readUTF();
                		if(greeting.equals("Are you EV3 Camera?")){
                			dataOutput.writeBoolean(true);
                			dataOutput.writeUTF("This is a BT device I swear..");
                		}else{
                			dataOutput.writeBoolean(false);
                		}
                		dataOutput.flush();
                		
                		dataOutput.close();
                		dataInput.close();
                		clientGreeting.close();
            		}
            		
        		} catch (IOException e) {
        			e.printStackTrace();
        		} 
            }
        };
        thread.start();
	}
	
	public void initStreaming(){
		Thread thread = new Thread() {
            public void run() {
            	try {
            		clientSocket = transferSocket.accept(); // This is blocking. It will wait.
            		dataOutput = new DataOutputStream(clientSocket.getOutputStream());
            		dataInput = new DataInputStream(clientSocket.getInputStream());
        			clientSocket.setKeepAlive(true);
        			clientOnline = true;
            	} catch (IOException e) {
        			e.printStackTrace();
        			clientOnline = false;
        		} 
            }
        };
        thread.start();
	}
	
	public boolean updateStreaming(byte[] picture){
		
		if(isTransferingData)
			return false; //Cannot send right now, busy.
		
		this.picture = picture;
		Thread thread = new Thread() {
			public void run() {
				updateSreaming();
				updateControls();
			}
		};
		thread.start();
		return true;
	}
	
	private boolean updateSreaming(){
		
		boolean requestCompleted = false;
		boolean reconnect = false;
		isTransferingData = true;
		int requestNumber = 0;
		
		if(clientSocket==null)
			reconnect = true;
		
		while (!requestCompleted && requestNumber++ < 100){ //100 tries
			Log.i(TAG, "Sending Data to server...");
			try {
				
				if(reconnect){
					
					if(clientSocket!=null && !clientSocket.isClosed())
						clientSocket.close();
					Log.e(TAG, "Client disconnected, connecting...");
					clientSocket = transferSocket.accept();
					dataOutput = new DataOutputStream(clientSocket.getOutputStream());
					dataInput = new DataInputStream(clientSocket.getInputStream());
					clientSocket.setKeepAlive(true);
					clientOnline = true;
					reconnect = false;
					Log.i(TAG, "Client connected");
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
				Log.i(TAG, "Data sendt successfully, tries: "+requestNumber);
			} catch (IOException e) {
				Log.e(TAG, "Sudden disconnection from the Server °O° ");
				e.printStackTrace();
				reconnect = true;
				clientOnline = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isTransferingData = false;
		return requestCompleted;
	}
	
	public void updateControls(){
		// controls send to ev3 and other stuff....
	}
	
	
	public boolean isServerOnline() {
		return serverOnline;
	}


	public void setServerOnline(boolean serverOnline) {
		this.serverOnline = serverOnline;
	}

	public String getControls() {
		return controls;
	}

	public void setControls(String controls) {
		this.controls = controls;
	}

	public boolean isClientOnline() {
		return clientOnline;
	}

	public void setClientOnline(boolean clientOnline) {
		this.clientOnline = clientOnline;
	}

}