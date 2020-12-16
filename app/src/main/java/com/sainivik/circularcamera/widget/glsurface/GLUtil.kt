package com.sainivik.circularcamera.widget.glsurface

import android.opengl.GLES20
import java.nio.IntBuffer

object GLUtil {
    private const val TAG = "GLUtil"

    /**
     * Vertex shader
     */
    private const val VERTEX_SHADER = "    attribute vec4 attr_position;\n" +
            "    attribute vec2 attr_tc;\n" +
            "    varying vec2 tc;\n" +
            "    void main() {\n" +
            "        gl_Position = attr_position;\n" +
            "        tc = attr_tc;\n" +
            "    }"
    @JvmField
    var FRAG_SHADER_NORMAL = """precision mediump float;
    varying vec2 tc;
    uniform sampler2D ySampler;
    uniform sampler2D uSampler;
    uniform sampler2D vSampler;
    const mat3 convertMat = mat3(1.0, 1.0, 1.0, 0, -0.344, 1.77, 1.403, -0.714,0);
    void main()
    {
        vec3 yuv;
        yuv.x = texture2D(ySampler, tc).r;
        yuv.y = texture2D(uSampler, tc).r - 0.5;
        yuv.z = texture2D(vSampler, tc).r - 0.5;
        gl_FragColor = vec4(convertMat * yuv, 1.0);
    }"""
    var FRAG_SHADER_GRAY = """precision mediump float;
    varying vec2 tc;
    uniform sampler2D ySampler;
    void main()
    {
        vec3 yuv;
        yuv.x = texture2D(ySampler, tc).r;
        gl_FragColor = vec4(vec3(yuv.x), 1.0);
    }"""
    var FRAG_SHADER_SCULPTURE = """precision mediump float;
varying vec2 tc;
    uniform sampler2D ySampler;
    const vec2 texSize = vec2(100.0, 100.0);
    const vec4 sculptureColor = vec4(0.5, 0.5, 0.5, 1.0);

void main()
{
    vec2 upLeftCoord = vec2(tc.x-1.0/texSize.x, tc.y-1.0/texSize.y);
    vec4 curColor = texture2D(ySampler, tc);
    vec4 upLeftColor = texture2D(ySampler, upLeftCoord);
    vec4 delColor = curColor - upLeftColor;
    gl_FragColor = vec4(vec3(delColor), 0.0) + sculptureColor;
}"""
    const val COUNT_PER_SQUARE_VERTICE = 2
    const val COUNT_PER_COORD_VERTICES = 2

    /**
     * Displayed vertices
     */
    @JvmField
    val SQUARE_VERTICES = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, 1.0f
    )

    /**
     * Original data display
     * 0,1***********1,1
     * * *
     * * *
     * * *
     * * *
     * * *
     * 0,0***********1,0
     */
    @JvmField
    val COORD_VERTICES = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    )

    /**
     * 逆时针旋转90度显示
     * 1,1***********1,0
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 0,1***********0,0
     */
    @JvmField
    val ROTATE_90_COORD_VERTICES = floatArrayOf(
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        0.0f, 0.0f
    )

    /**
     * 逆时针旋转180度显示
     * 0,1***********1,1
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 0,0***********1,0
     */
    @JvmField
    val ROTATE_180_COORD_VERTICES = floatArrayOf(
        1.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f
    )

    /**
     * 逆时针旋转270度显示
     * 0,1***********1,1
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 0,0***********1,0
     */
    @JvmField
    val ROTATE_270_COORD_VERTICES = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    /**
     * Mirror display
     * 1,1***********0,1
     * * *
     * * *
     * * *
     * * *
     * * *
     * 1,0***********0,0
     */
    @JvmField
    val MIRROR_COORD_VERTICES = floatArrayOf(
        1.0f, 1.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 0.0f
    )

    /**
     * Mirror and rotate 90 degrees counterclockwise to display
     * 0,1***********0,0
     * * *
     * * *
     * * *
     * * *
     * * *
     * 1,1***********1,0
     */
    @JvmField
    val ROTATE_90_MIRROR_COORD_VERTICES = floatArrayOf(
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        1.0f, 0.0f
    )
    @JvmField
    val ROTATE_180_MIRROR_COORD_VERTICES = floatArrayOf(
        1.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f
    )
    @JvmField
    val ROTATE_270_MIRROR_COORD_VERTICES = floatArrayOf(
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        0.0f, 1.0f
    )

    /**
     * Create OpenGL Program and link
     *
     * @param fragmentShaderCode fragment shader code
     * @return OpenGL Program object reference
     */
    @JvmStatic
    fun createShaderProgram(fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        val mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        val linked = IntBuffer.allocate(1)
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linked)
        return if (linked[0] == 0) {
            -1
        } else mProgram
    }

    /**
     * Load the shader
     *
     * @param type       shader type, which can be fragment shader [GLES20.GL_FRAGMENT_SHADER] or vertex shader [GLES20.GL_VERTEX_SHADER]
     * @param shaderCode shader code
     * @return a reference to the shader object
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntBuffer.allocate(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled)
        return if (compiled[0] == 0) {
            0
        } else shader
    }
}