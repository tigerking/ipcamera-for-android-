#ifndef _IPCAMERA_H_
#define _IPCAMERA_H_

#include <vector>
#include <string>
#include <jni.h>
#include <android/log.h>


#define  LOG_TAG    "TEAONLY"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)  

extern int begin_skip;
extern std::vector<unsigned char> sps_data;
extern std::vector<unsigned char> pps_data;

int CheckMedia(const std::string mp4_file);

int StartFormatingMedia(int infd, int outfd);
void StopFormatingMedia();

#endif
