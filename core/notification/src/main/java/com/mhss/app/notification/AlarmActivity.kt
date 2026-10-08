package com.mhss.app.notification

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mhss.app.ui.R
import com.mhss.app.util.Constants

class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 TÄND SKÄRM + VISA ÖVER LÅSSKÄRM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm)

        // ---- HÄMTA TASK-ID ----
        val taskId = intent.getIntExtra(Constants.TASK_ID_EXTRA, 0)

        // ---- COMPLETE-KNAPP ----
        val completeButton: Button = findViewById(R.id.completeButton)

        completeButton.setOnClickListener {
            if (taskId != 0) {
                val completeIntent =
                    Intent(this, TaskActionButtonBroadcastReceiver::class.java).apply {
                        action = Constants.ACTION_COMPLETE
                        putExtra(Constants.TASK_ID_EXTRA, taskId)
                    }

                sendBroadcast(completeIntent)
            }

            finish()
        }

    }
}
