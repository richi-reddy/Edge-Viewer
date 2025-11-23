package com.example.edgeviewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer : GLSurfaceView.Renderer {

    private var textureId: Int = 0
    private var programId: Int = 0

    @Volatile
    private var frameBuffer: ByteBuffer? = null
    private var frameWidth = 0
    private var frameHeight = 0
    @Volatile
    private var newFrameAvailable = false

    fun updateFrame(bytes: ByteArray, width: Int, height: Int) {
        synchronized(this) {
            if (frameBuffer == null || frameWidth != width || frameHeight != height) {
                frameWidth = width
                frameHeight = height
                frameBuffer = ByteBuffer.allocateDirect(width * height * 4)
            }

            frameBuffer?.apply {
                clear()
                put(bytes)
                position(0)
            }

            newFrameAvailable = true
        }
    }

    private val vertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    // ✅ Flipped vertically for camera orientation
    private val texCoordData = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )

    private val vertexBuffer =
        ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexData)
                position(0)
            }

    private val texBuffer =
        ByteBuffer.allocateDirect(texCoordData.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(texCoordData)
                position(0)
            }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        val vertexShaderCode = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        val fragmentShaderCode = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """

        val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vShader)
        GLES20.glAttachShader(programId, fShader)
        GLES20.glLinkProgram(programId)

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (!newFrameAvailable || frameBuffer == null) return

        synchronized(this) {
            newFrameAvailable = false

            GLES20.glUseProgram(programId)

            val posHandle = GLES20.glGetAttribLocation(programId, "aPosition")
            val texHandle = GLES20.glGetAttribLocation(programId, "aTexCoord")
            val samplerHandle = GLES20.glGetUniformLocation(programId, "uTexture")

            GLES20.glEnableVertexAttribArray(posHandle)
            GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

            GLES20.glEnableVertexAttribArray(texHandle)
            GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(samplerHandle, 0)

            frameBuffer?.position(0)

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                frameWidth,
                frameHeight,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                frameBuffer
            )

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(posHandle)
            GLES20.glDisableVertexAttribArray(texHandle)
        }
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}
