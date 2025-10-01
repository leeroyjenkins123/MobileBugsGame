package com.example.mobilebugsgame

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment

class RulesFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Используем layout файл вместо создания TextView программно
        val view = inflater.inflate(R.layout.fragment_rules, container, false)
        val textRules = view.findViewById<TextView>(R.id.textRules)

        val htmlText = getString(R.string.rules_html)
        textRules.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(htmlText)
        }

        return view
    }
}
