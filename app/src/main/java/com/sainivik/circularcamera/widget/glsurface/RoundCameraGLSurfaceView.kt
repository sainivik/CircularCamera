package com.sainivik.circularcamera.widget.glsurface

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import com.sainivik.circularcamera.widget.glsurface.GLUtil.createShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RoundCameraGLSurfaceView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private var dataInput = false
    private var frameWidth = 0
    private var frameHeight = 0
    private var isMirror = false
    private var rotateDegree = 0

    // Used to judge whether the preview data is passed in, to avoid a period of green background during initialization (y, u, v are all 0)    private boolean dataInput = false;
    var radius = 0
    private var yBuf: ByteBuffer? = null
    private var uBuf: ByteBuffer? = null
    private var vBuf: ByteBuffer? = null
    private val yTexture = IntArray(1)
    private val uTexture = IntArray(1)
    private val vTexture = IntArray(1)
    lateinit var yArray: ByteArray
    lateinit var uArray: ByteArray
    lateinit var vArray: ByteArray
    private var fragmentShaderCode = GLUtil.FRAG_SHADER_NORMAL
    private var squareVertices: FloatBuffer? = null
    private var coordVertices: FloatBuffer? = null
    private var rendererReady = false
    var coordVertice: FloatArray? = null
    fun turnRound() {
        invalidateOutline()
    }

    /**
     * Set different fragment shader codes to achieve different preview effects
     *
     * @param fragmentShaderCode fragment shader code
     */
    fun setFragmentShaderCode(fragmentShaderCode: String) {
        this.fragmentShaderCode = fragmentShaderCode
    }

    fun init(
        isMirror: Boolean,
        rotateDegree: Int,
        frameWidth: Int,
        frameHeight: Int
    ) {
        if (this.frameWidth == frameWidth && this.frameHeight == frameHeight && this.rotateDegree == rotateDegree && this.isMirror == isMirror
        ) {
            return
        }
        dataInput = false
        this.frameWidth = frameWidth
        this.frameHeight = frameHeight
        this.rotateDegree = rotateDegree
        this.isMirror = isMirror
        yArray = ByteArray(this.frameWidth * this.frameHeight)
        uArray = ByteArray(this.frameWidth * this.frameHeight / 4)
        vArray = ByteArray(this.frameWidth * this.frameHeight / 4)
        val yFrameSize = this.frameHeight * this.frameWidth
        val uvFrameSize = yFrameSize shr 2
        yBuf = ByteBuffer.allocateDirect(yFrameSize)
        yBuf!!.order(ByteOrder.nativeOrder()).position(0)
        uBuf = ByteBuffer.allocateDirect(uvFrameSize)
        uBuf!!.order(ByteOrder.nativeOrder()).position(0)
        vBuf = ByteBuffer.allocateDirect(uvFrameSize)
        vBuf!!.order(ByteOrder.nativeOrder()).position(0)
        squareVertices = ByteBuffer
            .allocateDirect(GLUtil.SQUARE_VERTICES.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        squareVertices!!.put(GLUtil.SQUARE_VERTICES).position(0)
        if (isMirror) {
            when (rotateDegree) {
                0 -> coordVertice = GLUtil.MIRROR_COORD_VERTICES
                90 -> coordVertice = GLUtil.ROTATE_90_MIRROR_COORD_VERTICES
                180 -> coordVertice = GLUtil.ROTATE_180_MIRROR_COORD_VERTICES
                270 -> coordVertice = GLUtil.ROTATE_270_MIRROR_COORD_VERTICES
                else -> {
                }
            }
        } else {
            when (rotateDegree) {
                0 -> coordVertice = GLUtil.COORD_VERTICES
                90 -> coordVertice = GLUtil.ROTATE_90_COORD_VERTICES
                180 -> coordVertice = GLUtil.ROTATE_180_COORD_VERTICES
                270 -> coordVertice = GLUtil.ROTATE_270_COORD_VERTICES
                else -> {
                }
            }
        }
        coordVertices =
            ByteBuffer.allocateDirect(coordVertice!!.size * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        coordVertices!!.put(coordVertice).position(0)
    }

    /**
     * Create OpenGL Program and associate variables in GLSL
     *
     * @param fragmentShaderCode fragment shader code
     */
    private fun createGLProgram(fragmentShaderCode: String) {
        val programHandleMain = createShaderProgram(fragmentShaderCode)
        if (programHandleMain != -1) {
            GLES20.glUseProgram(programHandleMain)
            val glPosition = GLES20.glGetAttribLocation(programHandleMain, "attr_position")
            val textureCoord = GLES20.glGetAttribLocation(programHandleMain, "attr_tc")
            val ySampler = GLES20.glGetUniformLocation(programHandleMain, "ySampler")
            val uSampler = GLES20.glGetUniformLocation(programHandleMain, "uSampler")
            val vSampler = GLES20.glGetUniformLocation(programHandleMain, "vSampler")

            //Assign a value to the variable
            /**
             * GLES20.GL_TEXTURE0 is bound to ySampler
             * GLES20.GL_TEXTURE1 is bound to uSampler
             * GLES20.GL_TEXTURE2 is bound to vSampler
             *
             * In other words, the second parameter of glUniform1i represents the layer number
             */
            GLES20.glUniform1i(ySampler, 0)
            GLES20.glUniform1i(uSampler, 1)
            GLES20.glUniform1i(vSampler, 2)
            GLES20.glEnableVertexAttribArray(glPosition)
            GLES20.glEnableVertexAttribArray(textureCoord)
            squareVertices!!.position(0)
            GLES20.glVertexAttribPointer(
                glPosition,
                GLUtil.COUNT_PER_SQUARE_VERTICE,
                GLES20.GL_FLOAT,
                false,
                8,
                squareVertices
            )
            coordVertices!!.position(0)
            GLES20.glVertexAttribPointer(
                textureCoord,
                GLUtil.COUNT_PER_COORD_VERTICES,
                GLES20.GL_FLOAT,
                false,
                8,
                coordVertices
            )
        }
    }

    inner class YUVRenderer : Renderer {
        private fun initRenderer() {
            rendererReady = false
            createGLProgram(fragmentShaderCode)
            GLES20.glEnable(GLES20.GL_TEXTURE_2D)
            createTexture(frameWidth, frameHeight, GLES20.GL_LUMINANCE, yTexture)
            createTexture(frameWidth / 2, frameHeight / 2, GLES20.GL_LUMINANCE, uTexture)
            createTexture(frameWidth / 2, frameHeight / 2, GLES20.GL_LUMINANCE, vTexture)
            rendererReady = true
        }

        override fun onSurfaceCreated(
            unused: GL10,
            config: EGLConfig
        ) {
            initRenderer()
        }

        private fun createTexture(
            width: Int,
            height: Int,
            format: Int,
            textureId: IntArray
        ) {
            GLES20.glGenTextures(1, textureId, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            /**
             * [GLES20.GL_TEXTURE_WRAP_S] represents the texture wrapping mode in the left and right directions
             * [GLES20.GL_TEXTURE_WRAP_T] represents the texture wrapping mode in the up and down direction
             *
             * [GLES20.GL_REPEAT]: Repeat
             * [GLES20.GL_MIRRORED_REPEAT]: mirror duplicate
             * [GLES20.GL_CLAMP_TO_EDGE]: Ignore border capture
             *
             * For example, we use [GLES20.GL_REPEAT]:
             *
             * squareVertices coordVertices
             * -1.0f, -1.0f, 1.0f, 1.0f,
             * 1.0f, -1.0f, 1.0f, 0.0f, -> same as textureView preview
             * -1.0f, 1.0f, 0.0f, 1.0f,
             * 1.0f, 1.0f 0.0f, 0.0f
             *
             * squareVertices coordVertices
             * -1.0f, -1.0f, 2.0f, 2.0f,
             * 1.0f, -1.0f, 2.0f, 0.0f, -> Compared with textureView preview, it is divided into 4 same previews (lower left, lower right, upper left, upper right)
             * -1.0f, 1.0f, 0.0f, 2.0f,
             * 1.0f, 1.0f 0.0f, 0.0f
             */
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            /**
             * [GLES20.GL_TEXTURE_MIN_FILTER] represents the situation where the displayed texture is smaller than the loaded texture
             * [GLES20.GL_TEXTURE_MAG_FILTER] represents the situation when the displayed texture is larger than the loaded texture
             *
             * [GLES20.GL_NEAREST]: Use the color of the closest pixel in the texture as the pixel color to be drawn
             * [GLES20.GL_LINEAR]: Use several colors with the closest coordinates in the texture to get the pixel color to be drawn through the weighted average algorithm
             */
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                format,
                width,
                height,
                0,
                format,
                GLES20.GL_UNSIGNED_BYTE,
                null
            )
        }

        override fun onDrawFrame(gl: GL10) {
            if (dataInput) {
                //y
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexture[0])
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    frameWidth,
                    frameHeight,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    yBuf
                )

                //u
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTexture[0])
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    frameWidth shr 1,
                    frameHeight shr 1,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    uBuf
                )

                //v
                GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTexture[0])
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    frameWidth shr 1,
                    frameHeight shr 1,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    vBuf
                )
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            }
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }
    }

    fun refreshFrameNV21(data: ByteArray) {
        if (rendererReady) {
            yBuf!!.clear()
            uBuf!!.clear()
            vBuf!!.clear()
            putNV21(data, frameWidth, frameHeight)
            dataInput = true
            requestRender()
        }
    }

    fun refreshFrameYV12(data: ByteArray) {
        if (rendererReady) {
            yBuf!!.clear()
            uBuf!!.clear()
            vBuf!!.clear()
            putYV12(data, frameWidth, frameHeight)
            dataInput = true
            requestRender()
        }
    }

    /**
     * Take out the Y, U and V components of NV21 data
     *
     * @param src nv21 frame data
     * @param width width
     * @param height height
     */
    private fun putNV21(src: ByteArray, width: Int, height: Int) {
        val ySize = width * height
        val frameSize = ySize * 3 / 2
        System.arraycopy(src, 0, yArray, 0, ySize)
        var k = 0
        var index = ySize
        while (index < frameSize) {
            vArray[k] = src[index++]
            uArray[k++] = src[index++]
        }
        yBuf!!.put(yArray).position(0)
        uBuf!!.put(uArray).position(0)
        vBuf!!.put(vArray).position(0)
    }

    /**
     * Take out Y, U, V components of YV12 data
     *
     * @param src YV12 frame data
     * @param width width
     * @param height height
     */
    private fun putYV12(src: ByteArray, width: Int, height: Int) {
        val ySize = width * height
        System.arraycopy(src, 0, yArray, 0, ySize)
        System.arraycopy(src, ySize, vArray, 0, vArray.size)
        System.arraycopy(src, ySize + vArray.size, uArray, 0, uArray.size)
        yBuf!!.put(yArray).position(0)
        uBuf!!.put(uArray).position(0)
        vBuf!!.put(vArray).position(0)
    }

    companion object {
        private const val TAG = "CameraGLSurfaceView"
        private const val FLOAT_SIZE_BYTES = 4
    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(YUVRenderer())
        renderMode = RENDERMODE_WHEN_DIRTY
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val rect =
                    Rect(0, 0, view.measuredWidth, view.measuredHeight)
                outline.setRoundRect(rect, radius.toFloat())
            }
        }
        clipToOutline = true
    }
}