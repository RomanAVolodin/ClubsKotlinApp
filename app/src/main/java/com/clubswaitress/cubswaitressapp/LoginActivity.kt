package com.clubswaitress.cubswaitressapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.clubswaitress.cubswaitressapp.Models.User
import com.clubswaitress.cubswaitressapp.Pages.MenuClubActivity
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.Parameters
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {

    var fetch_url = "${MainActivity.serverBaseUrl}/users/auth"

    var user: User? = null

    companion object {
        var mainActivity: MainActivity? = null
    }

    override fun onBackPressed() {

    }

    suspend fun fetchUser() {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            user = it.post(fetch_url) {
                fillHeadersCaseParameters()
            }
        }
    }

    private fun HttpRequestBuilder.fillHeadersCaseParameters() {

        body = FormDataContent( // создаем параметры, которые будут переданы в form
            Parameters.build {
                append("login", login_activity_username.text.toString())
                append("password", login_activity_password.text.toString())
                append("hardware_id", terminal_id_textfield_in_login_view.text.toString())
            }
        )

    }

    fun fetchUserAndUpdateUI() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Проверяю данные пользователя...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {

            try {
                fetchUser()
            } catch (cause: Throwable) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "Ошибка авторизации или не тот терминал", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            if (user != null) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    val sharedPref = this@LoginActivity.getPreferences(Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("user_pass", login_activity_password.text.toString())
                        putString("user_name", login_activity_username.text.toString())
                        putString("ipAddress", ip_address_textfield_in_login_view.text.toString())
                        putString("terminalID", terminal_id_textfield_in_login_view.text.toString())
                        putString("edit_text_delay_to_exit_time", edit_text_delay_to_exit_time.text.toString())
                        commit()
                    }

                    MainActivity.currentUser = user

                    finish()
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.title = "XPOS mobile v.${BuildConfig.VERSION_NAME}"

        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        val user_pass = sharedPref.getString("user_pass", "")
        val user_name = sharedPref.getString("user_name", "")

        val ipAddress = sharedPref.getString("ipAddress", "")
        val terminalID = sharedPref.getString("terminalID", "")
        val club_menu_url = sharedPref.getString("club_menu_url", "")
        val delayTime = sharedPref.getString("delayTime", "60")

        val adapter = ArrayAdapter.createFromResource(this,
            R.array.clubs_list, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_menu_club.adapter = adapter

        if (club_menu_url != "") {
            val index = adapter.getPosition(club_menu_url)
            spinner_menu_club.setSelection(index)
        }

        Log.i(MainActivity.TAG, "try to get users creads user_pass: ${delayTime}")

        if (user_name != "" && user_pass != "") {
//            login_activity_username.setText(user_name)
//            login_activity_password.setText(user_pass)
            edit_text_delay_to_exit_time.setText(delayTime)
        }

        ip_address_textfield_in_login_view.setText(ipAddress)
        terminal_id_textfield_in_login_view.setText(terminalID)

        login_page_loader.visibility = View.GONE

        imageButton_for_menu.setOnClickListener{
            val intent = Intent(this@LoginActivity, MenuClubActivity::class.java)
            MenuClubActivity.club_url = spinner_menu_club.selectedItem.toString()
            MenuClubActivity.server_ip = "http://" + ip_address_textfield_in_login_view.text.toString()
            MenuClubActivity.menuType = "old"
            ContextCompat.startActivity(this, intent, null)
        }

        imageButton_for_menu_new.setOnClickListener{
            val intent = Intent(this@LoginActivity, MenuClubActivity::class.java)
            MenuClubActivity.club_url = spinner_menu_club.selectedItem.toString()
            MenuClubActivity.server_ip = "http://" + ip_address_textfield_in_login_view.text.toString()
            MenuClubActivity.menuType = "new"
            ContextCompat.startActivity(this, intent, null)
        }

        save_settings_button_in_login_view.setOnClickListener {
            val ipAddress = ip_address_textfield_in_login_view.text.toString()
            val terminalID = terminal_id_textfield_in_login_view.text.toString()
            val delayTime = edit_text_delay_to_exit_time.text.toString()
            val club_menu_url = spinner_menu_club.selectedItem.toString()

            val sharedPref = this@LoginActivity.getPreferences(Context.MODE_PRIVATE)
            with (sharedPref.edit()) {

                putString("ipAddress", ip_address_textfield_in_login_view.text.toString())
                putString("terminalID", terminal_id_textfield_in_login_view.text.toString())
                putString("club_menu_url", spinner_menu_club.selectedItem.toString())
                putString("delayTime", delayTime)

                Log.i(MainActivity.TAG, "settings saved")
                commit()
            }

            MainActivity.currentHardwareID = terminal_id_textfield_in_login_view.text.toString()
            MainActivity.serverBaseUrl = "http://" + ip_address_textfield_in_login_view.text.toString()
            MainActivity.club_menu_url = spinner_menu_club.selectedItem.toString()
            MainActivity.iddleBeforeExit = edit_text_delay_to_exit_time.text.toString().toLong()

            pin_code_TextPassword.setText("")
            settings_container_in_login_view.visibility = View.GONE

        }

        update_app_button.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("http://aurora.zavods.net/clubs.apk"))
            startActivity(browserIntent)

        }

        floatingActionButtonShowSettings.setOnClickListener {
            settings_container_in_login_view.visibility = View.VISIBLE
            pin_code_container.visibility = View.VISIBLE
            pin_code_TextPassword.setText("")
        }

        pin_cancel_button.setOnClickListener {
            pin_code_container.visibility = View.GONE
            settings_container_in_login_view.visibility = View.GONE
        }

        pin_ok_button.setOnClickListener {
            if (pin_code_TextPassword.text.toString() == "17112000") {
                pin_code_container.visibility = View.GONE
            }
        }

        login_activity_login_button.setOnClickListener {
            hideKeyboard()

            val username = login_activity_username.text.toString()
            val password = login_activity_password.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this, "Пожалуйста заполните все поля", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val ipAddress = "http://" + ip_address_textfield_in_login_view.text.toString()
            val terminalID = terminal_id_textfield_in_login_view.text.toString()

            fetch_url = "${ipAddress}/users/auth"
            MainActivity.serverBaseUrl = ipAddress
            MainActivity.currentHardwareID = terminalID
            MainActivity.club_menu_url = spinner_menu_club.selectedItem.toString()
            MainActivity.iddleBeforeExit = edit_text_delay_to_exit_time.text.toString().toLong()

            if (ipAddress.isEmpty() || terminalID.isEmpty()) {
                Toast.makeText(this, "Пожалуйста заполните настройки", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            fetchUserAndUpdateUI()

        }

    }

    fun androidx.fragment.app.Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Activity.hideKeyboard() {
        if (currentFocus == null) View(this) else currentFocus?.let { hideKeyboard(it) }
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
