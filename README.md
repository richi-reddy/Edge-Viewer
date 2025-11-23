# EdgeViewer – Android + OpenCV C++ + OpenGL ES + Web Viewer

## 📌 Overview

EdgeViewer is a real-time edge detection viewer that:

✅ Captures camera frames on Android (Camera2 + TextureView)  
✅ Sends each frame to C++ via JNI  
✅ Processes it using OpenCV (C++ / NDK)  
✅ Returns an ARGB8888 image  
✅ Renders it using OpenGL ES 2.0 as a texture  
✅ Includes a TypeScript-based web viewer that displays a processed sample frame

This project demonstrates integration across:

- Android SDK (Kotlin)
- NDK / JNI
- OpenCV C++
- OpenGL ES 2.0
- TypeScript (Web)

---

## 📂 Project Structure
EdgeViewer/
├── app/ # Android Kotlin source
├── jni/ # C++ OpenCV processing (JNI)
├── gl/ # OpenGL ES renderer classes
├── web/ # TypeScript web viewer
│ ├── src/
│ ├── dist/
│ ├── public/
│ ├── package.json
│ ├── tsconfig.json
├── screenshots/ # app screenshots
└── README.md

---

## ✅ Features Implemented

✅ Camera feed using Camera2 + TextureView  
✅ JNI bridge to native C++  
✅ OpenCV processing (Canny edge detection)  
✅ OpenGL ES rendering at ~10–15 FPS  
✅ TypeScript web viewer displaying processed frame  
✅ Proper repository structure  
✅ Public GitHub repo with commits

---

## 🧠 Architecture Flow
Camera2 (Android)
↓ Y-plane (NV21)
JNI (Kotlin → C++)
↓
OpenCV C++ (Canny / grayscale)
↓ ARGB8888 buffer
OpenGL ES texture
↓
Screen rendering

The Android app captures frames using Camera2 and extracts the Y (luma) plane.
This grayscale data is passed through JNI into native C++ code.

OpenCV performs Canny edge detection and produces a binary edge map.
The result is converted to ARGB8888 format and returned to Kotlin.

The GLRenderer uploads this buffer into an OpenGL ES texture and renders it
on a full-screen quad.

A separate TypeScript web viewer displays a sample processed frame, showing
the ability to bridge native output to a web layer.


---

## ⚙️ Setup Instructions

### ✅ Android

Requirements:

- Android Studio
- NDK installed
- OpenCV Android native library

Build:
Open project in Android Studio
Build & Run on a device

---

### ✅ Web Viewer
cd web
npm install
npm run build
npm start


Open in browser:
http://localhost:8081

---

## 🏗 Tech Stack

- Kotlin
- C++ (OpenCV)
- JNI / NDK
- OpenGL ES 2.0
- TypeScript
- HTML/CSS

---

## ⭐ Bonus Implemented

✅ FPS visible on web view  
✅ Clean modular structure

---

## 📦 Submission Notes

This project fulfills:

✅ Native C++ integration (JNI)  
✅ OpenCV usage  
✅ OpenGL rendering  
✅ TypeScript web viewer  
✅ Proper Git repository



