package com.clubswaitress.cubswaitressapp.Pages

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.clubswaitress.cubswaitressapp.ActivityListener
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.Models.Client
import com.clubswaitress.cubswaitressapp.R
import com.google.zxing.integration.android.IntentIntegrator
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.android.synthetic.main.activity_client_search.*
import kotlinx.android.synthetic.main.client_in_list_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ClientSearchActivity : ActivityListener() {
    companion object {
        val TAG = "ClientSearchActivity"
        var BILL_ID = "BILL_ID"
    }

    val adaptor = GroupAdapter<ViewHolder>()
    val fetch_url = "${MainActivity.serverBaseUrl}/clients"
    val search_url = "${MainActivity.serverBaseUrl}/clients/search"

    var clients: List<Client> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_search)
        supportActionBar?.title = "Работа с клиентом счета"

        clients_list_recycler_vuiew.adapter = adaptor


        client_search_button.setOnClickListener {
            if (client_search_field.text.isNotEmpty()) {
                fetchClientsAndUpdateUI()
            }
        }

        scan_barcode_button.setOnClickListener {
            scanQRCode()
        }

        //Клиенты ручной ввод запрещен и карты дисконтые ручной ввод запрещен
        if (MainActivity.currentUser?.isActionAllowed(113) == false && MainActivity.currentUser?.isActionAllowed(111) == false) {
            client_search_button.visibility = View.GONE
        }

        if (MainActivity.currentUser?.isActionAllowed(113) == false) {
            client_search_field.hint = "Укажите номер карты"
        }
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
                    val clientItem = item as ClientItem
                    val client = clientItem.client

                    if (!client.blocked) {
                        val builder = AlertDialog.Builder(view.context)
                        builder.setMessage("Выбрать ${client.full_name}?")
                            .setCancelable(false)
                            .setPositiveButton("Да") { dialog, id ->
                                dialog.dismiss()
//                                proceed_transfer(billItem.bill.id, view)
                                setResult(client.id)
                                finish()
                            }
                            .setNegativeButton("Нет") { dialog, id ->
                                // Dismiss the dialog
                                dialog.dismiss()
                            }
                        val alert = builder.create()
                        alert.show()
                    }
                }
                progressDialog.hide()
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
}

class ClientItem(val client: Client): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.client_name_in_list_view.text = client.full_name
        viewHolder.itemView.client_phone_in_list_view.text = client.phone
        viewHolder.itemView.client_card_in_list_view.text = client.card
        viewHolder.itemView.client_group_in_list_view.text = client.group_name
        viewHolder.itemView.client_note_in_list_view.text = client.note
        if (client.blocked) {
            viewHolder.itemView.client_block_in_list_view.text = "Заблокирован"
            viewHolder.itemView.client_block_in_list_view.setTextColor(Color.RED)
        } else {
            viewHolder.itemView.client_block_in_list_view.text = "Активен"
            viewHolder.itemView.client_block_in_list_view.setTextColor(Color.GREEN)
        }
        viewHolder.itemView.user_dateadd_in_list_view.text = client.dateadd
        viewHolder.itemView.user_last_date_in_list_view.text = client.last_date
        viewHolder.itemView.client_cli_sum_in_list_view.text = client.cli_sum.toString()
    }

    override fun getLayout(): Int {
        return R.layout.client_in_list_view
    }


}