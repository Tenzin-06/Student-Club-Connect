package com.studentclubconnect.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.studentclubconnect.R

class EventsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_placeholder, container, false)
        view.findViewById<android.widget.TextView>(R.id.tvPlaceholder).text = "Events Screen"
        return view
    }
}