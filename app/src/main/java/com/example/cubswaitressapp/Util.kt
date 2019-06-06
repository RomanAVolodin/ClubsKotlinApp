package com.example.cubswaitressapp

import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.IdRes

/**
 * Created by nir21 on 23-01-2018.
 */
fun AppCompatActivity.replaceFragmenty(fragment: androidx.fragment.app.Fragment,
                                       allowStateLoss: Boolean = false,
                                       @IdRes containerViewId: Int) {
    val ft = supportFragmentManager
            .beginTransaction()
            .replace(containerViewId, fragment)
    if (!supportFragmentManager.isStateSaved) {
        ft.commit()
    } else if (allowStateLoss) {
        ft.commitAllowingStateLoss()
    }
}