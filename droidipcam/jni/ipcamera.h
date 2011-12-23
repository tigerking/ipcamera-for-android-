#ifndef _IPCAMERA_H_
#define _IPCAMERA_H_

#include <string>
#include <jni.h>
#include <android/log.h>
#include "ipcamera.h"

#define  LOG_TAG    "TEAONLY"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)  


int CheckMedia(const std::string mp4_file);

#endif
