package com.example.edgeviewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer : GLSurfaceView.Renderer {

    // Full-screen quad
    private val vertexData = floatArrayOf(
        -1f, -1f,   0f, 1f,   // bottom-left
        1f, -1f,   1f, 1f,   // bottom-right
        -1f,  1f,   0f, 0f,   // top-left
        1f,  1f,   1f, 0f    // top-right
    )

    private lateinit var vertexBuffer: FloatBuffer
    private var program = 0
    private var textureId = 0

    @Volatile
    private var frameBuffer: ByteBuffer? = null
    @Volatile
    private var frameWidth = 0
    @Volatile
    private var frameHeight = 0
    @Volatile
    private var hasNewFrame = false

    fun updateFrame(bytes: ByteArray, width: Int, height: Int) {
        synchronized(this) {
            if (!::vertexBuffer.isInitialized) {
                vertexBuffer = ByteBuffer
                    .allocateDirect(vertexData.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                vertexBuffer.put(vertexData)
                vertexBuffer.position(0)
            }

            if (frameBuffer == null ||
                width != frameWidth ||
                height != frameHeight
            ) {
                frameWidth = width
                frameHeight = height
                frameBuffer = ByteBuffer
                    .allocateDirect(bytes.size)
                    .order(ByteOrder.nativeOrder())
            }

            frameBuffer!!.clear()
            frameBuffer!!.put(bytes)
            frameBuffer!!.position(0)
            hasNewFrame = true
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val fb: ByteBuffer?
        val w: Int
        val h: Int
        synchronized(this) {
            if (!hasNewFrame || frameBuffer == null || frameWidth == 0 || frameHeight == 0) {
                return
            }
            fb = frameBuffer
            w = frameWidth
            h = frameHeight
            hasNewFrame = false
        }

        GLES20.glUseProgram(program)

        // Upload texture from RGBA buffer
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            w,
            h,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            fb
        )

        val stride = 4 * 4  // 4 floats per vertex (pos.xy, tex.xy) * 4 bytes

        val aPos = GLES20.glGetAttribLocation(program, "aPosition")
        val aTex = GLES20.glGetAttribLocation(program, "aTexCoord")

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aTex)
        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTex)
    }

    private fun loadShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)

        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile error: $log")
        }
        return shader
    }

    private fun createProgram(vs: String, fs: String): Int {
        val v = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)

        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(p)
            GLES20.glDeleteProgram(p)
            throw RuntimeException("Program link error: $log")
        }
        return p
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                vTexCoord = aTexCoord;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
