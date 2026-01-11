package com.shimizu.ar.core.ar_message.kotlin.domain.collection

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.ArMessageData
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.UserProfileData
import com.shimizu.ar.core.ar_message.kotlin.ar_message.ArMessageActivity
import com.shimizu.ar.core.ar_message.kotlin.domain.GeospatialArMessage
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class GeospatialArMessageCollection {
    var geospatialArMessages = CopyOnWriteArrayList<GeospatialArMessage>()
        private set
    var createArMessageObject = false
        private set

    fun addGeospatialArMessageCollection(arMessageData: ArMessageData, userProfileData: UserProfileData) {
        val fileNo = geospatialArMessages.size
        val geospatialArMessage = GeospatialArMessage(arMessageData, userProfileData, "geospatial_message_$fileNo")
        geospatialArMessages.add(geospatialArMessage)
    }

    fun cleanCollection() {
        val dir = ArMessageActivity.context.filesDir
        val regex = Regex("geospatial_message_\\d+\\.obj")
        dir.walkTopDown().forEach { file ->
            if (regex.matches(file.name)) {
                file.delete()
            }
        }
        geospatialArMessages.clear()
    }

    fun changeCreateArMessageObject(bool: Boolean) {
        createArMessageObject = bool
    }
}