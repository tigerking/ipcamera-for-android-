#include "ipcamera.h"


#define  JNIDEFINE(fname) Java_teaonly_projects_droidipcam_NativeAgent_##fname

extern "C" {
    JNIEXPORT jint JNICALL JNIDEFINE(nativeCheckMedia)(JNIEnv* env, jclass clz, jstring file_path);
};

static std::string convert_jstring(JNIEnv *env, const jstring &js) {
    static char outbuf[1024];
    std::string str;

    int len = env->GetStringLength(js);
    env->GetStringUTFRegion(js, 0, len, outbuf);

    str = outbuf;
    return str;
}

JNIEXPORT jint JNICALL JNIDEFINE(nativeCheckMedia)(JNIEnv* env, jclass clz, jstring file_path) {
    std::string mp4_file = convert_jstring(env, file_path);
    int ret = CheckMedia(mp4_file);

    return ret;
}
