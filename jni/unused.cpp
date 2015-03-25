#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT void JNICALL Java_ca_ualberta_ev3ye_camera_MainActivity_Unused(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

JNIEXPORT void JNICALL Java_ca_ualberta_ev3ye_camera_MainActivity_Unused(JNIEnv*, jobject, jlong addrGray, jlong addrRgba){
	Mat& mGr  = *(Mat*)addrGray;
	Mat& mRgb = *(Mat*)addrRgba;
	vector<KeyPoint> v;

	FastFeatureDetector detector(50);
	detector.detect(mGr, v);
	int radious = mRgb.cols / 192 + 1;
	for( unsigned int i = 0; i < v.size(); i++ ){
	    const KeyPoint& kp = v[i];
	    circle(mRgb, Point(kp.pt.x, kp.pt.y), radious, Scalar(255,0,0,255));
	}
}
}
