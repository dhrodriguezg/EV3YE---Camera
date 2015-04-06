package ca.ualberta.ev3ye.camera;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import ca.ualberta.ev3ye.camera.R;
import ca.ualberta.ev3ye.camera.comm.BluetoothCom;
import ca.ualberta.ev3ye.camera.comm.ServerTCP;
import ca.ualberta.ev3ye.camera.comm.WiFiP2PBroadcastReceiver;

public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener, SensorEventListener, WiFiP2PBroadcastReceiver.WiFiP2PBroadcastCallbacks {
	
    private static final String    TAG = "MainActivity";
    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_FEATURES = 1;
    
    private SoundPlayer sound = null;
    
    
    private int desiredWidth = 800; //1280-800-320
    private int desiredHeight = 450; //720-450-240
    private boolean takePicture = false;
    private boolean isRunning = false;
    private boolean startStreaming = false;
    private boolean isStreaming = false;
    
    private int mViewMode;
    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat mGray;

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemStartStreaming;
    private MenuItem               mItemPreviewFeatures;

    private CameraView   mOpenCvCameraView;
    
    private MatOfInt compression_params;
    private List<Size> mResolutionList;
    
	private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    
    //private ClientTCP clientTCP = null;
    private ServerTCP serverTCP = null;
    private int porc = 0;

    private int xpoint;
	private int ypoint;
	
	private static final boolean isWiFiDirect = false;
	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter = null;
	
	private BluetoothCom btComm = null;
	private SensorManager mSensorManager;
	private Sensor mPressure;

    @SuppressLint("ClickableViewAccessibility")
	private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("jniOCV");
                    horribleResolutionFix();
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    compression_params = new MatOfInt(Highgui.CV_IMWRITE_JPEG_QUALITY, 70);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(isWiFiDirect){
        	mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        	mChannel=mManager.initialize(this, getMainLooper(), null);
        
        	mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
        		@Override
        		public void onSuccess() {
        		}
        		@Override
        		public void onFailure(int reason) {				
        		}
        	});
        
        	mReceiver = new WiFiP2PBroadcastReceiver(this);
        	mIntentFilter = new IntentFilter();
        	mIntentFilter.addAction( WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION );
        	mIntentFilter.addAction( WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION );
        	mIntentFilter.addAction( WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION );
        	mIntentFilter.addAction( WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION );
        }
        
        btComm = new BluetoothCom();
        btComm.startBT(this);
		serverTCP = new ServerTCP(this);
		serverTCP.initGreeting();
		serverTCP.initStreaming();
		serverTCP.initController();
		updateControls();
		
        setContentView(R.layout.camera_view);
        mOpenCvCameraView = (CameraView) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        
        sound = new SoundPlayer(getApplicationContext());
    }

    @Override
    public void onPause()
    {
    	if(btComm!=null)
        	btComm.close();
        super.onPause();
        if(isWiFiDirect)
        	unregisterReceiver( mReceiver );
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mSensorManager.unregisterListener(this);
        sound.cameraOffline();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        btComm.searchForRobot(sound);
        if(isWiFiDirect){
        	this.registerReceiver( mReceiver, mIntentFilter );
            mManager.discoverPeers( mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                }
                @Override
                public void onFailure(int reasonCode) {
                }
            });
        }
        
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (serverTCP != null)
        	serverTCP.shutdown();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }
    
    @SuppressLint({ "SimpleDateFormat", "ClickableViewAccessibility" })
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.i(TAG,"onTouch event");
		//mOpenCvCameraView.toggleFlashLight(sound);
        return false;
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	Log.i(TAG, "called onCreateOptionsMenu");
    	
    	mItemPreviewRGBA = menu.add("View");
        mItemPreviewFeatures = menu.add("Features");
        mItemStartStreaming = menu.add("EV3");
        
        //Resolution
        mResolutionMenu = menu.addSubMenu("Resolutions");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];
        
        for(int idx = 0;idx<mResolutionList.size();idx++) {
            Size element = mResolutionList.get(idx);
            mResolutionMenuItems[idx] = mResolutionMenu.add(1, idx, Menu.NONE, element.width + "x" + element.height);
        }
        
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        } else if (item == mItemStartStreaming) {
        	btComm.searchForRobot(sound);
        }
        
        else if (item.getGroupId() == 1){//Change Resolution
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = resolution.width + "x" + resolution.height;
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @SuppressLint("SimpleDateFormat")
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	isRunning = true;
    	
    	
        final int viewMode = mViewMode;
        switch (viewMode) {
        case VIEW_MODE_RGBA:
            mRgba = inputFrame.rgba();
            break;
        case VIEW_MODE_FEATURES:
            mRgba = inputFrame.rgba();
            mGray = inputFrame.gray();
            Unused(mGray.getNativeObjAddr(),mRgba.getNativeObjAddr());
            break;
        }
        
        int width = (int)((float)mRgba.rows() * (float)mRgba.rows()/(float)mRgba.cols()); // Keeps the original aspect ratio.
    	int x = (100-porc)*(mRgba.cols()-width)/100;
    	Rect crop = new Rect(x,0,width,mRgba.rows());
    	
        if(serverTCP.isStreamingOnline()){ //Connected to client, stream pics
        	Mat imageCropMat= new Mat(mRgba, crop);
        	
        	Mat imageRotMat = new Mat();
        	Core.flip(imageCropMat.t(), imageRotMat, 1);
        	
        	Mat imageMat = new Mat();
        	Imgproc.cvtColor(imageRotMat, imageMat, Imgproc.COLOR_RGBA2BGR, 3);
            MatOfByte bufByte = new MatOfByte();
        	Highgui.imencode(".jpg", imageMat, bufByte, compression_params);
        	serverTCP.updateStreaming(bufByte.toArray());
        }
        
        Core.rectangle(mRgba, new Point(x,0),new Point(x+width-1,mRgba.rows()-1), new Scalar(255,0,0,255));
        return mRgba;
    }


    public native void Unused(long matAddrGr, long matAddrRgba);
    
    private void horribleResolutionFix() { //f***ng OpenCV doesn't let me change the initial resolution automatically...So I had to do it like this.
		Thread thread = new Thread() {
			public void run() {
				while(!isRunning){
					try {
						Thread.sleep(16);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				setNearestResolution(desiredWidth,desiredHeight);
				sound.cameraOnline();
			}
		};
		thread.start();
	}
    
    private void updateControls(){
    	Thread thread = new Thread() {
			public void run() {
				while(!serverTCP.isControllerOnline()){ //Waiting until controller connection is made
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				while(serverTCP.isControllerOnline()){
					
					try {
						Thread.sleep(10); //100FPS
						serverTCP.updateControls();
						
						while(!serverTCP.hasControlChanged()){//Wait until is has finished reading...
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						if(serverTCP.hasControlChanged()){
							//:operator;leftPower;rightPower;cameraHeight
							String[] control = serverTCP.getControls().split(":");
							String[] command = control[control.length-1].split(";");
							int operator = Integer.parseInt(command[0]);
							if(operator<5){
								porc = Integer.parseInt(command[3]);
								if(btComm.isSuccess())
									btComm.sendCommands(control[control.length-1]);
							}else if(operator==5){ //Resolutions
								String[] width_height=command[1].split("x");
								setNearestResolution(Integer.parseInt(width_height[0]), Integer.parseInt(width_height[1]));
							}else if(operator==6){ //Flash
								if(command[1].equals("ON"))
									mOpenCvCameraView.turnFlashLightOn(sound);
								else if(command[1].equals("OFF"))
									mOpenCvCameraView.turnFlashLightOff(sound);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}	
				}
			}
		};
		thread.start();
    }
    
    private void setNearestResolution(int width, int height){
    	mResolutionList = mOpenCvCameraView.getResolutionList();
		int minError = Integer.MAX_VALUE;
		Size nearestResolution = null;
		for(int n = 0; n<mResolutionList.size(); n++){
			Size resolution = mResolutionList.get(n);
			int error = Math.abs(resolution.width-width)+Math.abs(resolution.height-height);
			if(error<minError){
				minError=error;
				nearestResolution=resolution;
			}
		}
		mOpenCvCameraView.setResolution(nearestResolution);
		String caption = nearestResolution.width + "x" + nearestResolution.height;
		Log.i(TAG, "Resolution: "+caption);
    }
    
    public List<Size> getmResolutionList() {
		return mResolutionList;
	}

	public void setmResolutionList(List<Size> mResolutionList) {
		this.mResolutionList = mResolutionList;
	}

	@Override
	public void onP2pStateChanged(Context context, Intent intent) {
	}

	@Override
	public void onP2pPeersChanged(Context context, Intent intent) {
	}

	@Override
	public void onP2pConnectionChanged(Context context, Intent intent) {
	}

	@Override
	public void onP2pThisDeviceChanged(Context context, Intent intent) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		//Log.e(TAG,"something changed");
		if(event.sensor.getType()==Sensor.TYPE_LIGHT){
			//float light = event.values[0];
			/*if (light<30){
				mOpenCvCameraView.turnFlashLightOn(sound);
			}else{
				mOpenCvCameraView.turnFlashLightOff(sound);
			}*/
		}else if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
			if (Math.abs(event.values[1]) < 2)
				sound.ev3Down();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
}
