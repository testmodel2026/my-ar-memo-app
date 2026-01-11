package com.shimizu.ar.core.ar_message.kotlin.domain.interfaces

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.interfaces.GeospatialData
import com.shimizu.ar.core.ar_message.java.common.samplerender.Mesh
import com.shimizu.ar.core.ar_message.java.common.samplerender.Shader
import com.shimizu.ar.core.ar_message.java.common.samplerender.Texture

interface RenderObject {

    // Virtual object
    var virtualObjectMesh: Mesh
    var virtualObjectShader: Shader
    var virtualObjectAlbedoTexture: Texture

    fun saveAnchor(longitude: Double, latitude: Double, altitude: Double, quaternion: FloatArray): GeospatialData
}