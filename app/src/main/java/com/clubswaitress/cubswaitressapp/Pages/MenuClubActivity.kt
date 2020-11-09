package com.clubswaitress.cubswaitressapp.Pages

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.clubswaitress.cubswaitressapp.R
import kotlinx.android.synthetic.main.activity_menu_club.*

class MenuClubActivity : AppCompatActivity() {
    companion object {
        var server_ip = ""
        var club_url = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_club)

        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Меню"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setDisplayHomeAsUpEnabled(true)

        val url = "${server_ip}/webmenus/${club_url}/qr-menu.html"
        Log.w("TEST", "${server_ip}/webmenus/${club_url}/qr-menu.html")

        supportActionBar?.hide()
        menu_web_view.loadUrl(url)

        this.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
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