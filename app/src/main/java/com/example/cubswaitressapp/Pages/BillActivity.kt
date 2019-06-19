package com.example.cubswaitressapp.Pages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.cubswaitressapp.MainActivity
import com.example.cubswaitressapp.Models.*
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
import kotlinx.android.synthetic.main.activity_bill.*
import kotlinx.android.synthetic.main.activity_bill.view.*
import kotlinx.android.synthetic.main.bill_short_in_table_list_view.view.*
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BillActivity : AppCompatActivity() {

    companion object {
        val TAG = "BillActiity"
        var BILL_KEY = "BILL_KEY"
    }

    var tableBill: TableBill? = null
    var bill: Bill? = null

    val fetch_url = "${MainActivity.serverBaseUrl}/bills/view"

    val adaptor = GroupAdapter<ViewHolder>()

    suspend fun fetchBill() {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            bill = it.get(fetch_url) {
                fillHeadersCaseParameters()
            }

        }

    }

    private fun HttpRequestBuilder.fillHeadersCaseParameters() {
        parameter("bill_id", tableBill?.id)
    }

    fun fetchTablesAndUpdateUI() {

        GlobalScope.launch(Dispatchers.IO) {

            try {

                fetchBill()

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("from url: $fetch_url")
                println("id: ${tableBill?.id}")

                launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Ошибка при получении счета", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            if (bill != null) {
                launch(Dispatchers.Main) {

                    for (order in bill!!.orders) {
                        println("order:::::::::::::::::::: $order")
                        adaptor.add(OrderItem(order))
                    }

                    bill_view_total.text = bill?.total.toString()
                    bill_view_total_discount.text = bill?.total_discount.toString()
                    bill_view_total_payment.text = bill?.total_payment.toString()
                    bill_view_clientsName.text = bill?.clientsName
                    bill_view_date_opened.text = "Открыт: ${bill?.opened}"
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill)

        tableBill = intent.getParcelableExtra<TableBill>(BILL_KEY)

        supportActionBar?.title = "Счет № ${tableBill?.number.toString()}"
        supportActionBar?.subtitle = "${tableBill?.personalName}"



        bill_view_orders_recycler_view.adapter = adaptor

        fetchTablesAndUpdateUI()

        menu_container_in_bill_activity.setOnClickListener {
            Toast.makeText(this, "Прячу меню", Toast.LENGTH_SHORT).show()
            menu_container_in_bill_activity.visibility = View.GONE
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
}


class OrderItem(val order: Order): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.order_name_in_bill_orders_list.text = order.menu_item
        viewHolder.itemView.order_price_in_bill_orders_list.text = order.price
        viewHolder.itemView.order_qnt_in_bill_orders_list.text = order.qnt.toString()
        var childsDescription = ""
        order.childs.forEach {
            childsDescription = "${childsDescription} \n ${it.title}"
        }
        viewHolder.itemView.bill_view_childs.text = childsDescription

    }

    override fun getLayout(): Int {
        return R.layout.order_line_in_bill_view
    }

}