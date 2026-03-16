package com.example.androidlocaltts.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ModelManager {
    private const val TAG = "ModelManager"
    private const val ASSET_DIR = "kokoro"

    fun getModelDir(context: Context): File {
        return File(context.filesDir, ASSET_DIR)
    }

    fun isModelPrepared(context: Context): Boolean {
        val modelDir = getModelDir(context)
        // Check for essential files: model.onnx and tokens.txt
        val modelFile = File(modelDir, "model.onnx")
        val tokensFile = File(modelDir, "tokens.txt")
        return modelFile.exists() && tokensFile.exists()
    }

    fun copyAssets(context: Context): Boolean {
        val modelDir = getModelDir(context)
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }

        return try {
            val assets = context.assets.list(ASSET_DIR) ?: return false
            for (asset in assets) {
                if (asset == ".gitkeep") continue
                
                val assetPath = "$ASSET_DIR/$asset"
                val outFile = File(modelDir, asset)
                
                // For simplicity, always copy if not exists or size differs
                // In production, use versioning or checksums
                copyFile(context, assetPath, outFile)
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error copying assets", e)
            false
        }
    }

    private fun copyFile(context: Context, assetPath: String, outFile: File) {
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(outFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.d(TAG, "Copied $assetPath to ${outFile.absolutePath}")
    }
}
