# Path of the sources
JNI_DIR := $(call my-dir)

include lzo/Android.mk
include openssl/Android.mk
include openvpn/Android.mk

LOCAL_PATH := $(JNI_DIR)

# The only real JNI libraries
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  -lz
LOCAL_CFLAGS =  -DTARGET_ARCH_ABI=\"${TARGET_ARCH_ABI}\"
LOCAL_SRC_FILES:= jniglue.c scan_ifs.c
LOCAL_MODULE = opvpnutil
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -lz  -lc 
LOCAL_CFLAGS= -fPIE -pie
LOCAL_CFLAGS = -fPIE
LOCAL_LDFLAGS = -fPIE -pie
LOCAL_STATIC_LIBRARIES := libssl_static libcrypto_static openvpn
LOCAL_SRC_FILES:= minivpn.c
LOCAL_MODULE = openvpn_executable
include $(BUILD_EXECUTABLE)
