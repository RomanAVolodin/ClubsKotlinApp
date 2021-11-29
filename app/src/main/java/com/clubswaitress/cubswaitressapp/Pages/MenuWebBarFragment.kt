package com.clubswaitress.cubswaitressapp.Pages

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import com.clubswaitress.cubswaitressapp.MainActivity
import com.clubswaitress.cubswaitressapp.R
import kotlinx.android.synthetic.main.activity_menu_club.*
import kotlinx.android.synthetic.main.fragment_menu_web_bar.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MenuWebBarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MenuWebBarFragment : Fragment() {
    companion object {
        fun newInstance(): MenuWebBarFragment = MenuWebBarFragment()
        val TAG = "MenuWebBarFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_menu_web_bar, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //val url = "${MainActivity.serverBaseUrl}/webmenu/bar"
        val url = "${MainActivity.serverBaseUrl}/webmenu/bar"
        menu_bar_web_page.settings.setAppCacheEnabled(false)
        menu_bar_web_page.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        menu_bar_web_page.loadUrl(url)

    }
}