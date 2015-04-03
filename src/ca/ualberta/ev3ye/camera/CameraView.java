package ca.ualberta.ev3ye.camera;

import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;

public class CameraView extends JavaCameraView {
	
	private boolean isFlashLightON = false; 

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }
    
    public void turnFlashLightOn(SoundPlayer sp){
    	if(!isFlashLightON){
    		Parameters params = mCamera.getParameters();
    		params.setFlashMode(Parameters.FLASH_MODE_TORCH);
			mCamera.setParameters(params);
			sp.lightOn();
			isFlashLightON=true;
    	}
    }
    public void turnFlashLightOff(SoundPlayer sp){
    	if(isFlashLightON){
    		Parameters params = mCamera.getParameters();
    		params.setFlashMode(Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(params);
			sp.lightOff();
			isFlashLightON=false;
    	}
    }
    
	public void toggleFlashLight(SoundPlayer sp) { 
		if (isFlashLightON)
			turnFlashLightOff(sp);
		else
			turnFlashLightOn(sp);
		
	}
	
}
