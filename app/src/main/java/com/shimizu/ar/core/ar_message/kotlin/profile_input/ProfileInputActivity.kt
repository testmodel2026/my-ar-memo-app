package com.shimizu.ar.core.ar_message.kotlin.profile_input

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.UserProfileModel
import com.shimizu.ar.core.ar_message.kotlin.ar_message.ArMessageActivity
import com.shimizu.ar.core.ar_message.kotlin.ar_message.R
import com.shimizu.ar.core.ar_message.kotlin.domain.UserProfile
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream

class ProfileInputActivity: AppCompatActivity() {

    private var textColor: Int = Color.BLACK
    private var objectColor: Int = Color.WHITE
    private lateinit var profileImageView: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val selectedImageUri: Uri? = data?.data
            profileImageView.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_from_bottom, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, R.anim.slide_out_to_bottom)

        setContentView(R.layout.activity_profile_input)

        profileImageView = findViewById(R.id.profile_image_view)

        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        val context: Context = applicationContext
        try {
            val userProfile = UserProfile(context)
            val profileUpdateButton    = findViewById<ImageView>(R.id.profile_image_view)
            profileUpdateButton.setImageBitmap(userProfile.userIcon)

            val userNameEditText = findViewById<EditText>(R.id.user_name)
            userNameEditText.setText(userProfile.userName)

            val textColorPickerButton = findViewById<Button>(R.id.text_color_picker)
            textColorPickerButton.backgroundTintList = ColorStateList.valueOf(userProfile.textColor)

            val objectColorPickerButton = findViewById<Button>(R.id.object_color_picker)
            objectColorPickerButton.backgroundTintList = ColorStateList.valueOf(userProfile.objectColor)
        } catch (e: IllegalStateException) {
            val cancelButton = findViewById<ImageButton>(R.id.cancel_button)
            cancelButton.visibility = View.GONE

            val space = findViewById<Space>(R.id.cancel_space)
            val params = space.layoutParams as LinearLayout.LayoutParams
            params.width = 52
            params.height = 52
            space.layoutParams = params
        }
    }

    fun textColorPickerOnClick(view: View) {
        AmbilWarnaDialog(this, textColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
            }
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                textColor = color
                val textColorPickerButton = findViewById<Button>(R.id.text_color_picker)
                textColorPickerButton.backgroundTintList = ColorStateList.valueOf(color)
            }
        }).show()
    }

    fun objectColorPickerOnClick(view: View) {
        AmbilWarnaDialog(this, objectColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
            }
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                objectColor = color
                val objectColorPickerButton = findViewById<Button>(R.id.object_color_picker)
                objectColorPickerButton.backgroundTintList = ColorStateList.valueOf(color)
            }
        }).show()
    }

    fun saveProfileOnClick(view: View) {
        val userName: EditText  = findViewById<EditText>(R.id.user_name)
        val userIcon: ImageView = findViewById<ImageView>(R.id.profile_image_view)

        if (userName.text.toString().isEmpty()) {
            Toast.makeText(this, "ユーザー名を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        val context: Context = applicationContext

        val userNameFile = File(context.filesDir, "user_name.txt")
        userNameFile.writeText(userName.text.toString())

        val userIconFile           = File(context.filesDir, "user_icon.png")
        val userIconBitmap: Bitmap = (userIcon.drawable as BitmapDrawable).bitmap
        val userIconFileStream     = FileOutputStream(userIconFile)
        val scaledBitmap: Bitmap   = Bitmap.createScaledBitmap(userIconBitmap, 200, 200, false)
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, userIconFileStream)
        userIconFileStream.close()

        val textColorFile = File(context.filesDir, "text_color.txt")
        textColorFile.writeText(textColor.toString())

        val objectColorFile = File(context.filesDir, "object_color.txt")
        objectColorFile.writeText(objectColor.toString())

        val userProfile = UserProfile(context)
        if (userProfile.userId != 0) {
            UserProfileModel().updateUserProfile(userProfile)
        } else {
            UserProfileModel().insertUserProfile(userProfile, context)
        }

        val intent = Intent(this, ArMessageActivity::class.java)
        startActivity(intent)
    }

    fun CancelButtonOnClick(view: View) {
        finish()
    }

    override fun onBackPressed() {
        // 何も処理を行わない
    }
}