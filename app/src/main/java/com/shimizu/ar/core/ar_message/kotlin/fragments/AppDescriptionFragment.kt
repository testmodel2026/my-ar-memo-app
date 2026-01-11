package com.shimizu.ar.core.ar_message.kotlin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.shimizu.ar.core.ar_message.kotlin.ar_message.R

class AppDescriptionPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 4 // ページ数

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppDescriptionFragment1()
            1 -> AppDescriptionFragment2()
            2 -> AppDescriptionFragment3()
            3 -> AppDescriptionFragment4()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}

class AppDescriptionFragment1 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_description_1, container, false)
    }
}

class AppDescriptionFragment2 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_description_2, container, false)
    }
}

class AppDescriptionFragment3 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_description_3, container, false)
    }
}

class AppDescriptionFragment4 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_description_4, container, false)
    }
}