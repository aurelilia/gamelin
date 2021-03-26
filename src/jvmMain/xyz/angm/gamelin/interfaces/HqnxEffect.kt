/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 7:38 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException

/**
 * [HQnX](https://en.wikipedia.org/wiki/Pixel-art_scaling_algorithms#hqnx_family)
 * upscale algorithm GLSL implementation based on
 * [CrossVR](https://github.com/CrossVR/hqx-shader/tree/master/glsl) project.
 *
 * This was originally made by metaphore: https://gist.github.com/metaphore/b4750be45289109b3d49c97b5c300db6
 * This file is slightly abridged from their implementation.
 * Thank you for making it!
 */
class HqnxEffect(private val scaleFactor: Int) : Disposable {

    private val mesh = ViewportQuadMesh(
        VertexAttribute(Usage.Position, 2, "a_position"),
        VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0")
    )

    private val program = compileShader(
        Gdx.files.internal("shader/hqnx.vert"),
        Gdx.files.internal("shader/hqnx.frag"),
        "#define SCALE ${scaleFactor}.0"
    )
    private val lutTexture = Texture(Gdx.files.internal("shader/hq${scaleFactor}x.png"))

    internal var dstBuffer: FrameBuffer? = null
    private var dstWidth = 0
    private var dstHeight = 0

    override fun dispose() {
        mesh.dispose()
        program.dispose()
        lutTexture.dispose()
        dstBuffer?.dispose()
    }

    private fun rebind() {
        program.bind()
        program.setUniformi(U_TEXTURE, TEXTURE_HANDLE0)
        program.setUniformi(U_LUT, TEXTURE_HANDLE1)
        program.setUniformf(
            U_TEXTURE_SIZE,
            dstWidth / scaleFactor.toFloat(),
            dstHeight / scaleFactor.toFloat()
        )
    }

    fun renderToBuffer(src: Texture): Texture {
        validate(src)
        validateDstBuffer()

        lutTexture.bind(TEXTURE_HANDLE1)
        src.bind(TEXTURE_HANDLE0)

        dstBuffer!!.begin()
        program.bind()
        mesh.render(program)
        dstBuffer!!.end()

        return dstBuffer!!.colorBufferTexture
    }

    private fun validate(src: Texture) {
        val targetWidth = src.width * scaleFactor
        val targetHeight = src.height * scaleFactor

        if (dstWidth != targetWidth || dstHeight != targetHeight) {
            dstWidth = targetWidth
            dstHeight = targetHeight
            rebind()
        }
    }

    private fun validateDstBuffer() {
        if (dstBuffer == null || dstBuffer!!.width != dstWidth || dstBuffer!!.height != dstHeight) {
            dstBuffer?.dispose()
            dstBuffer = FrameBuffer(Pixmap.Format.RGB888, dstWidth, dstHeight, false)
            dstBuffer!!.colorBufferTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            )
        }
    }

    companion object {
        private const val TEXTURE_HANDLE0 = 0
        private const val TEXTURE_HANDLE1 = 1

        private const val U_TEXTURE = "u_texture"
        private const val U_LUT = "u_lut"
        private const val U_TEXTURE_SIZE = "u_textureSize"
    }
}

/**
 * Encapsulates a fullscreen quad mesh. Geometry is aligned to the viewport corners.
 *
 * @author bmanuel
 * @author metaphore
 */
private class ViewportQuadMesh(vararg attributes: VertexAttribute) : Disposable {

    private val mesh: Mesh = Mesh(true, 4, 0, *attributes)

    init {
        mesh.setVertices(verts)
    }

    override fun dispose() = mesh.dispose()

    /** Renders the quad with the specified shader program. */
    fun render(program: ShaderProgram) {
        mesh.render(program, GL20.GL_TRIANGLE_FAN, 0, 4)
    }

    companion object {
        private const val VERT_SIZE = 16
        private const val X1 = 0
        private const val Y1 = 1
        private const val U1 = 2
        private const val V1 = 3
        private const val X2 = 4
        private const val Y2 = 5
        private const val U2 = 6
        private const val V2 = 7
        private const val X3 = 8
        private const val Y3 = 9
        private const val U3 = 10
        private const val V3 = 11
        private const val X4 = 12
        private const val Y4 = 13
        private const val U4 = 14
        private const val V4 = 15

        private val verts: FloatArray = FloatArray(VERT_SIZE)

        init {
            // Vertex coords
            verts[X1] = -1f
            verts[Y1] = -1f
            verts[X2] = 1f
            verts[Y2] = -1f
            verts[X3] = 1f
            verts[Y3] = 1f
            verts[X4] = -1f
            verts[Y4] = 1f

            // Tex coords
            verts[U1] = 0f
            verts[V1] = 0f
            verts[U2] = 1f
            verts[V2] = 0f
            verts[U3] = 1f
            verts[V3] = 1f
            verts[U4] = 0f
            verts[V4] = 1f
        }
    }
}

private fun compileShader(vertexFile: FileHandle, fragmentFile: FileHandle, defines: String): ShaderProgram {
    val srcVert = vertexFile.readString()
    val srcFrag = fragmentFile.readString()
    val shader = ShaderProgram("$defines\n$srcVert".trimIndent(), "$defines\n$srcFrag".trimIndent())

    if (!shader.isCompiled) {
        throw GdxRuntimeException("Shader compilation error: ${vertexFile.name()}/${fragmentFile.name()}\n${shader.log}")
    }
    return shader
}