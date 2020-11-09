package com.clubswaitress.cubswaitressapp.Pages

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.R
import kotlinx.android.synthetic.main.fragment_menu_web.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MenuWebFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MenuWebFragment : Fragment() {

    companion object {
        fun newInstance(): MenuWebFragment = MenuWebFragment()
        val TAG = "MenuWebFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_menu_web, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val url = "${MainActivity.serverBaseUrl}/webmenus/${MainActivity.club_menu_url}/qr-menu.html"
        Log.w("TEST", "${MainActivity.serverBaseUrl}/webmenus/${MainActivity.club_menu_url}/qr-menu.html")
        menu_web_page.loadUrl(url)

    }


}