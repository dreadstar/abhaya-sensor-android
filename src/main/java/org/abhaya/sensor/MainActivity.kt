package com.ustadmobile.meshrabiya.sensor

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        // Placeholder: you can load a local HTML bundle (built from the TSX prototype) or replace with native UI
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("about:blank")
    }
}
