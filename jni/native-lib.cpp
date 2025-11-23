#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeviewer_MainActivity_processImageJNI(
        JNIEnv* env,
        jobject,
        jbyteArray input,
        jint width,
        jint height) {

    jbyte* data = env->GetByteArrayElements(input, nullptr);

    cv::Mat yuv(height + height/2, width, CV_8UC1, (unsigned char*)data);
    cv::Mat bgr;
    cv::cvtColor(yuv, bgr, cv::COLOR_YUV2BGR_NV21);

    cv::Mat edges;
    cv::Canny(bgr, edges, 80, 150);

    cv::Mat output;
    cv::cvtColor(edges, output, cv::COLOR_GRAY2BGRA);

    jbyteArray result = env->NewByteArray(output.total() * 4);
    env->SetByteArrayRegion(result, 0, output.total() * 4, (jbyte*)output.data);

    env->ReleaseByteArrayElements(input, data, 0);

    return result;
}
