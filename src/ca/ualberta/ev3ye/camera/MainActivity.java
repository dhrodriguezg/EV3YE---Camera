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
import ca.ualberta.ev3ye.camera.comm.ClientTCP;
import ca.ualberta.ev3ye.camera.comm.ServerTCP;
import ca.ualberta.ev3ye.camera.comm.WiFiP2PBroadcastReceiver;

public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener, WiFiP2PBroadcastReceiver.WiFiP2PBroadcastCallbacks {
	
    private static final String    TAG = "MainActivity";

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_FEATURES = 1;
    
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
	
	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	protected IntentFilter mIntentFilter = null;

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
        
		serverTCP = new ServerTCP();
		serverTCP.initGreeting();
		serverTCP.initStreaming();
		
		
        setContentView(R.layout.camera_view);
        mOpenCvCameraView = (CameraView) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver( mReceiver );
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.registerReceiver( mReceiver, mIntentFilter );
        mManager.discoverPeers( mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFailure(int reasonCode) {
            }
        });
        
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
		mOpenCvCameraView.toggleFlashLight();
		/*
		//Top-left = (0,0)  User's point
		float x = event.getX();
		float y = event.getY();
		//Log.i(TAG, "X="+x+" Y="+y);
		
		//Screen Resolution
		Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		float widthS = size.x;
		float heightS = size.y;
		//Log.i(TAG, "XS="+widthS+" YS="+heightS);
		
		//Image Resolution
		float heightI = mOpenCvCameraView.getResolution().height;
		float widthI = mOpenCvCameraView.getResolution().width;
		//Log.i(TAG, "XI="+widthI+" YI="+heightI);
		
		xpoint =(int) (x*widthI/widthS);
		ypoint = (int) (y*heightI/heightS);
		//Log.i(TAG, "XP="+xpoint+" YP="+ypoint);
		*/
        return false;
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	Log.i(TAG, "called onCreateOptionsMenu");
    	
    	mItemPreviewRGBA = menu.add("Normal View");
        mItemPreviewFeatures = menu.add("View Features");
        mItemStartStreaming = menu.add("Begin");
        
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
        	//clientTCP = new ClientTCP();
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
    	
        if(serverTCP.isClientOnline()){ //Connected to client, stream pics
        	Mat imageCropMat= new Mat(mRgba, crop);
        	
        	Mat imageRotMat = new Mat();
        	Core.flip(imageCropMat.t(), imageRotMat, 1);
        	
        	Mat imageMat = new Mat();
        	Imgproc.cvtColor(imageRotMat, imageMat, Imgproc.COLOR_RGBA2BGR, 3);
            MatOfByte bufByte = new MatOfByte();
        	Highgui.imencode(".jpg", imageMat, bufByte, compression_params);
        	serverTCP.updateStreaming(bufByte.toArray());
        	porc = Integer.parseInt(serverTCP.getControls());
        }
        Core.rectangle(mRgba, new Point(x,0),new Point(x+width-1,mRgba.rows()-1), new Scalar(255,0,0,255));
        return mRgba;
    }


    public native void Unused(long matAddrGr, long matAddrRgba);
    
    public void horribleResolutionFix() { //f***ng OpenCV doesn't let me change the initial resolution automatically...So I had to do it like this.
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
}
