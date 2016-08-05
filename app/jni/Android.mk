# Path of the sources
JNI_DIR := $(call my-dir)
LOCAL_PATH := $(JNI_DIR)

include lzo/Android.mk
include openssl/Android.mk

ifeq ($(TARGET_ARCH),mips)
	USE_BREAKPAD=0
endif
ifeq ($(TARGET_ARCH),mips64)
	USE_BREAKPAD=0
endif

ifeq ($(USE_BREAKPAD),1)
	WITH_BREAKPAD=1
	include breakpad/android/google_breakpad/Android.mk 
else
	WITH_BREAKPAD=0
endif

include openvpn/Android.mk

# The only real JNI libraries
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  -lz
LOCAL_CFLAGS = --std=c99 -DTARGET_ARCH_ABI=\"${TARGET_ARCH_ABI}\"
LOCAL_SRC_FILES:= jniglue.c  scan_ifs.c
LOCAL_MODULE = opvpnutil
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -lz  -lc 
LOCAL_SHARED_LIBRARIES := libssl libcrypto openvpn
LOCAL_SRC_FILES:= minivpn.c dummy.cpp
LOCAL_MODULE = nopie_openvpn
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -lz  -lc 
LOCAL_CFLAGS= -fPIE -pie
LOCAL_CFLAGS = -fPIE
LOCAL_LDFLAGS = -fPIE -pie
LOCAL_SHARED_LIBRARIES := libssl libcrypto openvpn
LOCAL_SRC_FILES:= minivpn.c dummy.cpp
LOCAL_MODULE = pie_openvpn
include $(BUILD_EXECUTABLE)
