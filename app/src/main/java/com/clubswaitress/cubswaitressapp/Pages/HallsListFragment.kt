package com.clubswaitress.cubswaitressapp.Pages

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.Models.Hall

import com.clubswaitress.cubswaitressapp.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.fragment_halls_list.*
import kotlinx.android.synthetic.main.hall_in_list_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class HallsListFragment : androidx.fragment.app.Fragment() {

    companion object {
        fun newInstance(): HallsListFragment = HallsListFragment()
        val TAG = "HallsListFragment"
    }

    val fetch_url = "${MainActivity.serverBaseUrl}/halls"

    var halls:List<Hall> = emptyList()

    val adaptor = GroupAdapter<ViewHolder>()

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

        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Получаю список залов...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        GlobalScope.launch(Dispatchers.IO) {

            try {

                fetchHalls()

            } catch (cause: Throwable) {
                println("Error: $cause")
                println("Error: $fetch_url")
                println("user_id ${MainActivity.currentUser?.id}")

                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
//                    Toast.makeText(activity, "Ошибка при получении списка залов", Toast.LENGTH_LONG).show()
                }

                return@launch
            }

            launch(Dispatchers.Main) {
                for (hall in halls) {
                    adaptor.add(HallItem(hall))
                }

                progressDialog.dismiss()

                adaptor.setOnItemClickListener { item, view ->
                    val userItem = item as HallItem

                    val intent = Intent(view.context, HallwithTablesActivity::class.java)

                    intent.putExtra(HallwithTablesActivity.HALL_KEY, userItem.hall)

                    startActivity(intent)

                }

            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_halls_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        halls_list_recycler_view.adapter = adaptor
        halls_list_recycler_view.itemAnimator = SlideInLeftAnimator()

        fetchHallsAndUpdateUI()



    }

}


class HallItem(val hall: Hall): Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.hall_in_list_view_hall_name.text = hall.name

    }

    override fun getLayout(): Int {
        return R.layout.hall_in_list_view
    }
}