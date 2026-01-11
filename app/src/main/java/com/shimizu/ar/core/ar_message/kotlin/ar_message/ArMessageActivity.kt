/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shimizu.ar.core.ar_message.kotlin.ar_message

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewpager2.widget.ViewPager2
import com.google.ar.core.Config
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.Session
import com.shimizu.ar.core.ar_message.java.common.helpers.CameraPermissionHelper
import com.shimizu.ar.core.ar_message.java.common.helpers.DepthSettings
import com.shimizu.ar.core.ar_message.java.common.helpers.FullScreenHelper
import com.shimizu.ar.core.ar_message.java.common.helpers.InstantPlacementSettings
import com.shimizu.ar.core.ar_message.java.common.helpers.LocationPermissionHelper
import com.shimizu.ar.core.ar_message.java.common.samplerender.SampleRender
import com.shimizu.ar.core.ar_message.kotlin.common.helpers.ARCoreSessionLifecycleHelper
import com.shimizu.ar.core.ar_message.kotlin.domain.MessageTextBitmap
import com.shimizu.ar.core.ar_message.kotlin.domain.MessageTextObject
import com.shimizu.ar.core.ar_message.kotlin.domain.UserProfile
import com.shimizu.ar.core.ar_message.kotlin.fragments.AppDescriptionPagerAdapter
import com.shimizu.ar.core.ar_message.kotlin.message_text_input.MessageTextInputActivity
import com.shimizu.ar.core.ar_message.kotlin.profile_input.ProfileInputActivity
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.shimizu.ar.core.ar_message.kotlin.emoji_select.EmoijSelectBottomSheetDialogFragment
import java.io.ByteArrayOutputStream
import java.io.File

class ArMessageActivity : AppCompatActivity() {
  companion object {
    private const val TAG = "ArMessageActivity"
    lateinit var context: Context
  }

  lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
  lateinit var view: ArMessageView
  private lateinit var renderer: MessageRenderer

  private var messageTextBitmap: MessageTextBitmap? = null
  private var emojiType: String? = null

  val instantPlacementSettings = InstantPlacementSettings()
  val depthSettings = DepthSettings()

  override fun onCreate(savedInstanceState: Bundle?) {

    installSplashScreen()
    super.onCreate(savedInstanceState)

    context = applicationContext
    // ユーザ設定の確認
    if(!checkProfileExist(context)) return

    // メモする文字列取得
    val messageText: String? = intent.getStringExtra("MESSAGE_TEXT")
    val textColor: Int       = intent.getIntExtra("TEXT_COLOR", Color.BLACK)
    val objectColor: Int     = intent.getIntExtra("OBJECT_COLOR", Color.WHITE)
    if (messageText != null) {
      val resizeUserIcon = Bitmap.createScaledBitmap(UserProfile(context).userIcon, 80, 80, true)
      val binaryUserIcon: ByteArray = getBinaryFromBitmap(resizeUserIcon)
      messageTextBitmap = MessageTextBitmap(messageText, textColor, objectColor, UserProfile(context).userName, binaryUserIcon)
      MessageTextObject(messageTextBitmap!!, "message").convertObjFile()
    }

    // 絵文字取得
    if (savedInstanceState != null) {
      emojiType = savedInstanceState.getString("emojiTypeKey")
      supportFragmentManager.beginTransaction()
        .remove(supportFragmentManager.findFragmentByTag("half_screen_dialog")!!)
        .commit()
      messageTextBitmap = null
    }

    // Setup ARCore session lifecycle helper and configuration.
    arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
    // If Session creation or Session.resume() fails, display a message and log detailed
    // information.
    arCoreSessionHelper.exceptionCallback =
      { exception ->
        val message =
          when (exception) {
            is UnavailableUserDeclinedInstallationException ->
              "Please install Google Play Services for AR"
            is UnavailableApkTooOldException -> "Please update ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            is CameraNotAvailableException -> "Camera not available. Try restarting the app."
            else -> "Failed to create AR session: $exception"
          }
        Log.e(TAG, "ARCore threw an exception", exception)
        view.snackbarHelper.showError(this, message)
      }

    // Configure session features, including: Lighting Estimation, Depth mode, Instant Placement.
    arCoreSessionHelper.beforeSessionResume = ::configureSession
    lifecycle.addObserver(arCoreSessionHelper)

    // Set up the Hello AR renderer.
    renderer = MessageRenderer(this, messageTextBitmap, messageText, emojiType)
    lifecycle.addObserver(renderer)

    // Set up Hello AR UI.
    view = ArMessageView(this)
    lifecycle.addObserver(view)
    setContentView(view.root)

    // Sets up an example renderer using our HelloARRenderer.
    SampleRender(view.surfaceView, renderer, assets, context)

    depthSettings.onCreate(this)
    instantPlacementSettings.onCreate(this)
    // 初回起動の案内
    firstStartGuid()
  }

  override fun onStart() {
    super.onStart()

    val userIconFile    = File(context.filesDir, "user_icon.png")
    if (userIconFile.exists()) {
      val profileUpdateButton = findViewById<ImageButton>(R.id.profile_update_button)
      val userIconBitmap: Bitmap = BitmapFactory.decodeFile(userIconFile.absolutePath)
      profileUpdateButton.setImageBitmap(userIconBitmap)
    }
  }

  // Configure the session, using Lighting Estimation, and Depth mode.
  fun configureSession(session: Session) {
    session.configure(
      session.config.apply {
        lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

        // Depth API is used if it is configured in Hello AR's settings.
        depthMode =
          if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            Config.DepthMode.AUTOMATIC
          } else {
            Config.DepthMode.DISABLED
          }

        // Instant Placement is used if it is configured in Hello AR's settings.
        instantPlacementMode =
          if (instantPlacementSettings.isInstantPlacementEnabled) {
            InstantPlacementMode.LOCAL_Y_UP
          } else {
            InstantPlacementMode.DISABLED
          }

        geospatialMode = Config.GeospatialMode.ENABLED
      }
    )
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    results: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, results)
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "このアプリではカメラの権限が必須です。", Toast.LENGTH_LONG)
        .show()
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this)
      }
      finish()
    }

    else if (!LocationPermissionHelper.hasFineLocationPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "このアプリでは位置情報の権限が必須です。", Toast.LENGTH_LONG)
        .show()
      if (!LocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        LocationPermissionHelper.launchPermissionSettings(this)
      }
      finish()
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString("emojiTypeKey", emojiType)
  }

  fun MessagePostButtonOnClick(view: View) {
    val intent = Intent(this, MessageTextInputActivity::class.java)
    startActivity(intent)
  }

  fun profileUpdateButtonOnClick(view: View) {
    val intent = Intent(this, ProfileInputActivity::class.java)
    startActivity(intent)
  }

  fun emojiSelectButtonOnClick(view: View) {
    val dialog = EmoijSelectBottomSheetDialogFragment()
    dialog.show(supportFragmentManager, "half_screen_dialog")
  }

  fun emojiSelectedButtonOnClick(view: View) {
    val id = view.id
    emojiType = resources.getResourceEntryName(id)
    recreate()
  }

  fun appDescriptionCloseButtonOnClick(view: View) {
    val viewPager = findViewById<ViewPager2>(R.id.view_pager)
    viewPager.visibility = View.INVISIBLE
  }

  private fun checkProfileExist(context: Context) : Boolean {
    try {
      UserProfile(context)
      return true
    } catch (e: IllegalStateException) {
      val intent = Intent(this, ProfileInputActivity::class.java)
      startActivity(intent)
      finish()
      return false
    }
  }

  private fun firstStartGuid() {
    val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val isFirstLaunch = sharedPrefs.getBoolean("first_launch", true)
    if (isFirstLaunch) {
      // フラグを更新
      val viewPager = findViewById<ViewPager2>(R.id.view_pager)
      viewPager.visibility = View.VISIBLE
      viewPager.adapter = AppDescriptionPagerAdapter(supportFragmentManager, lifecycle)
      sharedPrefs.edit().putBoolean("first_launch", false).apply()
    }
  }

  private fun getBinaryFromBitmap(bitmap: Bitmap): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
  }
}