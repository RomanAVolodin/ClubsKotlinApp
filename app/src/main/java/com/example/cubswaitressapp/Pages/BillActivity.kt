package com.example.cubswaitressapp.Pages

import android.app.ProgressDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
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
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.Parameters
import kotlinx.android.synthetic.main.activity_bill.*
import kotlinx.android.synthetic.main.menu_entity_addition_item.view.*
import kotlinx.android.synthetic.main.menu_entity_addition_type.view.*
import kotlinx.android.synthetic.main.menu_entity_in_menu_bill_view.view.*
import kotlinx.android.synthetic.main.menu_group_item_in_bill_view.view.*
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BillActivity : AppCompatActivity() {

    companion object {
        val TAG = "BillActiity"
        var BILL_KEY = "BILL_KEY"
    }

    var updatingOrderId: Int? = null

    var lastSelectedIndex = -1

    var progressDialog: ProgressDialog? = null

    var selectedMenuEntity: MenuEntity? = null

    var selectedMenuGroup = 0

    var isHotkeysEnabled = true

    var tableBill: TableBill? = null
    var bill: Bill? = null
    var menuGroups: List<MenuGroup> = emptyList()
    var menu: List<MenuEntity> = emptyList()

    val fetch_url = "${MainActivity.serverBaseUrl}/bills/view"
    val fetch_url_menu_groups = "${MainActivity.serverBaseUrl}/menu/group-menu" //
    val fetch_url_menu_groups_hots = "${MainActivity.serverBaseUrl}/menu/hotkeys-in-hall"
    val fetch_url_menu= "${MainActivity.serverBaseUrl}/menu/menu-items-in-menu"
    val finish_editing_bill_url= "${MainActivity.serverBaseUrl}/bills/finish-editing"

    val adaptor = GroupAdapter<ViewHolder>()
    val adaptorMenuGroups = GroupAdapter<ViewHolder>()
    val adaptorMenu = GroupAdapter<ViewHolder>()
    val adaptorAdditionItems = GroupAdapter<ViewHolder>()
    val adaptorAdditionTypes= GroupAdapter<ViewHolder>()

    fun fetchMenuAndUpdateUI(group_id: Int) {

        loader_in_menu_view.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {

            if (group_id > 0) {
                try {

                    fetchMenuForGroup(group_id)



                } catch (cause: Throwable) {
                    println("gmenu_id  $group_id")
                    println("hall_id ${bill?.hall_id}")
                    println("isHotkeys ${isHotkeysEnabled.toString()}")
                    println("Error: $cause")
                    println("from url: $fetch_url_menu")

                    launch(Dispatchers.Main) {
                        loader_in_menu_view.visibility = View.GONE
                        Toast.makeText(applicationContext, "Ошибка при получении товаров", Toast.LENGTH_LONG).show()
                    }

                    return@launch
                }
            } else {
                menu = emptyList()
            }


                launch(Dispatchers.Main) {

                    adaptorMenu.clear()

                    menu.forEach {
                        adaptorMenu.add(MenuItem(it, bill))
                        Log.w(TAG, "Menu item: ${it}")
                    }

                    adaptorMenu.setOnItemClickListener { item, view ->

                        menu_entity_amount_in_menu_entity_container.text = "1"

                        val menuEntityItem = item as com.example.cubswaitressapp.Pages.MenuItem
                        selectedMenuEntity = menuEntityItem.menuEntity
                        updatingOrderId = null

                        if (selectedMenuEntity?.additions?.first { it.isNeed }?.items?.count() == 0) {
                            saveNewOrderAndUpdateUI()
                            return@setOnItemClickListener
                        }

                        updateMenuEntityContainer()

                    }


                    loader_in_menu_view.visibility = View.GONE
                }


        }
    }

    fun updateMenuEntityContainer() {

        menu_entity_container_in_bill_view.visibility = View.VISIBLE
        menu_entity_title_in_menu_entity_container.text = selectedMenuEntity?.name_button

        adaptorAdditionTypes.clear()
        adaptorAdditionItems.clear()

        selectedMenuEntity?.additions?.forEach {
            adaptorAdditionTypes.add(MenuEntityAdditiionTypeItem(it))
        }

        adaptorAdditionTypes.setOnItemClickListener { item, view ->
            val menuEntityTypeItem = item as MenuEntityAdditiionTypeItem
            val typeID = menuEntityTypeItem.additionType.id
            val selectedAddition = selectedMenuEntity?.additions?.first {
                it.id == typeID
            }

            var count = 0
            selectedMenuEntity?.additions?.forEach {
                it.isSelected = false
                if (it == selectedAddition) {
                    it.isSelected = true
                }
                adaptorAdditionTypes.notifyItemChanged(count)
                count++
            }


            adaptorAdditionItems.clear()
            selectedAddition?.items?.forEach {
                adaptorAdditionItems.add(MenuEntityAdditiionItem(it))
            }

            adaptorAdditionItems.setOnItemClickListener { item, view ->
                val menuEntityItem = item as MenuEntityAdditiionItem
                menuEntityItem.additionItem.isSelected = !menuEntityItem.additionItem.isSelected
                val index = selectedAddition?.items?.indexOf(menuEntityItem.additionItem)
                if (index != -1) {
                    adaptorAdditionItems.notifyItemChanged(index!!)
                }

            }

        }


/////////начальная загрузка обязательных
        val selectedAddition = selectedMenuEntity?.additions?.first {
            it.isSelected
        }
        adaptorAdditionItems.clear()
        selectedAddition?.items?.forEach {
            adaptorAdditionItems.add(MenuEntityAdditiionItem(it))
        }
        adaptorAdditionItems.setOnItemClickListener { item, view ->
            val menuEntityItem = item as MenuEntityAdditiionItem
            menuEntityItem.additionItem.isSelected = !menuEntityItem.additionItem.isSelected
            val index = selectedAddition?.items?.indexOf(menuEntityItem.additionItem)
            if (index != -1) {
                adaptorAdditionItems.notifyItemChanged(index!!)
            }

        }
/////////начальная загрузка обязательных

    }

    suspend fun fetchMenuForGroup(group_id: Int) {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            menu = it.get(fetch_url_menu) {
                fillMenuHeadersCaseParameters(group_id)
            }
            selectedMenuGroup = group_id
        }

    }

    private fun HttpRequestBuilder.fillMenuHeadersCaseParameters(group_id: Int) {
        parameter("gmenu_id", group_id)
        parameter("hall_id", bill?.hall_id)
        parameter("isHotkeys", isHotkeysEnabled.toString())
    }


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

    suspend fun fetchMenuGroups(parent_id: Int) {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            var url = fetch_url_menu_groups

            if (isHotkeysEnabled) url = fetch_url_menu_groups_hots

            menuGroups = it.get(url) {
                fillMenuGroupsHeadersCaseParameters(parent_id)
            }


        }

    }

    private fun HttpRequestBuilder.fillMenuGroupsHeadersCaseParameters(parent_id: Int) {
        parameter("parent_id", parent_id)
        parameter("hall_id", bill?.hall_id.toString())
    }

    fun fetchMenuGroupsAndUpdateUI(parent_id: Int = 0) {

        GlobalScope.launch(Dispatchers.IO) {

            try {

                fetchMenuGroups(parent_id)

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("from url: $fetch_url_menu_groups")
                println("parent_id $parent_id")
                println("hall_id ${bill?.hall_id.toString()}")

                launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Ошибка при получении меню", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            if (menuGroups != null) {
                launch(Dispatchers.Main) {

                    println("PARENT ID IS ====:$parent_id")

                    adaptorMenuGroups.clear()
                    lastSelectedIndex = -1

                    if (parent_id > 0) {
                        val backButton = MenuGroup(menuGroups.first().parent_id, "Назад", true, menuGroups.first().parent_id)
                        adaptorMenuGroups.add(MenuGroupItem(backButton))
                    }

                    menuGroups.forEach {
                        adaptorMenuGroups.add(MenuGroupItem(it))
                    }

                    progressBar_menugroups_global.visibility = View.GONE

                    adaptorMenuGroups.setOnItemClickListener { item, view ->
                        val menuGroupItem = item as MenuGroupItem

                        val parent_id_inside_clicker = menuGroupItem.menuGroup.id
                        Log.w(TAG, "click on button ${parent_id_inside_clicker}")
                        if (menuGroupItem.menuGroup.isSubmenus) {
                            view.progressBar_in_menugroup.visibility = View.VISIBLE
                            fetchMenuGroupsAndUpdateUI(parent_id_inside_clicker)
                        } else {

                            if (lastSelectedIndex > -1) {
                                val menuGroupItem = adaptorMenuGroups.getItem(lastSelectedIndex) as MenuGroupItem
                                menuGroupItem.isSelected = false
                                adaptorMenuGroups.notifyItemChanged(lastSelectedIndex)
                            }

                            var indexToUpdate = menuGroups.indexOf(menuGroupItem.menuGroup)
                            if (parent_id > 0) {
                                indexToUpdate = indexToUpdate + 1
                            }
                            menuGroupItem.isSelected = true
                            adaptorMenuGroups.notifyItemChanged(indexToUpdate)
//                            view.menugroup_container.setBackgroundColor(Color.LTGRAY)

                            lastSelectedIndex = indexToUpdate
                        }

                        fetchMenuAndUpdateUI(parent_id_inside_clicker)
                    }

                }
            }

        }
    }

    private fun HttpRequestBuilder.fillHeadersCaseParameters() {
        parameter("bill_id", tableBill?.id)
        parameter("hardware_id", MainActivity.currentHardwareID)
    }

    fun fetchBillAndUpdateUI() {

        progressDialog?.show()

        GlobalScope.launch(Dispatchers.IO) {

            try {

                fetchBill()

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("from url: $fetch_url")
                println("id: ${tableBill?.id}")
                println("\"hardware_id\", ${MainActivity.currentHardwareID}")

                launch(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(applicationContext, "Ошибка при получении счета", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            if (bill != null) {
                launch(Dispatchers.Main) {

                    updateUIForBill()

                    progressDialog?.dismiss()

                }
            }

        }
    }

    private fun updateUIForBill() {
        adaptor.clear()

        var count = 1
        for (order in bill!!.orders) {
            println("order:::::::::::::::::::: $order")
            adaptor.add(OrderItem(order, count))
            count++
        }

        Log.i(TAG, "Just fetched bill ${bill}")

        bill_view_total.text = bill?.total.toString()
        bill_view_total_discount.text = bill?.total_discount.toString()
        bill_view_total_payment.text = bill?.total_payment.toString()
        bill_view_clientsName.text = bill?.clientsName
        bill_view_date_opened.text = "Открыт: ${bill?.opened}"

        if (bill != null && bill!!.isPrinted) {
            floatingActionButton_open_menu.setOnClickListener {
                Toast.makeText(applicationContext, "Счет распечатан - редактирование не возможно", Toast.LENGTH_LONG).show()
            }
        } else {
            adaptor.setOnItemClickListener { item, view ->
                val orderItem = item as OrderItem
                val order = orderItem.order

                Log.i(TAG, "Click on order ${order}")

                selectedMenuEntity = order.menuEntity
                updatingOrderId = order.id
                menu_entity_amount_in_menu_entity_container.text = order.qnt.toString()

                menu_entity_container_in_bill_view.visibility = View.VISIBLE

                updateMenuEntityContainer()
            }

        }

    }


    val save_url = "${MainActivity.serverBaseUrl}/bills/add-order"

    suspend fun saveMenuEntitytoBill() {

        println("bill_id ${bill?.id.toString()}")
        println("menu_id ${selectedMenuEntity?.id.toString()}")
        println("qnt ${menu_entity_amount_in_menu_entity_container.text.toString()}")
        println("price ${selectedMenuEntity?.basePrice.toString()}")
        println("price_discount ${selectedMenuEntity?.actualPriceInHall.toString()}")
        println("price_fix 1")
        println("updatingOrderId ${updatingOrderId.toString()}")

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            bill = it.post(save_url) {
                body = FormDataContent( // создаем параметры, которые будут переданы в form
                    Parameters.build {
                        append("bill_id", bill?.id.toString())
                        append("menu_id", selectedMenuEntity?.id.toString())
                        append("qnt", menu_entity_amount_in_menu_entity_container.text.toString())
                        append("price", selectedMenuEntity?.basePrice.toString())
                        append("price_discount", selectedMenuEntity?.actualPriceInHall.toString())
                        append("price_fix", "1")
                        append("name_menu_add_id", "1")
                        if (updatingOrderId != null) append("updatingOrderId", updatingOrderId.toString())
                    }
                )
            }
        }

        selectedMenuEntity?.additions?.forEach { additionType ->
            val lastIdOfOrder = bill?.orders?.last()?.id
            additionType.items.forEach { menuEntityAddition ->
                if (menuEntityAddition.isSelected) {

                    println("id0 ${lastIdOfOrder.toString()}")
                    println("bill_id ${bill?.id.toString()}")
                    println("menu_id ${menuEntityAddition.id.toString()}")
                    println("qnt 1")
                    println("price 1")
                    println("price_discount 1")
                    println("price_fix 1")
                    println("name_menu_add_id ${additionType.id.toString()}")

                    val client = HttpClient(Android) {
                        install(JsonFeature) {
                            serializer = GsonSerializer()
                        }
                    }

                    client.use {
                        bill = it.post(save_url) {
                            body = FormDataContent( // создаем параметры, которые будут переданы в form
                                Parameters.build {
                                    append("id0", lastIdOfOrder.toString())
                                    append("bill_id", bill?.id.toString())
                                    append("menu_id", menuEntityAddition.id.toString())
                                    append("qnt", "1")
                                    append("price", "1")
                                    append("price_discount", "1")
                                    append("price_fix", "1")
                                    append("name_menu_add_id", additionType.id.toString())
                                }
                            )
                        }
                    }
                }
            }
        }

    }


    fun saveNewOrderAndUpdateUI() {

        Log.i(TAG, "Trying to update bill: $bill")

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Добавляю позицию в счет...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {

            try {

                saveMenuEntitytoBill()

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("from url: $save_url")

                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "Ошибка при обновлении счета", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            launch(Dispatchers.Main) {

                progressDialog.dismiss()

                updateUIForBill()

                Log.i(TAG, "Updated bill: $bill")

                close_menu_entity_button_in_bill_view.callOnClick()

                fetchMenuAndUpdateUI(selectedMenuGroup)
                //menu_container_in_bill_activity.callOnClick()
            }

        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill)

        tableBill = intent.getParcelableExtra<TableBill>(BILL_KEY)

        supportActionBar?.title = "Счет № ${tableBill?.number.toString()}"
        if (tableBill != null && tableBill!!.isPrinted) {
            supportActionBar?.title = "Счет № ${tableBill?.number.toString()} - распечатан"
        }
        supportActionBar?.subtitle = "${tableBill?.personalName}"


        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage("Получаю список заказов...")
        progressDialog?.setCancelable(false)


        bill_view_orders_recycler_view.adapter = adaptor
        menu_groups_in_bill_view_recycler_view.adapter = adaptorMenuGroups

        menu_in_bill_view_recycler_view.adapter = adaptorMenu
        menu_in_bill_view_recycler_view.setLayoutManager(GridLayoutManager(null, 3))

        additions_recycler_view_in_bill_view.adapter = adaptorAdditionItems
        additions_recycler_view_in_bill_view.setLayoutManager(GridLayoutManager(null, 3))

        addition_types_recycler_view_in_bill_view.adapter = adaptorAdditionTypes
        addition_types_recycler_view_in_bill_view.setLayoutManager(GridLayoutManager(null, 5))

        fetchBillAndUpdateUI()

        menu_container_in_bill_activity.setOnClickListener {
            menu_container_in_bill_activity.visibility = View.GONE
            menu_entity_container_in_bill_view.visibility = View.GONE
        }

        floatingActionButton_open_menu.setOnClickListener {
            menu_container_in_bill_activity.visibility = View.VISIBLE
            progressBar_menugroups_global.visibility = View.VISIBLE
            adaptorMenuGroups.clear()
            adaptorMenu.clear()
            fetchMenuGroupsAndUpdateUI()
        }

        toggleButton_hot_regular_menu_in_bill_view.setOnClickListener {
            isHotkeysEnabled = !isHotkeysEnabled
            fetchMenuGroupsAndUpdateUI()
        }

        close_menu_entity_button_in_bill_view.setOnClickListener {
            menu_entity_container_in_bill_view.visibility = View.GONE
        }


        save_menu_entity_button_in_bill_view.setOnClickListener {
            val needAdditions = selectedMenuEntity?.additions?.first {
                it.isNeed
            }
            if (needAdditions?.items != null && needAdditions.items.count() > 0) {

                if (needAdditions.items.indexOfFirst { it.isSelected == true } != -1){
                    saveNewOrderAndUpdateUI()

                } else {
                    Toast.makeText(applicationContext, "Не выбраны обязательные параметры", Toast.LENGTH_LONG).show()
                }

            }  else {
                saveNewOrderAndUpdateUI()

            }


        }

        menu_entity_plus_button_in_menu_entity_container.setOnClickListener {
            var amount = menu_entity_amount_in_menu_entity_container.text.toString().toDouble()
            amount = amount + 0.5
            menu_entity_amount_in_menu_entity_container.text = amount.toString()
            selectedMenuEntity?.amount = amount
        }

        menu_entity_minus_button_in_menu_entity_container.setOnClickListener {
            var amount = menu_entity_amount_in_menu_entity_container.text.toString().toDouble()
            amount = amount - 0.5
            menu_entity_amount_in_menu_entity_container.text = amount.toString()
            selectedMenuEntity?.amount = amount
        }



    }


    suspend fun finishEditing() {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            var url = finish_editing_bill_url

            bill = it.get(url) {
                parameter("bill_id", bill?.id)
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()

        GlobalScope.launch(Dispatchers.IO) {

            try {

                finishEditing()

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("from url: $finish_editing_bill_url")
                println("bill_id: ${bill?.id}")

                launch(Dispatchers.Main) {
                    loader_in_menu_view.visibility = View.GONE
                    Toast.makeText(applicationContext, "Ошибка при завершении работы со счетом", Toast.LENGTH_LONG).show()
                }

                return@launch
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
}

class MenuEntityAdditiionTypeItem(val additionType: MenuAdditionType): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.addition_type_button_in_bill_view.text = additionType.name_button

        if (additionType.isSelected) {
            viewHolder.itemView.addition_type_container_in_bill_view.setBackgroundColor(Color.RED)
        } else {
            viewHolder.itemView.addition_type_container_in_bill_view.setBackgroundColor(Color.WHITE)
        }
    }

    override fun getLayout(): Int {
        return R.layout.menu_entity_addition_type
    }

}

class MenuEntityAdditiionItem(val additionItem: MenuEntityAddition): Item<ViewHolder>() {

    var isSelected: Boolean = false

    override fun bind(viewHolder: ViewHolder, position: Int) {

        isSelected = additionItem.isSelected

        viewHolder.itemView.addition_item_name_in_addition_items_recycler_view.text = additionItem.name

        if (isSelected) {
            viewHolder.itemView.addition_item_container_in_addition_items_recycler_view.setBackgroundColor(Color.RED)
        } else {
            viewHolder.itemView.addition_item_container_in_addition_items_recycler_view.setBackgroundColor(Color.WHITE)
        }
    }

    override fun getLayout(): Int {
        return R.layout.menu_entity_addition_item
    }

}

class OrderItem(val order: Order, val count: Int): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.order_count_in_bill_orders_list.text = count.toString()
        viewHolder.itemView.order_name_in_bill_orders_list.text = order.menu_item
        viewHolder.itemView.order_price_in_bill_orders_list.text = order.price
        viewHolder.itemView.order_qnt_in_bill_orders_list.text = order.qnt.toString()
        var childsDescription = ""
        order.childs.forEach {
            childsDescription = "${childsDescription} \n ${it.prefix_title} ${it.title}"
        }
        viewHolder.itemView.bill_view_childs.text = childsDescription

    }

    override fun getLayout(): Int {
        return R.layout.order_line_in_bill_view
    }

}

class MenuGroupItem(val menuGroup: MenuGroup): Item<ViewHolder>() {

    var isSelected = false

    override fun bind(viewHolder: ViewHolder, position: Int) {
        var text = menuGroup.name
        if (menuGroup.isSubmenus) {
            text = "${menuGroup.name} \n•••"
        }
        viewHolder.itemView.menu_group_title_in_bill_view.text = text

        viewHolder.itemView.progressBar_in_menugroup.visibility = View.GONE

        if (isSelected) {
            viewHolder.itemView.menugroup_container.setBackgroundColor(Color.LTGRAY)
        } else {
            viewHolder.itemView.menugroup_container.setBackgroundColor(Color.WHITE)
        }


    }

    override fun getLayout(): Int {
        return R.layout.menu_group_item_in_bill_view
    }

}

class MenuItem(val menuEntity: MenuEntity, val bill: Bill?): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.menu_entity_title_in_bill_view.text = menuEntity.name_button
        viewHolder.itemView.menu_entity_price_in_bill_view.text = menuEntity.actualPriceInHall.toString()
        var amountAlreadyOrdered: Double = 0.toDouble()
        bill?.orders?.forEach { order ->
            if (order.menu_item_id == menuEntity.id) {
                amountAlreadyOrdered += order.qnt
            }
        }

        Log.i(BillActivity.TAG, "Какая-то херня $bill")

        if (amountAlreadyOrdered > 0 ) {
            viewHolder.itemView.menu_entity_amount_in_bill_view.text = amountAlreadyOrdered.toString()
            viewHolder.itemView.menu_entity_amount_in_bill_view.visibility = View.VISIBLE
        } else {
            viewHolder.itemView.menu_entity_amount_in_bill_view.text = ""
            viewHolder.itemView.menu_entity_amount_in_bill_view.visibility = View.GONE
        }
    }

    override fun getLayout(): Int {
        return R.layout.menu_entity_in_menu_bill_view
    }

}