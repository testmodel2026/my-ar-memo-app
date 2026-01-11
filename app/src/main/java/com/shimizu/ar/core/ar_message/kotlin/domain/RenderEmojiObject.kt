package com.shimizu.ar.core.ar_message.kotlin.domain

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.GeospatialEmojiModel
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.EmojiData
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.interfaces.GeospatialData
import com.shimizu.ar.core.ar_message.java.common.samplerender.Mesh
import com.shimizu.ar.core.ar_message.java.common.samplerender.SampleRender
import com.shimizu.ar.core.ar_message.java.common.samplerender.Shader
import com.shimizu.ar.core.ar_message.java.common.samplerender.Texture
import com.shimizu.ar.core.ar_message.java.common.samplerender.arcore.SpecularCubemapFilter
import com.shimizu.ar.core.ar_message.kotlin.domain.interfaces.RenderObject

class RenderEmojiObject (render: SampleRender,
                         private val emojiType:String,
                         cubemapFilter: SpecularCubemapFilter,
                         dfgTexture: Texture): RenderObject {

    override lateinit var virtualObjectMesh: Mesh
    override lateinit var virtualObjectAlbedoTexture: Texture
    override lateinit var virtualObjectShader: Shader

    init {
        virtualObjectMesh = Mesh.createFromAsset(render,  "models/$emojiType.obj")

        virtualObjectAlbedoTexture =
            Texture.createFromAsset(
                render,
                "models/$emojiType.jpg",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

        virtualObjectShader =
            Shader.createFromAssets(
                render,
                "shaders/environmental_hdr.vert",
                "shaders/environmental_hdr.frag",
                mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString())
            )
                .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
                .setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
                .setTexture("u_DfgTexture", dfgTexture)
    }

    override fun saveAnchor(longitude: Double, latitude: Double, altitude: Double, quaternion: FloatArray): GeospatialData {
        val emojiData = EmojiData(longitude, latitude, altitude, quaternion[0], quaternion[1], quaternion[2], quaternion[3], emojiType)
        GeospatialEmojiModel().insertEmoji(emojiData)
        return emojiData
    }
}