package com.clubswaitress.cubswaitressapp.Pages


import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.clubswaitress.cubswaitressapp.ActivityListener
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.Models.*
import com.clubswaitress.cubswaitressapp.R
import com.google.gson.Gson
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.Parameters
import kotlinx.android.synthetic.main.activity_bill_transfer.*
import kotlinx.android.synthetic.main.bill_short_in_table_list_view.view.*
import kotlinx.android.synthetic.main.hall_in_list_view.view.*
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.bill_view_childs
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.materialCardView
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.order_name_in_bill_orders_list
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.order_price_in_bill_orders_list
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.order_qnt_in_bill_orders_list
import kotlinx.android.synthetic.main.order_line_in_bill_view_for_transfer.view.*
import kotlinx.android.synthetic.main.table_in_lict_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min


class BillTransferActivity : ActivityListener(){
    companion object {
        val TAG = "BillTransferActivity"
        var BILL_ID = "BILL_ID"
    }

    var bill: Bill? = null
    val adaptorOrders = GroupAdapter<ViewHolder>()
    val fetch_url = "${MainActivity.serverBaseUrl}/halls"

    var halls:List<Hall> = emptyList()
    var orders_positive: List<Order>? = emptyList()

    val adaptorHalls = GroupAdapter<ViewHolder>()

    var hall: Hall? = null

    val fetch_tables_url = "${MainActivity.serverBaseUrl}/halls/tables-in-hall"

    var tables: List<Table> = emptyList()

    val adaptorTables = GroupAdapter<ViewHolder>()

    suspend fun fetchTables() {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
            tables = it.get(fetch_tables_url) {
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
            } catch (cause: Throwable) {

                launch(Dispatchers.Main) {

                }

                return@launch
            }

            launch(Dispatchers.Main) {
                adaptorTables.clear()
                for (table in tables) {
                    adaptorTables.add(TableItemTransfer(table, hall, this@BillTransferActivity))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill_transfer)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Перенос заказов"

        val gson = Gson()
        bill = gson.fromJson(intent.getStringExtra(BILL_ID), Bill::class.java)

        val bill_original = bill
        var orders = bill?.orders

        if (orders != null) {
            for (order in orders) {
                if (order.qnt > 0) {
                    order.qnt = BillActivity.calculateOrderQNTincludingCancelations(order, bill)
                }

            }
        }

        orders_positive = orders?.filter {
            it.qnt > 0
        }

        orders_recycler_view.adapter = adaptorOrders

        var count = 0
        if (orders_positive != null) {
            for (order in orders_positive!!) {
                if (order.isPrinted) {
                    adaptorOrders.add(OrderItemInTransferList(order, count, adaptorOrders, order.qnt))
                    count++
                }
            }
        }

        choose_all_orders_to_transfer.setOnClickListener {
            for ((counter, order) in bill!!.orders.withIndex()) {
                order.isReadyForTransfer = true
                adaptorOrders.notifyItemChanged(counter)
            }

        }


        order_transfer_cancel_button.setOnClickListener {
            finish()
        }

        hals_recycler_view_in_transfer_activity.adapter = adaptorHalls

        tables_recycler_view_in_transfer.adapter = adaptorTables

        fetchHallsAndUpdateUI()

        transfer_choose_destination_button.setOnClickListener {

            val orders = orders_positive?.filter {
                it.isReadyForTransfer
            }

            if (orders != null) {
                if (orders.count() > 0) {
                    order_transfer_destination_container.visibility = View.VISIBLE
                    transfer_choose_destination_button.isEnabled = false
                    choose_all_orders_to_transfer.isEnabled = false
                }
                else {
                    Toast.makeText(applicationContext, "Выберите позиции для переноса...", Toast.LENGTH_SHORT).show()
                }
            }


        }


    }

    suspend fun fetchHalls() {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            halls = it.get(fetch_url) {
                parameter("user_id", MainActivity.currentUser?.id)
            }
        }

    }

    fun fetchHallsAndUpdateUI() {
        GlobalScope.launch(Dispatchers.IO) {

            try {
                fetchHalls()
            } catch (cause: Throwable) {
                println("Error: $cause")
                println("Error: $fetch_url")
                println("user_id ${MainActivity.currentUser?.id}")

                launch(Dispatchers.Main) {

                }

                return@launch
            }

            launch(Dispatchers.Main) {
                for (hall in halls) {
                    adaptorHalls.add(HallItemInTransfer(hall))
                }
                adaptorHalls.setOnItemClickListener { item, view ->
                    val userItem = item as HallItemInTransfer
                    hall = userItem.hall
                    fetchTablesAndUpdateUI()
                }
            }
        }
    }

}

class OrderItemInTransferList(val order: Order, val count: Int, val adapter: GroupAdapter<ViewHolder>, val original_qnt: Double): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.order_name_in_bill_orders_list.text = order.menu_item
        viewHolder.itemView.order_price_in_bill_orders_list.text = order.price
        viewHolder.itemView.order_qnt_in_bill_orders_list.text = order.qnt.toString()
        var childsDescription = ""
        order.childs.forEach {
            childsDescription = "${childsDescription} \n ${it.prefix_title} ${it.title}"
        }
        viewHolder.itemView.bill_view_childs.text = childsDescription

        if (order.isPrinted) {
            viewHolder.itemView.order_qnt_in_bill_orders_list.setBackgroundColor(Color.RED)
            viewHolder.itemView.order_qnt_in_bill_orders_list.setTextColor(Color.WHITE)
        } else {
            viewHolder.itemView.order_qnt_in_bill_orders_list.setBackgroundColor(Color.WHITE)
            viewHolder.itemView.order_qnt_in_bill_orders_list.setTextColor(Color.BLACK)
        }

        if (order.isReadyForTransfer) {
            viewHolder.itemView.materialCardView.setBackgroundColor(Color.RED)
        } else {
            viewHolder.itemView.materialCardView.setBackgroundColor(Color.WHITE)
        }

        viewHolder.itemView.materialCardView.setOnClickListener {
            Log.w("TEST", order.toString())
            order.isReadyForTransfer = !order.isReadyForTransfer
            adapter.notifyItemChanged(count)
        }
        viewHolder.itemView.increase_amount_of_order_in_transfer.setOnClickListener {
            order.qnt += 1
            if (order.qnt > original_qnt) {
                order.qnt -= 1
            }
            adapter.notifyItemChanged(count)
        }
        viewHolder.itemView.decrease_amount_of_order_in_transfer.setOnClickListener {
            order.qnt -= 1
            if (order.qnt < 0) {
                order.qnt += 1
            }
            adapter.notifyItemChanged(count)
        }

        if (order.qnt == original_qnt) {
            viewHolder.itemView.increase_amount_of_order_in_transfer.visibility = View.GONE
        } else {
            viewHolder.itemView.increase_amount_of_order_in_transfer.visibility = View.VISIBLE
        }

        if (order.qnt == 0.0) {
            viewHolder.itemView.decrease_amount_of_order_in_transfer.visibility = View.GONE
        } else {
            viewHolder.itemView.decrease_amount_of_order_in_transfer.visibility = View.VISIBLE
        }

    }


    override fun getLayout(): Int {
        return R.layout.order_line_in_bill_view_for_transfer
    }

}

class HallItemInTransfer(val hall: Hall): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.hall_in_list_view_hall_name.text = hall.name

    }

    override fun getLayout(): Int {
        return R.layout.hall_in_list_view_for_transfer
    }
}


class TableItemTransfer(val table: Table, val hall: Hall?, val parentActivity: BillTransferActivity): Item<ViewHolder>() {

    val adaptor = GroupAdapter<ViewHolder>()


    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.itemView.table_in_list_view_table_number.text = table.number.toString()
        if (hall != null && hall.show_tables_total) {
            viewHolder.itemView.table_in_list_view_total_sum.text = table.total
        } else {
            viewHolder.itemView.table_in_list_view_total_sum.text = ""
        }

        table.bills.forEach { bill: TableBill ->
            adaptor.add(BillInTableItemTransfer(bill))
        }

        adaptor.setOnItemClickListener { item, view ->
            val billItem = item as BillInTableItemTransfer

            if (billItem.bill.isOpened && billItem.bill.editingHardwareID.toString() != MainActivity.currentHardwareID) {
                Toast.makeText(view.context, "Счет открыт на другом терминале", Toast.LENGTH_LONG)
                    .show()
            }
            else if (billItem.bill.personal_id != MainActivity.currentUser?.id && MainActivity.currentUser?.isActionAllowed(101) == false) {
                Log.d(HallwithTablesActivity.TAG, "user_id: " + MainActivity.currentUser?.id + ", bill_user_id: " + billItem.bill.personal_id)
                Toast.makeText(view.context, "Вам запрещено открывать чужие счета", Toast.LENGTH_LONG)
                    .show()
            } else {
                val builder = AlertDialog.Builder(view.context)
                builder.setMessage("Перенести выбранные заказы в указанный счет?")
                    .setCancelable(false)
                    .setPositiveButton("Да") { dialog, id ->
                        dialog.dismiss()
                        proceed_transfer(billItem.bill.id, view)
                    }
                    .setNegativeButton("Нет") { dialog, id ->
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }

        }

        viewHolder.itemView.add_new_bill_to_table_button.setOnClickListener { view ->
            val builder = AlertDialog.Builder(view.context)
            builder.setMessage("Перенести выбранные заказы в новый счет?")
                .setCancelable(false)
                .setPositiveButton("Да") { dialog, id ->
                    dialog.dismiss()
                    fetchBillAndUpdateUI(view)
                }
                .setNegativeButton("Нет") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        viewHolder.itemView.table_in_list_view_bills_list_recyclerView.adapter = adaptor
    }

    val transfer_orders_url = "${MainActivity.serverBaseUrl}/bills/transfer-order"
    val transfer_orders_finish_url = "${MainActivity.serverBaseUrl}/bills/finish-transfer-order"

    suspend fun transfer_orders(from_id: Int, bill_id: Int, qnt: Double, price: String, price_discount: String, price_fix: String, bill_source: Int) {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
            val params = Parameters.build {
                append("from_id", from_id.toString())
                append("bill_id", bill_id.toString())
                append("qnt", qnt.toString())
                append("price", price)
                append("price_discount", price_discount)
                append("price_fix", price_fix)
                append("from_bill_id", bill_source.toString())
                append("to_bill_id", bill_id.toString())
                append("personal_id", MainActivity.currentUser?.id.toString())
            }
            newBill = it.post(transfer_orders_url) {
                body = FormDataContent(
                    params
                )
            }

            Log.w("TEST", "TRANSFER order $params")

        }

    }

    suspend fun transfer_orders_finish(from_id: Int, to_id: Int, personal_id: Int, action_autorize: Int) {
        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        client.use {
            newBill = it.post(transfer_orders_finish_url) {
                body = FormDataContent( // создаем параметры, которые будут переданы в form
                    Parameters.build {
                        append("from_id", from_id.toString())
                        append("to_id", to_id.toString())
                        append("personal_id", personal_id.toString())
                        append("action_autorize", action_autorize.toString())
                    }
                )

            }

        }

    }

    fun proceed_transfer(bill_id: Int, view: View) {
        parentActivity.finish()

        GlobalScope.launch(Dispatchers.IO) {

            try {
                val orders = parentActivity.orders_positive!!.filter {
                    it.isReadyForTransfer
                }
                for (order in orders) {
                    transfer_orders(order.id, bill_id, order.qnt, order.price_num, order.price_discount, order.price_fix, parentActivity.bill!!.id)
                }
            } catch (cause: Throwable) {
                launch(Dispatchers.Main) {

                    Toast.makeText(view.context, "Ошибка при переносе заказов", Toast.LENGTH_LONG).show()
                    Log.w("TEST", cause.localizedMessage)
                }
                return@launch

            }

            launch(Dispatchers.Main) {

            }

        }
    }

    override fun getLayout(): Int {
        return R.layout.table_in_lict_view
    }

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
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(view.context, "Ошибка при создании счета", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            launch(Dispatchers.Main) {
                progressDialog.dismiss()

                newBill?.id?.let { it -> proceed_transfer(it, view) }
            }

        }
    }
}

class BillInTableItemTransfer(val bill: TableBill): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.itemView.bill_in_table_list_number.text = bill.number.toString()
        viewHolder.itemView.bill_in_table_list_personal_name.text = bill.personalName
        viewHolder.itemView.bill_in_table_list_client_name.text = bill.clientsName
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