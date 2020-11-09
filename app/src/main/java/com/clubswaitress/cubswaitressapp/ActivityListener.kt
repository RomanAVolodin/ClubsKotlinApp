package com.clubswaitress.cubswaitressapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


public open class ActivityListener: AppCompatActivity() {

    override fun onUserInteraction() {
        super.onUserInteraction()
        Log.w("ActivityListener", "---------------   ACTIVITY DETECTED ---------------------------")
        stopHandler()
        startHandler()
    }

    var handler = Handler()

    var r  = Runnable {
        inactivityAction()
    }


    open fun inactivityAction() {
        stopHandler()
        Log.w("ActivityListener", "---------------   IIIIINNNNNACTIVITY DETECTED ---------------------------")
        val intent = Intent(applicationContext, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)

        MainActivity.currentUser = null
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        stopHandler()
        startHandler()

    }

    override fun onDestroy() {
        stopHandler()
        Log.w("ActivityListener", "---------------   DESTROY DETECTOR ---------------------------")
        super.onDestroy()
    }


    override fun onStop() {
        stopHandler()
        Log.w("ActivityListener", "---------------   STOP DETECTOR ---------------------------")
        super.onStop()
    }

    override fun onStart() {
        stopHandler()
        startHandler()
        super.onStart()

    }

    override fun onBackPressed() {

    }

    open fun stopHandler() {
        handler.removeCallbacks(r)
    }

    open fun startHandler() {
        val delayStr = MainActivity.iddleBeforeExit
        handler.postDelayed(r, delayStr * 1000)
    }

}