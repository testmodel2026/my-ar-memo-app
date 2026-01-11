package com.shimizu.ar.core.ar_message.kotlin.message_text_input

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shimizu.ar.core.ar_message.kotlin.ar_message.ArMessageActivity
import com.shimizu.ar.core.ar_message.kotlin.ar_message.R
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File


class MessageTextInputActivity: AppCompatActivity() {

    var textColor: Int   = Color.BLACK
    var objectColor: Int = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_from_bottom, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, R.anim.slide_out_to_bottom)

        // 設定値で上書き
        val context: Context = applicationContext
        textColor   = File(context.filesDir, "text_color.txt").readText().toInt()
        objectColor = File(context.filesDir, "object_color.txt").readText().toInt()

        setContentView(R.layout.activity_message_text_input)

        val textColorPickerButton = findViewById<Button>(R.id.text_color_picker)
        textColorPickerButton.setTextColor(textColor)

        val objectColorPickerButton = findViewById<Button>(R.id.object_color_picker)
        objectColorPickerButton.backgroundTintList = ColorStateList.valueOf(objectColor)

        val editText = findViewById<EditText>(R.id.input_message_text)
        editText.requestFocus()
    }

    fun PostMessageTextOnClick(view: View) {
        val intent     = Intent(this, ArMessageActivity::class.java)
        val editText   = findViewById<EditText>(R.id.input_message_text)
        val exportText = insertLineBreaks(editText.text.toString())
        if (textValidations(exportText)) {
            Toast.makeText(this, "行数がオーバーしています。", Toast.LENGTH_SHORT).show()
            return
        }
        intent.putExtra("MESSAGE_TEXT", exportText)
        intent.putExtra("TEXT_COLOR", textColor)
        intent.putExtra("OBJECT_COLOR", objectColor)
        startActivity(intent)
    }

    private fun insertLineBreaks(text: String): String {
        var testList: List<String> =  text.split("\n")
        val newTextList: MutableList<String> = mutableListOf<String>()

        for (i in testList.indices) {
            newTextList.add(testList[i].chunked(15).joinToString("\n"))
        }

        return newTextList.joinToString("\n")
    }

    private fun textValidations(text: String): Boolean {
        return text.split("\n").size > 5
    }

    fun textColorPickerOnClick(view: View) {
        AmbilWarnaDialog(this, textColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
            }
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                textColor = color
                val textColorPickerButton = findViewById<Button>(R.id.text_color_picker)
                textColorPickerButton.setTextColor(color)
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

    fun CancelButtonOnClick(view: View) {
        finish()
    }
}