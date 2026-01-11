package com.shimizu.ar.core.ar_message.kotlin.domain

import com.shimizu.ar.core.ar_message.kotlin.ar_message.ArMessageActivity
import java.io.File
import java.io.InputStream
import kotlin.text.split
import kotlin.text.startsWith
import kotlin.text.toFloat

class MessageTextObject (private val messageTextBitmap: MessageTextBitmap, private val fileName: String) {

    private val baseImageWidth: Double  = 50.0
    private val baseImageHeight: Double = 60.0

    fun convertObjFile() {
        val imageWidth  = messageTextBitmap.messageTextBitmap.width
        val imageHeight = messageTextBitmap.messageTextBitmap.height

        val ratioWidth: Double  = imageWidth.toDouble() / baseImageWidth
        val ratioHeight: Double = imageHeight.toDouble() / baseImageHeight

        val objFile = ArMessageActivity.context.assets.open("models/message.obj")
        val newObjFileLine = multiplyVertices(objFile, ratioWidth, ratioHeight)

        val newObjFile = File(ArMessageActivity.context.filesDir, "$fileName.obj")
        newObjFile.writeText(newObjFileLine)
    }

    private fun multiplyVertices(objFile: InputStream, xMultiplier: Double = 1.0, yMultiplier: Double = 1.0, zMultiplier: Double = 1.0): String {
        val lines = objFile.bufferedReader().readLines()
        val newLines = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("v ")) {
                val parts = line.split(" ")
                if (parts.size >= 4) {
                    val x = parts[1].toFloat() * xMultiplier
                    val z = parts[2].toFloat() * zMultiplier
                    val y = parts[3].toFloat() * yMultiplier
                    newLines.add("v $x $z $y")
                } else {
                    newLines.add(line)
                }
            } else {
                newLines.add(line)
            }
        }

        return newLines.joinToString("\n")
    }
}