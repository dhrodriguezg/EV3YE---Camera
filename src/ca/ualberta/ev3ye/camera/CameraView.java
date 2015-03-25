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
    
	public void toggleFlashLight() {
		Parameters params = mCamera.getParameters();
		if (isFlashLightON) {
			isFlashLightON = false;
			params.setFlashMode(Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(params);
		} else {
			isFlashLightON = true;
			params.setFlashMode(Parameters.FLASH_MODE_TORCH);
			mCamera.setParameters(params);
		}
	}
	
}
