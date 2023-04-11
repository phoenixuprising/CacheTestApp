package com.example.cachetest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import com.example.cachetest.Location.APP_DATA
import com.example.cachetest.Location.CACHE
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextUBytes

class MainActivity() : AppCompatActivity() {
  private val TAG = "cach_test"

  private val scope = MainScope()

  private lateinit var cacheAdd1mbButton: Button
  private lateinit var cacheAdd10mbButton: Button
  private lateinit var cacheAdd100mbButton: Button
  private lateinit var cacheAdd1000mbButton: Button
  private lateinit var cacheTextSize: TextView
  private lateinit var cacheClearButton: Button

  private lateinit var appDataAdd1mbButton: Button
  private lateinit var appDataAdd10mbButton: Button
  private lateinit var appDataAdd100mbButton: Button
  private lateinit var appDataAdd1000mbButton: Button
  private lateinit var appDataTextSize: TextView
  private lateinit var appDataClearButton: Button

  private lateinit var totalSpaceTextView: TextView
  private lateinit var allocatableSpaceTextView: TextView

  private var storageMonitorJob: Job? = null

  lateinit var totalSpaceText: String
  lateinit var allocatableSpaceText: String
  lateinit var appDataSizeText: String
  lateinit var cacheSizeText: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    cacheAdd1mbButton = findViewById(R.id.add_cache_1mb)
    cacheAdd1mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(1, CACHE) }
    }
    cacheAdd10mbButton = findViewById(R.id.add_cache_10mb)
    cacheAdd10mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(10, CACHE) }
    }
    cacheAdd100mbButton = findViewById(R.id.add_cache_100mb)
    cacheAdd100mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(100, CACHE) }
    }
    cacheAdd1000mbButton = findViewById(R.id.add_cache_1000mb)
    cacheAdd1000mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(1000, CACHE) }
    }
    cacheTextSize = findViewById(R.id.cache_size)
    cacheClearButton = findViewById(R.id.clear_cache_data)
    cacheClearButton.setOnClickListener {
      clearFiles(CACHE)
    }

    appDataAdd1mbButton = findViewById(R.id.add_app_data_1mb)
    appDataAdd1mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(1, APP_DATA) }
    }
    appDataAdd10mbButton = findViewById(R.id.add_app_data_10mb)
    appDataAdd10mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(10, APP_DATA) }
    }
    appDataAdd100mbButton = findViewById(R.id.add_app_data_100mb)
    appDataAdd100mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(100, APP_DATA) }
    }
    appDataAdd1000mbButton = findViewById(R.id.add_app_data_1000mb)
    appDataAdd1000mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(1000, APP_DATA) }
    }
    appDataClearButton = findViewById(R.id.clear_app_data)
    appDataClearButton.setOnClickListener {
      clearFiles(APP_DATA)
    }
    allocatableSpaceTextView = findViewById(R.id.allocatable_space)
    totalSpaceTextView = findViewById(R.id.total_space)


    appDataTextSize = findViewById(R.id.app_size)

    startStorageMonitorJob()
  }

  private fun clearFiles(location: Location) {
    val dir = when (location) {
      CACHE -> cacheDir
      APP_DATA -> filesDir
    }

    for (file in dir.listFiles()) {
      file.delete()
    }
    setSizesTextViews()
  }

  private fun allocateMegabytes(sizeMegabytes: Int, location: Location) = scope.launch {
    Log.d(TAG, "Allocating $sizeMegabytes to $location")

    val dir = when (location) {
      CACHE -> cacheDir
      APP_DATA -> filesDir
    }

    val file = File(dir, UUID.randomUUID().toString() + ".bin")
    try {
      for (i in 1..sizeMegabytes) {
        val bytes = Random.nextUBytes(MEGABYTE).asByteArray()
        file.appendBytes(bytes)
      }
    } catch (e: IOException) {
      Toast.makeText(applicationContext, "Failed to write file $file", LENGTH_SHORT).show()
    }

    setSizesTextViews()
  }

  private fun setSizesTextViews() = scope.launch {
    totalSpaceText = (filesDir.totalSpace / MEGABYTE).toString() + "mb"
    totalSpaceTextView.text = totalSpaceText
    allocatableSpaceText = (filesDir.usableSpace / MEGABYTE).toString() + "mb"
    allocatableSpaceTextView.text = allocatableSpaceText

    var cacheSize: Long = 0
    for (cachedFile  in cacheDir.listFiles()) {
      val size = cachedFile.length()
      // Log.d(TAG, "$cachedFile - $size")
      cacheSize += size
    }
    cacheSizeText = (cacheSize / MEGABYTE).toString() + "mb"
    cacheTextSize.text = cacheSizeText

    var appDataSize: Long = 0
    for (appFile  in filesDir.listFiles()) {
      val size = appFile.length()
      // Log.d(TAG, "$appFile - $size")
      appDataSize += size
    }
    appDataSizeText = (appDataSize / MEGABYTE).toString() + "mb"
    appDataTextSize.text = appDataSizeText
  }

  private fun logSizes() = scope.launch {
    Log.d(TAG, "%s: %s".format("allocatableSpace", allocatableSpaceText))
    Log.d(TAG, "%s: %s".format("totalSpace", totalSpaceText))
    Log.d(TAG, "%s: %s".format("cacheSize", cacheSizeText))
    Log.d(TAG, "%s: %s".format("appData", appDataSizeText))
  }

  private fun startStorageMonitorJob() {
    stopStorageMonitorJob()
    storageMonitorJob = scope.launch {
      var i = 0
      while(true) {
        setSizesTextViews()
        if (i == 0) logSizes()
        delay(250)
        i += 1
        i %= 4
      }
    }
  }

  private fun stopStorageMonitorJob() {
    storageMonitorJob?.cancel()
    storageMonitorJob = null
  }
}

const val MEGABYTE = 1_048_576

enum class Location {
  CACHE,
  APP_DATA
}