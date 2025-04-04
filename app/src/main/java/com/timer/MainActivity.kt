package com.timer

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.timer.services.TimerService
import com.timer.ui.theme.TimerTheme
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val MAX_MINUTES_LENGTH  = 2

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestNotificationPermission(this)
        enableEdgeToEdge()
        setContent {
            TimerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Timer(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Timer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var timerValue by rememberSaveable { mutableStateOf("") }
    val timeRemaining by TimerService.remainingTime.collectAsState()
    var isTimerActive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(timeRemaining) {
        isTimerActive = timeRemaining > 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "До истечения таймера: ${formatTime(timeRemaining)}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomEditText(
            onEntered = {
                if (it.length <= MAX_MINUTES_LENGTH && it.all { char -> char.isDigit() }) {
                    timerValue = it
                }
            },
            keyboardType = KeyboardType.Decimal,
            input = timerValue,
            focusRequester = focusRequester
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick =
                {
                    if (!isTimerActive) {

                        val action = when {
                            timeRemaining > 0 -> TimerService.ACTION_RESUME
                            timerValue.toIntOrNull()?.takeIf { it > 0 } != null -> TimerService.ACTION_START
                            else -> null
                        }

                        action?.let {
                            val intent = Intent(context, TimerService::class.java).apply {
                                this.action = it
                                if (it == TimerService.ACTION_START) {
                                    putExtra(TimerService.INTENT_EXTRA_KEY_TIME_IN_MINUTES, timerValue.toInt())
                                }
                            }
                            ContextCompat.startForegroundService(context, intent)
                        }

                        focusManager.clearFocus()
                    }
                },
                enabled = !isTimerActive,
            ) {
                Text("Старт")
            }

            Button(onClick =
                {
                    if (isTimerActive) {
                        val intent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP
                        }
                        context.startService(intent)
                        isTimerActive = false
                    }
                }
            ) {
                Text("Стоп")
            }

            Button(onClick =
                {
                    if (timeRemaining > 0) {
                        val intent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_RESET
                        }
                        context.startService(intent)
                    }
                }
            ) {
                Text("Сброс")
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val minutes = millis / 60000
    val seconds = (millis % 60000) / 1000
    return "%02d:%02d".format(minutes, seconds)
}

fun checkAndRequestNotificationPermission(context: Context) {
    val notificationManager = NotificationManagerCompat.from(context)

    if (!notificationManager.areNotificationsEnabled()) {
        AlertDialog.Builder(context)
            .setTitle("Уведомления отключены")
            .setMessage("Для корректной работы приложения включите уведомления.")
            .setPositiveButton("Включить") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
