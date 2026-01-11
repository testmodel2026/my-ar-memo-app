package com.shimizu.ar.core.ar_message.kotlin.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import com.shimizu.ar.core.ar_message.kotlin.ar_message.ArMessageActivity

class MessageTextBitmap (messageText: String,
                         val textColor: Int,
                         val backGroundColor: Int,
                         private val userName: String,
                         private val userIcon: ByteArray) {

    private val textSize = 60f

    val messageTextBitmap: Bitmap = textToBitmap(messageText)

    private fun textToBitmap(text: String): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize  = textSize
        paint.color     = textColor
        paint.textAlign = Align.LEFT
        paint.typeface  = Typeface.createFromAsset(ArMessageActivity.context.assets, "fonts/Pro-OT2.0.otf")

        val userNameBitmap = getUserNameBitmap()
        val userIconBitmap = getUserIconBitmap()

        val testList: List<String> =  text.split("\n")

        val longestText = testList.reduce { longestText, text -> if (text.length > longestText.length) text else longestText }
        val width =
            if (paint.measureText(longestText).toInt() < userNameBitmap.width) {
                userNameBitmap.width + userIconBitmap.width + 50
            } else {
                (paint.measureText(longestText)).toInt() + userIconBitmap.width + 50
            }
        val height = ((-paint.ascent() + paint.descent() + 5f) * testList.size).toInt() + userNameBitmap.height + 50

        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawColor(backGroundColor)

        canvas.drawBitmap(userIconBitmap,20f, 20f, null)
        canvas.drawBitmap(userNameBitmap, (userIconBitmap.width + 30).toFloat(), 20f, null)

        val x = (userIconBitmap.width + 30).toFloat()
        for (i in testList.indices) {
            val y =
                if (i == 0) {
                    -paint.ascent() + (40f / 2f)
                } else {
                    -paint.ascent() + (40f / 2f) + ((-paint.ascent() + paint.descent() + 5f) * (i))
                }
            canvas.drawText(testList[i], x, userNameBitmap.height.toFloat() + y + 10f, paint)
        }

        return image
    }

    private fun getUserNameBitmap(): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize  = textSize
        paint.color     = textColor
        paint.textAlign = Align.LEFT
        paint.typeface  = Typeface.createFromAsset(ArMessageActivity.context.assets, "fonts/Pro-OT2.0.otf")

        val width  = (paint.measureText(userName)).toInt()
        val height = (-paint.ascent() + paint.descent()).toInt()

        val userNameImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val userNameCanvas = Canvas(userNameImage)
        userNameCanvas.drawColor(backGroundColor)

        userNameCanvas.drawText(userName, 0f, -paint.ascent(), paint)

        return userNameImage
    }

    private fun getUserIconBitmap(): Bitmap {
        val imageSize = 80

        val userIconBitmap = BitmapFactory.decodeByteArray(userIcon, 0, userIcon.size)

        val circleIcon = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(circleIcon)

        val paint = Paint()

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(imageSize / 2f, imageSize / 2f, imageSize / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val max = Math.max(userIconBitmap.width, userIconBitmap.height)
        val start = (imageSize - max) / 2
        val left = if (userIconBitmap.width < userIconBitmap.height) 0 else start
        val top = if (userIconBitmap.height < userIconBitmap.width) 0 else start
        val rect = Rect(left, top, userIconBitmap.width + left, userIconBitmap.height + top)
        canvas.drawBitmap(userIconBitmap, Rect(0, 0, userIconBitmap.width, userIconBitmap.height), rect, paint)
        return circleIcon
    }
}