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

  private lateinit var cacheAdd10mbButton: Button
  private lateinit var cacheAdd100mbButton: Button
  private lateinit var cacheTextSize: TextView
  private lateinit var cacheClearButton: Button

  private lateinit var appDataAdd10mbButton: Button
  private lateinit var appDataAdd100mbButton: Button
  private lateinit var appDataTextSize: TextView
  private lateinit var appDataClearButton: Button

  private lateinit var totalSpaceTextView: TextView
  private lateinit var allocatableSpaceTextView: TextView

  private var storageMonitorJob: Job? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    cacheAdd10mbButton = findViewById(R.id.add_cache_10mb)
    cacheAdd10mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(10, CACHE) }
    }

    cacheAdd100mbButton = findViewById(R.id.add_cache_100mb)
    cacheAdd100mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(100, CACHE) }
    }
    cacheTextSize = findViewById(R.id.cache_size)

    cacheClearButton = findViewById(R.id.clear_cache_data)
    cacheClearButton.setOnClickListener {
      clearFiles(CACHE)
    }

    appDataAdd10mbButton = findViewById(R.id.add_app_data_10mb)
    appDataAdd10mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(10, APP_DATA) }
    }

    appDataAdd100mbButton = findViewById(R.id.add_app_data_100mb)
    appDataAdd100mbButton.setOnClickListener {
      scope.launch { allocateMegabytes(100, APP_DATA) }
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
      for (i in 0..sizeMegabytes) {
        val bytes = Random.nextUBytes(MEGABYTE).asByteArray()
        file.appendBytes(bytes)
      }
    } catch (e: IOException) {
      Toast.makeText(applicationContext, "Failed to write file $file", LENGTH_SHORT).show()
    }

    setSizesTextViews()
  }

  private fun setSizesTextViews() = scope.launch {
    totalSpaceTextView.text = (filesDir.totalSpace / MEGABYTE).toString() + " mb"
    allocatableSpaceTextView.text = (filesDir.usableSpace / MEGABYTE).toString() + " mb"

    var cacheSize: Long = 0
    for (cachedFile  in cacheDir.listFiles()) {
      val size = cachedFile.length()
      // Log.d(TAG, "$cachedFile - $size")
      cacheSize += size
    }
    cacheTextSize.text = (cacheSize / MEGABYTE).toString() + " mb"

    var appDataSize: Long = 0
    for (appFile  in filesDir.listFiles()) {
      val size = appFile.length()
      // Log.d(TAG, "$appFile - $size")
      appDataSize += size
    }
    appDataTextSize.text = (appDataSize / MEGABYTE).toString() + " mb"
  }

  private fun startStorageMonitorJob() {
    stopStorageMonitorJob()
    storageMonitorJob = scope.launch {
      while(true) {
        setSizesTextViews()
        delay(250)
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