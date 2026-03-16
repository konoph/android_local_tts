package com.example.androidlocaltts.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.androidlocaltts.R
import com.example.androidlocaltts.utils.ModelManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)

        val isPrepared = ModelManager.isModelPrepared(filesDir)
        statusText.text = if (isPrepared) {
            "モデル準備完了: Kokoro-82M (Sherpa-ONNX)"
        } else {
            "モデル未準備: assets/kokoro にモデルファイルを配置してください"
        }

        openSettingsButton.setOnClickListener {
            val intent = Intent()
            intent.action = "com.android.settings.TTS_SETTINGS"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}
