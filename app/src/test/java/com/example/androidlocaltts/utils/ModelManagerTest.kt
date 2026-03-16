package com.example.androidlocaltts.utils

import android.content.Context
import android.content.res.AssetManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ModelManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockFilesDir: File

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockFilesDir = tempFolder.newFolder("files")
    }

    @Test
    fun testCopyAssets_CopiesRecursively() {
        // Mocking Context and AssetManager
        val context: Context = mock()
        val assetManager: AssetManager = mock()
        `when`(context.assets).thenReturn(assetManager)
        `when`(context.filesDir).thenReturn(mockFilesDir)

        // Mocking directory structure in assets:
        // kokoro/
        //   model.onnx
        //   espeak-ng-data/
        //     config.json
        `when`(assetManager.list("kokoro")).thenReturn(arrayOf("model.onnx", "espeak-ng-data"))
        `when`(assetManager.list("kokoro/espeak-ng-data")).thenReturn(arrayOf("config.json"))
        `when`(assetManager.list("kokoro/model.onnx")).thenReturn(arrayOf<String>()) // Not a dir

        // Mocking input streams
        `when`(assetManager.open("kokoro/model.onnx")).thenReturn(ByteArrayInputStream("model data".toByteArray()))
        `when`(assetManager.open("kokoro/espeak-ng-data/config.json")).thenReturn(ByteArrayInputStream("config data".toByteArray()))

        // When
        ModelManager.copyAssets(context)

        // Then
        val modelFile = File(mockFilesDir, "kokoro/model.onnx")
        val configFile = File(mockFilesDir, "kokoro/espeak-ng-data/config.json")
        
        assertTrue("model.onnx should exist", modelFile.exists())
        assertTrue("espeak-ng-data/config.json should exist", configFile.exists())
    }

    @Test
    fun testIsModelPrepared_ReturnsFalseWhenFilesMissing() {
        // Given an empty directory
        val modelDir = File(mockFilesDir, "kokoro")
        modelDir.mkdirs()

        // When
        val result = ModelManager.isModelPrepared(mockFilesDir)
        
        // Then
        assertFalse("Should return false when files are missing", result)
    }

    @Test
    fun testIsModelPrepared_ReturnsTrueWhenAllFilesExist() {
        // Given a directory with ALL essential files
        val modelDir = File(mockFilesDir, "kokoro")
        modelDir.mkdirs()
        File(modelDir, "model.onnx").createNewFile()
        File(modelDir, "tokens.txt").createNewFile()
        File(modelDir, "voices.bin").createNewFile()
        File(modelDir, "espeak-ng-data").mkdirs()

        // When
        val result = ModelManager.isModelPrepared(mockFilesDir)
        
        // Then
        assertTrue("Should return true when ALL essential files/dirs exist", result)
    }

    @Test
    fun testIsModelPrepared_ReturnsFalseWhenPartialFilesMissing() {
        // Given a directory with only SOME essential files
        val modelDir = File(mockFilesDir, "kokoro")
        modelDir.mkdirs()
        File(modelDir, "model.onnx").createNewFile()
        File(modelDir, "tokens.txt").createNewFile()
        // voices.bin and espeak-ng-data are missing

        // When
        val result = ModelManager.isModelPrepared(mockFilesDir)
        
        // Then
        assertFalse("Should return false when voices.bin or espeak-ng-data is missing", result)
    }

    @Test
    fun testIsModelPrepared_ReturnsFalseWhenDataDirIsFile() {
        // Given a directory with essential files but espeak-ng-data is a file
        val modelDir = File(mockFilesDir, "kokoro")
        modelDir.mkdirs()
        File(modelDir, "model.onnx").createNewFile()
        File(modelDir, "tokens.txt").createNewFile()
        File(modelDir, "voices.bin").createNewFile()
        File(modelDir, "espeak-ng-data").createNewFile()

        // When
        val result = ModelManager.isModelPrepared(mockFilesDir)

        // Then
        assertFalse("Should return false when espeak-ng-data is not a directory", result)
    }
}
