package com.clubswaitress.cubswaitressapp.Pages

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.clubswaitress.cubswaitressapp.ActivityListener
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.Models.Hall
import com.clubswaitress.cubswaitressapp.Models.Table
import com.clubswaitress.cubswaitressapp.Models.TableBill
import com.clubswaitress.cubswaitressapp.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.android.synthetic.main.activity_hallwith_tables.*
import kotlinx.android.synthetic.main.bill_short_in_table_list_view.view.*
import kotlinx.android.synthetic.main.table_in_lict_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.fixedRateTimer

class HallwithTablesActivity : ActivityListener() {

    companion object {
        val TAG = "HallsWithTables"
        var HALL_KEY = "HALL_KEY"
    }

    var progressDialog: ProgressDialog? = null

    var updateTimer: Timer? = null

    var hall: Hall? = null

    val fetch_url = "${MainActivity.serverBaseUrl}/halls/tables-in-hall"

    var tables: List<Table> = emptyList()

    val adaptor = GroupAdapter<ViewHolder>()

    suspend fun fetchTables() {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            tables = it.get(fetch_url) {
                fillHeadersCaseParameters()
            }
        }

    }


    private fun HttpRequestBuilder.fillHeadersCaseParameters() {
        parameter("hall_id", hall?.id)
        parameter("personal_id", MainActivity.currentUser?.id)
    }

    fun fetchTablesAndUpdateUI() {

       GlobalScope.launch(Dispatchers.IO) {

            try {

                fetchTables()

                Log.w("TEST", "UPdatting tables")

            } catch (cause: Throwable) {

                launch(Dispatchers.Main) {
                    progressDialog?.dismiss()
//                    Toast.makeText(applicationContext, "Ошибка при получении списка столов", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            launch(Dispatchers.Main) {
                adaptor.clear()
                for (table in tables) {
                    adaptor.add(TableItem(table, hall))
                }

                progressDialog?.dismiss()
            }

        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hallwith_tables)

        hall = intent.getParcelableExtra<Hall>(HALL_KEY)
        var spanCount = 2
        if (hall?.dynamic == true) {
            spanCount = 1
        }
        hall_with_tables_tables_recycler_view.setLayoutManager(GridLayoutManager(null, spanCount))

        Log.w("TEST", hall?.name + " : " + hall?.dynamic)

        supportActionBar?.title = "Зал: ${hall?.name}"
        supportActionBar?.subtitle = "${MainActivity.currentUser?.username}, терминал: ${MainActivity.currentUser?.hardware_name}"

        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage("Получаю список столов...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()

        hall_with_tables_tables_recycler_view.adapter = adaptor


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
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

    override fun onBackPressed() {

    }


    override fun onDestroy() {
        super.onDestroy()
        updateTimer?.cancel()
        updateTimer?.purge()
    }


    override fun onStart() {
        super.onStart()
        Log.w("TEST", "welcome back")
        updateTimer = fixedRateTimer("timer",false, 0, MainActivity.updateTimer){
            this@HallwithTablesActivity.runOnUiThread {
                fetchTablesAndUpdateUI()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        updateTimer?.cancel()
        updateTimer?.purge()
    }
}


class TableItem(val table: Table, val hall: Hall?): Item<ViewHolder>() {

    val adaptor = GroupAdapter<ViewHolder>()

    var newBill: TableBill? = null

    val create_bill_url = "${MainActivity.serverBaseUrl}/bills/create-bill"

    suspend fun createBill() {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            newBill = it.get(create_bill_url) {
                fillHeadersCaseParameters()
            }

        }

    }

    private fun HttpRequestBuilder.fillHeadersCaseParameters() {
        parameter("table_id", table.id)
        parameter("personal_id", MainActivity.currentUser?.id)
        parameter("hardware_id", MainActivity.currentHardwareID)
    }

    fun fetchBillAndUpdateUI(view: View) {

        val progressDialog = ProgressDialog(view.context)
        progressDialog.setMessage("Создаю счет...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {

            try {

                createBill()

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("create bill by url : $create_bill_url, table_id: ${table.id}, user_id: ${MainActivity.currentUser?.id}")
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(view.context, "Ошибка при создании счета", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            launch(Dispatchers.Main) {
                progressDialog.dismiss()
//                if (newBill != null) {
//                    adaptor.add(BillInTableItem(newBill!!))
//                }
//                Toast.makeText(view.context, "Счет создан", Toast.LENGTH_LONG).show()

                val intent = Intent(view.context, BillActivity::class.java)
                intent.putExtra(BillActivity.BILL_KEY, newBill)
                startActivity(view.context, intent, null)
            }

        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        if (table.number > 0) {
            viewHolder.itemView.table_in_list_view_table_number.text = table.number.toString()
        } else {
            viewHolder.itemView.table_in_list_view_table_number.text = "Динамический"
        }
        if (hall != null && hall.show_tables_total) {
            viewHolder.itemView.table_in_list_view_total_sum.text = table.total
        } else {
            viewHolder.itemView.table_in_list_view_total_sum.text = ""
        }

        table.bills.forEach { bill: TableBill ->
            adaptor.add(BillInTableItem(bill))
        }

        adaptor.setOnItemClickListener { item, view ->
            val billItem = item as BillInTableItem

            if (billItem.bill.isOpened && billItem.bill.editingHardwareID.toString() != MainActivity.currentHardwareID) {
                Toast.makeText(view.context, "Счет открыт на другом терминале", Toast.LENGTH_LONG)
                    .show()
            }
            else if (billItem.bill.personal_id != MainActivity.currentUser?.id && MainActivity.currentUser?.isActionAllowed(101) == false) {
                Log.d(HallwithTablesActivity.TAG, "user_id: " + MainActivity.currentUser?.id + ", bill_user_id: " + billItem.bill.personal_id)
                Toast.makeText(view.context, "Вам запрещено открывать чужие счета", Toast.LENGTH_LONG)
                    .show()
            } else {
                val intent = Intent(view.context, BillActivity::class.java)
                intent.putExtra(BillActivity.BILL_KEY, billItem.bill)
                startActivity(view.context, intent, null)
            }

        }

        viewHolder.itemView.add_new_bill_to_table_button.setOnClickListener {
            fetchBillAndUpdateUI(it)
        }

        viewHolder.itemView.table_in_list_view_bills_list_recyclerView.adapter = adaptor
    }

    override fun getLayout(): Int {
        return R.layout.table_in_lict_view
    }
}

class BillInTableItem(val bill: TableBill): Item<ViewHolder>() {


    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.itemView.bill_in_table_list_number.text = bill.number.toString()
        viewHolder.itemView.bill_in_table_list_personal_name.text = bill.personalName
        if (bill.clientsName != "") {
            viewHolder.itemView.bill_in_table_list_client_name.text = "${bill.clientsName} (${bill.clientsGroup})"
        }
        viewHolder.itemView.bill_in_table_list_price.text = bill.total_payment
        viewHolder.itemView.bill_in_table_total_without_discount_list_price.text = "Сумма заказа ${bill.total}"

        if (bill.isPrinted) {
            viewHolder.itemView.bill_in_table_list_number.text = "${viewHolder.itemView.bill_in_table_list_number.text} - распечатан "
            viewHolder.itemView.bill_in_table_list_number.setTextColor(Color.RED)
        } else {
            viewHolder.itemView.bill_in_table_list_number.setTextColor(Color.BLACK)
        }

        if (bill.isOpened && bill.editingHardwareID.toString() != MainActivity.currentHardwareID) {
            viewHolder.itemView.bill_in_table_view_container.setBackgroundColor(Color.parseColor("#4BF349EA"))
        } else {
            viewHolder.itemView.bill_in_table_view_container.setBackgroundColor(Color.parseColor("#6AB3D5F3"))
        }
    }

    override fun getLayout(): Int {
        return R.layout.bill_short_in_table_list_view
    }


}