#include <jni.h>
#include <opencv2/opencv.hpp>
#include <bits/stdc++.h>
#include <android/log.h>

#define  LOG_TAG    "JNI_PART"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG, __VA_ARGS__)
using namespace cv;
using namespace std;

extern "C"{
void Java_com_peelson_camerashot4peelson_MainActivity_nativeProcessFrame(JNIEnv *env, jobject instance,
                                                                    jlong addrGray,
                                                                    jlong addrRgba) {

    Mat &mGr = *(Mat *) addrGray;
    Mat &mRgb = *(Mat *) addrRgba;
    vector<KeyPoint> v;

/*    LOGD("%d image size %d",mGr.rows,mGr.cols);
    resize(mGr,mGr,Size(mGr.cols*480/mGr.rows,480));
    LOGD("%d image new-size %d",mGr.rows,mGr.cols);*/

    Ptr <FeatureDetector> detector = FastFeatureDetector::create(50);
    detector->detect(mGr, v);
    for (unsigned int i = 0; i < v.size(); i++) {
        const KeyPoint &kp = v[i];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(0, 255, 255, 255));

    }
}

}