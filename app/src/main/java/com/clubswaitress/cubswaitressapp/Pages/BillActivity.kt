package com.clubswaitress.cubswaitressapp.Pages

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.clubswaitress.cubswaitressapp.ActivityListener
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.Models.*
import com.clubswaitress.cubswaitressapp.R
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.BadResponseStatusException
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.Parameters
import kotlinx.android.synthetic.main.activity_bill.*
import kotlinx.android.synthetic.main.activity_client_search.*
import kotlinx.android.synthetic.main.menu_entity_addition_item.view.*
import kotlinx.android.synthetic.main.menu_entity_addition_type.view.*
import kotlinx.android.synthetic.main.menu_entity_in_menu_bill_view.view.*
import kotlinx.android.synthetic.main.menu_group_item_in_bill_view.view.*
import kotlinx.android.synthetic.main.order_line_in_bill_view.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.ConnectException
import kotlin.math.abs

class BillActivity : ActivityListener() {

    companion object {
        val TAG = "BillActiity"
        var BILL_KEY = "BILL_KEY"

        public fun calculateOrderQNTincludingCancelations(order: Order, bill: Bill?): Double {
            var only_cancelation_of_order_qnt = 0.0
            bill?.orders?.forEach {
                if (it.cancel_order_id == order.id) {
                    only_cancelation_of_order_qnt += it.qnt
                }
            }
            return order.qnt + only_cancelation_of_order_qnt
        }
    }

    var updatingOrderId: Int? = null
    var updatingOrderEntity: Order? = null

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
    val delete_position_url= "${MainActivity.serverBaseUrl}/bills/delete-order"
    val print_positions_url= "${MainActivity.serverBaseUrl}/bills/print-bill"
    val print_orders_url= "${MainActivity.serverBaseUrl}/bills/print-orders"
    val change_guests_amount_url= "${MainActivity.serverBaseUrl}/bills/amount-of-guests"
    val search_menu_item_url= "${MainActivity.serverBaseUrl}/menu/menu-search-in-hall"
    val add_client_to_bill_url= "${MainActivity.serverBaseUrl}/bills/add-client"

    val adaptor = GroupAdapter<ViewHolder>()
    val adaptorMenuGroups = GroupAdapter<ViewHolder>()
    val adaptorMenu = GroupAdapter<ViewHolder>()
    val adaptorAdditionItems = GroupAdapter<ViewHolder>()
    val adaptorAdditionTypes= GroupAdapter<ViewHolder>()


    fun fetchMenuAndUpdateUI(group_id: Int, searchString: String = "") {

        loader_in_menu_view.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {

            if (group_id > 0) {
                try {
                    fetchMenuForGroup(group_id)
                } catch (cause: Throwable) {
                    launch(Dispatchers.Main) {
                        loader_in_menu_view.visibility = View.GONE
                        Toast.makeText(applicationContext, "Ошибка при получении товаров", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
            } else if (searchString != ""){
                try {
                    fetchMenuForGroup(group_id, searchString)
                } catch (cause: Throwable) {
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
                    }

                    adaptorMenu.setOnItemClickListener { item, view ->

                        menu_entity_amount_in_menu_entity_container.text = "1"

                        val menuEntityItem = item as com.clubswaitress.cubswaitressapp.Pages.MenuItem
                        selectedMenuEntity = menuEntityItem.menuEntity

                        updatingOrderId = null

                        Log.w(TAG, "Menu item: ${selectedMenuEntity}")

                        if (true || selectedMenuEntity?.additions?.first { it.isNeed }?.items?.count() == 0) {
                            saveNewOrderAndUpdateUI()

                            item.newAmountAlreadyOrdered = item.amountAlreadyOrdered + 1
                            val index = menu.indexOfFirst {
                                it == menuEntityItem.menuEntity
                            }
                            adaptorMenu.notifyItemChanged(index)

                            return@setOnItemClickListener
                        }

//                        saveNewOrderAndUpdateUI()
                    }

                    loader_in_menu_view.visibility = View.GONE
                }
        }
    }

    fun updateMenuEntityContainer() {
        menu_entity_container_in_bill_view.visibility = View.VISIBLE
        menu_entity_title_in_menu_entity_container.text = selectedMenuEntity?.name_button

        if ((updatingOrderEntity != null && isOrderFullyCanceled(updatingOrderEntity!!, bill)) || updatingOrderEntity?.isPrinted == true) {
            backdrop_in_menu_entity_container.visibility = View.VISIBLE
            if (updatingOrderEntity?.isNeededMissed == true) {
                backdrop_in_menu_entity_container.visibility = View.GONE
            }
        } else {
            backdrop_in_menu_entity_container.visibility = View.GONE
        }


        adaptorAdditionTypes.clear()
        adaptorAdditionItems.clear()

        selectedMenuEntity?.additions?.forEach {
            adaptorAdditionTypes.add(MenuEntityAdditiionTypeItem(it))
        }

        if (updatingOrderEntity?.isNeededMissed == true && updatingOrderEntity?.isPrinted == true) {
            adaptorAdditionTypes.setOnItemClickListener { item, view ->
                //отключаем кликание на выборе тирпа параметра, если не выбраны обязательные
            }
        } else {
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

    suspend fun fetchMenuForGroup(group_id: Int, searchString: String = "") {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        if (searchString == "") {
            client.use {
                menu = it.get(fetch_url_menu) {
                    fillMenuHeadersCaseParameters(group_id)
                }
                selectedMenuGroup = group_id
            }
        } else {
            client.use {
                menu = it.get(search_menu_item_url) {
                    fillMenuHeadersSearchCaseParameters(searchString)
                }
            }
        }


    }

    private fun HttpRequestBuilder.fillMenuHeadersCaseParameters(group_id: Int) {
        parameter("gmenu_id", group_id)
        parameter("hall_id", bill?.hall_id)
        parameter("isHotkeys", isHotkeysEnabled.toString())
    }

    private fun HttpRequestBuilder.fillMenuHeadersSearchCaseParameters(search: String) {
        parameter("hall_id", bill?.hall_id)
        parameter("search", search)
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

                    //если открыли горячие кнопки, то первую из них активируем
                    if (isHotkeysEnabled) {
                        val menuGroupItem = adaptorMenuGroups.getItem(0) as MenuGroupItem
                        menuGroupItem.isSelected = true
                        adaptorMenuGroups.notifyItemChanged(0)
                        lastSelectedIndex = 0
                        fetchMenuAndUpdateUI(menuGroupItem.menuGroup.id)
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
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Получаю данные счета...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                fetchBill()
            } catch (cause: Throwable) {
                when(cause) {
                    is ConnectException -> {
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(applicationContext, "Пропала связь, пробую еще...", Toast.LENGTH_SHORT).show()
                            Log.w("TEST", cause)
                            Handler().postDelayed(
                                {
                                    fetchBillAndUpdateUI()
                                },
                                3000
                            )
                        }
                        return@launch
                    }
                    is BadResponseStatusException -> {
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(applicationContext, "Вероятно, счет пустой и был удален в соответствии с правилами клуба", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }
                    else -> {
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Log.w("TEST", cause.localizedMessage)
                            Log.w("TEST", cause)
                            Toast.makeText(applicationContext, "Произошла ошибка севрера", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
            }

            if (bill != null) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    updateUIForBill()
                    supportActionBar?.title = "Счет № ${bill?.number.toString()}"
                    if (bill != null && bill!!.isPrinted) {
                        supportActionBar?.title = "Счет № ${bill?.number.toString()} - распечатан"
                    }

                    if (bill?.isNeededMissed() == true) {
                        val indexOfOrderToEdit = bill?.orders?.indexOfFirst {
                            it.isNeededMissed
                        }
                        if (indexOfOrderToEdit != -1) {
                            val order = indexOfOrderToEdit?.let { bill?.orders?.get(it) }
                            if (order != null) {
                                openOrderDetails(order)
                                Toast.makeText(applicationContext, "Укажите обязательный модификатор...", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }

        }
    }

    private fun updateUIForBill() {
        adaptor.clear()

        var count = 1
        if (bill != null) {
            for (order in bill!!.orders) {
                val needSection = order.menuEntity.additions.first { it.isNeed }
                if (needSection.items.count() > 0) {
                    val selectedItemIndex = needSection.items.indexOfFirst {
                        it.isSelected
                    }
                    if (selectedItemIndex == -1) {
                        order.isNeededMissed = true
                    }
                }
                adaptor.add(OrderItem(order, count))
                count++
            }
        }

        bill_view_total.text = bill?.total.toString()
        bill_view_total_discount.text = bill?.total_discount.toString()
        bill_view_total_payment.text = bill?.total_payment.toString()
        if (bill?.gclients_total_discount != "0") {
            bill_view_total_payment.text = "${bill?.total_payment.toString()} (${bill?.gclients_total_discount}%)"
        }
        if (bill?.gclients_total_fix_discount != "0") {
            bill_view_total_payment.text = "${bill?.total_payment.toString()} (${bill?.gclients_total_fix_discount} руб.)"
        }

        if (bill?.clientsName != "") {
            bill_view_clientsName.visibility = View.VISIBLE
            bill_view_clientsName.text = "${bill?.clientsName} (${bill?.clientsGroup})"
            bill_view_clientsName.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Убрать клиента из счета ?")
                    .setCancelable(false)
                    .setPositiveButton("Да") { dialog, id ->
                        dialog.dismiss()
                      addNewClientAndUpdateUI(0)
                    }
                    .setNegativeButton("Нет") { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
            scan_qr_button.visibility = View.GONE
        } else {
            scan_qr_button.visibility = View.VISIBLE
            bill_view_clientsName.visibility = View.GONE
            bill_view_clientsName.setOnClickListener {
            }
        }

        if (bill?.isPrinted == true) {
            scan_qr_button.visibility = View.GONE
            bill_view_clientsName.setOnClickListener {
            }
        }

        bill_view_date_opened.text = "Открыт: ${bill?.opened}"

        amount_of_guests_text_view.text = bill?.guests.toString()

        if (bill?.guests == 0) {
            amount_of_guests_text_view.visibility = View.GONE
            textView13.visibility = View.GONE
            guests_amount_floatingActionButton.hide()
        } else {
            amount_of_guests_text_view.visibility = View.VISIBLE
            textView13.visibility = View.VISIBLE
            guests_amount_floatingActionButton.show()
        }

        amount_of_guests_input_text_field.setText(bill?.guests.toString())

        if (bill != null && bill!!.isGuestsNeed && bill!!.guests < 1) {
            amount_of_guests_container.visibility = View.VISIBLE
            amount_of_guests_input_text_field.requestFocusFromTouch()
            amount_of_guests_input_text_field.requestFocus()
            showKeyboard()
        }

        if (bill != null && bill!!.isPrinted) {
            floatingActionButton_open_menu.setOnClickListener {
                Toast.makeText(applicationContext, "Счет распечатан - редактирование не возможно", Toast.LENGTH_LONG).show()
            }
            print_bill_button_in_bill_view.text = "Отмена печати"
            print_orders_button_in_bill_view.visibility = View.GONE
            bill_transfer_button.visibility = View.GONE
            adaptor.setOnItemClickListener { item, view ->
            }

        } else {

            floatingActionButton_open_menu.setOnClickListener {
                supportActionBar?.hide()
                menu_container_in_bill_activity.visibility = View.VISIBLE
                progressBar_menugroups_global.visibility = View.VISIBLE
                adaptorMenuGroups.clear()
                adaptorMenu.clear()
                fetchMenuGroupsAndUpdateUI()
            }

            print_bill_button_in_bill_view.text = "Счет"
            print_orders_button_in_bill_view.visibility = View.VISIBLE
            bill_transfer_button.visibility = View.VISIBLE

            adaptor.setOnItemClickListener { item, view ->

                val orderItem = item as OrderItem
                val order = orderItem.order

                if (order.qnt <= 0) {
                    Toast.makeText(this, "Этот заказ отменен", Toast.LENGTH_SHORT).show()
                    return@setOnItemClickListener
                }

                openOrderDetails(order)
            }


        }

    }

    fun openOrderDetails(order: Order) {
        supportActionBar?.hide()
        selectedMenuEntity = order.menuEntity
        updatingOrderId = order.id
        updatingOrderEntity = order

        var only_cancelation_of_order_qnt = 0.0
        bill?.orders?.forEach {
            if (it.cancel_order_id == order.id) {
                only_cancelation_of_order_qnt += it.qnt
            }
        }

        menu_entity_amount_in_menu_entity_container.text = order.qnt.toString()

        menu_entity_container_in_bill_view.visibility = View.VISIBLE

        if (isOrderFullyCanceled(order, bill)) {
            delete_order_button_in_bill_view.visibility = View.INVISIBLE
        } else {
            delete_order_button_in_bill_view.visibility = View.VISIBLE
        }

        updateMenuEntityContainer()
    }


    fun calculateOrderCancelationsAmount(order: Order, bill: Bill?): Double {
        var only_cancelation_of_order_qnt = 0.0
        bill?.orders?.forEach {
            if (it.cancel_order_id == order.id) {
                only_cancelation_of_order_qnt += it.qnt
            }
        }
        return only_cancelation_of_order_qnt
    }

    fun isOrderFullyCanceled(order: Order, bill: Bill?): Boolean {
        return calculateOrderQNTincludingCancelations(order, bill) <= 0
    }

    val save_url = "${MainActivity.serverBaseUrl}/bills/add-order"

    suspend fun saveMenuEntitytoBill() {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            var newStuff: String = ""
            newStuff = it.post(save_url) {
                body = FormDataContent(
                    Parameters.build {
                        append("bill_id", bill?.id.toString())
                        append("menu_id", selectedMenuEntity?.id.toString())
                        append("qnt", menu_entity_amount_in_menu_entity_container.text.toString())
                        append("price", selectedMenuEntity?.basePrice.toString())
                        append("price_discount", selectedMenuEntity?.actualPriceInHall.toString())
                        append("price_fix", "1")
                        append("name_menu_add_id", "1")
                        append("hardware_id", MainActivity.currentHardwareID)
                        if (updatingOrderId != null) append("updatingOrderId", updatingOrderId.toString())
                    }
                )
            }

            if (updatingOrderId != null) {
                selectedMenuEntity?.additions?.forEach { additionType ->

                    val lastIdOfOrder = updatingOrderId

                    additionType.items.forEach { menuEntityAddition ->
                        if (menuEntityAddition.isSelected) {
                            newStuff = it.post(save_url) {
                                body =
                                    FormDataContent( // создаем параметры, которые будут переданы в form
                                        Parameters.build {
                                            append("id0", lastIdOfOrder.toString())
                                            append("bill_id", bill?.id.toString())
                                            append("menu_id", menuEntityAddition.id.toString())
                                            append("qnt", "1")
                                            append("price", "0")
                                            append("price_discount", "0")
                                            append("price_fix", "1")
                                            append("hardware_id", MainActivity.currentHardwareID)
                                            append("name_menu_add_id", additionType.id.toString())
                                        }
                                    )
                            }
                        }
                    }
                }
            }
        }

    }


    fun saveNewOrderAndUpdateUI() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                saveMenuEntitytoBill()
            } catch (cause: Throwable) {
                when(cause) {
                    is ConnectException -> {
                        launch(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Пропала связь, пробую еще...", Toast.LENGTH_SHORT).show()
                            Log.w("TEST", cause)
                            Handler().postDelayed(
                                {
                                    saveNewOrderAndUpdateUI()
                                },
                                3000
                            )
                        }
                        return@launch
                    }
                    else -> {
                        launch(Dispatchers.Main) {
//                            Toast.makeText(applicationContext, "Что-то пошло не так...", Toast.LENGTH_SHORT).show()
                            Log.w("TEST", "Here i am testing QNT0")
                            Log.w("TEST", cause.localizedMessage)
                        }
//                        return@launch
                    }
                }
            }

            launch(Dispatchers.Main) {
                fetchBillAndUpdateUI()
            }
        }
    }

    val CHOOSE_CLIENT = 100
    private fun callUserChooseActivity(){
        val intent = Intent(this, ClientSearchActivity::class.java)
        startActivityForResult(intent, CHOOSE_CLIENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (resultCode != null) {
            addNewClientAndUpdateUI(resultCode)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    suspend fun addClientToBill(client_id: Int) {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            var newStuff: String = ""
            newStuff = it.post(add_client_to_bill_url) {
                body = FormDataContent(
                    Parameters.build {
                        append("bill_id", bill?.id.toString())
                        append("client_id", client_id.toString())
                        append("personal_id", MainActivity.currentUser?.id.toString())
                    }
                )
            }
        }

    }

    fun addNewClientAndUpdateUI(client_id: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                addClientToBill(client_id)
            } catch (cause: Throwable) {
                when(cause) {
                    is ConnectException -> {
                        launch(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Пропала связь, пробую еще...", Toast.LENGTH_SHORT).show()
                            Log.w("TEST", cause)
                            Handler().postDelayed(
                                {
                                    addNewClientAndUpdateUI(client_id)
                                },
                                3000
                            )
                        }
                        return@launch
                    }
                    else -> {
                        launch(Dispatchers.Main) {
                            Log.w("TEST", cause)
                        }
                    }
                }
            }

            launch(Dispatchers.Main) {
                fetchBillAndUpdateUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill)

        tableBill = intent.getParcelableExtra<TableBill>(BILL_KEY)

        supportActionBar?.subtitle = "Ответственный за счет: ${tableBill?.personalName}"


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

        search_in_menu_main_button.setOnClickListener {
            floatingActionButton_open_menu.callOnClick()
            search_button.callOnClick()
        }

        manual_order_qnt_cancel_button.setOnClickListener {
            manual_order_qnt_input_container.visibility = View.GONE
            manual_qnt_of_order_text_input.setText("")
            hideKeyboard()
        }

        manual_set_order_qnt_button.setOnClickListener {
            manual_order_qnt_input_container.visibility = View.VISIBLE

            if (updatingOrderEntity?.menuEntity?.weight == 1) {
                manual_qnt_of_order_text_input.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                is_drob_allowed.setText("Дробная часть отделяется точкой")
                manual_qnt_of_order_text_input.setText(updatingOrderEntity?.qnt?.toString())
            } else {
                manual_qnt_of_order_text_input.inputType = InputType.TYPE_CLASS_NUMBER
                is_drob_allowed.setText("Разрешены только целочисленные значения")
                manual_qnt_of_order_text_input.setText(updatingOrderEntity?.qnt?.toInt().toString())
            }

            manual_qnt_of_order_text_input.requestFocusFromTouch()
            manual_qnt_of_order_text_input.requestFocus()
            showKeyboard()
        }

        manual_qnt_enter_button.setOnClickListener {
            val newAmount = manual_qnt_of_order_text_input.text.toString()
            try {
                val newAmountDouble = newAmount.toDouble()
                val cancelations = abs(calculateOrderCancelationsAmount(updatingOrderEntity!!, bill))
                if (cancelations < newAmountDouble) {
                    manual_order_qnt_input_container.visibility = View.GONE
                    menu_entity_amount_in_menu_entity_container.setText(newAmountDouble.toString())
                    error_message_in_manual_qnt_container.visibility = View.GONE
                    hideKeyboard()
                } else {
                    error_message_in_manual_qnt_container.visibility = View.VISIBLE
                    error_message_in_manual_qnt_container.setText("По этой позиции уже есть отмена в количестве: ${cancelations}. Укажите большее значение. ")
                }
            } catch (e: Exception) {
                return@setOnClickListener
            }

        }

        menu_group_home_button.setOnClickListener {
//            isHotkeysEnabled = !isHotkeysEnabled
            fetchMenuGroupsAndUpdateUI()
        }

        scan_qr_button.setOnClickListener {
            callUserChooseActivity()
        }

        menu_container_in_bill_activity.setOnClickListener {
//            menu_container_in_bill_activity.visibility = View.GONE
//            menu_entity_container_in_bill_view.visibility = View.GONE
//            supportActionBar?.show()
        }

        close_menu_button_in_bill_view.setOnClickListener {
            menu_container_in_bill_activity.visibility = View.GONE
            menu_entity_container_in_bill_view.visibility = View.GONE
            supportActionBar?.show()
        }

        toggleButton_hot_regular_menu_in_bill_view.setOnClickListener {
            isHotkeysEnabled = !isHotkeysEnabled

            if (isHotkeysEnabled) {
                menu_group_home_button.visibility = View.GONE
            } else {
                menu_group_home_button.visibility = View.VISIBLE
            }
            fetchMenuGroupsAndUpdateUI()
        }

        close_menu_entity_button_in_bill_view.setOnClickListener {
            // вот эта вся херабора, для того, чтоб при закрытии окна при невыбранных обязательных оно не давало закрыть, а после выбора - сохраняло
            val needAdditions = selectedMenuEntity?.additions?.first {
                it.isNeed
            }
            if (updatingOrderEntity?.isNeededMissed == true && needAdditions?.items != null && needAdditions.items.count() > 0) {

                if (needAdditions.items.indexOfFirst { it.isSelected == true } != -1){
                    saveNewOrderAndUpdateUI()
                    supportActionBar?.show()
                    menu_entity_container_in_bill_view.visibility = View.GONE
                } else {
                    Toast.makeText(applicationContext, "Не выбраны обязательные параметры", Toast.LENGTH_LONG).show()
                }

            }  else {
                supportActionBar?.show()
                menu_entity_container_in_bill_view.visibility = View.GONE
            }

        }


        save_menu_entity_button_in_bill_view.setOnClickListener {
            val needAdditions = selectedMenuEntity?.additions?.first {
                it.isNeed
            }
            if (needAdditions?.items != null && needAdditions.items.count() > 0) {

                if (needAdditions.items.indexOfFirst { it.isSelected == true } != -1){
                    saveNewOrderAndUpdateUI()
                    close_menu_entity_button_in_bill_view.callOnClick()
                } else {
                    Toast.makeText(applicationContext, "Не выбраны обязательные параметры", Toast.LENGTH_LONG).show()
                }

            }  else {
                saveNewOrderAndUpdateUI()
                close_menu_entity_button_in_bill_view.callOnClick()
            }
        }

        menu_entity_plus_button_in_menu_entity_container.setOnClickListener {
            var amount = menu_entity_amount_in_menu_entity_container.text.toString().toDouble()
            amount += 1
            menu_entity_amount_in_menu_entity_container.text = amount.toString()
            selectedMenuEntity?.amount = amount
        }

        menu_entity_minus_button_in_menu_entity_container.setOnClickListener {
            var amount = menu_entity_amount_in_menu_entity_container.text.toString().toDouble()
            amount -= 1
            if (updatingOrderEntity != null && (amount + calculateOrderCancelationsAmount(updatingOrderEntity!!, bill) < 0)) {
                amount += 1
            }

            selectedMenuEntity?.amount = amount
            menu_entity_amount_in_menu_entity_container.text = amount.toString()

        }

        delete_order_button_in_bill_view.setOnClickListener {
            supportActionBar?.show()
            deleteSelectedOrder()
        }

        print_bill_button_in_bill_view.setOnClickListener {
            printBillAndExit()
        }

        print_orders_button_in_bill_view.setOnClickListener {
            printOrdersAndExit()
        }

        guests_amount_floatingActionButton.setOnClickListener {
            amount_of_guests_container.visibility = View.VISIBLE
            amount_of_guests_input_text_field.requestFocusFromTouch()
            amount_of_guests_input_text_field.requestFocus();
            showKeyboard()
        }

        amount_of_guests_submit_button.setOnClickListener {
            changeAmountOfGuestsinBill()
        }

        search_button.setOnClickListener {
            search_menu_container.visibility = View.VISIBLE
            search_menu_text_field.requestFocusFromTouch()
            search_menu_text_field.requestFocus();
            search_menu_text_field.setText("")

            showKeyboard()
        }

        bill_transfer_button.setOnClickListener {
            if (MainActivity.currentUser?.isActionAllowed(106) == false) {
                Toast.makeText(applicationContext, "Перенос для Вас запрещен", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            transferBill(it)
        }

        search_menu_text_cancel.setOnClickListener {
            search_menu_container.visibility = View.GONE
            hideKeyboard()
        }

        search_menu_text_button.setOnClickListener {
            search_menu_container.visibility = View.GONE
            val search = search_menu_text_field.text.toString()
            if (search == "") {
                return@setOnClickListener
            }
            fetchMenuAndUpdateUI(0, search)
            hideKeyboard()
        }
    }

    fun showKeyboard() {
        val imm = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun transferBill(view: View) {
        val intent = Intent(view.context, BillTransferActivity::class.java)
        val gson = Gson()
        intent.putExtra(BillTransferActivity.BILL_ID, gson.toJson(bill))
        finish()
        ContextCompat.startActivity(view.context, intent, null)
    }

    fun changeAmountOfGuestsinBill() {

        hideKeyboard()

        if (amount_of_guests_input_text_field.text.isEmpty()) {
            Toast.makeText(applicationContext, "Укажите значение большее 0", Toast.LENGTH_SHORT).show()
            return
        }

        val amount_of_guests = amount_of_guests_input_text_field.text.toString().toInt()
        if (amount_of_guests <= 0) {
            Toast.makeText(applicationContext, "Укажите значение большее 0", Toast.LENGTH_SHORT).show()
            return
        }
        amount_of_guests_container.visibility = View.GONE

//        val progressDialog = ProgressDialog(this)
//        progressDialog.setMessage("Обновляю счет...")
//        progressDialog.setCancelable(false)
//        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {

            try {

                val client = HttpClient(Android) {
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                }

                client.use {
                    bill = it.get(change_guests_amount_url) {
                        parameter("bill_id", bill?.id.toString())
                        parameter("amount_of_guests", amount_of_guests.toString())
                    }
                }

            } catch (cause: Throwable) {

                launch(Dispatchers.Main) {
//                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "Связь пропала, попробуйте позже...", Toast.LENGTH_LONG).show()
                    Log.w("TEST", cause.localizedMessage)
                }

                return@launch
            }

            launch(Dispatchers.Main) {
//                progressDialog.dismiss()
                updateUIForBill()
            }

        }
    }

    fun printOrdersAndExit() {

        if (bill?.isNeededMissed() == true) {
            Toast.makeText(applicationContext, "Не выбраны обязательные параметры", Toast.LENGTH_LONG).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Печатаю заказы...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            try {

                val client = HttpClient(Android) {
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                }

                client.use {
                    bill = it.get(print_orders_url) {
                        parameter("bill_id", bill?.id.toString())
                        parameter("hardware_from_id", MainActivity.currentHardwareID)
                    }

                }

            } catch (cause: Throwable) {
                when(cause) {
                    is ConnectException -> {
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(applicationContext, "Пропала связь, пробую еще...", Toast.LENGTH_SHORT).show()
                            Log.w("TEST", cause)
                            Handler().postDelayed(
                                {
                                    printOrdersAndExit()
                                },
                                3000
                            )
                        }
                        return@launch
                    }
                    else -> {
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(applicationContext, "Что-то пошло не так...", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
            }

            launch(Dispatchers.Main) {
                progressDialog.dismiss()
                fetchBillAndUpdateUI()
            }

        }


    }

    fun printBillAndExit() {
        if (MainActivity.currentUser?.isActionAllowed(107) == false) {
            Toast.makeText(applicationContext, "Печать счета для Вас запрещена", Toast.LENGTH_SHORT).show()
            return
        }

        if (bill?.isPrinted == true && MainActivity.currentUser?.isActionAllowed(110) == false) {
            Toast.makeText(applicationContext, "Отмена печати для Вас запрещена", Toast.LENGTH_SHORT).show()
            return
        }

        if (bill?.isNeededMissed() == true) {
            Toast.makeText(applicationContext, "Не выбраны обязательные параметры", Toast.LENGTH_LONG).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Печатаю счет...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {

            try {
                val client = HttpClient(Android) {
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                }

                client.use {
                    bill = it.get(print_positions_url) {
                        parameter("bill_id", bill?.id.toString())
                        parameter("hardware_from_id", MainActivity.currentHardwareID)
                        parameter("personal_id", MainActivity.currentUser?.id.toString())
                    }

                    launch(Dispatchers.Main) {
                        fetchBillAndUpdateUI()
                    }
                }

            } catch (cause: Throwable) {
                when(cause) {
                    is ConnectException -> {
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(applicationContext, "Пропала связь, пробую еще...", Toast.LENGTH_SHORT).show()
                            Log.w("TEST", cause)
                            Handler().postDelayed(
                                {
                                    printBillAndExit()
                                },
                                3000
                            )
                        }
                        return@launch
                    }
                    else -> {
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(applicationContext, "Что-то пошло не так...", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
            }

            launch(Dispatchers.Main) {
                progressDialog.dismiss()
                updateUIForBill()
            }

        }
    }

    fun deleteSelectedOrder() {
        if (MainActivity.currentUser?.isActionAllowed(103) == false && updatingOrderEntity?.isPrinted == true) {
            Toast.makeText(applicationContext, "Удаление распечатанной позиции для Вас запрещено", Toast.LENGTH_SHORT).show()
            return
        }

        if (updatingOrderEntity != null) {
            val new_amount = calculateOrderQNTincludingCancelations(updatingOrderEntity!!, bill)
            menu_entity_amount_in_menu_entity_container.text = (updatingOrderEntity!!.qnt - new_amount).toString()
            save_menu_entity_button_in_bill_view.callOnClick()
            menu_entity_container_in_bill_view.visibility = View.GONE
        }


//        GlobalScope.launch(Dispatchers.IO) {
//
//            try {
//
//                val client = HttpClient(Android) {
//                    install(JsonFeature) {
//                        serializer = GsonSerializer()
//                    }
//                }
//
//                client.use {
//                    bill = it.get(delete_position_url) {
//                        parameter("order_id", updatingOrderId.toString())
//                    }
//                }
//
//            } catch (cause: Throwable) {
//
//                launch(Dispatchers.Main) {
//                    Toast.makeText(applicationContext, "Пропала связь, пробую еще...", Toast.LENGTH_SHORT).show()
//
//                    Handler().postDelayed(
//                        {
//                            deleteSelectedOrder()
//                        },
//                        3000 // value in milliseconds
//                    )
//                }
//
//                return@launch
//            }
//
//            launch(Dispatchers.Main) {
//                updateUIForBill()
//            }
//
//        }
    }

    suspend fun finishEditing() {

        val client = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        client.use {
            val url = finish_editing_bill_url
            Log.w("TEST", bill?.id.toString())
            Log.w("TEST", finish_editing_bill_url)
            bill= it.get(url) {
                parameter("bill_id", bill?.id)
            }

        }

    }

    fun saveBillFinishState() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                finishEditing()
            } catch (cause: Throwable) {
                Log.w("TEST", cause.toString())
                when(cause) {
                    is ConnectException -> {
                        launch(Dispatchers.Main) {
                            loader_in_menu_view.visibility = View.GONE
                            Toast.makeText(applicationContext, "Пропала связь, пробую еще...", Toast.LENGTH_SHORT).show()
                            Log.w("TEST", cause)
                            Handler().postDelayed(
                                {
                                    saveBillFinishState()
                                },
                                3000
                            )
                        }
                        return@launch
                    }
                    is BadResponseStatusException -> {
                        launch(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Вероятно, счет пустой и был удален, создайте новый счет", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }
                    else -> {
                        launch(Dispatchers.Main) {
                            loader_in_menu_view.visibility = View.GONE
                            Log.w("TEST", cause.localizedMessage)
                            Toast.makeText(applicationContext, "Счет пустой и был удален или проблема со связью", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        saveBillFinishState()

    }

    override fun inactivityAction() {
        saveBillFinishState()
        super.inactivityAction()
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
//        viewHolder.itemView.order_count_in_bill_orders_list.text = count.toString()
        viewHolder.itemView.order_name_in_bill_orders_list.text = order.menu_item
        viewHolder.itemView.order_price_in_bill_orders_list.text = order.price_discount_text
        if (order.price != order.price_discount_text) {
            viewHolder.itemView.order_price_base_price.text = "Базовая цена: ${order.price}"
            viewHolder.itemView.order_price_discount_percentage.text = "Скидка: ${order.price_discount_percentage} %"
        } else {
            viewHolder.itemView.order_price_base_price.text = ""
            viewHolder.itemView.order_price_discount_percentage.text = ""
        }

        viewHolder.itemView.order_qnt_in_bill_orders_list.text = order.qnt.toString()
        viewHolder.itemView.order_time_in_order_inline_view.text = order.input_time.toString()

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

        if (order.isNeededMissed) {
            viewHolder.itemView.needed_addition_missed.visibility = View.VISIBLE
        } else {
            viewHolder.itemView.needed_addition_missed.visibility = View.GONE
        }

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

    var amountAlreadyOrdered: Double = 0.toDouble()
    var newAmountAlreadyOrdered: Double = 0.toDouble()

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.menu_entity_title_in_bill_view.text = menuEntity.name_button
        viewHolder.itemView.menu_entity_price_in_bill_view.text = menuEntity.actualPriceInHall.toString()

        bill?.orders?.forEach { order ->
            if (order.menu_item_id == menuEntity.id) {
                amountAlreadyOrdered += order.qnt
            }
        }

        if (newAmountAlreadyOrdered > 0) {
            amountAlreadyOrdered = newAmountAlreadyOrdered
        }

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