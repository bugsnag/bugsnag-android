include $(CLEAR_VARS)

LOCAL_ARM_MODE  := arm
LOCAL_PATH      := $(NDK_PROJECT_PATH)
LOCAL_MODULE    := bugsnag_bridge
LOCAL_CFLAGS    := -Werror
LOCAL_SRC_FILES := src/bugsnag_bridge.cpp
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)