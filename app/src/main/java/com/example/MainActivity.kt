package com.example

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.BingeRepository
import com.example.ui.BingeModeApp
import com.example.ui.viewmodel.BingeViewModel
import com.example.ui.viewmodel.BingeViewModelFactory

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
        ContextCompat.registerReceiver(
            this,
            cancelReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            BingeModeApp(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        cancelReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }
}
