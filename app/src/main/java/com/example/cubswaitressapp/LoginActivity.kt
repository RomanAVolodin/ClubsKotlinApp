package com.example.cubswaitressapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.cubswaitressapp.Models.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
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

                println("success")
                println("URL: $fetch_url")

            } catch (cause: Throwable) {

                Log.w("TEST", cause.message)
                println("URL: $fetch_url")

                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "Ошибка авторизации или не тот терминал", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            if (user != null) {
                launch(Dispatchers.Main) {

                    progressDialog.dismiss()

                    Log.i(MainActivity.TAG, "try .... to savedusers creds")
                    val sharedPref = this@LoginActivity.getPreferences(Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("user_pass", login_activity_password.text.toString())
                        putString("user_name", login_activity_username.text.toString())


                        putString("ipAddress", ip_address_textfield_in_login_view.text.toString())
                        putString("terminalID", terminal_id_textfield_in_login_view.text.toString())
                        Log.i(MainActivity.TAG, "savedusers creds")
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

        Log.i(MainActivity.TAG, "try to get users creads")
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        val user_pass = sharedPref.getString("user_pass", "")
        val user_name = sharedPref.getString("user_name", "")

        val ipAddress = sharedPref.getString("ipAddress", "")
        val terminalID = sharedPref.getString("terminalID", "")

        Log.i(MainActivity.TAG, "try to get users creads user_pass: ${user_pass} user_name: ${user_name}")

        if (user_name != "" && user_pass != "") {
            login_activity_username.setText(user_name)
            login_activity_password.setText(user_pass)
//            fetchUserAndUpdateUI()
        }

        ip_address_textfield_in_login_view.setText(ipAddress)
        terminal_id_textfield_in_login_view.setText(terminalID)

        login_page_loader.visibility = View.GONE


        save_settings_button_in_login_view.setOnClickListener {
            val ipAddress = ip_address_textfield_in_login_view.text.toString()
            val terminalID = terminal_id_textfield_in_login_view.text.toString()

            val sharedPref = this@LoginActivity.getPreferences(Context.MODE_PRIVATE)
            with (sharedPref.edit()) {

                putString("ipAddress", ip_address_textfield_in_login_view.text.toString())
                putString("terminalID", terminal_id_textfield_in_login_view.text.toString())
                Log.i(MainActivity.TAG, "settings saved")
                commit()
            }

            MainActivity.currentHardwareID = terminal_id_textfield_in_login_view.text.toString()
            MainActivity.serverBaseUrl = ip_address_textfield_in_login_view.text.toString()

            settings_container_in_login_view.visibility = View.GONE
        }

        floatingActionButtonShowSettings.setOnClickListener {
            settings_container_in_login_view.visibility = View.VISIBLE
        }

        login_activity_login_button.setOnClickListener {
            hideKeyboard()

            val username = login_activity_username.text.toString()
            val password = login_activity_password.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Пожалуйста заполните все поля", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val ipAddress = ip_address_textfield_in_login_view.text.toString()
            val terminalID = terminal_id_textfield_in_login_view.text.toString()

            fetch_url = "${ipAddress}/users/auth"
            MainActivity.serverBaseUrl = ipAddress
            MainActivity.currentHardwareID = terminalID

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
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
