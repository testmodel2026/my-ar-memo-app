package com.shimizu.ar.core.ar_message.kotlin.domain.collection

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.EmojiData
import java.util.concurrent.CopyOnWriteArrayList

class GeospatialEmojiCollection {
    var geospatialEmojis = CopyOnWriteArrayList<EmojiData>()
        private set
    var createEmojiObject = false
        private set

    fun addGeospatialArMessageCollection(emojiData: EmojiData) {
       geospatialEmojis.add(emojiData)
    }

    fun cleanCollection() {
        geospatialEmojis.clear()
    }

    fun changeCreateEmojiObject(bool: Boolean) {
        createEmojiObject = bool
    }
}