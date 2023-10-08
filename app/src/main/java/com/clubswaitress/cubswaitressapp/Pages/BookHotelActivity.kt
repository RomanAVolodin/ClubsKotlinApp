package com.clubswaitress.cubswaitressapp.Pages

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.clubswaitress.cubswaitressapp.ActivityListener
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.Models.Client
import com.clubswaitress.cubswaitressapp.Models.RecordedActionsOrderedByRoom
import com.clubswaitress.cubswaitressapp.Models.RoomInHotel
import com.clubswaitress.cubswaitressapp.Models.User
import com.clubswaitress.cubswaitressapp.R
import com.google.zxing.integration.android.IntentIntegrator
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.android.synthetic.main.activity_bill.*
import kotlinx.android.synthetic.main.activity_book_hotel.*
import kotlinx.android.synthetic.main.activity_client_search.client_search_button
import kotlinx.android.synthetic.main.activity_client_search.client_search_field
import kotlinx.android.synthetic.main.activity_client_search.clients_list_recycler_vuiew
import kotlinx.android.synthetic.main.activity_client_search.scan_barcode_button
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.client_in_list_view.view.*
import kotlinx.android.synthetic.main.room_in_hotel_in_list_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext
import java.util.*
import java.util.regex.Pattern
import kotlin.concurrent.fixedRateTimer
import kotlin.coroutines.coroutineContext


class BookHotelActivity : ActivityListener() {
    companion object {
        val TAG = "HotelBookActivity"
        var BILL_ID = "BILL_ID"
    }

    val adaptor = GroupAdapter<ViewHolder>()
    val adaptorRooms = GroupAdapter<ViewHolder>()
    val search_url = "${MainActivity.serverBaseUrl}/clients/search"
    var create_client_url = "${MainActivity.serverBaseUrl}/clients/create"
    var rooms_url = "${MainActivity.serverBaseUrl}/hotel/shifts/active"
    var qr_code_url = "${MainActivity.serverBaseUrl}/clients/get-client-qr"

    var clients: List<Client> = emptyList()
    var roomsGlobalObject: RecordedActionsOrderedByRoom? = null
    var roomsInHotel: List<RoomInHotel> = emptyList()
    var chosenClient: Client? = null

    var updateTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_hotel)
        supportActionBar?.title = "Бронирование гостиницы"

        clients_list_recycler_vuiew.adapter = adaptor
        room_chooser_recycler_view.adapter = adaptorRooms


        client_search_button.setOnClickListener {
            if (client_search_field.text.isNotEmpty()) {
                fetchClientsAndUpdateUI()
            }
        }

        scan_barcode_button.setOnClickListener {
            scanQRCode()
        }

        cancel_add_user_button.setOnClickListener {
            user_add_container.visibility = View.GONE
        }

        add_client_button.setOnClickListener {
            add_client_last_name.text.clear()
            add_client_first_name.text.clear()
            add_client_phone.text.clear()
            add_client_email.text.clear()
            user_add_container.visibility = View.VISIBLE
            add_client_wron_email_caption.visibility = View.GONE
            add_client_wrong_firstname_caption.visibility = View.GONE
            add_client_wrong_lastname_caption.visibility = View.GONE
            add_client_wrong_phone_caption.visibility = View.GONE
        }

        val REG = "^(\\+7|7|8)?[\\s\\-]?\\(?[489][0-9]{2}\\)?[\\s\\-]?[0-9]{3}[\\s\\-]?[0-9]{2}[\\s\\-]?[0-9]{2}\$"
        val PATTERN: Pattern = Pattern.compile(REG)
        fun CharSequence.isPhoneNumber() : Boolean = PATTERN.matcher(this).find()

        add_client_proceed_button.setOnClickListener {
            add_client_wron_email_caption.visibility = View.GONE
            add_client_wrong_firstname_caption.visibility = View.GONE
            add_client_wrong_lastname_caption.visibility = View.GONE
            add_client_wrong_phone_caption.visibility = View.GONE

            if (add_client_last_name.text.isEmpty()) {
                add_client_wrong_lastname_caption.visibility = View.VISIBLE
            }

            if (add_client_first_name.text.isEmpty()) {
                add_client_wrong_firstname_caption.visibility = View.VISIBLE
            }

            if (add_client_phone.text.isEmpty()) {
                add_client_wrong_phone_caption.text = "номер телефона обязателен для заполнения"
                add_client_wrong_phone_caption.visibility = View.VISIBLE
            }

            if (add_client_first_name.text.isEmpty() || add_client_last_name.text.isEmpty() || add_client_phone.text.isEmpty()) {
                return@setOnClickListener
            }


            if (!add_client_email.text.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(add_client_email.text).matches()) {
                Toast.makeText(applicationContext, "Адрес электронной почты указан с ошибкой", Toast.LENGTH_LONG).show()
                add_client_wron_email_caption.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!add_client_phone.text.toString().isPhoneNumber()) {
                add_client_wrong_phone_caption.text = "номер телефона содержит ошибку"
                add_client_wrong_phone_caption.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!personal_data_agreement.isChecked) {
                add_client_agreement_caption.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Добавляю клиента в базу данных...")
            progressDialog.setCancelable(false)
            progressDialog.show()


            GlobalScope.launch(Dispatchers.IO) {
                try {
                    createClient()
                } catch (cause: Throwable) {
                    launch(Dispatchers.Main) {
                        Log.w(TAG, "ERRROr $cause")
                        Toast.makeText(applicationContext, "Ошибка при работе с сетью", Toast.LENGTH_LONG).show()
                        progressDialog.hide()
                    }
                    return@launch
                }
                launch(Dispatchers.Main) {
                    user_add_container.visibility = View.GONE
                    Toast.makeText(applicationContext, "Клиент добавлен в базу данных", Toast.LENGTH_LONG).show()
                    progressDialog.hide()

                    startChoosingRoom()
                }
            }
        }

        cancel_chosen_user.setOnClickListener {
            chosenClient = null
            stopChoosingRoom()
        }

        qr_code__small_view.setOnClickListener {
            fetchClientQRAndUpdateUI()
        }

        close_card_detailed_view.setOnClickListener {
            user_card_view.visibility = View.GONE
        }
    }

    fun startChoosingRoom() {
        choose_user_container.visibility = View.GONE
        choose_room_for_client.visibility = View.VISIBLE
        chosen_client_name.text = chosenClient?.full_name ?: ""
        chosen_client_phone.text = chosenClient?.phone ?: ""
    }

    fun fetchClientQRAndUpdateUI() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Получаю карту клиента...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                fetchClientQR()
            } catch (cause: Throwable) {
                launch(Dispatchers.Main) {
                    Log.w(TAG, "ERRROr $cause")
                    Toast.makeText(applicationContext, "Не удалось получить подробности карты клиента", Toast.LENGTH_LONG).show()
                    progressDialog.hide()
                }
                return@launch
            }

            launch(Dispatchers.Main) {
                val imageBytes = android.util.Base64.decode(chosenClient?.qr_code, android.util.Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                qr_code_big_view.setImageBitmap(decodedImage)
                qr_code__number_big_view.text = chosenClient?.card ?: ""
                user_name_big_view.text = chosenClient?.full_name ?: ""
                progressDialog.hide()
                user_card_view.visibility = View.VISIBLE
            }
        }
    }

    suspend fun fetchClientQR() {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
            Log.w(TAG, qr_code_url + "/" + chosenClient?.card)
            chosenClient?.qr_code = it.get(qr_code_url + "/" + chosenClient?.card) {}
        }
    }

    fun stopChoosingRoom() {
        choose_user_container.visibility = View.VISIBLE
        choose_room_for_client.visibility = View.GONE
    }

    suspend fun createClient() {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
            chosenClient = it.post(create_client_url) {
                fillHeadersCaseParameters()
            }
        }
    }

    private fun HttpRequestBuilder.fillHeadersCaseParameters() {
        body = FormDataContent(
            Parameters.build {
                append("first_name", add_client_first_name.text.toString())
                append("last_name", add_client_last_name.text.toString())
                append("phone", add_client_phone.text.toString())
                append("email", add_client_email.text.toString())
            }
        )
    }

    suspend fun fetchClients() {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
            clients = it.get(search_url) {
                parameter("search", client_search_field.text)
                //Клиенты ручной выбор
                if (MainActivity.currentUser?.isActionAllowed(113) == true) {
                    parameter("search_type", "all")
                }
            }
        }
    }


    fun fetchClientsAndUpdateUI() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Получаю список клиентов...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                fetchClients()
            } catch (cause: Throwable) {
                launch(Dispatchers.Main) {
                    Log.w(TAG, "ERRROr $cause")
                    progressDialog.hide()
                }
                return@launch
            }

            launch(Dispatchers.Main) {
                adaptor.clear()
                for (client in clients) {
                    adaptor.add(ClientItem(client))
                }

                adaptor.setOnItemClickListener { item, view ->
                    val user = item as ClientItem
                    Log.w(TAG, user.client.card)
                    chosenClient = user.client
                    Log.w(TAG, "Client been chosen, fetching rooms")
                    startChoosingRoom()
                }

                progressDialog.hide()
            }
        }
    }

    suspend fun fetchRooms() {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
                roomsGlobalObject = it.get(rooms_url) {
                roomsInHotel = roomsGlobalObject?.recordedActionsOrderedByRoom ?: emptyList()
            }
        }
    }

    fun fetchRoomsAndUpdateUI() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                fetchRooms()
            } catch (cause: Throwable) {
                launch(Dispatchers.Main) {
                    Log.w(TAG, "ERRROr $cause")
                    Toast.makeText(applicationContext, "Не удалось получить список комнат", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            launch(Dispatchers.Main) {
                adaptorRooms.clear()
                roomsInHotel = roomsInHotel.filter { it.closedAt == null }
                for (room in roomsInHotel) {
                    adaptorRooms.add(RoomInHotelItem(room, chosenClient))
                    Log.w(TAG, "room $room")
                }

                adaptorRooms.setOnItemClickListener { item, view ->
                    Log.w(TAG, item.id.toString())
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.hall_top_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chatlog_close_button -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun scanQRCode(){
        val integrator = IntentIntegrator(this).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(false)
            setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            setPrompt("Сканирование кода")
            setTorchEnabled(true)
        }
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) Toast.makeText(this, "Отмена", Toast.LENGTH_LONG).show()
            else  {
                Toast.makeText(this, "Отсканирован код: " + result.contents, Toast.LENGTH_LONG).show()
                client_search_field.setText(result.contents)
                fetchClientsAndUpdateUI()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTimer?.cancel()
        updateTimer?.purge()
    }


    override fun onStart() {
        super.onStart()
        Log.w("TEST", "welcome back")
        updateTimer = fixedRateTimer("timer",false, 0, 5000){
            this@BookHotelActivity.runOnUiThread {
                fetchRoomsAndUpdateUI()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        updateTimer?.cancel()
        updateTimer?.purge()
    }
}


class RoomInHotelItem(var room: RoomInHotel, val clientUser: Client?): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.hotel_room_number.text = room.room.roomNumber

        if (room.ocupationStatus == 2) {
            viewHolder.itemView.room_occupation_status.text = "Забронирован"
            viewHolder.itemView.room_occupation_status.setTextColor(Color.BLUE)
        } else {
            viewHolder.itemView.room_occupation_status.text = "Свободен"
            viewHolder.itemView.room_occupation_status.setTextColor(Color.GREEN)
        }

        if (room.startedAt != null) {
            var dateStart = ""
            try {
                dateStart = room.startedAt!!.split("T")[0]
                val hours = room.startedAt!!.split("T")[1].split(":")[0]
                val minutes = room.startedAt!!.split("T")[1].split(":")[1]
                dateStart = "$dateStart $hours:$minutes"
            } catch (_: Throwable){

            }
            viewHolder.itemView.room_started_at.text = dateStart
            viewHolder.itemView.room_duration_minutes.text = room.durationMinutes


            viewHolder.itemView.room_occupation_status.text = "Занят"
            viewHolder.itemView.room_occupation_status.setTextColor(Color.RED)

            viewHolder.itemView.room_started_at.visibility = View.VISIBLE
            viewHolder.itemView.room_duration_minutes.visibility = View.VISIBLE
            viewHolder.itemView.room_started_at_caption.visibility = View.VISIBLE
            viewHolder.itemView.room_duration_caption.visibility = View.VISIBLE
        } else {
            viewHolder.itemView.room_started_at.visibility = View.GONE
            viewHolder.itemView.room_duration_minutes.visibility = View.GONE
            viewHolder.itemView.room_started_at_caption.visibility = View.GONE
            viewHolder.itemView.room_duration_caption.visibility = View.GONE
            viewHolder.itemView.room_book_button.visibility = View.VISIBLE
        }

        if (room.ocupationStatus != 2 && room.startedAt == null) {
            viewHolder.itemView.room_book_button.visibility = View.VISIBLE
        } else {
            viewHolder.itemView.room_book_button.visibility = View.GONE
        }

        viewHolder.itemView.room_book_button.setOnClickListener {
            viewHolder.itemView.room_occupation_status.text = "Забронирован"
            viewHolder.itemView.room_occupation_status.setTextColor(Color.BLUE)
            viewHolder.itemView.room_book_button.visibility = View.GONE

            val progressDialog = ProgressDialog(viewHolder.itemView.context)
            progressDialog.setMessage("Идет бронирование номера...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    setOcupationStatus(clientUser)
                } catch (cause: Throwable) {
                    launch(Dispatchers.Main) {
                        Log.w(BookHotelActivity.TAG, "ERRROr $cause")
                        viewHolder.itemView.room_occupation_status.text = "Свободен"
                        viewHolder.itemView.room_occupation_status.setTextColor(Color.GREEN)
                        viewHolder.itemView.room_book_button.visibility = View.VISIBLE
                        progressDialog.dismiss()
                    }
                    return@launch
                }
                Thread.sleep(7000L)
                progressDialog.dismiss()
            }
        }
    }

    var rooms_book_url = "${MainActivity.serverBaseUrl}/hotel/recorded-action/"

    suspend fun setOcupationStatus(clientUser: Client?) {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
            room = it.patch(rooms_book_url + room.id.toString()) {
                body = FormDataContent(
                    Parameters.build {
                        append("ocupationStatus", "2")
                        if (clientUser != null) {
                            append("ocupiedByClientId", clientUser.id.toString())
                            append("ocupiedByClientName", clientUser.full_name)
                        }
                    }
                )
            }
        }
    }

    override fun getLayout(): Int {
        return R.layout.room_in_hotel_in_list_view
    }


}