package com.example.edgeviewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer : GLSurfaceView.Renderer {

    private var textureId = 0
    private lateinit var buffer: ByteBuffer
    var width = 640
    var height = 480

    fun updateFrame(data: ByteArray) {
        buffer = ByteBuffer.wrap(data)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (::buffer.isInitialized) {
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                width,
                height,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                buffer
            )
        }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {}
}
