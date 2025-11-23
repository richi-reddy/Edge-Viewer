EdgeViewer – Android + OpenCV C++ + OpenGL ES + Web Viewer
📌 Overview

EdgeViewer is a real-time edge detection viewer that:

✅ Captures camera frames on Android (Camera2 + TextureView)
✅ Sends each frame to C++ via JNI
✅ Processes it using OpenCV (C++ / NDK)
✅ Returns an ARGB image
✅ Renders it using OpenGL ES 2.0 as a texture
✅ Also provides a TypeScript-based web viewer that displays a processed sample frame

This project demonstrates integration across:

Android SDK (Kotlin)

NDK / JNI

OpenCV C++

OpenGL ES 2.0

TypeScript (Web)

📂 Project Structure
EdgeViewer/
 ├── app/              Android Kotlin source
 ├── jni/              C++ OpenCV processing (JNI)
 ├── gl/               OpenGL ES renderer classes
 ├── web/              TypeScript web viewer
 │    ├── src/
 │    ├── dist/
 │    ├── public/
 │    ├── package.json
 │    ├── tsconfig.json
 ├── screenshots/
 ├── README.md         ✅ this file

🚀 Features

✅ Camera feed using Camera2 + TextureView
✅ JNI bridge to native C++
✅ OpenCV processing (Canny / grayscale)
✅ OpenGL ES 2.0 rendering at ~10–15 FPS
✅ TypeScript web viewer showing processed frame

🧠 Architecture Flow
Camera2 (Android)
     ↓ Y-plane
JNI (Kotlin → C++)
     ↓
OpenCV C++ (Canny / grayscale)
     ↓ ARGB8888 buffer
OpenGL ES texture
     ↓
Screen rendering


Web viewer:

dist/sample.png → HTML + TypeScript → Display

🔧 Android Setup

Requirements:

Android Studio

NDK installed

OpenCV for Android (included via native libs)

Run:

Open in Android Studio
Build & Run

🧩 Native C++ (JNI + OpenCV)

Located in:

jni/


Responsibilities:

✅ Receive grayscale frame
✅ Process with OpenCV
✅ Return ARGB buffer to Kotlin

🎨 OpenGL ES Renderer

Located in:

gl/GLRenderer.kt


Responsibilities:

✅ Upload texture from native output
✅ Render full-screen quad
✅ Maintain 10–15 FPS minimum

🌐 Web Viewer (TypeScript)

Located in:

web/


To build:

cd web
npm install
npm run build
npm start


Then open:

http://localhost:8081

🖼 Screenshots

(You will add)

screenshots/android.png
screenshots/web.png

✅ Submission Notes

Git repository includes:

✅ Android processing pipeline
✅ JNI + C++ OpenCV integration
✅ OpenGL ES renderer
✅ Web viewer
✅ Documentation + structure