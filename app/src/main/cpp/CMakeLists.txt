cmake_minimum_required(VERSION 3.22.1)

project("edgeviewer")

# Native library
add_library(edgeviewer SHARED
        native-lib.cpp)

# ---- OpenCV headers ----
# CMakeLists.txt is in app/src/main/cpp
# opencv_include is in app/src/main/opencv_include
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../opencv_include)

# ---- Prebuilt OpenCV shared library (.so) ----
add_library(opencv_java4 SHARED IMPORTED)

# jniLibs is in app/src/main/jniLibs/<ABI>/libopencv_java4.so
set_target_properties(opencv_java4 PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so)

# ---- log library from NDK ----
find_library(log-lib log)

# ---- Link everything together ----
target_link_libraries(
        edgeviewer
        opencv_java4
        ${log-lib}
)
