package com.clubswaitress.cubswaitressapp.Pages

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import com.clubswaitress.cubswaitressapp.R
import kotlinx.android.synthetic.main.activity_menu_club.*


class MenuClubActivity : AppCompatActivity() {
    companion object {
        var server_ip = ""
        var club_url = ""
    }

    private fun isNetworkConnected(): Boolean {
        val cm =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_club)

        var counter_for_cache = 0

        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Меню"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setDisplayHomeAsUpEnabled(true)

//        val url = "${server_ip}/webmenus/${club_url}/qr-menu.html"
        val url = "${server_ip}/webmenus/main.menu/qr-menu.html"
        Log.w("TEST", "${server_ip}/webmenus/main.menu/qr-menu.html")

        supportActionBar?.hide()


        menu_web_view.getSettings().setJavaScriptEnabled(true)
        menu_web_view.getSettings().setAppCacheEnabled(true)

        if (isNetworkConnected()) {
            menu_web_view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            Log.w("TEST", "LOAD WITHOUT CACHE")
        } else {
            menu_web_view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
            Log.w("TEST", "LOAD CACHE")
        }


        menu_web_view.loadUrl(url)

        this.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        clear_cache_button.setOnClickListener {
            menu_web_view.clearCache(true)
            menu_web_view.loadUrl(url)

            clear_cache_button.visibility = View.GONE
        }

        show_cache_button_1.setOnClickListener {
            counter_for_cache += 1
            if (counter_for_cache != 1) {
                counter_for_cache = 0
            }
            Log.w("test", counter_for_cache.toString())
        }
        show_cache_button_2.setOnClickListener {
            counter_for_cache += 1
            if (counter_for_cache != 2) {
                counter_for_cache = 0
            }
            Log.w("test", counter_for_cache.toString())
        }
        show_cache_button_3.setOnClickListener {
            counter_for_cache += 1
            Log.w("test", counter_for_cache.toString())
            if (counter_for_cache == 3) {
                clear_cache_button.visibility = View.VISIBLE
            } else {
                counter_for_cache = 0
            }
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_test, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
//        R.id.action_fav -> {
//            val url = "${server_ip}/webmenus/${club_url}/qr-menu.html"
//            Log.w("TEST", "${server_ip}/webmenus/${club_url}/qr-menu.html")
//            menu_web_view.loadUrl(url)
//            true
//        }
//        else -> super.onOptionsItemSelected(item)
//    }


}