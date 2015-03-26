package ca.ualberta.ev3ye.camera.comm;


import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTCP {
	
	private static final String TAG = "ServerTCP";
	private static final int GREETING_PORT = 7777;
	private static final int STREAMING_PORT = 8888;
	private static final int CONTROLLER_PORT = 9999;
    
	private ServerSocket greetingSocket = null;
	private ServerSocket streamingSocket = null;
	private ServerSocket controllerSocket = null;
	
	private boolean serverOnline = false;
	
    private Socket clientStreamingSocket = null;
    private DataInputStream dataStreamingInput = null;
    private DataOutputStream dataStreamingOutput = null;
    private boolean isTransferingStreaming = false;
    private boolean streamingOnline = false;
    
    private Socket clientControllerSocket = null;
    private DataInputStream dataControllerInput = null;
    private DataOutputStream dataControllerOutput = null;
    private boolean isTransferingController = false;
    private boolean controllerOnline = false;
    
    private byte[] picture = null;
    private String controls = "";
    private boolean controlChanged = false;
    
	public ServerTCP(){
		try {
			greetingSocket = new ServerSocket(GREETING_PORT);
			streamingSocket = new ServerSocket(STREAMING_PORT);
			controllerSocket = new ServerSocket(CONTROLLER_PORT);
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
			if(!streamingSocket.isClosed())
				streamingSocket.close();
			if(!controllerSocket.isClosed())
				controllerSocket.close();
			serverOnline = false;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("**** Error shutting down the server: "+STREAMING_PORT);
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
            		clientStreamingSocket = streamingSocket.accept(); // This is blocking. It will wait.
            		dataStreamingOutput = new DataOutputStream(clientStreamingSocket.getOutputStream());
            		dataStreamingInput = new DataInputStream(clientStreamingSocket.getInputStream());
        			clientStreamingSocket.setKeepAlive(true);
        			streamingOnline = true;
            	} catch (IOException e) {
        			e.printStackTrace();
        			streamingOnline = false;
        		} 
            }
        };
        thread.start();
	}
	
	public void initController(){
		Thread thread = new Thread() {
            public void run() {
            	try {
            		Log.i("Control", "Waiting for controller");
            		clientControllerSocket = controllerSocket.accept(); // This is blocking. It will wait.
            		dataControllerOutput = new DataOutputStream(clientControllerSocket.getOutputStream());
            		dataControllerInput = new DataInputStream(clientControllerSocket.getInputStream());
        			clientControllerSocket.setKeepAlive(true);
        			controllerOnline = true;
        			Log.i("Control", "Controller stabliesh");
            	} catch (IOException e) {
        			e.printStackTrace();
        			controllerOnline = false;
        		} 
            }
        };
        thread.start();
	}
	
	
	
	public boolean updateStreaming(byte[] picture){
		if(isTransferingStreaming)
			return false; //Cannot send right now, busy.
		this.picture = picture;
		Thread thread = new Thread() {
			public void run() {
				updateSreaming();
			}
		};
		thread.start();
		return true;
	}
	
	private boolean updateSreaming(){
		
		boolean requestCompleted = false;
		boolean reconnect = false;
		isTransferingStreaming = true;
		int requestNumber = 0;
		
		if(clientStreamingSocket==null)
			reconnect = true;
		
		while (!requestCompleted && requestNumber++ < 100){ //100 tries
			Log.i(TAG, "Sending Data to server...");
			try {
				
				if(reconnect){
					
					if(clientStreamingSocket!=null && !clientStreamingSocket.isClosed())
						clientStreamingSocket.close();
					Log.e(TAG, "Client disconnected, connecting...");
					clientStreamingSocket = streamingSocket.accept();
					dataStreamingOutput = new DataOutputStream(clientStreamingSocket.getOutputStream());
					dataStreamingInput = new DataInputStream(clientStreamingSocket.getInputStream());
					clientStreamingSocket.setKeepAlive(true);
					streamingOnline = true;
					reconnect = false;
					Log.i(TAG, "Client connected");
				}
				
				//Transfering picture
				dataStreamingOutput.writeInt(picture.length);
				dataStreamingOutput.write(picture);
				dataStreamingOutput.flush();
				
				//Reciving controls
				while(dataStreamingInput.available()==0){ //maybe this device is going too fast, so wait until there is new data...
					Thread.sleep(1);
				}
				dataStreamingInput.readBoolean(); //receive ACK, just for sync
				
				//Data transfer completed
				requestCompleted = true;
				Log.i(TAG, "Data sendt successfully, tries: "+requestNumber);
			} catch (IOException e) {
				Log.e(TAG, "Sudden disconnection from the Server °O° ");
				e.printStackTrace();
				reconnect = true;
				streamingOnline = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isTransferingStreaming = false;
		return requestCompleted;
	}
	
	public boolean updateControls(){
		if(isTransferingController)
			return false; //Cannot send right now, busy.
		
		Thread thread = new Thread() {
			public void run() {
				updateControls(false);
			}
		};
		thread.start();
		return true;
	}
	
	public void updateControls(boolean unused){
		
		boolean requestCompleted = false;
		boolean reconnect = false;
		isTransferingController = true;
		int requestNumber = 0;
		
		if(clientControllerSocket==null)
			reconnect = true;
		
		while (!requestCompleted && requestNumber++ < 100){ //100 tries
			Log.i(TAG, "Getting Controller from server...");
			try {
				
				if(reconnect){
					
					if(clientControllerSocket!=null && !clientControllerSocket.isClosed())
						clientControllerSocket.close();
					Log.e(TAG, "Controller disconnected, connecting...");
					clientControllerSocket = controllerSocket.accept();
					dataControllerOutput = new DataOutputStream(clientControllerSocket.getOutputStream());
					dataControllerInput = new DataInputStream(clientControllerSocket.getInputStream());
					clientControllerSocket.setKeepAlive(true);
					controllerOnline = true;
					reconnect = false;
					Log.i(TAG, "Controller connected");
				}
				
				//Reciving controls
				while(dataControllerInput.available()==0){ //maybe this device is going too fast, so wait until there is new data...
					Thread.sleep(1);
				}
				controls=dataControllerInput.readUTF();
				
				//Sending ACK
				dataControllerOutput.writeBoolean(true);
				dataControllerOutput.flush();
				
				//Data transfer completed
				requestCompleted = true;
				controlChanged = true;
				Log.i(TAG, "Controller received successfully, tries: "+requestNumber);
			} catch (IOException e) {
				Log.e(TAG, "Sudden disconnection from the Controller client °O° ");
				e.printStackTrace();
				reconnect = true;
				controllerOnline = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isTransferingController = false;
	//	return requestCompleted;
	}
	
	
	public boolean isServerOnline() {
		return serverOnline;
	}


	public void setServerOnline(boolean serverOnline) {
		this.serverOnline = serverOnline;
	}

	public String getControls() {
		controlChanged=false;
		return controls;
	}

	public void setControls(String controls) {
		this.controls = controls;
	}

	public boolean isStreamingOnline() {
		return streamingOnline;
	}

	public void setStreamingOnline(boolean clientOnline) {
		this.streamingOnline = clientOnline;
	}

	public boolean isControllerOnline() {
		return controllerOnline;
	}

	public void setControllerOnline(boolean controllerOnline) {
		this.controllerOnline = controllerOnline;
	}

	public boolean isTransferingController() {
		return isTransferingController;
	}

	public void setTransferingController(boolean isTransferingController) {
		this.isTransferingController = isTransferingController;
	}

	public boolean hasControlChanged() {
		return controlChanged;
	}

	public void setControlChanged(boolean controlChanged) {
		this.controlChanged = controlChanged;
	}

}