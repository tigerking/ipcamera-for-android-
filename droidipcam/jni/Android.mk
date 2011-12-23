#####################################################################
# the build script for android libjingle
#

LOCAL_PATH:= $(call my-dir)

###########################################################
# the native jni interface library
#
include $(CLEAR_VARS)
LOCAL_MODULE    := ipcamera
LOCAL_CPPFLAGS := -Werror -Wall -O2
LOCAL_C_INCLUDES :=  ./
LOCAL_LDLIBS := -llog

include $(LOCAL_PATH)/build.mk

include $(BUILD_SHARED_LIBRARY)
