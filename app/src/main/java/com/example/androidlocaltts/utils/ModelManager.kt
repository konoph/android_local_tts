package com.example.androidlocaltts.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ModelManager {
    private const val TAG = "ModelManager"
    private const val ASSET_DIR = "kokoro"

    fun getModelDir(baseDir: File): File {
        return File(baseDir, ASSET_DIR)
    }

    fun isModelPrepared(baseDir: File): Boolean {
        val modelDir = getModelDir(baseDir)
        // Check for essential files: model.onnx, tokens.txt, voices.bin, and espeak-ng-data dir
        val modelFile = File(modelDir, "model.onnx")
        val tokensFile = File(modelDir, "tokens.txt")
        val voicesFile = File(modelDir, "voices.bin")
        val dataDir = File(modelDir, "espeak-ng-data")
        
        return modelFile.exists() && tokensFile.exists() && 
               voicesFile.exists() && dataDir.exists()
    }

    fun copyAssets(context: Context): Boolean {
        return try {
            copyAssetDir(context, ASSET_DIR, getModelDir(context.filesDir))
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error copying assets", e)
            false
        }
    }

    private fun copyAssetDir(context: Context, assetPath: String, destFile: File) {
        val assets = context.assets.list(assetPath)
        if (assets != null && assets.isNotEmpty()) {
            // It's a directory
            if (!destFile.exists()) {
                destFile.mkdirs()
            }
            for (asset in assets) {
                if (asset == ".gitkeep") continue
                val subAssetPath = "$assetPath/$asset"
                val subDestFile = File(destFile, asset)
                copyAssetDir(context, subAssetPath, subDestFile)
            }
        } else {
            // It might be a file or an empty directory
            try {
                copyFile(context, assetPath, destFile)
            } catch (e: IOException) {
                // It was likely an empty directory, or a real error
                Log.d(TAG, "Skipping $assetPath (could be an empty directory or missing file)")
            }
        }
    }

    private fun copyFile(context: Context, assetPath: String, outFile: File) {
        // Create parent directories if they don't exist (handle case where list() might be ambiguous)
        outFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(outFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.d(TAG, "Copied $assetPath to ${outFile.absolutePath}")
    }
}
