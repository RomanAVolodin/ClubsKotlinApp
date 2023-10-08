package com.clubswaitress.cubswaitressapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.clubswaitress.cubswaitressapp.Models.User
import com.clubswaitress.cubswaitressapp.Pages.*

class MainActivity : ActivityListener(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        val TAG = "MainPageActivity"
        var currentUser: User? = null
        var currentHardwareID  = ""
        var club_menu_url = ""
        var serverBaseUrl = ""
        val updateTimer: Long = 1000
        var iddleBeforeExit: Long = 60000

        var isTimerOfActivityEnabled = false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verifyIfUserIsLoggedin()

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Тут будет чат с сотрудниками", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        fab.visibility = View.GONE
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        startLockTask()

    }


    override fun onStart() {
        super.onStart()
        Log.i(TAG, "Started ACTIVITY")
        showHallsFragment()
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                callUserChooseActivity()
                return true
            }
            R.id.action_exit -> {
                currentUser = null

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_home -> {
                showHallsFragment()
            }

            R.id.nav_book_hotel-> {
                callHotelActivity()
            }

            R.id.nav_check_card-> {
                callUserChooseActivity()
            }

            R.id.nav_menu_rest -> {
                showMenuWebFragment()
            }

        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    val CHOOSE_CLIENT = 100
    private fun callUserChooseActivity(){
        val intent = Intent(this, ClientSimpleSearchActivity::class.java)
        startActivityForResult(intent, CHOOSE_CLIENT)
    }

    private fun callHotelActivity(){
        val intent = Intent(this, BookHotelActivity::class.java)
        startActivityForResult(intent, CHOOSE_CLIENT)
    }


    fun showHallsFragment() {
        val meetingPage = HallsListFragment.newInstance()
        replaceFragmenty(
            fragment = meetingPage as Fragment,
            allowStateLoss = true,
            containerViewId = R.id.mainContent
        )
        title = "${MainActivity.currentUser?.username}, ${MainActivity.currentUser?.hardware_name}"

    }

    fun showMenuWebFragment() {
        val meetingPage = MenuWebFragment.newInstance()
        replaceFragmenty(
            fragment = meetingPage as Fragment,
            allowStateLoss = true,
            containerViewId = R.id.mainContent
        )
        title = "Меню ресторана"

    }

    fun showMenuWebBarFragment() {
        val meetingPage = MenuWebBarFragment.newInstance()
        replaceFragmenty(
            fragment = meetingPage as Fragment,
            allowStateLoss = true,
            containerViewId = R.id.mainContent
        )
        title = "Меню бара"

    }

    private fun verifyIfUserIsLoggedin() {
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}

