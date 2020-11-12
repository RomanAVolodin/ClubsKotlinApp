package com.clubswaitress.cubswaitressapp

import android.app.PendingIntent.getActivity
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
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        stopHandler()
        startHandler()

    }

    override fun onDestroy() {
        stopHandler()
        Log.w("ActivityListener", "---------------   DESTROY DETECTOR ${localClassName}---------------------------")
        super.onDestroy()
    }


    override fun onStop() {
//        activity.getWindow().getDecorView().getRootView().isShown()
//        val isVisible = this.window.decorView.isShown
        if (MainActivity.isTimerOfActivityEnabled) {
            stopHandler()
            Log.w("ActivityListener", "--------------- STOP DETECTOR ${localClassName}---------------------------")
        }

        super.onStop()
    }

    override fun onPause() {
        Log.w("ActivityListener", "--------------- ON PAUSE ${localClassName}---------------------------")
        MainActivity.isTimerOfActivityEnabled = false
        super.onPause()
    }


    override fun onStart() {
        Log.w("ActivityListener", "---------------   RESTART DETECTOR ${localClassName}---------------------------")
        MainActivity.isTimerOfActivityEnabled = true
        stopHandler()
        startHandler()
        super.onStart()

    }

    override fun onBackPressed() {

    }

    open fun stopHandler() {
        handler.removeCallbacks(r)
//        MainActivity.handler = Handler()
    }

    open fun startHandler() {
        val delayStr = MainActivity.iddleBeforeExit
        handler.postDelayed(r, delayStr * 1000)
    }

}