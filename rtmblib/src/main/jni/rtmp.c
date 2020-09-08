//
// Created by sskbskdrin on 2019/April/26.
//

#include <jni.h>
#include <string.h>
#include "librtmp/rtmp.h"
#include <sys/types.h>
#include <android/log.h>
#include <malloc.h>


#define LOG_TAG "NativeRTMP"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

char *pubRtmpUrl;
RTMP *pubRtmp;

JNIEXPORT jstring JNICALL
Java_cn_sskbskdrin_server_rtmblib_Rtmp_getNativeString(JNIEnv *env, jclass type) {
    char *result = "hello rtmp";
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT void JNICALL
Java_cn_sskbskdrin_server_rtmblib_Rtmp_init(JNIEnv *env, jclass type, jstring jRtmpUrl) {
    const char *rtmpUrl = (*env)->GetStringUTFChars(env, jRtmpUrl, 0);

    pubRtmp = RTMP_Alloc();
    RTMP_Init(pubRtmp);
    if (!RTMP_Serve(pubRtmp))//启动一个rtmp的server,此处说白了就是进行了rtmp的握手操作
    {
        LOGE("Handshake failed");
        return;
    }
    LOGI("RTMP Connect  ok");
}

JNIEXPORT void JNICALL
Java_cn_sskbskdrin_server_rtmblib_Rtmp_release(JNIEnv *env, jclass type) {
    if (RTMP_IsConnected(pubRtmp)) {
        RTMP_Close(pubRtmp);
    }
    RTMP_Free(pubRtmp);
    free(pubRtmpUrl);
}
