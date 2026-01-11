package com.shimizu.ar.core.ar_message.kotlin.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File

class UserProfile(context: Context) {
    var userId: Int
        private set
    var userName: String
      private set
    var userIcon: Bitmap
        private set
    var textColor: Int
        private set
    var objectColor: Int
        private set

    init {
        val userNameFile     = File(context.filesDir, "user_name.txt")
        val userIconFile     = File(context.filesDir, "user_icon.png")
        val textColorFile    = File(context.filesDir, "text_color.txt")
        val objectColorFile  = File(context.filesDir, "object_color.txt")
        if (!userNameFile.exists() && !userIconFile.exists() && !textColorFile.exists() && !objectColorFile.exists()) {
            throw IllegalStateException()
        }
        userId       = getUserId(context)
        userName     = userNameFile.readText()
        userIcon     = BitmapFactory.decodeFile(userIconFile.absolutePath)
        textColor    = textColorFile.readText().toInt()
        objectColor  = objectColorFile.readText().toInt()
    }

    private fun getUserId(context: Context): Int {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "user_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return sharedPrefs.getString("user_id", null)?.toInt() ?: 0
    }
}