package com.example

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.BingeRepository
import com.example.ui.BingeModeApp
import com.example.ui.viewmodel.BingeViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: BingeViewModel
    private var cancelReceiver: BroadcastReceiver? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val repository = BingeRepository(applicationContext)
        val viewModelFactory = BingeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[BingeViewModel::class.java]

        cancelReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: Intent) {
                if (intent.action == "com.example.ACTION_CANCEL_SIMULATION") {
                    viewModel.cancelSimulation()
                }
            }
        }

        val filter = IntentFilter("com.example.ACTION_CANCEL_SIMULATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cancelReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(cancelReceiver, filter)
        }

        setContent {
            BingeModeApp(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        cancelReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }
}
