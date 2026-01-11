package com.shimizu.ar.core.ar_message.kotlin.domain

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.ArMessageData
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.UserProfileData

class GeospatialArMessage(arMessageData: ArMessageData, userProfileData: UserProfileData, fileName: String) {

    val arMessageData = arMessageData
    val messageTextBitmap: MessageTextBitmap = MessageTextBitmap(arMessageData.message, arMessageData.textColor, arMessageData.backgroundColor, userProfileData.userName, userProfileData.userIcon)
    val messageTextObjectPath: String = "$fileName.obj"

    init {
        MessageTextObject(messageTextBitmap, fileName).convertObjFile()
    }
}