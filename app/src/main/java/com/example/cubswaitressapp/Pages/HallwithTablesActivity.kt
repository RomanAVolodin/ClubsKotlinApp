package com.example.cubswaitressapp.Pages

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cubswaitressapp.MainActivity
import com.example.cubswaitressapp.Models.Bill
import com.example.cubswaitressapp.Models.Hall
import com.example.cubswaitressapp.Models.Table
import com.example.cubswaitressapp.Models.TableBill
import com.example.cubswaitressapp.R
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
import kotlin.coroutines.coroutineContext

class HallwithTablesActivity : AppCompatActivity() {

    companion object {
        val TAG = "HallsWithTables"
        var HALL_KEY = "HALL_KEY"
    }

    var progressDialog: ProgressDialog? = null

    var updateTimer: Timer? = null

    var hall: Hall? = null

    val fetch_url = "${MainActivity.serverBaseUrl}/halls/tables-in-hall"

    var tables:List<Table> = emptyList()

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
            Log.d(TAG, "HALL tables for hall id: " + hall?.id)
        }

    }

    private fun HttpRequestBuilder.fillHeadersCaseParameters() {
        parameter("hall_id", hall?.id)
    }

    fun fetchTablesAndUpdateUI() {

       GlobalScope.launch(Dispatchers.IO) {

            try {

                fetchTables()

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("from url: $fetch_url")

                launch(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(applicationContext, "Ошибка при получении списка столов", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            launch(Dispatchers.Main) {
                adaptor.clear()
                for (table in tables) {
                    adaptor.add(TableItem(table))
                }

                progressDialog?.dismiss()
            }

        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hallwith_tables)

        hall_with_tables_tables_recycler_view.setLayoutManager(GridLayoutManager(null, 2));


        hall = intent.getParcelableExtra<Hall>(HALL_KEY)

        supportActionBar?.title = hall?.name

        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage("Получаю список столов...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()

        hall_with_tables_tables_recycler_view.adapter = adaptor

        updateTimer = fixedRateTimer("timer",false, 0, MainActivity.updateTimer){
            this@HallwithTablesActivity.runOnUiThread {
                fetchTablesAndUpdateUI()
//                Toast.makeText(this@HallwithTablesActivity, "ОБНОВЛЯЮ", Toast.LENGTH_SHORT).show()
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        updateTimer?.cancel()
        updateTimer?.purge()

    }
}


class TableItem(val table: Table): Item<ViewHolder>() {

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
                if (newBill != null) {
                    adaptor.add(BillInTableItem(newBill!!))
                }
                Toast.makeText(view.context, "Счет создан", Toast.LENGTH_LONG).show()

                val intent = Intent(view.context, BillActivity::class.java)
                intent.putExtra(BillActivity.BILL_KEY, newBill)
                startActivity(view.context, intent, null)
            }

        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.itemView.table_in_list_view_table_number.text = table.number.toString()
        viewHolder.itemView.table_in_list_view_total_sum.text = table.total

        table.bills.forEach { bill: TableBill ->

            adaptor.add(BillInTableItem(bill))

        }

        adaptor.setOnItemClickListener { item, view ->
            val billItem = item as BillInTableItem

            val intent = Intent(view.context, BillActivity::class.java)
            intent.putExtra(BillActivity.BILL_KEY, billItem.bill)
            startActivity(view.context, intent, null)

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
        viewHolder.itemView.bill_in_table_list_client_name.text = bill.clientsName
        viewHolder.itemView.bill_in_table_list_price.text = bill.total
    }

    override fun getLayout(): Int {
        return R.layout.bill_short_in_table_list_view
    }

}